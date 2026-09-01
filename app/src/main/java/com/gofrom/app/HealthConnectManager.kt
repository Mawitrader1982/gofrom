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
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

data class HealthSnapshot(
    val steps: Long = 0,
    val heartRate: Long? = null,
    val sleepMinutes: Long = 0,
    val calories: Int = 0,
    val weightKg: Double? = null,
    val lastSynced: Instant? = null,
    val error: String? = null
)

class HealthConnectManager(private val context: Context) {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    private fun client() = HealthConnectClient.getOrCreate(context)

    suspend fun grantedPermissions(): Set<String> =
        if (availability() == HealthConnectClient.SDK_AVAILABLE) client().permissionController.getGrantedPermissions() else emptySet()

    suspend fun sync(): HealthSnapshot = runCatching {
        val end = Instant.now()
        val start = end.minus(7, ChronoUnit.DAYS)
        val range = TimeRangeFilter.between(start, end)
        val hc = client()
        val steps = hc.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records.sumOf { it.count }
        val sleep = hc.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range)).records.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
        val calories = hc.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, range)).records.sumOf { it.energy.inKilocalories }.toInt()
        val weight = hc.readRecords(ReadRecordsRequest(WeightRecord::class, range, ascendingOrder = false, pageSize = 1)).records.firstOrNull()?.weight?.inKilograms
        val heart = hc.readRecords(ReadRecordsRequest(HeartRateRecord::class, range, ascendingOrder = false, pageSize = 1)).records.firstOrNull()?.samples?.lastOrNull()?.beatsPerMinute
        HealthSnapshot(steps, heart, sleep, calories, weight, end)
    }.getOrElse { HealthSnapshot(error = it.message ?: "Synchronisatie mislukt") }
}
