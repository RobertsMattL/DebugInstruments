package com.debuginstruments.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
<<<<<<< HEAD
import android.util.Log
=======
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
>>>>>>> b48507636e200eabbf17ff2c8f6331a233f74528
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mapView: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

    private lateinit var tvAccelX: TextView
    private lateinit var tvAccelY: TextView
    private lateinit var tvAccelZ: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLng: TextView
    private lateinit var tvElev: TextView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called - test log")

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.USGS_SAT)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(5.0)
        mapController.setCenter(GeoPoint(39.8283, -98.5795))

        tvAccelX = findViewById(R.id.tvAccelX)
        tvAccelY = findViewById(R.id.tvAccelY)
        tvAccelZ = findViewById(R.id.tvAccelZ)
        tvLat = findViewById(R.id.tvLat)
        tvLng = findViewById(R.id.tvLng)
        tvElev = findViewById(R.id.tvElev)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { updateLocationUI(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    // SensorEventListener
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            tvAccelX.text = String.format(Locale.US, "ACC X: %7.2f", event.values[0])
            tvAccelY.text = String.format(Locale.US, "ACC Y: %7.2f", event.values[1])
            tvAccelZ.text = String.format(Locale.US, "ACC Z: %7.2f", event.values[2])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LocationListener
    override fun onLocationChanged(location: Location) {
        updateLocationUI(location)
    }

    private fun updateLocationUI(location: Location) {
        tvLat.text = String.format(Locale.US, "LAT:  %11.6f", location.latitude)
        tvLng.text = String.format(Locale.US, "LNG:  %11.6f", location.longitude)
        tvElev.text = String.format(Locale.US, "ELEV: %8.1f m", location.altitude)
    }
}
