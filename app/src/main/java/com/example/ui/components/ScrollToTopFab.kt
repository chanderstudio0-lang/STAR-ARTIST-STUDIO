package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkStudioBlack
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

/**
 * Standard threshold (item index) beyond which the scroll-to-top FAB becomes visible.
 */
const val SCROLL_TO_TOP_THRESHOLD = 2

/**
 * Floating Action Button (FAB) that dynamically appears when a user scrolls down
 * on a LazyColumn and smoothly scrolls back to the top of the content when tapped.
 */
@Composable
fun ScrollToTopFab(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentDescription: String = "Scroll back to top",
    testTag: String = "scroll_to_top_fab"
) {
    val coroutineScope = rememberCoroutineScope()
    val isVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= SCROLL_TO_TOP_THRESHOLD ||
                    (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 250)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(index = 0)
                }
            },
            containerColor = GoldPrimary,
            contentColor = DarkStudioBlack,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldLight, GoldDark))),
                    CircleShape
                )
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = contentDescription,
                tint = DarkStudioBlack,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Floating Action Button (FAB) that dynamically appears when a user scrolls down
 * on a LazyVerticalGrid (e.g. Gallery) and smoothly scrolls back to the top when tapped.
 */
@Composable
fun ScrollToTopGridFab(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    contentDescription: String = "Scroll back to top",
    testTag: String = "scroll_to_top_grid_fab"
) {
    val coroutineScope = rememberCoroutineScope()
    val isVisible by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex >= SCROLL_TO_TOP_THRESHOLD ||
                    (gridState.firstVisibleItemIndex == 1 && gridState.firstVisibleItemScrollOffset > 250)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    gridState.animateScrollToItem(index = 0)
                }
            },
            containerColor = GoldPrimary,
            contentColor = DarkStudioBlack,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(GoldLight, GoldDark))),
                    CircleShape
                )
                .testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = contentDescription,
                tint = DarkStudioBlack,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
