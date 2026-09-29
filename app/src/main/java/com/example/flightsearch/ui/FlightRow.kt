package com.example.flightsearch.ui

import com.example.flightsearch.data.Airport

data class FlightRow(
    val departureAirport: Airport,
    val destinationAirport: Airport,
    val isFavorite: Boolean
)
