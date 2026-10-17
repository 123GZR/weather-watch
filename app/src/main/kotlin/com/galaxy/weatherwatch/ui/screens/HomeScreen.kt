package com.galaxy.weatherwatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.galaxy.weatherwatch.data.model.CurrentWeather
import com.galaxy.weatherwatch.data.model.DailySummary
import com.galaxy.weatherwatch.data.model.HourlyForecast
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import com.galaxy.weatherwatch.ui.components.LocationPinIcon
import com.galaxy.weatherwatch.ui.components.WeatherIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 静态共享时间格式化器：DateTimeFormatter 线程安全可复用，
 * 避免每次刷新/滚动重组时重复创建格式化器与时区查询。
 */
private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val HOUR_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH").withZone(ZoneId.systemDefault())

/**
 * 首页（成功态）：TransformingLazyColumn 列表。
 * 位置入口为列表首项（点击进入城市搜索）；刷新入口为右侧 EdgeButton。
 */
@Composable
fun HomeScreen(
    snapshot: WeatherSnapshot,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        modifier = modifier,
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("刷新")
                }
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // 旋转表圈输入：TransformingLazyColumn 内部会用 rememberActiveFocusRequester()
            // 绑定 focusRequester，这里只需提供 snap 行为。
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            // 位置入口：列表首项，点击进入城市搜索页
            item(key = "location-header") {
                LocationHeader(locationName = snapshot.locationName, onClick = onLocationClick)
            }
            item(key = "current-header") { CurrentWeatherHeader(snapshot.current) }
            item(key = "today-summary") { TodaySummaryRow(snapshot.today) }
            item(key = "hourly-title") {
                ListHeader {
                    Text(
                        text = "逐时预报",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // 稳定 key：刷新后同一小时的 item 直接跳过重组，只重组数据变化的行
            snapshot.hourly.forEach { forecast ->
                item(key = "hourly-${forecast.epochSecond}") { HourlyForecastRow(forecast) }
            }
            item(key = "updated-at") {
                UpdatedAtText(updatedAt = snapshot.current.updatedAt)
            }
        }
    }
}

/**
 * 位置入口：以列表首项形式展示当前位置，点击进入城市搜索页。
 */
@Composable
private fun LocationHeader(locationName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocationPinIcon(sizeDp = 16)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = locationName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CurrentWeatherHeader(current: CurrentWeather) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIcon(icon = current.icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${current.temp.toInt()}°",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "体感 ${current.feelsLike.toInt()}° · ${current.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 「今日」摘要卡：非交互卡片（NonClickableCard 语义） */
@Composable
private fun TodaySummaryRow(today: DailySummary) {
    TitleCard(
        title = { Text("今日") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryCell(label = "最高", value = "${today.maxTemp.toInt()}°")
            SummaryCell(label = "最低", value = "${today.minTemp.toInt()}°")
            SummaryCell(label = "降水", value = "${(today.precipProbability * 100).toInt()}%")
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 列表末项：数据更新时间（刷新动作已由右侧 EdgeButton 承担） */
@Composable
private fun UpdatedAtText(updatedAt: Long) {
    val time = remember(updatedAt) { TIME_FORMATTER.format(Instant.ofEpochMilli(updatedAt)) }
    Text(
        text = "更新于 $time",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}

@Composable
private fun HourlyForecastRow(item: HourlyForecast) {
    val time = remember(item.epochSecond) { HOUR_FORMATTER.format(Instant.ofEpochSecond(item.epochSecond)) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$time 时", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        WeatherIcon(icon = item.icon)
        Text(text = "${item.temp.toInt()}°", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = if (item.precipProbability > 0.05) "${(item.precipProbability * 100).toInt()}%" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
