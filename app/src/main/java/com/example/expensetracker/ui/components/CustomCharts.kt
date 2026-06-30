package com.example.expensetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.expensetracker.utils.CurrencyFormatter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.local.CategorySum
import kotlin.math.atan2
import kotlin.math.min

// Vibrant category colors
val CategoryColors = mapOf(
    "Food" to Color(0xFFFF7043),          // Deep Orange
    "Shopping" to Color(0xFF26A69A),      // Teal
    "Transport" to Color(0xFF42A5F5),     // Blue
    "Bills" to Color(0xFFAB47BC),         // Purple
    "Entertainment" to Color(0xFFFFCA28),  // Amber
    "Uncategorized" to Color(0xFF78909C),   // Blue Grey
    "Others" to Color(0xFF8D6E63)          // Brown
)

fun getCategoryColor(category: String): Color {
    return CategoryColors[category] ?: Color(0xFFEC407A) // Pink default
}

@Composable
fun InteractiveDonutChart(
    categorySums: List<CategorySum>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalSpend = categorySums.sumOf { it.total }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySums) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (totalSpend <= 0.0) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 30.dp.toPx())
                )
            }
            Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(categorySums) {
                    detectTapGestures { offset ->
                        val canvasSize = size
                        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                        val x = offset.x - center.x
                        val y = offset.y - center.y
                        val radius = min(canvasSize.width, canvasSize.height) / 2f
                        val distance = Math.sqrt((x * x + y * y).toDouble())

                        // Only register taps inside the donut arc boundary
                        if (distance in (radius - 30.dp.toPx())..(radius + 10.dp.toPx())) {
                            var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble()))
                            if (angle < 0) angle += 360.0

                            var currentAngle = 0.0
                            var clickedIndex = -1
                            for (i in categorySums.indices) {
                                val sweep = (categorySums[i].total / totalSpend) * 360.0
                                if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                    clickedIndex = i
                                    break
                                }
                                currentAngle += sweep
                            }
                            selectedIndex = if (selectedIndex == clickedIndex) -1 else clickedIndex
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f
            val strokeWidth = 30.dp.toPx()
            val innerRadius = outerRadius - strokeWidth / 2f

            var startAngle = 0f
            categorySums.forEachIndexed { index, item ->
                val sweepAngle = ((item.total / totalSpend) * 360f).toFloat()
                val isSelected = index == selectedIndex
                val extraStroke = if (isSelected) 10.dp.toPx() else 0f

                drawArc(
                    color = getCategoryColor(item.category),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animationProgress.value,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2f, innerRadius * 2f),
                    style = Stroke(width = strokeWidth + extraStroke, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selectedIndex != -1 && selectedIndex < categorySums.size) {
                val item = categorySums[selectedIndex]
                Text(
                    text = item.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getCategoryColor(item.category)
                )
                Text(
                    text = CurrencyFormatter.format(item.total, context),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = "Total Spent",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(totalSpend, context),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SpendingTrendLineChart(
    dailyAmounts: List<Double>, // 7 values for 7 days
    modifier: Modifier = Modifier
) {
    val maxVal = remember(dailyAmounts) { dailyAmounts.maxOrNull()?.toFloat() ?: 1f }
    val animProgress = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(dailyAmounts) {
        animProgress.animateTo(1f, tween(1200))
    }

    Canvas(modifier = modifier) {
        if (dailyAmounts.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (dailyAmounts.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        dailyAmounts.forEachIndexed { index, amount ->
            // Invert Y coordinate since Canvas starts from top left
            val x = index * stepX
            val normalizedY = if (maxVal > 0f) (amount.toFloat() / maxVal) else 0f
            val y = height - (normalizedY * height * animProgress.value)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == dailyAmounts.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }

            // Draw data points
            drawCircle(
                color = primaryColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw Line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Fill Gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        )
    }
}

@Composable
fun HorizontalBudgetBar(
    spent: Double,
    limit: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(spent, limit) {
        animProgress.animateTo(progress, tween(800))
    }

    // Color code based on consumption
    val barColor = when {
        progress >= 0.9f -> Color(0xFFEF5350)  // Red
        progress >= 0.75f -> Color(0xFFFFA726) // Orange
        else -> Color(0xFF66BB6A)              // Green
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            // Track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
                // Fill
                drawRoundRect(
                    color = barColor,
                    size = Size(size.width * animProgress.value, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}
