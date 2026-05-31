package com.haoze.keynote.ui.bill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.haoze.keynote.ui.theme.AppAlertDialog
import com.haoze.keynote.ui.theme.AppDatePickerDialog
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillStatsScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: BillStatsViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val totalSpending by viewModel.totalSpending.collectAsState()
    val billCount by viewModel.billCount.collectAsState()
    val categoryStats by viewModel.categoryStats.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val dailySpending by viewModel.dailySpending.collectAsState()
    val dailyTrend by viewModel.dailyTrend.collectAsState()
    val weeklyTrend by viewModel.weeklyTrend.collectAsState()
    val granularity by viewModel.granularity.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()

    var selectedPreset by remember { mutableStateOf(RangePreset.THIS_MONTH) }
    var showCustomRangePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账单统计") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    RangePreset.THIS_WEEK to "本周",
                    RangePreset.THIS_MONTH to "本月",
                    RangePreset.THIS_YEAR to "本年",
                    RangePreset.ALL to "全部"
                ).forEach { (preset, label) ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = {
                            selectedPreset = preset
                            viewModel.setPresetRange(preset)
                        },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = selectedPreset == RangePreset.CUSTOM,
                    onClick = { showCustomRangePicker = true },
                    label = { Text("自定义") },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (selectedPreset == RangePreset.CUSTOM) {
                Text(
                    "${dateFormat.format(Date(dateRange.first))} ~ ${dateFormat.format(Date(dateRange.second))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline
                )
            }

            HorizontalDivider()

            Text("摘要", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("总支出", "¥${String.format("%.2f", totalSpending)}", colors.primary, Modifier.weight(1f))
                SummaryCard("账单数", "$billCount", colors.secondary, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dayCount = ((dateRange.second - dateRange.first) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                val dailyAvg = totalSpending / dayCount
                SummaryCard("日均", "¥${String.format("%.2f", dailyAvg)}", colors.tertiary, Modifier.weight(1f))
                val topCategory = categoryStats.maxByOrNull { it.total }
                SummaryCard(
                    "最高类别",
                    topCategory?.categoryName ?: "-",
                    colors.error,
                    Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Text("支出分布", style = MaterialTheme.typography.titleMedium)
            DonutChart(
                data = categoryStats,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            val trendData = when (granularity) {
                Granularity.DAY -> dailyTrend.toChartPoints()
                Granularity.WEEK -> weeklyTrend.toChartPoints()
                Granularity.MONTH -> monthlyTrend.toChartPoints()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("支出趋势", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Granularity.DAY to "日",
                        Granularity.WEEK to "周",
                        Granularity.MONTH to "月"
                    ).forEach { (g, label) ->
                        FilterChip(
                            selected = granularity == g,
                            onClick = { viewModel.setGranularity(g) },
                            label = { Text(label) }
                        )
                    }
                }
            }
            LineChart(
                data = trendData,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text("每日支出", style = MaterialTheme.typography.titleMedium)
            BarChart(
                data = dailySpending,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showCustomRangePicker) {
        var startMillis by remember { mutableStateOf(dateRange.first) }
        var endMillis by remember { mutableStateOf(dateRange.second) }
        var pickingStart by remember { mutableStateOf(true) }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (pickingStart) startMillis else endMillis
        )

        AppDatePickerDialog(
            onDismissRequest = { showCustomRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val localCal = java.util.Calendar.getInstance().apply {
                            timeInMillis = utcMillis
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val localMillis = localCal.timeInMillis
                        if (pickingStart) {
                            startMillis = localMillis
                            pickingStart = false
                        } else {
                            val endCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = localMillis
                                set(java.util.Calendar.HOUR_OF_DAY, 23)
                                set(java.util.Calendar.MINUTE, 59)
                                set(java.util.Calendar.SECOND, 59)
                                set(java.util.Calendar.MILLISECOND, 999)
                            }
                            endMillis = endCal.timeInMillis
                            selectedPreset = RangePreset.CUSTOM
                            viewModel.setCustomRange(startMillis, endMillis)
                            showCustomRangePicker = false
                        }
                    }
                }) { Text(if (pickingStart) "下一步" else "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
