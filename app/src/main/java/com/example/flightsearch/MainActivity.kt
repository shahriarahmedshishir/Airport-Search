package com.example.flightsearch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightsearch.data.FlightDatabase
import com.example.flightsearch.data.UserPreferencesRepository
import com.example.flightsearch.ui.FlightScreen
import com.example.flightsearch.ui.FlightSearchViewModel
import com.example.flightsearch.ui.theme.FlightSearchTheme

private const val LAYOUT_PREFERENCE_NAME = "layout_preferences"
private val Context.dataStore by preferencesDataStore(name = LAYOUT_PREFERENCE_NAME)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = FlightDatabase.getDatabase(applicationContext)
        val airportDao = database.airportDao()
        val userPreferencesRepository = UserPreferencesRepository(dataStore)

        setContent {
            FlightSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FlightSearchViewModel = viewModel(
                        factory = FlightSearchViewModel.provideFactory(
                            airportDao = airportDao,
                            userPreferencesRepository = userPreferencesRepository
                        )
                    )
                    FlightScreen(viewModel = viewModel)
                }
            }
        }
    }
}
