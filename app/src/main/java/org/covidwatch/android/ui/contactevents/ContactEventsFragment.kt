package org.covidwatch.android.ui.contactevents

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.covidwatch.android.R
import org.covidwatch.android.data.BluetoothViewModel
import org.covidwatch.android.ui.contactevents.adapters.FragmentDataBindingComponent
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.databinding.FragmentContactEventsBinding


class ContactEventsFragment : Fragment() {

    private lateinit var contactEventsViewModel: ContactEventsViewModel
    private lateinit var vm: BluetoothViewModel
    private lateinit var binding: FragmentContactEventsBinding
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = context ?: return null

        val database = CovidWatchDatabase.getInstance(context)
        val viewModel: ContactEventsViewModel by viewModels(factoryProducer = {
            ContactEventsViewModelFactory(database.contactEventDAO(), context.applicationContext as Application)
        })
        contactEventsViewModel = viewModel
        vm = ViewModelProvider(this).get(BluetoothViewModel::class.java)

        binding =
            DataBindingUtil.inflate<FragmentContactEventsBinding>(
                inflater,
                R.layout.fragment_contact_events,
                container,
                false,
                dataBindingComponent
            ).apply {
                lifecycleOwner = this@ContactEventsFragment
            }

        val adapter = ContactEventsAdapter()
        binding.contactEventsRecyclerview.adapter = adapter
        binding.contactEventsRecyclerview.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        viewModel.contactEvents.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val isContactEventLoggingEnabled =
            contactEventsViewModel.isContactEventLoggingEnabled.value ?: false
        if (isContactEventLoggingEnabled) {
            inflater.inflate(R.menu.menu_contact_events_stop, menu)
        } else {
            inflater.inflate(R.menu.menu_contact_events_start, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.self_report -> {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setMessage(R.string.message_dialog_self_report)
                    builder.apply {
                        setPositiveButton(getString(R.string.title_upload),
                            DialogInterface.OnClickListener { dialog, id ->
                                // TODO: Generate and upload report
                            })
                        setNegativeButton(getString(R.string.title_cancel),
                            DialogInterface.OnClickListener { dialog, id ->
                                // User cancelled the dialog
                            })
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
            R.id.clear -> {
                val alertDialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setMessage(R.string.title_dialog_clear)
                    builder.apply {
                        setPositiveButton(getString(R.string.title_clear),
                            DialogInterface.OnClickListener { dialog, id ->
                                CovidWatchDatabase.databaseWriteExecutor.execute {
                                    val dao: ContactEventDAO =
                                        CovidWatchDatabase.getInstance(requireActivity()).contactEventDAO()
                                    dao.deleteAll()
                                }
                            })
                        setNegativeButton(getString(R.string.title_cancel),
                            DialogInterface.OnClickListener { dialog, id ->
                                // User cancelled the dialog
                            })
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
            R.id.start_logging -> {
                initLocationManager()
                initBluetoothAdapter()
                setContactEventLogging(true)
                activity?.invalidateOptionsMenu()
            }
            R.id.stop_logging -> {
                setContactEventLogging(false)
                activity?.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setContactEventLogging(enabled: Boolean) {

        contactEventsViewModel.isContactEventLoggingEnabled.value = enabled

        val application = context?.applicationContext ?: return
        val sharedPref = application.getSharedPreferences(
            application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ) ?: return
        with(sharedPref.edit()) {
            putBoolean(
                application.getString(R.string.preference_is_contact_event_logging_enabled),
                enabled
            )
            commit()
        }
    }

    /**
     * Initializes the BluetoothAdapter. Manifest file is already setup to allow bluetooth access.
     * The user will be asked to enable bluetooth if it is turned off
     */
    private fun initBluetoothAdapter() {
        val bluetoothAdapter = (activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
    }

    /**
     * Initializes the Location Manager used to obtain coarse bluetooth/wifi location
     * and fine GPS location, logged on a contact event.
     *
     * TODO add GPS initialization here, for now we just ask for location permissions
     */
    private val LOCATION_REQUEST_CODE = 1
    fun initLocationManager() {
        val activity = activity ?: return
        activity.runOnUiThread {
            val permissionStatus = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            when (permissionStatus){
                PackageManager.PERMISSION_GRANTED -> vm.permissionRequestResultLiveData.value = true
                PackageManager.PERMISSION_DENIED -> {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )){
                        Toast.makeText(activity,
                            getString(R.string.ble_location_permission),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //TODO: If a user selects "Don't ask again" the function below does not show the system dialogue.  This prevents the user from ever progressing. Maybe this is an OK UX?
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    ActivityCompat.requestPermissions(activity, permissions, LOCATION_REQUEST_CODE)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode){
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setContactEventLogging(true)
                    vm.permissionRequestResultLiveData.value = true
                }
            }
            else -> {}//Ignore all other requests
        }
    }
}
