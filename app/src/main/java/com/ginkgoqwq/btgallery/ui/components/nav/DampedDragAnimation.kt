/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * 本文件移植自 KernelSU。
 *
 *   原项目：KernelSU  https://github.com/tiann/KernelSU
 *   原文件：me.weishu.kernelsu.ui.component.miuix.animation.DampedDragAnimation
 *   原作者：weishu 及 KernelSU 贡献者
 *   原许可：GNU General Public License v3.0
 *
 * 本项目（BTGallery）同样以 GNU GPL v3.0 发布，详见根目录 LICENSE。
 *
 * 相对原实现的改动：
 *   把 KernelSU 内部的 `inspectDragGestures` 替换为官方 `detectDragGestures`。
 */

package com.ginkgoqwq.btgallery.ui.components.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * 可拖拽的阻尼动画容器。
 *
 * 维护一个连续的小数位置 [value]（单位：tab 序号），支持：
 * - **拖拽**：手指按住横向拖动时直接改 [value]
 * - **吸附**：松手后吸附到最近整数位，并回调 [onDragStopped]
 * - **按压反馈**：按下时 [pressProgress] / [scaleX] / [scaleY] 变化
 * - **速度**：拖动中记录速度，供上层做倾斜等效果
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit = {},
    val onDragStopped: DampedDragAnimation.() -> Unit = {},
    val onDragCancelled: DampedDragAnimation.() -> Unit = onDragStopped,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private var pressJob: Job? = null
    private var releaseJob: Job? = null
    private val velocityTracker = VelocityTracker()
    private val startMark = TimeSource.Monotonic.markNow()

    /** 当前连续位置（小数）。 */
    val value: Float get() = valueAnimation.value

    /** 目标位置。 */
    val targetValue: Float get() = valueAnimation.targetValue

    /** 按压进度 0f..1f。 */
    val pressProgress: Float get() = pressProgressAnimation.value

    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value

    /** 归一化速度。 */
    val velocity: Float get() = velocityAnimation.value

    /** 挂到底栏上以接管横向拖拽手势。 */
    val modifier: Modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { down ->
                onDragStarted(down)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragCancelled()
                release()
            }
        ) { change, dragAmount ->
            val position = change.position
            val previousPosition = change.previousPosition
            // 只有起点与终点都在可拖拽区域内才响应，避免从外部拖进来时误触发
            if (canDrag(position) && canDrag(previousPosition)) {
                onDrag(size, dragAmount)
            }
        }
    }

    /** 按下。 */
    fun press() {
        releaseJob?.cancel()
        pressJob?.cancel()
        velocityTracker.resetTracking()
        pressJob = animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    /** 松手：等位置动画基本到位后，再收回按压反馈。 */
    fun release() {
        releaseJob?.cancel()
        releaseJob = animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .first { abs(it - valueAnimation.targetValue) < threshold }
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    /** 立即把目标值改到 [value]（拖动过程中用）。 */
    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange)
        animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            valueAnimation.animateTo(target, valueAnimationSpec) {
                updateVelocity()
            }
        }
    }

    /** 动画到 [value]（点击 tab、或外部同步时用）。 */
    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            startMark.elapsedNow().inWholeMilliseconds,
            Offset(value, 0f)
        )
        val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
        val targetVelocity = velocityTracker.calculateVelocity().x / span
        animationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            velocityAnimation.snapTo(targetVelocity)
        }
    }
}
