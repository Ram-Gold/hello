package com.example.hello.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.launch

/**
 * Reusable, high-fluidity tab component inspired by PixelPlayer's library tabs.
 *
 * Core architectural invariants:
 * - Single source of truth: PagerState is the sole authority for active tab index and page layout.
 * - Derived active tab index prevents double-state desynchronization bugs.
 * - Hardware-accelerated scale and color transitions for 60/120fps motion.
 */
@Composable
fun SmoothTabs(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    userScrollEnabled: Boolean = false,
    animationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween(),
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    indicatorShape: Shape = RoundedCornerShape(20.dp),
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    unselectedTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    compactMode: Boolean = false,
    content: List<@Composable () -> Unit>
) {
    SmoothTabs(
        tabs = tabs,
        modifier = modifier,
        initialPage = initialPage,
        userScrollEnabled = userScrollEnabled,
        animationSpec = animationSpec,
        indicatorColor = indicatorColor,
        indicatorShape = indicatorShape,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        selectedTextStyle = selectedTextStyle,
        unselectedTextStyle = unselectedTextStyle,
        compactMode = compactMode
    ) { page ->
        if (page in content.indices) {
            content[page].invoke()
        }
    }
}

@Composable
fun SmoothTabs(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    userScrollEnabled: Boolean = false,
    animationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween(),
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    indicatorShape: Shape = RoundedCornerShape(20.dp),
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    unselectedTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    compactMode: Boolean = false,
    content: @Composable (page: Int) -> Unit
) {
    val pageCount = tabs.size
    val safeInitialPage = if (pageCount > 0) initialPage.coerceIn(0, pageCount - 1) else 0
    val pagerState = rememberPagerState(initialPage = safeInitialPage) {
        tabs.size
    }

    SmoothTabs(
        tabs = tabs,
        pagerState = pagerState,
        modifier = modifier,
        userScrollEnabled = userScrollEnabled,
        animationSpec = animationSpec,
        indicatorColor = indicatorColor,
        indicatorShape = indicatorShape,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        selectedTextStyle = selectedTextStyle,
        unselectedTextStyle = unselectedTextStyle,
        compactMode = compactMode,
        content = content
    )
}

@Composable
fun SmoothTabs(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = false,
    animationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween(),
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    indicatorShape: Shape = RoundedCornerShape(20.dp),
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    unselectedTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    compactMode: Boolean = false,
    content: @Composable (page: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    // Single source of truth: targetPage gives immediate response on tap, currentPage gives settled state
    val selectedIndex by remember {
        derivedStateOf { pagerState.targetPage }
    }

    Column(modifier = modifier) {
        SmoothTabBar(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { index ->
                if (pagerState.currentPage != index || pagerState.targetPage != index) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = index,
                            animationSpec = animationSpec
                        )
                    }
                }
            },
            indicatorColor = indicatorColor,
            indicatorShape = indicatorShape,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
            selectedTextStyle = selectedTextStyle,
            unselectedTextStyle = unselectedTextStyle,
            compactMode = compactMode,
            animationSpec = animationSpec
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = userScrollEnabled,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            content(page)
        }
    }
}

/**
 * Lightweight, high-performance tab bar with sliding pill indicator and micro-scale animations.
 */
@Composable
fun SmoothTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    indicatorShape: Shape = RoundedCornerShape(20.dp),
    selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextStyle: TextStyle = MaterialTheme.typography.titleSmall,
    unselectedTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    compactMode: Boolean = false,
    animationSpec: AnimationSpec<Float> = MotionTokens.tabTransitionTween()
) {
    if (tabs.isEmpty()) return
    val safeSelectedIndex = selectedIndex.coerceIn(0, tabs.size - 1)

    if (compactMode || tabs.size <= 4) {
        PrimaryTabRow(
            selectedTabIndex = safeSelectedIndex,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactMode) 8.dp else 16.dp, vertical = 6.dp),
            containerColor = Color.Transparent,
            divider = {},
            indicator = {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(safeSelectedIndex, matchContentSize = false)
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .background(color = indicatorColor, shape = indicatorShape)
                        .zIndex(-1f)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == safeSelectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.94f,
                    animationSpec = animationSpec,
                    label = "tabScale_$index"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                    animationSpec = tween(
                        durationMillis = MotionTokens.TAB_TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    ),
                    label = "tabTextColor_$index"
                )

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .clip(indicatorShape)
                        .padding(vertical = if (compactMode) 4.dp else 8.dp),
                    selectedContentColor = selectedContentColor,
                    unselectedContentColor = unselectedContentColor,
                    text = {
                        Text(
                            text = title,
                            color = textColor,
                            style = if (isSelected) selectedTextStyle else unselectedTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                )
            }
        }
    } else {
        PrimaryScrollableTabRow(
            selectedTabIndex = safeSelectedIndex,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            containerColor = Color.Transparent,
            edgePadding = 16.dp,
            divider = {},
            indicator = {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(safeSelectedIndex, matchContentSize = false)
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .background(color = indicatorColor, shape = indicatorShape)
                        .zIndex(-1f)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == safeSelectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.94f,
                    animationSpec = animationSpec,
                    label = "tabScale_$index"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                    animationSpec = tween(
                        durationMillis = MotionTokens.TAB_TRANSITION_DURATION_MS,
                        easing = FastOutSlowInEasing
                    ),
                    label = "tabTextColor_$index"
                )

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .clip(indicatorShape)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    selectedContentColor = selectedContentColor,
                    unselectedContentColor = unselectedContentColor,
                    text = {
                        Text(
                            text = title,
                            color = textColor,
                            style = if (isSelected) selectedTextStyle else unselectedTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                    }
                )
            }
        }
    }
}
