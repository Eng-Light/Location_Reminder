package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), MenuProvider, OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var poi: PointOfInterest? = null
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var title = ""
    private var isLocationSelected = false

    private lateinit var map: GoogleMap
    private var requestPermissionLauncher = registerCallBack()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectLocationBinding.inflate(layoutInflater, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        setDisplayHomeAsUpEnabled(true)
        setupMap()

//        add the map setup implementation
//        zoom to the user location after taking his permission
//        add style to the map
//        put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        val sydney = LatLng(-34.0, 151.0)
        val zoomLevel = 15f
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoomLevel))
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        setStyle(map)
        getLocation()
        setOnPoiClick(map)
        setOnLongClick(map)
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    /**
     * Manage what happen when user clicks on a poi in the map.
     */
    private fun setOnPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )?.showInfoWindow()
            map.addCircle(
                CircleOptions().center(poi.latLng).radius(150.0)
                    .strokeColor(Color.argb(255, 255, 0, 0)).fillColor(Color.argb(64, 255, 0, 0))
                    .strokeWidth(4F)

            )
            this.poi = poi
            lat = poi.latLng.latitude
            long = poi.latLng.longitude
            title = poi.name
            isLocationSelected = true
        }
    }

    /**
     * Manage what happen when user longClicks on any point of the map.
     */
    private fun setOnLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLong ->
            val zoomLevel = 15f
            map.clear()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoomLevel))
            map.addMarker(
                MarkerOptions().position(latLong).title(getString(R.string.dropped_pin))
            )?.showInfoWindow()
            lat = latLong.latitude
            long = latLong.longitude
            title = getString(R.string.dropped_pin)
            isLocationSelected = true
        }
    }

    /**
     * Register callback to request location permissions
     */
    private fun registerCallBack(): ActivityResultLauncher<Array<String>> {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    //Enable MyLocationButton.
                    isUserLocationEnabled()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Toast.makeText(
                        context, "Only approximate location access granted.", Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        context, "Location permission was not granted.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return requestPermissionLauncher
    }

    /**
     * Shows dialog to inform the user why we need those permissions.
     */
    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title).setMessage(message).setPositiveButton("Ok") { _, _ ->
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        builder.create().show()
    }

    /**
     * Check if the location permissions is granted and request it if not.
     */
    private fun isUserLocationEnabled(): Boolean {
        var result = false
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // This condition only becomes true if the user has denied the permission previously
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                showRationaleDialog(
                    getString(R.string.rationale_title), getString(R.string.rationale_desc)
                )
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            Toast.makeText(
                context, "Location permissions is granted.", Toast.LENGTH_LONG
            ).show()
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            result = true
        }
        return result
    }

    /**
     * Get User's last location if exist and if not -> request current location.
     */
    @Suppress("MissingPermission")
    private fun getLocation() {
        if (isUserLocationEnabled()) {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { resultLocation ->
                if (resultLocation.isSuccessful) {
                    resultLocation.run {
                        if (this.result != null) {
                            focusOnCurrentLocation(this.result)
                        } else {
                            showRationaleDialog(
                                "Location is Disabled",
                                "Please Enable Your Location Settings"
                            )
                        }
                    }
                } else {
                    getCurrentLocation()
                }
            }
        }
    }

    /**
     * Get current location.
     */
    @Suppress("MissingPermission")
    private fun getCurrentLocation() {
        val priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(priority, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    focusOnCurrentLocation(location)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please Enable your Location Settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun focusOnCurrentLocation(location: Location) {
        val zoomLevel = 15f
        val latLong = LatLng(location.latitude, location.longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoomLevel))
        map.addMarker(
            MarkerOptions().position(latLong).title(getString(R.string.my_location_pin))
        )?.showInfoWindow()
        map.addCircle(
            CircleOptions().center(latLong).radius(150.0).strokeColor(Color.argb(255, 255, 0, 0))
                .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(4F)

        )
        lat = latLong.latitude
        long = latLong.longitude
        title = getString(R.string.dropped_pin)
    }

    /**
     * Set Custom Style to the map (Blue Water Style -by Xavier)
     */
    private fun setStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(context?.let {
                MapStyleOptions.loadRawResourceStyle(
                    it, R.raw.map_style
                )
            })
            if (!success) {
                Toast.makeText(context, "Error Styling map.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Resources.NotFoundException) {
            Toast.makeText(context, "Style Not Found Exception $e", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.map_options, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        //Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> false
    }
}
