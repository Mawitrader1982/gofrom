package com.gofrom.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_HISTORY
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.YearMonth
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

data class HealthMonthPoint(
    val month: YearMonth,
    val steps: Long? = null,
    val activeCalories: Int? = null,
    val sleepMinutes: Long? = null,
    val averageHeartRate: Long? = null,
    val averageWeightKg: Double? = null
)

data class HealthYearSnapshot(
    val months: List<HealthMonthPoint> = emptyList(),
    val totalSteps: HealthMetric<Long> = HealthMetric(),
    val totalActiveCalories: HealthMetric<Int> = HealthMetric(),
    val recordedSleepMinutes: HealthMetric<Long> = HealthMetric(),
    val averageHeartRate: HealthMetric<Long> = HealthMetric(),
    val averageWeightKg: HealthMetric<Double> = HealthMetric(),
    val historySupported: Boolean = true,
    val historyAccessGranted: Boolean = false,
    val lastSynced: Instant? = null,
    val error: String? = null
)

data class HealthTimeRange(val start: Instant, val end: Instant)
data class HealthLocalTimeRange(val start: LocalDateTime, val end: LocalDateTime)

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

    fun lastTwelveCalendarMonths(now: Instant, zoneId: ZoneId): HealthLocalTimeRange {
        val localNow = now.atZone(zoneId).toLocalDateTime()
        val start = localNow.toLocalDate().withDayOfMonth(1).minusMonths(11).atStartOfDay()
        return HealthLocalTimeRange(start, localNow)
    }
}

class HealthConnectManager(private val context: Context) {
    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val heartPermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val caloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val weightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val metricPermissions = setOf(stepsPermission, heartPermission, sleepPermission, caloriesPermission, weightPermission)
    val permissions: Set<String>
        get() = metricPermissions + if (supportsHistoryRead()) setOf(PERMISSION_READ_HEALTH_DATA_HISTORY) else emptySet()

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    fun hasAnyMetricPermission(granted: Set<String>): Boolean = metricPermissions.any(granted::contains)
    fun hasAllMetricPermissions(granted: Set<String>): Boolean = permissions.all(granted::contains)
    fun hasHistoryPermission(granted: Set<String>): Boolean = PERMISSION_READ_HEALTH_DATA_HISTORY in granted
    fun supportsHistoryRead(): Boolean = availability() == HealthConnectClient.SDK_AVAILABLE && runCatching {
        client().features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }.getOrDefault(false)

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

    suspend fun syncYear(
        granted: Set<String>,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HealthYearSnapshot {
        val range = HealthTimeRanges.lastTwelveCalendarMonths(now, zoneId)
        val startMonth = YearMonth.from(range.start)
        val months = (0L..11L).map(startMonth::plusMonths)
        val historySupported = supportsHistoryRead()
        val historyGranted = hasHistoryPermission(granted)
        if (!historySupported || !historyGranted) {
            return HealthYearSnapshot(
                months = months.map { HealthMonthPoint(it) },
                totalSteps = unavailableYearMetric(stepsPermission in granted, historySupported),
                totalActiveCalories = unavailableYearMetric(caloriesPermission in granted, historySupported),
                recordedSleepMinutes = unavailableYearMetric(sleepPermission in granted, historySupported),
                averageHeartRate = unavailableYearMetric(heartPermission in granted, historySupported),
                averageWeightKg = unavailableYearMetric(weightPermission in granted, historySupported),
                historySupported = historySupported,
                historyAccessGranted = false,
                lastSynced = now
            )
        }

        val hc = runCatching { client() }.getOrElse {
            return HealthYearSnapshot(
                months = months.map { HealthMonthPoint(it) },
                historySupported = true,
                historyAccessGranted = true,
                lastSynced = now,
                error = it.message ?: "Year history could not be synchronized"
            )
        }

        val steps = readYearValues(stepsPermission in granted) {
            hc.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            ).mapNotNull { bucket ->
                bucket.result[StepsRecord.COUNT_TOTAL]?.let { YearMonth.from(bucket.startTime) to it }
            }.toMap()
        }

        val calories = readYearValues(caloriesPermission in granted) {
            hc.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            ).mapNotNull { bucket ->
                bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    ?.inKilocalories
                    ?.roundToInt()
                    ?.let { YearMonth.from(bucket.startTime) to it }
            }.toMap()
        }

        val sleep = readYearValues(sleepPermission in granted) {
            hc.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            ).mapNotNull { bucket ->
                bucket.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]
                    ?.toMinutes()
                    ?.let { YearMonth.from(bucket.startTime) to it }
            }.toMap()
        }

        val heartRate = readYearValues(heartPermission in granted) {
            hc.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            ).mapNotNull { bucket ->
                bucket.result[HeartRateRecord.BPM_AVG]?.let { YearMonth.from(bucket.startTime) to it }
            }.toMap()
        }

        val weight = readYearValues(weightPermission in granted) {
            hc.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(WeightRecord.WEIGHT_AVG),
                    timeRangeFilter = TimeRangeFilter.between(range.start, range.end),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            ).mapNotNull { bucket ->
                bucket.result[WeightRecord.WEIGHT_AVG]
                    ?.inKilograms
                    ?.let { YearMonth.from(bucket.startTime) to it }
            }.toMap()
        }

        return HealthYearSnapshot(
            months = months.map { month ->
                HealthMonthPoint(
                    month = month,
                    steps = steps.values[month],
                    activeCalories = calories.values[month],
                    sleepMinutes = sleep.values[month],
                    averageHeartRate = heartRate.values[month],
                    averageWeightKg = weight.values[month]
                )
            },
            totalSteps = steps.summarize { values -> values.sum() },
            totalActiveCalories = calories.summarize { values -> values.sum() },
            recordedSleepMinutes = sleep.summarize { values -> values.sum() },
            averageHeartRate = heartRate.summarize { values -> values.average().roundToInt().toLong() },
            averageWeightKg = weight.summarize { values -> values.average() },
            historySupported = true,
            historyAccessGranted = true,
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

    private fun <T> unavailableYearMetric(hasMetricPermission: Boolean, historySupported: Boolean): HealthMetric<T> =
        if (!hasMetricPermission) HealthMetric(state = HealthDataState.PERMISSION_REQUIRED)
        else if (!historySupported) HealthMetric(state = HealthDataState.ERROR, error = "Year history is not supported on this device")
        else HealthMetric(state = HealthDataState.PERMISSION_REQUIRED, error = "Past data access is required")

    private suspend fun <T : Any> readYearValues(
        hasPermission: Boolean,
        read: suspend () -> Map<YearMonth, T>
    ): YearValues<T> {
        if (!hasPermission) return YearValues(state = HealthDataState.PERMISSION_REQUIRED)
        return runCatching { read() }.fold(
            onSuccess = { values ->
                if (values.isEmpty()) YearValues(state = HealthDataState.NO_DATA)
                else YearValues(values = values, state = HealthDataState.AVAILABLE)
            },
            onFailure = { YearValues(state = HealthDataState.ERROR, error = it.message ?: "Year data could not be read") }
        )
    }

    private fun <T : Any, R : Any> YearValues<T>.summarize(summary: (List<T>) -> R): HealthMetric<R> = when (state) {
        HealthDataState.AVAILABLE -> HealthMetric(value = summary(values.values.toList()), state = HealthDataState.AVAILABLE)
        HealthDataState.NO_DATA -> HealthMetric(state = HealthDataState.NO_DATA)
        HealthDataState.PERMISSION_REQUIRED -> HealthMetric(state = HealthDataState.PERMISSION_REQUIRED)
        HealthDataState.ERROR -> HealthMetric(state = HealthDataState.ERROR, error = error)
    }

    private data class TimedValue<T>(val value: T, val measuredAt: Instant? = null)
    private data class YearValues<T>(
        val values: Map<YearMonth, T> = emptyMap(),
        val state: HealthDataState,
        val error: String? = null
    )
}
