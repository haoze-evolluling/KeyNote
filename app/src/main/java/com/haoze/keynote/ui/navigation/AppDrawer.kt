package com.haoze.keynote.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.keynote.R
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.ui.theme.LocalAppColors

internal sealed class DrawerItem {
    data class Nav(
        val label: String,
        val icon: ImageVector,
        val route: String,
    ) : DrawerItem()
}

private data class DrawerGroup(
    val label: String,
    val icon: ImageVector,
    val items: List<DrawerItem.Nav>
)

private val drawerGroups = listOf(
    DrawerGroup("智能", Icons.Default.AutoAwesome, listOf(
        DrawerItem.Nav("AI对话", Icons.Default.AutoAwesome, "ai_chat"),
    )),
    DrawerGroup("笔记", Icons.Default.Description, listOf(
        DrawerItem.Nav("全部笔记", Icons.Default.Description, "home"),
        DrawerItem.Nav("按日期查看", Icons.Default.CalendarMonth, "date_group_notes"),
    )),
    DrawerGroup("账单", Icons.Default.Receipt, listOf(
        DrawerItem.Nav("账单", Icons.Default.Receipt, "bill"),
        DrawerItem.Nav("账单统计", Icons.Default.Assessment, "bill_stats"),
    )),
    DrawerGroup("日程", Icons.Default.CalendarMonth, listOf(
        DrawerItem.Nav("日程列表", Icons.Default.CalendarMonth, "schedule"),
    )),
    DrawerGroup("系统", Icons.Default.Settings, listOf(
        DrawerItem.Nav("导出数据", Icons.Default.FileDownload, "data_export"),
        DrawerItem.Nav("回收站", Icons.Default.Delete, "trash"),
        DrawerItem.Nav("设置", Icons.Default.Settings, "settings"),
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDrawerContent(
    currentRoute: String?,
    tags: List<TagEntity>,
    tagsExpanded: Boolean,
    onTagsExpandedToggle: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToTag: (Long, String) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val colors = LocalAppColors.current
    val isTagSelected = currentRoute?.startsWith("tag_notes/") == true
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        DrawerHeader()

        HorizontalDivider()

        drawerGroups.forEach { group ->
            val isGroupExpanded = group.label in expandedGroups
            val isGroupSelected = group.items.any { it.route == currentRoute }

            DrawerGroupItem(
                label = group.label,
                icon = group.icon,
                isExpanded = isGroupExpanded,
                isSelected = isGroupSelected,
                onToggle = {
                    expandedGroups = if (isGroupExpanded) expandedGroups - group.label
                                     else expandedGroups + group.label
                }
            )

            AnimatedVisibility(
                visible = isGroupExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column {
                    group.items.forEach { item ->
                        DrawerNavItem(
                            item = item,
                            isSelected = currentRoute == item.route,
                            onClick = {
                                onNavigateToRoute(item.route)
                                onCloseDrawer()
                            }
                        )
                    }
                    // 标签分类放在笔记分组内
                    if (group.label == "笔记") {
                        DrawerTagsItem(
                            tags = tags,
                            isExpanded = tagsExpanded,
                            isSelected = isTagSelected,
                            onToggle = onTagsExpandedToggle,
                            onTagClick = { tag ->
                                onNavigateToTag(tag.id, tag.name)
                                onCloseDrawer()
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerHeader() {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "KeyNote Logo",
            modifier = Modifier.size(48.dp),
            tint = colors.unspecified
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "KeyNote",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "个人管理工具",
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline
            )
        }
    }
}

@Composable
private fun DrawerGroupItem(
    label: String,
    icon: ImageVector,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalAppColors.current
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrowRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(16.dp))

        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) colors.primary
                   else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            label,
            color = if (isSelected) colors.primary
                    else colors.onSurface,
            fontWeight = if (isSelected || isExpanded) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier
                .size(20.dp)
                .rotate(arrowRotation),
            tint = colors.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
private fun DrawerNavItem(
    item: DrawerItem.Nav,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(48.dp)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isSelected) 24.dp else 0.dp)
                .background(
                    color = if (isSelected) colors.primary
                            else colors.transparent
                )
        )

        Spacer(Modifier.width(16.dp))

        Icon(
            item.icon,
            contentDescription = item.label,
            tint = if (isSelected) colors.primary
                   else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            item.label,
            color = if (isSelected) colors.primary
                    else colors.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DrawerTagsItem(
    tags: List<TagEntity>,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onTagClick: (TagEntity) -> Unit,
) {
    val colors = LocalAppColors.current
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrowRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .height(48.dp)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar indicator (matching DrawerNavItem)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isSelected) 24.dp else 0.dp)
                .background(
                    color = if (isSelected) colors.primary
                            else colors.transparent
                )
        )

        Spacer(Modifier.width(16.dp))

        Icon(
            Icons.Default.Label,
            contentDescription = "标签分类",
            tint = if (isSelected) colors.primary
                   else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            "标签分类",
            color = if (isSelected) colors.primary
                    else colors.onSurface,
            fontWeight = if (isSelected || isExpanded) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier
                .size(20.dp)
                .rotate(arrowRotation),
            tint = colors.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(200))
    ) {
        Column(Modifier.padding(start = 48.dp)) {
            if (tags.isEmpty()) {
                Text(
                    "暂无标签",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagClick(tag) }
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = tag.name,
                            modifier = Modifier.size(18.dp),
                            tint = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "#${tag.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }
    }
}
