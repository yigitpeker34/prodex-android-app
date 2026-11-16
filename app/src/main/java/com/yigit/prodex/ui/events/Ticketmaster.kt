package com.yigit.prodex.ui.events

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- 1. Ticketmaster Data Models ---
// Ticketmaster's JSON structure is complex (nested inside _embedded).
// These data classes help parse that structure.

data class TicketmasterResponse(
    val _embedded: EmbeddedEvents? // Can be null if no events found
)

data class EmbeddedEvents(
    val events: List<TMEvent>
)

data class TMEvent(
    val id: String,
    val name: String,
    val dates: TMDates,
    val _embedded: EmbeddedVenues?,
    val priceRanges: List<TMPrice>?
)

data class TMDates(
    val start: TMStart
)

data class TMStart(
    val localDate: String, // Example: "2026-05-20"
    val localTime: String? // Example: "20:00:00"
)

data class EmbeddedVenues(
    val venues: List<TMVenue>
)

data class TMVenue(
    val name: String,
    val city: TMCity,
    val country: TMCountry
)

data class TMCity(val name: String)
data class TMCountry(val name: String)

data class TMPrice(
    val min: Double,
    val max: Double,
    val currency: String
)

// --- 2. API Interface ---
interface TicketmasterApi {
    @GET("discovery/v2/events.json")
    suspend fun getEvents(
        @Query("apikey") apiKey: String,
        @Query("latlong") latlong: String, // Format: "52.5200,13.4050"
        @Query("radius") radius: String = "50",
        @Query("unit") unit: String = "km", // Using kilometers for Germany
        @Query("sort") sort: String = "date,asc" // Show nearest dates first
    ): TicketmasterResponse
}

// --- 3. Singleton Service Object ---
object TicketmasterClient {
    // Base URL for Ticketmaster API
    private const val BASE_URL = "https://app.ticketmaster.com/"

    val api: TicketmasterApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TicketmasterApi::class.java)
    }
}