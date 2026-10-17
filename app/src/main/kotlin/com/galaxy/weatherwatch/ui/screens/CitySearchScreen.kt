package com.galaxy.weatherwatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.galaxy.weatherwatch.data.model.City
import com.galaxy.weatherwatch.ui.components.LocationPinIcon

/**
 * 城市搜索/选择屏幕。
 *
 * 功能：
 * - 顶部标题栏：返回按钮（M3 TextButton）+ 标题；右滑返回由 SwipeDismissableNavHost 提供
 * - 搜索框：支持输入中文名、拼音、首字母缩写进行搜索
 * - GPS 定位项：点击后使用 GPS 定位（或回退记忆城市）
 * - 城市列表：ChildButton 展示匹配城市，点击即选择并返回
 * - 清空按钮：清除搜索内容
 * - 表圈旋转：TransformingLazyColumn 滚动
 *
 * @param cities 搜索结果城市列表
 * @param onSearch 查询文本变化回调
 * @param onSelectCity 选择城市回调
 * @param onUseGps 点击 GPS 定位回调
 * @param onBack 返回回调（右滑/按钮返回均由导航层处理）
 * @param modifier 修饰符
 */
@Composable
fun CitySearchScreen(
    cities: List<City>,
    onSearch: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onUseGps: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }

    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            // 旋转表圈输入：TransformingLazyColumn 内部会用 rememberActiveFocusRequester()
            // 绑定 focusRequester，这里只需提供 snap 行为。
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = listState)
        ) {
            // 顶部标题栏：返回 + 标题
            item(key = "header") {
                HeaderRow(onBack = onBack)
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
                GpsLocationButton(onClick = onUseGps)
            }

            // 无结果提示
            if (cities.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        text = "未找到匹配的城市",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        CityItem(
                            city = city,
                            onClick = { onSelectCity(city) }
                        )
                    }
                }
            }
        }
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "返回",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = "选择城市",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 占位，保持标题居中
        Spacer(modifier = Modifier.width(64.dp))
    }
}

/**
 * 搜索输入行：定位图标 + 输入框 + 清空按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            query = ""
                            onSearch("")
                        },
                        modifier = Modifier
                            .height(28.dp)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "清空",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
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
 * GPS 定位选项：M3 Button（primary 变体，透明度底色）
 */
@Composable
private fun GpsLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        LocationPinIcon(sizeDp = 18)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "GPS 定位",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 城市选项：M3 ChildButton
 */
@Composable
private fun CityItem(
    city: City,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChildButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = city.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
