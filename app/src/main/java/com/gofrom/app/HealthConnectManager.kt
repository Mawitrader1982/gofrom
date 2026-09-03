package com.gofrom.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class HealthDataState { AVAILABLE, NO_DATA, PERMISSION_REQUIRED, ERROR }

data class HealthMetric<T>(
    val value: T? = null,
    val state: HealthDataState = HealthDataState.NO_DATA,
    val measuredAt: Instant? = null,
    val error: String? = null
)

data class HealthSnapshot(
    val stepsToday: HealthMetric<Long> = HealthMetric(),
    val latestHeartRate: HealthMetric<Long> = HealthMetric(),
    val lastNightSleepMinutes: HealthMetric<Long> = HealthMetric(),
    val activeCaloriesToday: HealthMetric<Int> = HealthMetric(),
    val latestWeightKg: HealthMetric<Double> = HealthMetric(),
    val lastSynced: Instant? = null,
    val error: String? = null
)

data class HealthTimeRange(val start: Instant, val end: Instant)

internal object HealthTimeRanges {
    fun today(now: Instant, zoneId: ZoneId): HealthTimeRange {
        val start = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
        return HealthTimeRange(start, now)
    }

    fun lastNight(now: Instant, zoneId: ZoneId): HealthTimeRange {
        val localNow = now.atZone(zoneId)
        val today = localNow.toLocalDate()
        val end = if (localNow.toLocalTime().isBefore(LocalTime.NOON)) {
            now
        } else {
            today.atTime(LocalTime.NOON).atZone(zoneId).toInstant()
        }
        val start = today.minusDays(1).atTime(18, 0).atZone(zoneId).toInstant()
        return HealthTimeRange(start, end)
    }
}

class HealthConnectManager(private val context: Context) {
    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val heartPermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val caloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val weightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    val permissions = setOf(stepsPermission, heartPermission, sleepPermission, caloriesPermission, weightPermission)

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    fun hasAnyMetricPermission(granted: Set<String>): Boolean = permissions.any(granted::contains)
    fun hasAllMetricPermissions(granted: Set<String>): Boolean = permissions.all(granted::contains)

    private fun client() = HealthConnectClient.getOrCreate(context)

    suspend fun grantedPermissions(): Set<String> =
        if (availability() == HealthConnectClient.SDK_AVAILABLE) client().permissionController.getGrantedPermissions() else emptySet()

    suspend fun sync(
        granted: Set<String>,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HealthSnapshot {
        val hc = runCatching { client() }.getOrElse {
            return HealthSnapshot(lastSynced = now, error = it.message ?: "Synchronisatie mislukt")
        }
        val today = HealthTimeRanges.today(now, zoneId)
        val lastNight = HealthTimeRanges.lastNight(now, zoneId)
        val recentHeartRate = HealthTimeRange(now.minus(1, ChronoUnit.DAYS), now)
        val recentWeight = HealthTimeRange(now.minus(30, ChronoUnit.DAYS), now)

        val steps = readMetric(stepsPermission in granted) {
            hc.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(today.start, today.end)
                )
            )[StepsRecord.COUNT_TOTAL]?.let { TimedValue(it) }
        }

        val sleep = readMetric(sleepPermission in granted) {
            hc.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(lastNight.start, lastNight.end)
                )
            )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()?.let { TimedValue(it) }
        }

        val activeCalories = readMetric(caloriesPermission in granted) {
            hc.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(today.start, today.end)
                )
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories
                ?.roundToInt()
                ?.let { TimedValue(it) }
        }

        val latestWeight = readMetric(weightPermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(recentWeight.start, recentWeight.end),
                    ascendingOrder = false,
                    pageSize = 1
                )
            ).records.firstOrNull()?.let { TimedValue(it.weight.inKilograms, it.time) }
        }

        val latestHeartRate = readMetric(heartPermission in granted) {
            hc.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(recentHeartRate.start, recentHeartRate.end),
                    ascendingOrder = false,
                    pageSize = 1000
                )
            ).records
                .asSequence()
                .flatMap { it.samples.asSequence() }
                .maxByOrNull { it.time }
                ?.let { TimedValue(it.beatsPerMinute, it.time) }
        }

        return HealthSnapshot(
            stepsToday = steps,
            latestHeartRate = latestHeartRate,
            lastNightSleepMinutes = sleep,
            activeCaloriesToday = activeCalories,
            latestWeightKg = latestWeight,
            lastSynced = now
        )
    }

    private suspend fun <T : Any> readMetric(
        hasPermission: Boolean,
        read: suspend () -> TimedValue<T>?
    ): HealthMetric<T> {
        if (!hasPermission) return HealthMetric(state = HealthDataState.PERMISSION_REQUIRED)
        return runCatching { read() }.fold(
            onSuccess = { result ->
                if (result == null) HealthMetric(state = HealthDataState.NO_DATA)
                else HealthMetric(value = result.value, state = HealthDataState.AVAILABLE, measuredAt = result.measuredAt)
            },
            onFailure = { HealthMetric(state = HealthDataState.ERROR, error = it.message ?: "Gegevens konden niet worden gelezen") }
        )
    }

    private data class TimedValue<T>(val value: T, val measuredAt: Instant? = null)
}
