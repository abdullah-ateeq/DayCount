package com.example.daycount.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.daycount.domain.DayCountCalculator
import com.example.daycount.domain.DayCountFormatter
import com.example.daycount.model.DayCountResult
import com.example.daycount.ui.SectionLabel
import com.example.daycount.ui.StagedReveal
import com.example.daycount.ui.revealStage
import com.example.daycount.ui.theme.NumberStyle
import java.time.LocalDate

@Composable
fun weekdayColor(): Color = MaterialTheme.colorScheme.primary

@Composable
fun weekendColor(): Color = MaterialTheme.colorScheme.tertiary

/** A large number that shrinks to fit instead of wrapping or clipping. */
@Composable
fun FittedNumber(
    text: String,
    style: TextStyle,
    color: Color,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 18.sp,
    textAlign: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        style = style.merge(NumberStyle).copy(color = color, textAlign = textAlign),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            stepSize = 2.sp,
        ),
        modifier = modifier,
    )
}

@Composable
fun DateCard(
    label: String,
    accessibilityName: String,
    date: LocalDate?,
    formatter: DayCountFormatter,
    onClick: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = date?.let { "$accessibilityName, ${formatter.fullDate(it)}." }
        ?: "$accessibilityName, not selected."
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Change ${accessibilityName.lowercase()}",
                        onClick = onClick,
                    )
                    .semantics { contentDescription = description }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.clearAndSetSemantics { }) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = date?.let(formatter::longDate) ?: "Select a date",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = date?.let(formatter::dayName) ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = onToday,
                modifier = Modifier.semantics {
                    contentDescription = "Set ${accessibilityName.lowercase()} to today"
                },
            ) {
                Text("TODAY", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SwapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = "Swap start and end dates." },
        ) {
            Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("SWAP", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Compact "‹ WEEK ›" style control that shifts the whole range. */
@Composable
fun ShiftControl(
    label: String,
    previousDescription: String,
    nextDescription: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = previousDescription)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = nextDescription)
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(container, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = content)
    }
}

@Composable
fun MainResultCard(
    result: DayCountResult,
    showReordered: Boolean,
    formatter: DayCountFormatter,
    reveal: StagedReveal,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(
                    text = "Inclusive range",
                    icon = Icons.Outlined.CheckCircle,
                    container = colors.onPrimaryContainer.copy(alpha = 0.10f),
                    content = colors.onPrimaryContainer,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Inclusive date range." },
                )
                if (showReordered) {
                    Pill(
                        text = "DATES REORDERED",
                        icon = Icons.Outlined.Info,
                        container = colors.surface.copy(alpha = 0.55f),
                        content = colors.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Dates reordered. The earlier date is used as the start."
                        },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clearAndSetSemantics { contentDescription = formatter.resultAccessibility(result) },
            ) {
                FittedNumber(
                    text = formatter.count(result.totalDays),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.onPrimaryContainer,
                    maxFontSize = 76.sp,
                    minFontSize = 28.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .revealStage(reveal, 0),
                )
                Text(
                    text = formatter.totalLabel(result.totalDays),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.revealStage(reveal, 0),
                )

                HorizontalDivider(
                    color = colors.onPrimaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 18.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ResultCount(
                        value = result.weekdays,
                        label = formatter.weekdayLabel(result.weekdays),
                        formatter = formatter,
                        accent = colors.primary,
                        modifier = Modifier
                            .weight(1f)
                            .revealStage(reveal, 1),
                    )
                    ResultCount(
                        value = result.weekendDays,
                        label = formatter.weekendLabel(result.weekendDays),
                        formatter = formatter,
                        accent = colors.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .revealStage(reveal, 2),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultCount(
    value: Long,
    label: String,
    formatter: DayCountFormatter,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (value > 0) accent else accent.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            FittedNumber(
                text = formatter.count(value),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (value > 0) 1f else 0.5f),
                maxFontSize = 28.sp,
                minFontSize = 14.sp,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
fun BreakdownCard(
    result: DayCountResult,
    showPercentages: Boolean,
    formatter: DayCountFormatter,
    reveal: StagedReveal,
    modifier: Modifier = Modifier,
) {
    val percentages = formatter.percentages(result)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionLabel("Weekday / weekend breakdown")
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BreakdownTile(
                    icon = Icons.Outlined.DateRange,
                    title = "WEEKDAYS",
                    value = result.weekdays,
                    days = "MON–FRI",
                    accent = weekdayColor(),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    summary = formatter.weekdaySummary(result.weekdays),
                    formatter = formatter,
                    modifier = Modifier
                        .weight(1f)
                        .revealStage(reveal, 1),
                )
                BreakdownTile(
                    icon = Icons.Outlined.Weekend,
                    title = "WEEKENDS",
                    value = result.weekendDays,
                    days = "SAT–SUN",
                    accent = weekendColor(),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    summary = formatter.weekendSummary(result.weekendDays),
                    formatter = formatter,
                    modifier = Modifier
                        .weight(1f)
                        .revealStage(reveal, 2),
                )
            }

            Spacer(Modifier.height(18.dp))
            DistributionBar(
                weekdays = result.weekdays,
                weekends = result.weekendDays,
                modifier = Modifier.revealStage(reveal, 3),
            )

            if (showPercentages) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .revealStage(reveal, 3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatter.weekdayPercent(percentages),
                        style = MaterialTheme.typography.labelLarge,
                        color = weekdayColor(),
                    )
                    Text(
                        text = formatter.weekendPercent(percentages),
                        style = MaterialTheme.typography.labelLarge,
                        color = weekendColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownTile(
    icon: ImageVector,
    title: String,
    value: Long,
    days: String,
    accent: Color,
    container: Color,
    summary: String,
    formatter: DayCountFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container.copy(alpha = 0.55f))
            .padding(14.dp)
            .clearAndSetSemantics { contentDescription = "$summary, $days." },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = accent)
        }
        Spacer(Modifier.height(6.dp))
        FittedNumber(
            text = formatter.count(value),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxFontSize = 34.sp,
            minFontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = days,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Two-segment proportional bar. Decorative: the numbers carry the meaning. */
@Composable
fun DistributionBar(weekdays: Long, weekends: Long, modifier: Modifier = Modifier) {
    val total = (weekdays + weekends).toDouble().coerceAtLeast(1.0)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { },
    ) {
        if (weekdays > 0) {
            Box(
                Modifier
                    .weight((weekdays / total).toFloat().coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(weekdayColor())
            )
        }
        if (weekdays > 0 && weekends > 0) Spacer(Modifier.width(3.dp))
        if (weekends > 0) {
            Box(
                Modifier
                    .weight((weekends / total).toFloat().coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(weekendColor())
            )
        }
    }
}

@Composable
fun DayBreakdownCard(
    result: DayCountResult,
    formatter: DayCountFormatter,
    modifier: Modifier = Modifier,
) {
    val counts = result.dayOfWeekCountList
    val max = counts.maxOf { it.count }.coerceAtLeast(1L).toDouble()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            SectionLabel("Day breakdown")
            Spacer(Modifier.height(12.dp))
            counts.forEach { item ->
                val weekend = DayCountCalculator.isWeekend(item.dayOfWeek)
                val accent = if (weekend) weekendColor() else weekdayColor()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${formatter.dayName(item.dayOfWeek)}, ${formatter.count(item.count)}"
                        },
                ) {
                    Text(
                        text = formatter.shortDayName(item.dayOfWeek),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (item.count > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth((item.count / max).toFloat().coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .background(accent, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = formatter.count(item.count),
                        style = MaterialTheme.typography.labelLarge.merge(NumberStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RangeDetailsCard(
    result: DayCountResult,
    formatter: DayCountFormatter,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel("Range details")
            DetailRow("FROM", formatter.fullDate(result.earlierDate))
            DetailRow("TO", formatter.fullDate(result.laterDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatter.inclusiveRange(result.totalDays),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                formatter.weekStructure(result)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) { }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun MessageCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isError) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { }
            ) {
                title?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            action?.invoke()
        }
    }
}
