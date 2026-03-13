package com.debuginstruments.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BluetoothPairingActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var scanning = false

    private lateinit var btnToggleScan: Button
    private lateinit var tvScanStatus: TextView
    private lateinit var rvDevices: RecyclerView

    private val devices = mutableListOf<BleDevice>()
    private val deviceAddresses = mutableSetOf<String>()
    private lateinit var adapter: BleDeviceAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val scanPeriod = 30_000L // 30 second scan window

    data class BleDevice(val name: String, val address: String, var rssi: Int)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startScan()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_pairing)

        supportActionBar?.title = "BLE Devices"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnToggleScan = findViewById(R.id.btnToggleScan)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        rvDevices = findViewById(R.id.rvDevices)

        adapter = BleDeviceAdapter(devices)
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = adapter

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            tvScanStatus.text = "BLE not supported on this device"
            btnToggleScan.isEnabled = false
            return
        }

        btnToggleScan.setOnClickListener {
            if (scanning) stopScan() else checkPermissionsAndScan()
        }

        checkPermissionsAndScan()
    }

    private fun checkPermissionsAndScan() {
        val neededPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            startScan()
        }
    }

    private fun startScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            tvScanStatus.text = "Bluetooth is disabled"
            return
        }

        bleScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bleScanner == null) {
            tvScanStatus.text = "BLE scanner unavailable"
            return
        }

        devices.clear()
        deviceAddresses.clear()
        adapter.notifyDataSetChanged()

        try {
            bleScanner?.startScan(scanCallback)
            scanning = true
            tvScanStatus.text = "Scanning..."
            btnToggleScan.text = "STOP"

            handler.postDelayed({ stopScan() }, scanPeriod)
        } catch (e: SecurityException) {
            tvScanStatus.text = "Permission denied"
        }
    }

    private fun stopScan() {
        if (!scanning) return
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) { }
        scanning = false
        tvScanStatus.text = "Found ${devices.size} device(s)"
        btnToggleScan.text = "SCAN"
        handler.removeCallbacksAndMessages(null)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            val name = try {
                result.device.name ?: "Unknown"
            } catch (_: SecurityException) {
                "Unknown"
            }
            val rssi = result.rssi

            runOnUiThread {
                val existing = devices.indexOfFirst { it.address == address }
                if (existing >= 0) {
                    devices[existing] = devices[existing].copy(rssi = rssi)
                    adapter.notifyItemChanged(existing)
                } else {
                    deviceAddresses.add(address)
                    devices.add(BleDevice(name, address, rssi))
                    devices.sortByDescending { it.rssi }
                    adapter.notifyDataSetChanged()
                    tvScanStatus.text = "Scanning... (${devices.size} found)"
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                tvScanStatus.text = "Scan failed (error $errorCode)"
                scanning = false
                btnToggleScan.text = "SCAN"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }

    // RecyclerView adapter
    inner class BleDeviceAdapter(
        private val items: List<BleDevice>
    ) : RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {

        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvDeviceName)
            val tvAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
            val tvRssi: TextView = view.findViewById(R.id.tvDeviceRssi)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ble_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = items[position]
            holder.tvName.text = device.name
            holder.tvAddress.text = device.address
            holder.tvRssi.text = "${device.rssi} dBm"
        }

        override fun getItemCount() = items.size
    }
}
