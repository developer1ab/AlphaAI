package com.yourname.alphaai.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

class AppResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    private val commonAppMap = mapOf(
        "微信" to "com.tencent.mm",
        "wechat" to "com.tencent.mm",
        "支付宝" to "com.eg.android.AlipayGphone",
        "淘宝" to "com.taobao.taobao",
        "京东" to "com.jingdong.app.mall",
        "美团" to "com.sankuai.meituan",
        "大众点评" to "com.dianping.v1",
        "高德地图" to "com.autonavi.minimap",
        "百度地图" to "com.baidu.BaiduMap",
        "滴滴" to "com.sdu.didi.psnger",
        "微博" to "com.sina.weibo",
        "qq" to "com.tencent.mobileqq",
        "网易云音乐" to "com.netease.cloudmusic",
        "抖音" to "com.ss.android.ugc.aweme",
        "哔哩哔哩" to "tv.danmaku.bili",
        "bilibili" to "tv.danmaku.bili",
        "小红书" to "com.xingin.xhs",
        "知乎" to "com.zhihu.android",
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
