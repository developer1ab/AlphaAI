package com.yourname.alphaai.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

class AppResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    private val commonAppMap = mapOf(
        "wechat" to "com.tencent.mm",
        "alipay" to "com.eg.android.AlipayGphone",
        "taobao" to "com.taobao.taobao",
        "jd" to "com.jingdong.app.mall",
        "meituan" to "com.sankuai.meituan",
        "dianping" to "com.dianping.v1",
        "amap" to "com.autonavi.minimap",
        "baidu map" to "com.baidu.BaiduMap",
        "didi" to "com.sdu.didi.psnger",
        "weibo" to "com.sina.weibo",
        "qq" to "com.tencent.mobileqq",
        "netease music" to "com.netease.cloudmusic",
        "douyin" to "com.ss.android.ugc.aweme",
        "bilibili" to "tv.danmaku.bili",
        "xiaohongshu" to "com.xingin.xhs",
        "zhihu" to "com.zhihu.android",
        "chrome" to "com.android.chrome",
        "google chrome" to "com.android.chrome",
        "settings" to "com.android.settings",
        "system settings" to "com.android.settings",
        "play store" to "com.android.vending",
        "google play" to "com.android.vending"
    )

    fun getAllLaunchableApps(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(intent, 0)
    }

    fun findPackageByAppName(query: String): String? {
        return resolvePackageName(query)
    }

    fun resolvePackageName(appName: String): String? {
        val apps = getAllLaunchableApps()
        val normalizedQuery = appName.trim()
        if (normalizedQuery.isEmpty()) return null

        commonAppMap[normalizedQuery.lowercase()]?.let { return it }

        apps.forEach { info ->
            val label = info.loadLabel(packageManager).toString()
            if (label.equals(normalizedQuery, ignoreCase = true)) {
                return info.activityInfo.packageName
            }
        }

        val lowerQuery = normalizedQuery.lowercase()
        apps.forEach { info ->
            val label = info.loadLabel(packageManager).toString()
            if (label.lowercase().contains(lowerQuery)) {
                return info.activityInfo.packageName
            }
        }

        // Fallback: match by package name when app label is localized/unexpected.
        apps.forEach { info ->
            val packageName = info.activityInfo.packageName
            if (packageName.equals(normalizedQuery, ignoreCase = true) ||
                packageName.lowercase().contains(lowerQuery)
            ) {
                return packageName
            }
        }

        return null
    }

    fun getAllInstalledApps(): List<AppInfo> {
        val launchable = getAllLaunchableApps()
        return launchable.mapNotNull { info ->
            val name = info.loadLabel(packageManager).toString()
            val packageName = info.activityInfo.packageName
            if (name.isBlank()) null else AppInfo(name, packageName)
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }

    data class AppInfo(val name: String, val packageName: String)

    fun getAppName(packageName: String): String? {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
