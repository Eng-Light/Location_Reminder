package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val TAG = "SaveReminderFragment"
    }

    private var requestFinePermissionLauncher = registerFineCallBack()
    private var requestBackgroundPermissionLauncher = registerBackgroundCallBack()

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var reminderData: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderData = ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateAndSaveReminder(reminderData)) {
                beginSaveReminderFlow()
            }
//            use the user entered reminder details to:
//             TODO: 1) add a geofencing request
//             TODO:2) save the reminder to the local db
        }
    }

    /**
     * Register callback to request foreground location permissions
     */
    private fun registerFineCallBack(): ActivityResultLauncher<Array<String>> {
        val requestForegroundPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            when {
                result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d(TAG, "Fine location access granted.")
                }
                else -> {
                    Log.e(TAG, "Location permission was not granted.")
                }
            }
        }
        return requestForegroundPermissionLauncher
    }

    /**
     * Register callback to request Background location permissions
     */
    private fun registerBackgroundCallBack(): ActivityResultLauncher<Array<String>> {
        val requestForegroundPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            when {
                result.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) -> {
                    Log.d(TAG, "Background permission is granted.")
                }
                else -> {
                    Log.e(TAG, "Location Background permission was not granted.")
                }
            }
        }
        return requestForegroundPermissionLauncher
    }

    /**
     * Shows dialog to inform the user why we need those permissions.
     */
    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title).setMessage(message).setPositiveButton("Ok") { _, _ ->
            requestBackgroundPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
        builder.create().show()
    }

    /**
     * Check if the background location permissions is granted and request it if not.
     */
    private fun isBackgroundLocationPermissionsGranted(): Boolean {
        var result = false
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // This condition only becomes true if the user has denied the permission previously
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showRationaleDialog(
                    getString(R.string.rationale_title_background),
                    getString(R.string.rationale_desc_background)
                )
            } else {
                requestBackgroundPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                )
            }
        } else {
            Log.d(TAG, "Background permission is granted.")
            result = true
        }
        return result
    }

    /**
     * Check if the location permissions is granted and request it if not.
     */
    private fun isLocationPermissionsGranted(): Boolean {
        var result = false
        //Check Fine Permissions
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestFinePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        } else {
            Log.d(TAG, "Fine permissions is granted.")
            //Check Background Permission.
            result = isBackgroundLocationPermissionsGranted()
        }
        return result
    }

    /**
     * Show rational permission / Get location permissions / Create the reminder's geofence
     */
    private fun beginSaveReminderFlow() {
        if (isLocationPermissionsGranted()) {
            //check Location And Add Geofence
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
