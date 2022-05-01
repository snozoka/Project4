package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.Exception


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE)
    }
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var contxt: Context
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private var locationPermissionGranted = false
    private lateinit var reminderDataToSave: ReminderDataItem

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = activity?.let { LocationServices.getGeofencingClient(it) }!!


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
//            if (!SelectLocationFragment().foregroundAndBackgroundLocationPermissionApproved()){
//                SelectLocationFragment().requestForegroundAndBackgroundLocationPermissions()
//            }

        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value


//            Use the user entered reminder details to:
//             1) add a geofencing request
            reminderDataToSave = ReminderDataItem(title,description,location,latitude,longitude)
            Log.i("Saved Reminder id:", reminderDataToSave.id)
            Log.i("Saved Reminder title:", reminderDataToSave.title.toString())
            Log.i("Saved Reminder description:", reminderDataToSave.description.toString())
            Log.i("Saved Reminder location:", reminderDataToSave.location.toString())
            Log.i("Saved Reminder latitude:", reminderDataToSave.latitude.toString())
            Log.i("Saved Reminder longitude:", reminderDataToSave.longitude.toString())
//            val geofence = Geofence.Builder()
//                .setRequestId(reminderDataToSave.id)
//                .setCircularRegion(
//                    reminderDataToSave.latitude!!,
//                    reminderDataToSave.longitude!!,
//                    100f
//                )
//                .setExpirationDuration(-1)
//                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
//                .build()
//
//            val geofencingRequest = GeofencingRequest.Builder()
//                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//                .addGeofence(geofence)
//                .build()

            if (
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                    requestForegroundAndBackgroundLocationPermissions()
                return@setOnClickListener
            }
            else{
                checkDeviceLocationSettingsAndStartGeofence()
            }
//            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
//                addOnSuccessListener {
////             2) save the reminder to the local db
//                    //var reminderDataToSave = ReminderDataItem(title,description,location,latitude,longitude)
//                    _viewModel.validateAndSaveReminder(reminderDataToSave)
//                    Toast.makeText(contxt, "Geofence added",
//                        Toast.LENGTH_SHORT)
//                        .show()
//                    Log.e("Add Geofence", geofence.requestId)
//                    //_viewModel.geofenceActivated()
//                }
//                addOnFailureListener {
//                    Toast.makeText(contxt, R.string.geofences_not_added,
//                        Toast.LENGTH_SHORT).show()
//                    if ((it.message != null)) {
//                        Log.w("Geofence error add", it.message!!)
//                    }
//                }
//            }
        }
    }


//    private fun getLocationPermission() {
//        /*
//         * Request location permission, so that we can get the location of the
//         * device. The result of the permission request is handled by a callback,
//         * onRequestPermissionsResult.
//         */
//        if (foregroundAndBackgroundLocationPermissionApproved()) {
//            locationPermissionGranted = true
//        } else {
//            requestForegroundAndBackgroundLocationPermissions()
////            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
////                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
//        }
//    }

    @TargetApi(29)
    fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        contxt?.let {
                            ContextCompat.checkSelfPermission(
                                contxt,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                        })
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        contxt?.let {
                            ContextCompat.checkSelfPermission(
                                contxt, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
            } else {
                true
            }
        Log.i("Permissions granted", "ForegroundLocationApproved = $foregroundLocationApproved and BackgroundLocationApproved = $backgroundPermissionApproved")
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
//        activity?.let {
//            requestPermissions(
//                requireActivity(),
//                permissionsArray,
//                resultCode
//            )
//        }
        requestPermissions(
            permissionsArray,
            resultCode)

        // Prompt the user for permission.
        //getLocationPermission()
    }

    /**
     * Prompts the user for permission to use the device location.
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
        locationPermissionGranted = false

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
            locationPermissionGranted = true
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            checkDeviceLocationSettingsAndStartGeofence()
        }
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
//                    activity?.let {
//                        exception.startResolutionForResult(
//                            it,
//                            REQUEST_TURN_DEVICE_LOCATION_ON
//                        )
//                    }
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resultLauncher.launch(intentSenderRequest)
                    //startIntentSenderForResult(IntentSender,REQUEST_CODE_DEVICE_LOCATION_SETTING,null,0,0,0,null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
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
        locationSettingsResponseTask?.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofence()
                //return@addOnCompleteListener
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(){
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataToSave.id)
            .setCircularRegion(
                reminderDataToSave.latitude!!,
                reminderDataToSave.longitude!!,
                100f
            )
            .setExpirationDuration(-1)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
//             2) save the reminder to the local db
                //var reminderDataToSave = ReminderDataItem(title,description,location,latitude,longitude)
                _viewModel.validateAndSaveReminder(reminderDataToSave)
                Toast.makeText(contxt, "Geofence added",
                    Toast.LENGTH_SHORT)
                    .show()
                Log.e("Add Geofence", geofence.requestId)
                //_viewModel.geofenceActivated()
            }
            addOnFailureListener {
                Toast.makeText(contxt, R.string.geofences_not_added,
                    Toast.LENGTH_SHORT).show()
                if ((it.message != null)) {
                    Log.w("Geofence error add", it.message!!)
                }
            }
        }

    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.reminderSelector.action.ACTION_GEOFENCE_EVENT"
        private val TAG = SaveReminderFragment::class.java.simpleName
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val REQUEST_CODE_DEVICE_LOCATION_SETTING = 100
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
