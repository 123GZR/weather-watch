package com.galaxy.weatherwatch.data

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FusedLocationRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    /**
     * 创建一个模拟失败的 Task<Location>
     */
    private fun mockFailureTask(exception: Exception): Task<Location> {
        val task = mockk<Task<Location>>(relaxed = true)
        every { task.isComplete } returns true
        every { task.isSuccessful } returns false
        every { task.exception } returns exception
        every { task.addOnCompleteListener(any<OnCompleteListener<Location>>()) } answers {
            val listener = arg<OnCompleteListener<Location>>(0)
            listener.onComplete(task)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            val listener = arg<OnFailureListener>(0)
            listener.onFailure(exception)
            task
        }
        return task
    }

    @Test
    fun `getCurrentLocation - 定位失败时返回 LocationUnavailableException`() = runTest(testDispatcher) {
        val fusedClient = mockk<FusedLocationProviderClient>(relaxed = true)
        every { fusedClient.lastLocation } returns mockFailureTask(RuntimeException("location failed"))

        val repo = FusedLocationRepository(fusedClient, testDispatcher)
        val result = repo.getCurrentLocation()

        assertTrue("should be failure", result.isFailure)
        assertTrue(
            "should be LocationUnavailableException",
            result.exceptionOrNull() is LocationUnavailableException
        )
    }

    @Test
    fun `getCurrentLocation - 权限缺失时返回 LocationPermissionDeniedException`() = runTest(testDispatcher) {
        val fusedClient = mockk<FusedLocationProviderClient>(relaxed = true)
        every { fusedClient.lastLocation } throws SecurityException("permission denied")

        val repo = FusedLocationRepository(fusedClient, testDispatcher)
        val result = repo.getCurrentLocation()

        assertTrue("should be failure", result.isFailure)
        assertTrue(
            "should be LocationPermissionDeniedException",
            result.exceptionOrNull() is LocationPermissionDeniedException
        )
    }
}
