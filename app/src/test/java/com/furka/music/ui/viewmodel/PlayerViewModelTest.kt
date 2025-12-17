package com.furka.music.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.media3.session.MediaController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PlayerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var mediaController: MediaController

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(mediaController.duration).thenReturn(100000L) // 100 seconds
        viewModel = PlayerViewModel(application, mediaController)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSliderChange updates position correctly`() = runTest {
        val sliderValue = 0.5f
        val expectedPosition = 50000f // 50% of 100,000ms
        viewModel.onSliderChange(sliderValue)
        val uiState = viewModel.uiState.value
        assert(uiState.currentPosition == expectedPosition)
    }

    @Test
    fun `onSliderChangeFinished seeks to correct position`() = runTest {
        val sliderValue = 0.75f
        val expectedSeekPosition = 75000L // 75% of 100,000ms
        viewModel.onSliderChangeFinished(sliderValue)
        org.mockito.Mockito.verify(mediaController).seekTo(expectedSeekPosition)
    }
}
