package com.kdelehoi.marshrutky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kdelehoi.marshrutky.ui.navigation.MarshrutkyNavGraph
import com.kdelehoi.marshrutky.ui.theme.MarshrutkyTheme
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val scheduleViewModel: ScheduleViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarshrutkyTheme {
                MarshrutkyNavGraph(viewModel = scheduleViewModel)
            }
        }
    }
}
