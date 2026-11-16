package com.yigit.prodex.ui.events

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

// Simple UI Model for display purposes
data class UiEvent(
    val id: String,
    val title: String,
    val date: String,
    val location: String,
    val price: String
)

@Composable
fun EventFinderScreen() {
    val context = LocalContext.current

    // Check Location Permission
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    // Main Container (Dark Background)
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        if (hasLocationPermission) {
            EventListSection(context)
        } else {
            PermissionRequestSection { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        }
    }
}

@Composable
fun PermissionRequestSection(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("Berlin Events", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text("We need location access to find events in Germany.", fontSize = 16.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Allow Access", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun EventListSection(context: Context) {
    var events by remember { mutableStateOf<List<UiEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🔥🔥 IMPORTANT: PASTE YOUR TICKETMASTER API KEY HERE 🔥🔥
    val TM_API_KEY = "YOUR_TICKETMASTER_API_KEY"

    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchEvents(lat: Double, lon: Double) {
        scope.launch {
            try {
                // Convert coordinates to "52.52,13.40" format
                val latlong = "$lat,$lon"

                // Call API
                val response = TicketmasterClient.api.getEvents(TM_API_KEY, latlong)

                // Get the raw list, handled null safety
                val rawEvents = response._embedded?.events ?: emptyList()

                if (rawEvents.isEmpty()) {
                    errorMessage = "No events found nearby (Check API Key or Location)."
                } else {
                    // Map API data to UI model
                    events = rawEvents.map { tmEvent ->
                        val venue = tmEvent._embedded?.venues?.firstOrNull()
                        val price = tmEvent.priceRanges?.firstOrNull()

                        UiEvent(
                            id = tmEvent.id,
                            title = tmEvent.name,
                            date = "${tmEvent.dates.start.localDate} • ${tmEvent.dates.start.localTime ?: ""}",
                            location = "${venue?.city?.name ?: "Berlin"}, ${venue?.name ?: "Unknown Venue"}",
                            price = if (price != null) "€${price.min}" else "See Details"
                        )
                    }
                    errorMessage = null
                }
                isLoading = false
            } catch (e: Exception) {
                Log.e("EventFinder", "API Error: ${e.message}")
                errorMessage = "Connection Error: ${e.localizedMessage}"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // --- IMPORTANT: HARDCODING BERLIN COORDINATES FOR TESTING ---
        // Android Emulators default to San Francisco (Mountain View).
        // We force Berlin coordinates to ensure we see German events.
        Log.d("EventFinder", "Using Berlin coordinates manually.")

        // Berlin Coordinates: 52.5200, 13.4050
        fetchEvents(52.5200, 13.4050)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Events in Berlin 🇩🇪", modifier = Modifier.padding(24.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFBB86FC)) }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(20.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(events) { event -> EventCard(event) }
            }
        }
    }
}

@Composable
fun EventCard(event: UiEvent) {
    Card(
        modifier = Modifier.fillMaxWidth().height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(event.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(event.location, fontSize = 14.sp, color = Color.DarkGray, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.date, fontSize = 13.sp, color = Color.Gray)
                Text(event.price, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
            }
        }
    }
}