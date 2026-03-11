package com.yourname.alphaai.skills

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yourname.alphaai.core.Skill
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationSkill(private val context: Context) : Skill {
    override val id = "location.get"
    override val name = "获取位置"
    override val description = "获取当前设备的经纬度"

    override suspend fun execute(params: Map<String, Any>): Result<Map<String, Any>> {
        return try {
            // 1. 检查位置权限
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("缺少位置权限"))
            }

            // 2. 获取位置
            val location = getCurrentLocation()

            Result.success(
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getCurrentLocation(): android.location.Location = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        // 请求当前位置（高精度）
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(Exception("无法获取位置"))
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
}
