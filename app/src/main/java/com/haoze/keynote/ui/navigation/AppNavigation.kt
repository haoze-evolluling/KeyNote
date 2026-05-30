package com.haoze.keynote.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.haoze.keynote.ui.home.HomeScreen
import com.haoze.keynote.ui.layout.PushDrawerLayout
import com.haoze.keynote.ui.edit.EditNoteScreen
import com.haoze.keynote.ui.settings.SettingsScreen
import com.haoze.keynote.ui.settings.AiProviderManageScreen
import com.haoze.keynote.ui.tag.TagNotesScreen
import com.haoze.keynote.ui.chat.AIChatScreen
import com.haoze.keynote.ui.bill.BillScreen
import com.haoze.keynote.ui.bill.BillStatsScreen
import com.haoze.keynote.ui.trash.TrashScreen
import com.haoze.keynote.ui.home.DateGroupNotesScreen
import com.haoze.keynote.ui.home.ExportDataScreen
import com.haoze.keynote.ui.schedule.ScheduleScreen
import com.haoze.keynote.ui.schedule.ScheduleViewModel
import com.haoze.keynote.viewmodel.DrawerViewModel
import com.haoze.keynote.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "笔记", Icons.Default.Home)
    data object EditNote : Screen("edit_note?noteId={noteId}", "编辑笔记") {
        fun createRoute(noteId: Long) = "edit_note?noteId=$noteId"
    }
    data object AIChat : Screen("ai_chat", "AI对话", null)
    data object Bill : Screen("bill", "账单", null)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object TagNotes : Screen("tag_notes/{tagId}/{tagName}", "标签笔记") {
        fun createRoute(tagId: Long, tagName: String) = "tag_notes/$tagId/$tagName"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerViewModel: DrawerViewModel = viewModel()
    val tags by drawerViewModel.tags.collectAsState()
    var tagsExpanded by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    PushDrawerLayout(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                tags = tags,
                tagsExpanded = tagsExpanded,
                onTagsExpandedToggle = { tagsExpanded = !tagsExpanded },
                onNavigateToRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.AIChat.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToTag = { tagId, tagName ->
                    navController.navigate(Screen.TagNotes.createRoute(tagId, tagName)) {
                        popUpTo(Screen.AIChat.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        },
        content = {
            NavHost(
                navController = navController,
                startDestination = Screen.AIChat.route,
                modifier = Modifier
            ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToEdit = { noteId ->
                                navController.navigate(Screen.EditNote.createRoute(noteId))
                            },
                            onNavigateToTagNotes = { tagId, tagName ->
                                navController.navigate(Screen.TagNotes.createRoute(tagId, tagName))
                            },
                            drawerState = drawerState,
                            scope = scope
                        )
                    }
                    composable(
                        route = Screen.EditNote.route,
                        arguments = listOf(navArgument("noteId") {
                            type = NavType.LongType
                        })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                        EditNoteScreen(
                            noteId = noteId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.AIChat.route) {
                        AIChatScreen(
                            drawerState = drawerState,
                            scope = scope,
                            onCreateNote = { noteId ->
                                navController.navigate(Screen.EditNote.createRoute(noteId))
                            }
                        )
                    }
                    composable(Screen.Bill.route) {
                        BillScreen(
                            drawerState = drawerState,
                            scope = scope,
                        )
                    }
                    composable("bill_stats") {
                        BillStatsScreen(
                            drawerState = drawerState,
                            scope = scope,
                        )
                    }
                    composable("date_group_notes") {
                        DateGroupNotesScreen(
                            drawerState = drawerState,
                            scope = scope,
                            onNavigateToEdit = { noteId ->
                                navController.navigate(Screen.EditNote.createRoute(noteId))
                            },
                            onNavigateToTagNotes = { tagId, tagName ->
                                navController.navigate(Screen.TagNotes.createRoute(tagId, tagName))
                            }
                        )
                    }
                    composable("trash") {
                        TrashScreen(
                            drawerState = drawerState,
                            scope = scope,
                        )
                    }
                    composable("data_export") {
                        ExportDataScreen(
                            drawerState = drawerState,
                            drawerScope = scope,
                        )
                    }
                    composable("schedule") {
                        ScheduleScreen(
                            drawerState = drawerState,
                            scope = scope,
                            viewModel = viewModel()
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            drawerState = drawerState,
                            scope = scope,
                            onNavigateToProviderManage = { navController.navigate("ai_provider_manage") }
                        )
                    }
                    composable("ai_provider_manage") {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        val providers by settingsViewModel.providers.collectAsState()
                        val activeProviderId by settingsViewModel.activeProviderId.collectAsState()

                        AiProviderManageScreen(
                            onNavigateBack = { navController.popBackStack() },
                            providers = providers,
                            activeProviderId = activeProviderId,
                            onSelectProvider = { settingsViewModel.selectProvider(it) },
                            onUpdateProvider = { settingsViewModel.updateProvider(it) },
                            onDeleteProvider = { settingsViewModel.deleteCustomProvider(it) },
                            onAddProvider = { name, url, model, key -> settingsViewModel.addCustomProvider(name, url, model, key) },
                            sealKey = { settingsViewModel.sealZidaipass(it) },
                            openKey = { settingsViewModel.openZidaipass(it) }
                        )
                    }
                    composable(
                        route = Screen.TagNotes.route,
                        arguments = listOf(
                            navArgument("tagId") { type = NavType.LongType },
                            navArgument("tagName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val tagId = backStackEntry.arguments?.getLong("tagId") ?: return@composable
                        val tagName = backStackEntry.arguments?.getString("tagName") ?: return@composable
                        TagNotesScreen(
                            drawerState = drawerState,
                            scope = scope,
                            tagId = tagId,
                            tagName = tagName,
                            onNavigateToEdit = { noteId ->
                                noteId?.let { navController.navigate(Screen.EditNote.createRoute(it)) }
                            }
                        )
                }
            }
        }
    )
}
