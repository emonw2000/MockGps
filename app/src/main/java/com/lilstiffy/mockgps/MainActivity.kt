package com.lilstiffy.mockgps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.lilstiffy.mockgps.service.LocationHelper
import com.lilstiffy.mockgps.service.MockLocationService
import com.lilstiffy.mockgps.service.VibratorService
import com.lilstiffy.mockgps.ui.theme.MockGpsTheme

class MainActivity : ComponentActivity() {
    private var mockLocationService: MockLocationService? = null
        private set(value) {
            field = value
            MockLocationService.instance = value
        }

    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MockLocationService.MockLocationBinder
            mockLocationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
        }
    }

    fun toggleMocking(): Boolean {
        if (isBound && LocationHelper.hasPermission(this)) {
            mockLocationService?.toggleMocking()
            if (mockLocationService?.isMocking == true) {
                Toast.makeText(this, "Mocking location...", Toast.LENGTH_SHORT).show()
                VibratorService.vibrate()
                return true
            } else if (mockLocationService?.isMocking == false) {
                Toast.makeText(this, "Stopped mocking location...", Toast.LENGTH_SHORT).show()
                VibratorService.vibrate()
                return false
            }
        } else if (!isBound && LocationHelper.hasPermission(this))
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this, "No Location permission", Toast.LENGTH_SHORT).show()

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MockGpsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreenWithMapTypeSelector(activity = this)
                }
            }
        }

        // Start the service
        val serviceIntent = Intent(this, MockLocationService::class.java)
        startService(serviceIntent)

        // Bind to the service
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind from the service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun MapScreenWithMapTypeSelector(activity: MainActivity) {
    var mapType by remember { mutableStateOf(GoogleMap.MAP_TYPE_NORMAL) }

    Column(modifier = Modifier.fillMaxSize()) {
        MapTypeSelector(mapType = mapType, onMapTypeChange = { mapType = it })
        MapScreen(activity = activity, mapType = mapType)
    }
}

@Composable
fun MapScreen(activity: MainActivity, mapType: Int) {
    AndroidView(factory = { context ->
        val mapView = MapView(context)
        mapView.onCreate(null)
        mapView.getMapAsync { googleMap ->
            googleMap.mapType = mapType
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-6.2, 106.8), 10f)) // Jakarta
        }
        mapView
    }, update = { mapView ->
        mapView.getMapAsync { googleMap ->
            googleMap.mapType = mapType
        }
    })
}

@Composable
fun MapTypeSelector(mapType: Int, onMapTypeChange: (Int) -> Unit) {
    val mapTypes = listOf(
        "Normal" to GoogleMap.MAP_TYPE_NORMAL,
        "Satellite" to GoogleMap.MAP_TYPE_SATELLITE,
        "Hybrid" to GoogleMap.MAP_TYPE_HYBRID
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedMapType = mapTypes.firstOrNull { it.second == mapType }?.first ?: "Normal"

    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Button(onClick = { expanded = true }) {
            Text(text = "Map Type: $selectedMapType")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            mapTypes.forEach { (label, type) ->
                DropdownMenuItem(onClick = {
                    onMapTypeChange(type)
                    expanded = false
                }) {
                    Text(text = label)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MockGpsTheme {}
}
