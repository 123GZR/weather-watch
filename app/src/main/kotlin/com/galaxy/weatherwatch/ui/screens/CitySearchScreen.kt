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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Text
import com.galaxy.weatherwatch.data.model.City
import com.galaxy.weatherwatch.ui.components.LocationPinIcon

/**
 * 城市搜索/选择屏幕。
 *
 * 功能：
 * - 顶部搜索框：支持输入中文名、拼音、首字母缩写进行搜索
 * - GPS 定位项：点击后使用 GPS 定位（或回退默认城市）
 * - 城市列表：展示匹配的城市，点击即选择并返回
 * - 清空按钮：清除搜索内容
 * - 返回按钮：关闭搜索页
 * - 表圈旋转：控制列表滚动
 *
 * @param cities 搜索结果城市列表
 * @param onSearch 查询文本变化回调
 * @param onSelectCity 选择城市回调
 * @param onUseGps 点击 GPS 定位回调
 * @param onClose 关闭/返回回调
 * @param modifier 修饰符
 */
@Composable
fun CitySearchScreen(
    cities: List<City>,
    onSearch: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onUseGps: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    Box(modifier = modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(
                scrollableState = listState
            )
        ) {
            // 顶部标题栏：返回 + 标题
            item(key = "header") {
                HeaderRow(onClose = onClose)
            }

            // 搜索输入框
            item(key = "search-input") {
                SearchInputRow(
                    focusRequester = focusRequester,
                    onSearch = onSearch
                )
            }

            // GPS 定位项（始终显示在列表顶部）
            item(key = "gps-location") {
                GpsLocationChip(
                    onClick = onUseGps
                )
            }

            // 无结果提示
            if (cities.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        text = "未找到匹配的城市",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 城市列表
                cities.forEach { city ->
                    item(key = "city-${city.name}") {
                        CityChip(
                            city = city,
                            onClick = { onSelectCity(city) }
                        )
                    }
                }
            }
        }

        PositionIndicator(scalingLazyListState = listState)
    }

    // 进入页面时自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * 顶部标题栏：返回按钮 + 标题
 */
@Composable
private fun HeaderRow(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.wear.compose.material.Button(
            onClick = onClose,
            modifier = Modifier.height(32.dp),
            colors = androidx.wear.compose.material.ButtonDefaults.secondaryButtonColors()
        ) {
            Text(
                text = "返回",
                style = MaterialTheme.typography.button
            )
        }
        Text(
            text = "选择城市",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface
        )
        // 占位，保持标题居中
        Spacer(modifier = Modifier.width(56.dp))
    }
}

/**
 * 搜索输入行：定位图标 + 输入框 + 清空按钮
 */
@Composable
private fun SearchInputRow(
    focusRequester: FocusRequester,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 搜索/定位图标
        LocationPinIcon(sizeDp = 16)

        // 搜索输入框
        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                onSearch(newValue)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .height(40.dp),
            placeholder = {
                Text(
                    text = "搜索城市",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    androidx.wear.compose.material.Button(
                        onClick = {
                            query = ""
                            onSearch("")
                        },
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp),
                        colors = androidx.wear.compose.material.ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(
                            text = "清空",
                            style = MaterialTheme.typography.caption2
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colors.surface,
                unfocusedContainerColor = MaterialTheme.colors.surface,
                focusedIndicatorColor = MaterialTheme.colors.primary,
                unfocusedIndicatorColor = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colors.primary,
                focusedPlaceholderColor = MaterialTheme.colors.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colors.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colors.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colors.onSurfaceVariant
            ),
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { /* 由用户点击列表项完成选择 */ }
            )
        )
    }
}

/**
 * GPS 定位选项
 */
@Composable
private fun GpsLocationChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ChipDefaults.primaryChipColors(
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colors.primary
        ),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LocationPinIcon(sizeDp = 18)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS 定位",
                    style = MaterialTheme.typography.button,
                    color = MaterialTheme.colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

/**
 * 城市选项
 */
@Composable
private fun CityChip(
    city: City,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface
        ),
        label = {
            Text(
                text = city.name,
                style = MaterialTheme.typography.button,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
