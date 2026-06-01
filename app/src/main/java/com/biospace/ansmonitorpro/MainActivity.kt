package com.biospace.ansmonitorpro

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.biospace.ansmonitorpro.ui.screens.AppNavigation
import com.biospace.ansmonitorpro.ui.theme.ANSMonitorTheme
import com.biospace.ansmonitorpro.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            doGetLocation()
        }
        // If denied, ViewModel uses saved/default coordinates
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestLocation()
        setContent {
            ANSMonitorTheme {
                AppNavigation(vm)
            }
        }
    }

    private fun checkAndRequestLocation() {
        val fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            doGetLocation()
        } else {
            permLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun doGetLocation() {
        if (vm.uiState.value.settings.useGps) {
            try {
                LocationServices.getFusedLocationProviderClient(this)
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            val city = try {
                                Geocoder(this, Locale.getDefault())
                                    .getFromLocation(loc.latitude, loc.longitude, 1)
                                    ?.firstOrNull()
                                    ?.let { "${it.locality ?: it.subAdminArea ?: "Unknown"}, ${it.adminArea ?: ""}" }
                                    ?: "%.4f°, %.4f°".format(loc.latitude, loc.longitude)
                            } catch (e: Exception) {
                                "%.4f°, %.4f°".format(loc.latitude, loc.longitude)
                            }
                            vm.setLocation(loc.latitude, loc.longitude, city)
                        }
                    }
            } catch (e: SecurityException) { /* no-op, uses saved coords */ }
        }
    }
}
