package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.getSystemServiceName
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val TAG = SelectLocationFragment::class.java.simpleName
    private val REQUEST_LOCATION_PERMISSION = 1
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedPointerTitle: String = ""
    private lateinit var selectedPoiMarker: PointOfInterest

    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)
    private lateinit var contxt: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        //Obtain the SupportMapFragment and get notified when the map is ready
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        val mapFragment = childFragmentManager.findFragmentByTag("my_mapTag") as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Construct a PlacesClient
        Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(requireContext())
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = activity?.let {
            LocationServices.getFusedLocationProviderClient(
                it
            )
        }!!


        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


//        Call this function after the user confirms on the selected location
        onLocationSelected()




        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
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
            val noPOI_Selected = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            selectedPointerTitle = noPOI_Selected?.title.toString()
            selectedLatitude = noPOI_Selected?.position?.latitude!!
            selectedLongitude = noPOI_Selected?.position?.longitude!!
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            //selectedPoiMarker = poiMarker.
            selectedLatitude = poiMarker?.position?.latitude!!
            selectedLongitude = poiMarker?.position?.longitude!!
            selectedPointerTitle = poiMarker?.title.toString()

        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                contxt?.let {
                    MapStyleOptions.loadRawResourceStyle(
                        it,
                        R.raw.map_style
                    )
                }
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }


    private fun isPermissionGranted() : Boolean {
        return contxt?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION)
        } === PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
            //Show error message letting users know that location permission is required for the app to function
            else{
                Toast.makeText(context, "Please provide locaiton permission to notify you of your reminders", Toast.LENGTH_SHORT )
            }
        }
    }

    //Enable location layer if permission is granted
    private fun enableMyLocation() {
//        if (isPermissionGranted()) {
//
//        }
        if (ActivityCompat.checkSelfPermission(
                contxt,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                contxt,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }
        else {
            map.isMyLocationEnabled = true
        }
    }


//    private fun getDeviceLocation() {
//        /*
//         * Get the best and most recent location of the device, which may be null in rare
//         * cases when a location is not available.
//         */
//        try {
//            if (locationPermissionGranted) {
//                val locationResult = fusedLocationProviderClient.lastLocation
//                locationResult.addOnCompleteListener(requireActivity()) { task ->
//                    if (task.isSuccessful) {
//                        // Set the map's camera position to the current location of the device.
//                        lastKnownLocation = task.result
//                        if (lastKnownLocation != null) {
//                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                LatLng(lastKnownLocation!!.latitude,
//                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
//                        }
//                    } else {
//                        Log.d(TAG, "Current location is null. Using defaults.")
//                        Log.e(TAG, "Exception: %s", task.exception)
//                        map?.moveCamera(CameraUpdateFactory
//                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
//                        map?.uiSettings?.isMyLocationButtonEnabled = false
//                    }
//                }
//            }
//        } catch (e: SecurityException) {
//            Log.e("Exception: %s", e.message, e)
//        }
//    }





//    /**
//     * Updates the map's UI settings based on whether the user has granted location permission.
//     */
//    private fun updateLocationUI() {
//        if (map == null) {
//            return
//        }
//        try {
//            if (locationPermissionGranted) {
//                map?.isMyLocationEnabled = true
//                map?.uiSettings?.isMyLocationButtonEnabled = true
//            } else {
//                map?.isMyLocationEnabled = false
//                map?.uiSettings?.isMyLocationButtonEnabled = false
//                lastKnownLocation = null
//                getLocationPermission()
//            }
//        } catch (e: SecurityException) {
//            Log.e("Exception: %s", e.message, e)
//        }
//    }



    private fun onLocationSelected() {
        //        When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        binding.buttonSaveReminder.setOnClickListener {
            if (selectedPointerTitle == "Dropped Pin"){
                _viewModel.reminderSelectedLocationStr.value = "Unknown location"
                Log.i("non-POI selection", _viewModel.selectedPOI.toString() )
            }
            else{
                _viewModel.reminderSelectedLocationStr.value = selectedPointerTitle
            }
            _viewModel.latitude.value = selectedLatitude
            _viewModel.longitude.value = selectedLongitude

            Toast.makeText(contxt, "Location saved", Toast.LENGTH_SHORT).show()
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
            //checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }


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

    override fun onMapReady(googleMap: GoogleMap) {
        if (googleMap != null) {
            map = googleMap
        }
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
//        this.map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
//            // Return null here, so that getInfoContents() is called next.
//            override fun getInfoWindow(arg0: Marker): View? {
//                return null
//            }
//            override fun getInfoContents(marker: Marker): View {
//                // Inflate the layouts for the info window, title and snippet.
//                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
//                    findViewById<FrameLayout>(R.id.map), false)
//                val title = infoWindow.findViewById<TextView>(R.id.title)
//                title.text = marker.title
//                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
//                snippet.text = marker.snippet
//                return infoWindow
//            }



        // Turn on the My Location layer and the related control on the map.
        enableMyLocation()
        //updateLocationUI()

        // Get the current location of the device and set the position of the map.
        //getDeviceLocation()

        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
    }

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
        private const val DEFAULT_ZOOM = 15

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

}

