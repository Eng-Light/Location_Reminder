package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 35
        const val GEOFENCE_RADIUS = 500f
        const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE"
    }

    private var requestFinePermissionLauncher = registerFineCallBack()
    private var requestBackgroundPermissionLauncher = registerBackgroundCallBack()

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var reminderData: ReminderDataItem
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        geofencingClient = LocationServices.getGeofencingClient(requireContext())

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
//             add a geofencing request
//             save the reminder to the local db
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

    @SuppressLint("MissingPermission")
    private fun startingGeofence(
        latLng: LatLng, geofenceId: String
    ) {
        val builder = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(
                latLng.latitude, latLng.longitude, GEOFENCE_RADIUS
            ).setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        val request =
            GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(builder)
                .build()

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        geofencingClient.addGeofences(request, pendingIntent).run {
            addOnSuccessListener {
                //Save Data to DataBase
                _viewModel.saveReminder(reminderData)
            }
            addOnFailureListener {
                _viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            }
        }
    }

    /**
     * Check current location sittings status and Add Geofence.
     */
    private fun checkLocationAndAddGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        TURN_DEVICE_LOCATION_ON_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "location settings Error : " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireView(), R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkLocationAndAddGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "Staring Geofence")
                val title = _viewModel.reminderTitle.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value
                val geofenceId = UUID.randomUUID().toString()
                if (latitude != null && longitude != null && !TextUtils.isEmpty(title)) {
                    startingGeofence(
                        LatLng(latitude, longitude),
                        geofenceId
                    )
                }
            }
        }
    }

    /**
     * Show rational permission / Get location permissions / Create the reminder's geofence
     */
    private fun beginSaveReminderFlow() {
        if (isLocationPermissionsGranted()) {
            checkLocationAndAddGeofence()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE) {
            checkLocationAndAddGeofence(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
