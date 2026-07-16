package com.rohan.mbtool.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohan.mbtool.ui.screens.HistoryScreen
import com.rohan.mbtool.ui.screens.HomeScreen
import com.rohan.mbtool.ui.screens.SettingsScreen
import com.rohan.mbtool.ui.theme.Muted
import com.rohan.mbtool.ui.theme.Primary
import com.rohan.mbtool.ui.theme.Surf
import kotlinx.coroutines.launch

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Home",     Icons.Rounded.Home),
    Tab("History",  Icons.Rounded.History),
    Tab("Settings", Icons.Rounded.Settings),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigation() {
    val pagerState = rememberPagerState { TABS.size }
    val scope      = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                modifier       = Modifier.fillMaxWidth(),
                containerColor = Surf,
                tonalElevation = 0.dp,
            ) {
                TABS.forEachIndexed { idx, tab ->
                    val selected = pagerState.currentPage == idx
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            scope.launch {
                                pagerState.animateScrollToPage(idx, animationSpec = tween(300))
                            }
                        },
                        icon  = { Icon(tab.icon, tab.label) },
                        label = {
                            Text(
                                tab.label,
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Primary,
                            selectedTextColor   = Primary,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor      = Primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state             = pagerState,
            modifier          = Modifier
                .fillMaxSize()
                .padding(padding),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> HomeScreen()
                1 -> HistoryScreen()
                2 -> SettingsScreen()
            }
        }
    }
}
