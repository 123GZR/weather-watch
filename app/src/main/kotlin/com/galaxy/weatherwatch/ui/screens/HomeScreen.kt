package com.galaxy.weatherwatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import com.galaxy.weatherwatch.data.model.CurrentWeather
import com.galaxy.weatherwatch.data.model.DailySummary
import com.galaxy.weatherwatch.data.model.HourlyForecast
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import com.galaxy.weatherwatch.ui.components.WeatherIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    snapshot: WeatherSnapshot,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // 旋转表圈输入：ScalingLazyColumn 内部会用 rememberActiveFocusRequester()
            // 绑定 focusRequester，这里只需提供 snap 行为。
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollableState = listState)
        ) {
            item { CurrentWeatherHeader(snapshot.current) }
            item { TodaySummaryRow(snapshot.today) }
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                Text(
                    text = "逐时预报",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface
                )
            }
            snapshot.hourly.forEach { forecast ->
                item { HourlyForecastRow(forecast) }
            }
            item {
                RefreshFooter(
                    updatedAt = snapshot.current.updatedAt,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
            }
        }
        PositionIndicator(scalingLazyListState = listState)
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
                style = MaterialTheme.typography.display2,
                color = MaterialTheme.colors.onSurface
            )
        }
        Text(
            text = "体感 ${current.feelsLike.toInt()}° · ${current.description}",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TodaySummaryRow(today: DailySummary) {
    TitleCard(
        onClick = { },
        title = { Text("今日") }
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
        Text(text = value, style = MaterialTheme.typography.title2, color = MaterialTheme.colors.onSurface)
        Text(text = label, style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun RefreshFooter(
    updatedAt: Long,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val time = remember(updatedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updatedAt))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "更新于 $time",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator()
            } else {
                Text("刷新")
            }
        }
    }
}

@Composable
private fun HourlyForecastRow(item: HourlyForecast) {
    val time = remember(item.epochSecond) {
        SimpleDateFormat("HH", Locale.getDefault()).format(Date(item.epochSecond * 1000))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$time 时", style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onSurface)
        WeatherIcon(icon = item.icon)
        Text(text = "${item.temp.toInt()}°", style = MaterialTheme.typography.title2, color = MaterialTheme.colors.onSurface)
        Text(
            text = if (item.precipProbability > 0.05) "${(item.precipProbability * 100).toInt()}%" else "—",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}
