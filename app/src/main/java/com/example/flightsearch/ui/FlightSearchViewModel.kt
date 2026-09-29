package com.example.flightsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.flightsearch.data.Airport
import com.example.flightsearch.data.AirportDao
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FlightSearchViewModel(
    private val airportDao: AirportDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val selectedAirport: MutableStateFlow<Airport?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            userPreferencesRepository.userSearchQuery.collect { savedQuery ->
                if (searchQuery.value.isEmpty() && savedQuery.isNotEmpty()) {
                    searchQuery.value = savedQuery
                }
            }
        }
    }

    val searchResults: StateFlow<List<Airport>> = searchQuery
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                flowOf(emptyList())
            } else {
                airportDao.getAirportsByQuery(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val destinationFlights: StateFlow<List<FlightRow>> = combine(
        selectedAirport,
        airportDao.getAllFavorites()
    ) { airport, favorites ->
        Pair(airport, favorites)
    }.flatMapLatest { (airport, favorites) ->
        if (airport == null) {
            flowOf(emptyList())
        } else {
            val favSet = favorites.map { "${it.departureCode}-${it.destinationCode}" }.toSet()
            airportDao.getDestinationsForDeparture(airport.iataCode).map { destinations ->
                destinations.map { destination ->
                    FlightRow(
                        departureAirport = airport,
                        destinationAirport = destination,
                        isFavorite = favSet.contains("${airport.iataCode}-${destination.iataCode}")
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteFlights: StateFlow<List<FlightRow>> = airportDao.getAllFavorites()
        .map { favorites ->
            favorites.mapNotNull { favorite ->
                val dep = airportDao.getAirportByIataCode(favorite.departureCode)
                val dest = airportDao.getAirportByIataCode(favorite.destinationCode)
                if (dep != null && dest != null) {
                    FlightRow(
                        departureAirport = dep,
                        destinationAirport = dest,
                        isFavorite = true
                    )
                } else null
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        selectedAirport.value = null
        viewModelScope.launch {
            userPreferencesRepository.saveSearchQuery(query)
        }
    }

    fun selectAirport(airport: Airport) {
        selectedAirport.value = airport
    }

    fun clearSearch() {
        searchQuery.value = ""
        selectedAirport.value = null
        viewModelScope.launch {
            userPreferencesRepository.saveSearchQuery("")
        }
    }

    fun toggleFavorite(departureCode: String, destinationCode: String, isCurrentFavorite: Boolean) {
        viewModelScope.launch {
            if (isCurrentFavorite) {
                airportDao.deleteFavorite(departureCode, destinationCode)
            } else {
                airportDao.insertFavorite(
                    Favorite(departureCode = departureCode, destinationCode = destinationCode)
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            airportDao: AirportDao,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FlightSearchViewModel(airportDao, userPreferencesRepository)
            }
        }
    }
}
