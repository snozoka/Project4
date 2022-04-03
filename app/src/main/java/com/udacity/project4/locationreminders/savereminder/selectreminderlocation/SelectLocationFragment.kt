package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val TAG = SelectLocationFragment::class.java.simpleName
    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedPointerTitle: String = ""
    private lateinit var selectedPoiMarker: PointOfInterest

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        //Obtain the SupportMapFragment and get notified when the map is ready

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        val mapFragment = childFragmentManager.findFragmentByTag("my_mapTag") as SupportMapFragment?
        mapFragment?.getMapAsync(this)


        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        geofencingClient = activity?.let { LocationServices.getGeofencingClient(it) }!!


        return binding.root
    }

    private fun setMapLongClick(map: GoogleMap){
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            //selectedPoiMarker = poiMarker.
            selectedLatitude = poiMarker.position.latitude
            selectedLongitude = poiMarker.position.longitude
            selectedPointerTitle = poiMarker.title

        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED && context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            map.isMyLocationEnabled = true
        }
        else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }
    private fun isPermissionGranted() : Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION)
        } === PackageManager.PERMISSION_GRANTED
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray) {
//        // Check if location permissions are granted and if so enable the
//        // location data layer.
//        if (requestCode == REQUEST_LOCATION_PERMISSION) {
//            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                enableMyLocation()
//            }
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                permissionsArray,
                resultCode
            )
        }
    }
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        context?.let {
                            ActivityCompat.checkSelfPermission(
                                it,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        })
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        context?.let {
                            ActivityCompat.checkSelfPermission(
                                it, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = activity?.let { LocationServices.getSettingsClient(it) }
        val locationSettingsResponseTask =
            settingsClient?.checkLocationSettings(builder.build())

        locationSettingsResponseTask?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    activity?.let {
                        exception.startResolutionForResult(
                            it,
                            REQUEST_TURN_DEVICE_LOCATION_ON)
                    }
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
//        locationSettingsResponseTask?.addOnCompleteListener {
//            if ( it.isSuccessful ) {
//                addGeofenceForClue()
//            }
//        }
    }



    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        binding.buttonSaveReminder.setOnClickListener {
            _viewModel.reminderSelectedLocationStr.value = selectedPointerTitle
            _viewModel.latitude.value = selectedLatitude
            _viewModel.longitude.value = selectedLongitude
            //_viewModel.selectedPOI.value = selectedPoiMarker
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
        }
    }


    /*
    *  When we get the result from asking the user to turn on device location, we call
    *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
    *  we don't resolve the check to keep the user from seeing an endless loop.
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }


//    private fun addGeofenceForClue() {
//        if (_viewModel.geofenceIsActive()) return
//        val currentGeofenceIndex = _viewModel.nextGeofenceIndex()
//        if(currentGeofenceIndex >= GeofencingConstants.NUM_LANDMARKS) {
//            removeGeofences()
//            _viewModel.geofenceActivated()
//            return
//        }
//        val currentGeofenceData = GeofencingConstants.LANDMARK_DATA[currentGeofenceIndex]
//
//        // Build the Geofence Object
//        val geofence = Geofence.Builder()
//            // Set the request ID, string to identify the geofence.
//            .setRequestId(currentGeofenceData.id)
//            // Set the circular region of this geofence.
//            .setCircularRegion(currentGeofenceData.latLong.latitude,
//                currentGeofenceData.latLong.longitude,
//                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
//            )
//            // Set the expiration duration of the geofence. This geofence gets
//            // automatically removed after this period of time.
//            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
//            // Set the transition types of interest. Alerts are only generated for these
//            // transition. We track entry and exit transitions in this sample.
//            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
//            .build()
//
//        // Build the geofence request
//        val geofencingRequest = GeofencingRequest.Builder()
//            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
//            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
//            // is already inside that geofence.
//            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//
//            // Add the geofences to be monitored by geofencing service.
//            .addGeofence(geofence)
//            .build()
//
//        // First, remove any existing geofences that use our pending intent
//        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
//            // Regardless of success/failure of the removal, add the new geofence
//            addOnCompleteListener {
//                // Add the new geofence request with the new geofence
//                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
//                    addOnSuccessListener {
//                        // Geofences added.
//                        Toast.makeText(this@HuntMainActivity, R.string.geofences_added,
//                            Toast.LENGTH_SHORT)
//                            .show()
//                        Log.e("Add Geofence", geofence.requestId)
//                        // Tell the viewmodel that we've reached the end of the game and
//                        // activated the last "geofence" --- by removing the Geofence.
//                        _viewModel.geofenceActivated()
//                    }
//                    addOnFailureListener {
//                        // Failed to add geofences.
//                        Toast.makeText(this@HuntMainActivity, R.string.geofences_not_added,
//                            Toast.LENGTH_SHORT).show()
//                        if ((it.message != null)) {
//                            Log.w(TAG, it.message)
//                        }
//                    }
//                }
//            }
//        }
//    }

//    private fun removeGeofences() {
//        if (!foregroundAndBackgroundLocationPermissionApproved()) {
//            return
//        }
//        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
//            addOnSuccessListener {
//                // Geofences removed
//                Log.d(TAG, getString(R.string.geofences_removed))
//                Toast.makeText(applicationContext, R.string.geofences_removed, Toast.LENGTH_SHORT)
//                    .show()
//            }
//            addOnFailureListener {
//                // Failed to remove geofences
//                Log.d(TAG, getString(R.string.geofences_not_removed))
//            }
//        }
//    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
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
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null) {
            map = googleMap
        }
        val latitude = 37.422160
        val longitude = -122.084270
        val zoomLevel = 15f

        val homeLatLng = LatLng(latitude,longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng,zoomLevel))
        map.addMarker(MarkerOptions().position(homeLatLng).title("Marker in Sydney"))
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        enableMyLocation()


    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SelectLocationFragment.reminderSelector.action.ACTION_GEOFENCE_EVENT"
    }


}
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "HuntMainActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1