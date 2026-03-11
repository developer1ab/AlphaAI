package com.yourname.alphaai.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.Locale

class AutoClickService : AccessibilityService() {

    data class TextClickResult(
        val success: Boolean,
        val matchedCount: Int,
        val clickableCandidates: Int,
        val clickedLabel: String?
    )

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Prototype keeps event handling minimal.
    }

    override fun onInterrupt() {
        // No-op for prototype.
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun activePackageName(): String? {
        return rootInActiveWindow?.packageName?.toString()
    }

    fun clickByText(text: String): TextClickResult {
        val root = rootInActiveWindow ?: return TextClickResult(false, 0, 0, null)
        val query = normalizeText(text)
        if (query.isBlank()) {
            return TextClickResult(false, 0, 0, null)
        }

        val candidates = mutableListOf<AccessibilityNodeInfo>()

        val directMatches = root.findAccessibilityNodeInfosByText(text)
        for (node in directMatches) {
            addIfMissing(candidates, node)
        }

        collectMatchingNodes(root, query, candidates)

        var clickableCandidates = 0
        for (node in candidates.sortedBy { scoreMatch(it, query) }) {
            val clickableNode = findClickableNode(node)
            if (clickableNode != null) {
                clickableCandidates += 1
                if (clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    val label = readableNodeLabel(node)
                    return TextClickResult(true, candidates.size, clickableCandidates, label)
                }
            }
        }

        return TextClickResult(false, candidates.size, clickableCandidates, null)
    }

    suspend fun clickByCoordinates(x: Float, y: Float): Boolean {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply { moveTo(x, y) },
                    0,
                    100
                )
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            val ok = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
                null
            )
            if (!ok && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    private fun findClickableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun collectMatchingNodes(
        node: AccessibilityNodeInfo?,
        query: String,
        out: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        val normalizedLabel = normalizeText(readableNodeLabel(node))
        if (normalizedLabel.contains(query)) {
            addIfMissing(out, node)
        }

        for (i in 0 until node.childCount) {
            collectMatchingNodes(node.getChild(i), query, out)
        }
    }

    private fun scoreMatch(node: AccessibilityNodeInfo, query: String): Int {
        val label = normalizeText(readableNodeLabel(node))
        return when {
            label == query -> 0
            label.startsWith(query) -> 1
            label.contains(query) -> 2
            else -> 3
        }
    }

    private fun readableNodeLabel(node: AccessibilityNodeInfo): String {
        val text = node.text?.toString().orEmpty().trim()
        if (text.isNotBlank()) return text
        return node.contentDescription?.toString().orEmpty().trim()
    }

    private fun addIfMissing(list: MutableList<AccessibilityNodeInfo>, node: AccessibilityNodeInfo) {
        if (list.none { it === node }) {
            list.add(node)
        }
    }

    private fun normalizeText(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return value
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        @Volatile
        var instance: AutoClickService? = null
            private set
    }
}
