package org.covidwatch.android.ui.temporarycontactnumbers

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import org.covidwatch.android.CovidWatchApplication
import org.covidwatch.android.R
import org.covidwatch.android.data.BluetoothViewModel
import org.covidwatch.android.ui.temporarycontactnumbers.adapters.FragmentDataBindingComponent
import org.covidwatch.android.data.TemporaryContactNumberDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.databinding.FragmentTemporaryContactNumbersBinding
import java.util.concurrent.TimeUnit


class TemporaryContactNumbersFragment : Fragment() {

    private lateinit var temporaryContactNumbersViewModel: TemporaryContactNumbersViewModel
    private lateinit var vm: BluetoothViewModel
    private lateinit var binding: FragmentTemporaryContactNumbersBinding
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = context ?: return null

        val database = CovidWatchDatabase.getInstance(context)
        val viewModelTemporary: TemporaryContactNumbersViewModel by viewModels(factoryProducer = {
            TemporaryContactNumbersViewModelFactory(
                database.temporaryContactNumberDAO(),
                context.applicationContext as Application
            )
        })
        temporaryContactNumbersViewModel = viewModelTemporary
        vm = ViewModelProvider(this).get(BluetoothViewModel::class.java)

        binding =
            DataBindingUtil.inflate<FragmentTemporaryContactNumbersBinding>(
                inflater,
                R.layout.fragment_temporary_contact_numbers,
                container,
                false,
                dataBindingComponent
            ).apply {
                lifecycleOwner = this@TemporaryContactNumbersFragment
            }

        val adapter = TemporaryContactNumbersAdapter()
        binding.temporaryContactNumbersRecyclerview.adapter = adapter
        binding.temporaryContactNumbersRecyclerview.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            (activity?.application as? CovidWatchApplication)?.refreshOneTime()
            Handler().postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, TimeUnit.SECONDS.toMillis(1))
        }

        viewModelTemporary.temporaryContactEvents.observe(
            viewLifecycleOwner,
            Observer { adapter.submitList(it) })

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_temporary_contact_numbers, menu)
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
                            DialogInterface.OnClickListener { _, _ ->
                                (activity?.application as? CovidWatchApplication)?.generateAndUploadReport()
                            })
                        setNegativeButton(getString(R.string.title_cancel),
                            DialogInterface.OnClickListener { _, _ ->
                                // User cancelled the dialog
                            })
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setContactEventLogging(enabled: Boolean) {

        temporaryContactNumbersViewModel.isContactEventLoggingEnabled.value = enabled

        val application = context?.applicationContext ?: return
        val sharedPref = application.getSharedPreferences(
            application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ) ?: return
        with(sharedPref.edit()) {
            putBoolean(
                application.getString(R.string.preference_is_temporary_contact_number_logging_enabled),
                enabled
            )
            commit()
        }
    }

}
