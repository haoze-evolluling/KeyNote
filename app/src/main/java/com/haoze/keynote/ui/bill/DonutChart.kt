package com.haoze.keynote.ui.bill

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.CategorySpending
import com.haoze.keynote.ui.theme.LocalAppColors

@Composable
fun DonutChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val total = data.sumOf { it.total }
    if (total <= 0.0 || data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据", color = colors.outline)
        }
        return
    }

    val chartColors = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary,
        colors.error,
        colors.primaryContainer,
        colors.secondaryContainer,
        colors.tertiaryContainer,
        colors.errorContainer,
        colors.outline,
        colors.outlineVariant,
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 40.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweep = (item.total / total * 360).toFloat()
                    val color = chartColors[index % chartColors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "¥${String.format("%.0f", total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "总计",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        data.forEachIndexed { index, item ->
            val color = chartColors[index % chartColors.size]
            val percent = if (total > 0) item.total / total * 100 else 0.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = color)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    item.categoryName ?: "未分类",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "¥${String.format("%.2f", item.total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${String.format("%.1f", percent)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline
                )
            }
        }
    }
}
