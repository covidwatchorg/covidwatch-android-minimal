package org.covidwatch.android.ui

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import org.covidwatch.android.CovidWatchApplication
import org.covidwatch.android.R
import org.covidwatch.android.data.BluetoothViewModel

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var vm: BluetoothViewModel

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vm = ViewModelProvider(this).get(BluetoothViewModel::class.java)

        title = getString(R.string.title_discovered_tcns)

        initBluetoothAdapter()
        initLocationManager()
        requestIgnoreBatteryOptimizations()
    }

    public override fun onResume() {
        super.onResume()
        (application as? CovidWatchApplication)?.scheduleRefreshOneTime()
    }

    private fun startContactEventLogging() {
        val sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ) ?: return

        with(sharedPref.edit()) {
            putBoolean(
                getString(R.string.preference_is_temporary_contact_number_logging_enabled),
                true
            )
            commit()
        }
    }

    /**
     * Initializes the BluetoothAdapter. Manifest file is already setup to allow bluetooth access.
     * The user will be asked to enable bluetooth if it is turned off
     */
    private fun initBluetoothAdapter() {
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //TODO: Handle result
            startActivityForResult(enableBtIntent, 1)
        }
    }

    /**
     * Initializes the Location Manager used to obtain coarse bluetooth/wifi location
     * and fine GPS location, logged on a temporary contact number.
     *
     * TODO add GPS initialization here, for now we just ask for location permissions
     */
    private val LOCATION_REQUEST_CODE = 1
    private fun initLocationManager() {
        runOnUiThread {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            )

            when (permissionStatus) {
                PackageManager.PERMISSION_GRANTED -> vm.permissionRequestResultLiveData.value = true
                PackageManager.PERMISSION_DENIED -> {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            ACCESS_COARSE_LOCATION
                        )
                    ) {
                        Toast.makeText(
                            this,
                            getString(R.string.ble_location_permission),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //TODO: If a user selects "Don't ask again" the function below does not show the system dialogue.  This prevents the user from ever progressing. Maybe this is an OK UX?
                    val permissions = arrayOf(ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION)
                    ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE)
                }
            }
        }
    }

    private val BATTERY_REQUEST = 2
    private fun requestIgnoreBatteryOptimizations() {
        val powerManager =
            this.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        val packageName = this.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:$packageName")
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.i(TAG, "Not ignoring battery optimizations")
                this.startActivityForResult(
                    intent,
                    BATTERY_REQUEST
                )
            } else {
                Log.i(TAG, "Ignoring battery optimizations")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startContactEventLogging()
                    vm.permissionRequestResultLiveData.value = true
                }
            }
            else -> {
            }//Ignore all other requests
        }
    }
}
