package com.haoze.keynote.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 推拉式侧边栏布局（Push Drawer）。
 *
 * 替代 Material 3 的 [ModalNavigationDrawer]（覆盖式 Overlay）。
 *
 * **推出屏幕外（Push off-screen）效果：**
 * - 主内容区以 [fillMaxSize] 铺满全屏，保持完整屏幕宽度布局
 * - 侧边栏展开时，主内容区整体向右平移 [drawerWidth]，右侧超出部分被 [clipToBounds] 裁切
 * - 内容本身的布局不变（文本不换行、元素不位移），仅可视区域偏移
 *
 * **手势方案（实时跟随手指 + 松手吸附）：**
 * - 拖拽中 [Animatable.snapTo] 让偏移量实时跟随手指位置
 * - 记录手势起始偏移量 [startOffset]，左右拖拽都能正确追踪
 * - 松手时根据当前位置（过半则全开/否则全关），直接在 gesture handler 中用 [animateTo] 吸附到终点
 * - [drawerState.snapTo] 在动画完成后才调用，不依赖 [LaunchedEffect] 触发，杜绝动画丢失
 * - 程序化打开/关闭（汉堡按钮/导航项）仍由 [LaunchedEffect] 驱动同一 [Animatable]
 * - 不拦截子节点已消费的事件
 *
 * @param dragSensitivity 拖拽灵敏度倍率，>1 则手指移动更小的距离就能推动侧边栏更大行程。默认 3.0f
 */
@Composable
fun PushDrawerLayout(
    drawerState: DrawerState,
    gestureEnabled: Boolean = true,
    drawerWidth: Dp = 300.dp,
    drawerShape: Shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
    dragSensitivity: Float = 2.0f,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    // 单一偏移量来源（单位 px），拖拽时 snapTo、松手后 animateTo
    val offsetPx = remember {
        Animatable(
            initialValue = if (drawerState.targetValue == DrawerValue.Open) drawerWidthPx else 0f
        )
    }

    // 程序化打开/关闭（汉堡按钮、导航项）→ 启动平滑动画
    LaunchedEffect(drawerState.targetValue) {
        val target = if (drawerState.targetValue == DrawerValue.Open) drawerWidthPx else 0f
        offsetPx.animateTo(target, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (gestureEnabled) {
                    Modifier.pointerInput(drawerState.targetValue) {
                        val touchSlop = viewConfiguration.touchSlop * 1.5f

                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: return@awaitEachGesture

                            // 子节点已消费的事件（按钮点击等）不拦截
                            if (event.type != PointerEventType.Press || change.isConsumed) {
                                return@awaitEachGesture
                            }

                            // 记录手势开始时的 finger 位置 + drawer 位置
                            val startX = change.position.x
                            val startOffset = offsetPx.value
                            var wasDragged = false

                            while (true) {
                                val moveEvent = awaitPointerEvent()
                                val moveChange = moveEvent.changes.firstOrNull()
                                    ?: return@awaitEachGesture

                                when (moveEvent.type) {
                                    PointerEventType.Move -> {
                                        val rawDx = moveChange.position.x - startX
                                        // 判断手指是否显著移动（超过触摸容差）
                                        if (rawDx * rawDx > touchSlop * touchSlop) {
                                            wasDragged = true
                                        }
                                        // 偏移量 = 初始偏移 + 手指位移 × 灵敏度倍率
                                        val newOffset = (startOffset + rawDx * dragSensitivity)
                                            .coerceIn(0f, drawerWidthPx)
                                        // ★ 实时跟随手指位置
                                        scope.launch {
                                            offsetPx.snapTo(newOffset)
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        val currentOffset = offsetPx.value

                                        // 轻触（未拖拽）且侧边栏未完全关闭 → 收起侧边栏
                                        if (!wasDragged && currentOffset > 0f) {
                                            scope.launch {
                                                offsetPx.animateTo(0f, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
                                                drawerState.snapTo(DrawerValue.Closed)
                                            }
                                            return@awaitEachGesture
                                        }

                                        // 正常拖拽 → 根据拖拽方向使用不对称阈值
                                        // 向哪边滑就用哪边的阈值，保证打开/关闭手感一致
                                        val dragDirection = currentOffset - startOffset
                                        val threshold = if (dragDirection >= 0f) {
                                            drawerWidthPx * 0.25f  // 展开方向，超过 25% 即吸附打开
                                        } else {
                                            drawerWidthPx * 0.75f  // 收起方向，低于 75% 即吸附关闭
                                        }
                                        val targetOffset: Float
                                        val targetState: DrawerValue
                                        if (currentOffset > threshold) {
                                            targetOffset = drawerWidthPx
                                            targetState = DrawerValue.Open
                                        } else {
                                            targetOffset = 0f
                                            targetState = DrawerValue.Closed
                                        }

                                        // ★ 直接 animateTo，不依赖 LaunchedEffect
                                        scope.launch {
                                            offsetPx.animateTo(targetOffset, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
                                            drawerState.snapTo(targetState)
                                        }
                                        return@awaitEachGesture
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // ── 主内容区 ── 偏移量 + 缩放效果（推拉时主内容等比缩小，露出下方侧边栏）
        // 外层 Box：控制偏移 + 背景色（不参与缩放，避免露出透明间隙）
        Box(
            modifier = Modifier
                .offset(x = with(density) { offsetPx.value.toDp() })
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 内层 Box：根据推拉进度实时缩放（1.0 → 0.88），锚点居左，形成"被推远"的视觉
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = (offsetPx.value / drawerWidthPx).coerceIn(0f, 1f)
                        val contentScale = 1f - 0.12f * progress
                        scaleX = contentScale
                        scaleY = contentScale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                content()
            }
        }

        // ── 侧边栏 ── 始终 Composed，通过 offset 从左侧滑入/滑出（与手指同步）
        Box(
            modifier = Modifier
                .offset(x = with(density) { (offsetPx.value - drawerWidthPx).toDp() })
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxSize(),
                drawerShape = drawerShape,
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                drawerContent()
            }
        }
    }
}
