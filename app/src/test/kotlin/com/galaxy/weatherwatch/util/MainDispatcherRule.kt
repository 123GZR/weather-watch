package com.galaxy.weatherwatch.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 把 `Dispatchers.Main` 替换为测试调度器。
 *
 * `viewModelScope` 依赖 `Dispatchers.Main.immediate`，在纯 JVM 单元测试中
 * 没有 Android Looper，会抛出 "Module with the Main dispatcher had failed to initialize"。
 *
 * 用法：
 * ```
 * @get:Rule val mainDispatcherRule = MainDispatcherRule()
 *
 * @Test fun x() = runTest(mainDispatcherRule.testDispatcher) { ... }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
