package com.galaxy.weatherwatch.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 在手表（或模拟器）上生成 Baseline Profile。
 *
 * 运行方式（手表需开启 ADB/无线调试并连接）：
 *   ./gradlew :baselineprofile:generateBaselineProfile
 *
 * 生成结果会打包进 app 模块，debug/release 安装后由 profileinstaller
 * 在首次启动时预编译热路径，缓解手表上的首帧与滚动卡顿。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.galaxy.weatherwatch",
        maxIterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // 滚动逐时预报列表，覆盖 TransformingLazyColumn（Wear Compose M3）的组合与绘制热路径
        val list = device.findObject(By.scrollable(true))
        if (list != null) {
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }
    }
}
