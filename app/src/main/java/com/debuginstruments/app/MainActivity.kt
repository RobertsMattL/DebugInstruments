package com.debuginstruments.app

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.RandomAccessFile
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var mapView: MapView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var batteryManager: BatteryManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var activityManager: ActivityManager

    // Accelerometer
    private lateinit var tvAccelX: TextView
    private lateinit var tvAccelY: TextView
    private lateinit var tvAccelZ: TextView

    // Gyroscope
    private lateinit var tvGyroX: TextView
    private lateinit var tvGyroY: TextView
    private lateinit var tvGyroZ: TextView

    // Magnetometer
    private lateinit var tvMagX: TextView
    private lateinit var tvMagY: TextView
    private lateinit var tvMagZ: TextView

    // Environmental sensors
    private lateinit var tvLight: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvProximity: TextView

    // Location data
    private lateinit var tvLat: TextView
    private lateinit var tvLng: TextView
    private lateinit var tvElev: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvBearing: TextView
    private lateinit var tvAccuracy: TextView

    // Battery info
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvBatteryTemp: TextView
    private lateinit var tvBatteryVoltage: TextView

    // Network info
    private lateinit var tvNetworkType: TextView
    private lateinit var tvSignalStrength: TextView

    // System info
    private lateinit var tvMemoryUsage: TextView
    private lateinit var tvCpuUsage: TextView
    private lateinit var tvStorageUsage: TextView

    private var lastCpuTime = 0L
    private var lastAppCpuTime = 0L

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val PHONE_STATE_PERMISSION_REQUEST = 1002
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryInfo(it) }
        }
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)
            updateSignalStrength()
        }
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
        Log.d("MainActivity", "test log line added for debugging")

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "Test log: MainActivity created")

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.USGS_SAT)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(5.0)
        mapController.setCenter(GeoPoint(39.8283, -98.5795))

        // Initialize TextViews - Accelerometer
        tvAccelX = findViewById(R.id.tvAccelX)
        tvAccelY = findViewById(R.id.tvAccelY)
        tvAccelZ = findViewById(R.id.tvAccelZ)

        // Gyroscope
        tvGyroX = findViewById(R.id.tvGyroX)
        tvGyroY = findViewById(R.id.tvGyroY)
        tvGyroZ = findViewById(R.id.tvGyroZ)

        // Magnetometer
        tvMagX = findViewById(R.id.tvMagX)
        tvMagY = findViewById(R.id.tvMagY)
        tvMagZ = findViewById(R.id.tvMagZ)

        // Environmental
        tvLight = findViewById(R.id.tvLight)
        tvPressure = findViewById(R.id.tvPressure)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvProximity = findViewById(R.id.tvProximity)

        // Location
        tvLat = findViewById(R.id.tvLat)
        tvLng = findViewById(R.id.tvLng)
        tvElev = findViewById(R.id.tvElev)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvBearing = findViewById(R.id.tvBearing)
        tvAccuracy = findViewById(R.id.tvAccuracy)

        // Battery
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvBatteryTemp = findViewById(R.id.tvBatteryTemp)
        tvBatteryVoltage = findViewById(R.id.tvBatteryVoltage)
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus)

        // Network
        tvNetworkType = findViewById(R.id.tvNetworkType)
        tvSignalStrength = findViewById(R.id.tvSignalStrength)

        // System
        tvMemoryUsage = findViewById(R.id.tvMemoryUsage)
        tvCpuUsage = findViewById(R.id.tvCpuUsage)
        tvStorageUsage = findViewById(R.id.tvStorageUsage)

        // Initialize system services
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // Register battery receiver
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Request phone state permission for signal strength
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PHONE_STATE_PERMISSION_REQUEST
            )
        }

        requestLocationUpdates()
        updateNetworkInfo()
        updateSystemInfo()
        Log.d("MainActivity", "Test log: onCreate completed successfully")
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
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates()
                }
            }
            PHONE_STATE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()

        // Register all available sensors
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering battery receiver", e)
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    // SensorEventListener
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                tvAccelX.text = String.format(Locale.US, "ACC X: %7.2f m/s²", event.values[0])
                tvAccelY.text = String.format(Locale.US, "ACC Y: %7.2f m/s²", event.values[1])
                tvAccelZ.text = String.format(Locale.US, "ACC Z: %7.2f m/s²", event.values[2])
            }
            Sensor.TYPE_GYROSCOPE -> {
                tvGyroX.text = String.format(Locale.US, "GYRO X: %6.2f rad/s", event.values[0])
                tvGyroY.text = String.format(Locale.US, "GYRO Y: %6.2f rad/s", event.values[1])
                tvGyroZ.text = String.format(Locale.US, "GYRO Z: %6.2f rad/s", event.values[2])
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                tvMagX.text = String.format(Locale.US, "MAG X: %7.1f µT", event.values[0])
                tvMagY.text = String.format(Locale.US, "MAG Y: %7.1f µT", event.values[1])
                tvMagZ.text = String.format(Locale.US, "MAG Z: %7.1f µT", event.values[2])
            }
            Sensor.TYPE_LIGHT -> {
                tvLight.text = String.format(Locale.US, "LIGHT: %8.1f lux", event.values[0])
            }
            Sensor.TYPE_PRESSURE -> {
                tvPressure.text = String.format(Locale.US, "PRESS: %7.2f hPa", event.values[0])
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                tvTemperature.text = String.format(Locale.US, "TEMP:  %6.1f °C", event.values[0])
            }
            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                tvHumidity.text = String.format(Locale.US, "HUMID: %6.1f %%", event.values[0])
            }
            Sensor.TYPE_PROXIMITY -> {
                tvProximity.text = String.format(Locale.US, "PROX:  %6.1f cm", event.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LocationListener
    override fun onLocationChanged(location: Location) {
        updateLocationUI(location)
    }

    private fun updateLocationUI(location: Location) {
        tvLat.text = String.format(Locale.US, "LAT:  %11.6f°", location.latitude)
        tvLng.text = String.format(Locale.US, "LNG:  %11.6f°", location.longitude)
        tvElev.text = String.format(Locale.US, "ALT:  %8.1f m", location.altitude)

        if (location.hasSpeed()) {
            tvSpeed.text = String.format(Locale.US, "SPD:  %6.2f m/s", location.speed)
        } else {
            tvSpeed.text = "SPD:       -- m/s"
        }

        if (location.hasBearing()) {
            tvBearing.text = String.format(Locale.US, "BEAR: %6.1f°", location.bearing)
        } else {
            tvBearing.text = "BEAR:      --°"
        }

        if (location.hasAccuracy()) {
            tvAccuracy.text = String.format(Locale.US, "ACC:  %6.1f m", location.accuracy)
        } else {
            tvAccuracy.text = "ACC:       -- m"
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val batteryPct = if (scale > 0) (level * 100 / scale.toFloat()) else 0f

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "CHG"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DIS"
            BatteryManager.BATTERY_STATUS_FULL -> "FULL"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT"
            else -> "UNK"
        }

        tvBatteryLevel.text = String.format(Locale.US, "BAT:  %5.1f%% (%s)", batteryPct, statusText)
        tvBatteryTemp.text = String.format(Locale.US, "B_TMP: %5.1f °C", temperature)
        tvBatteryVoltage.text = String.format(Locale.US, "B_VOL: %4d mV", voltage)
    }

    private fun updateNetworkInfo() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val networkType = when {
            capabilities == null -> "NONE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETH"
            else -> "OTHER"
        }

        tvNetworkType.text = String.format(Locale.US, "NET: %s", networkType)
    }

    private fun updateSignalStrength() {
        // This will be updated by the phone state listener
        // For now, just show a placeholder
        tvSignalStrength.text = "SIG: -- dBm"
    }

    private fun updateSystemInfo() {
        // Memory usage
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val usedMemoryMB = (memoryInfo.totalMem - memoryInfo.availMem) / (1024 * 1024)
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        tvMemoryUsage.text = String.format(Locale.US, "MEM: %d/%d MB", usedMemoryMB, totalMemoryMB)

        // CPU usage
        val cpuUsage = getCpuUsage()
        tvCpuUsage.text = String.format(Locale.US, "CPU: %5.1f%%", cpuUsage)

        // Storage usage
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val usedGB = (totalBytes - availableBytes) / (1024 * 1024 * 1024)
        val totalGB = totalBytes / (1024 * 1024 * 1024)
        tvStorageUsage.text = String.format(Locale.US, "STOR: %d/%d GB", usedGB, totalGB)
    }

    private fun getCpuUsage(): Float {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" +".toRegex())
            val idle = toks[4].toLong()
            val cpu = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() +
                     toks[5].toLong() + toks[6].toLong() + toks[7].toLong()

            val usage = if (lastCpuTime > 0) {
                val cpuDelta = cpu - lastAppCpuTime
                val totalDelta = (cpu + idle) - lastCpuTime
                if (totalDelta > 0) (cpuDelta.toFloat() / totalDelta.toFloat()) * 100f else 0f
            } else {
                0f
            }

            lastCpuTime = cpu + idle
            lastAppCpuTime = cpu

            return usage
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading CPU usage", e)
            return 0f
        }
    }
}
