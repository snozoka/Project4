package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = SelectLocationFragment.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private lateinit var geofencingClient: GeofencingClient

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


//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
            val geofenceList = arrayListOf<Geofence>()
            val geofence = Geofence.Builder()
                .setRequestId(_viewModel.reminderTitle.value!!)
                .setCircularRegion(_viewModel.latitude.value!!,
                    _viewModel.longitude.value!!,
                    100f
                )
                .setExpirationDuration(TimeUnit.HOURS.toMillis(1))
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnCompleteListener {
                    if (context?.let { it1 ->
                            ActivityCompat.checkSelfPermission(
                                it1,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        } != PackageManager.PERMISSION_GRANTED && context?.let { it1 ->
                            ActivityCompat.checkSelfPermission(
                                it1,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
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
                        SelectLocationFragment().requestForegroundAndBackgroundLocationPermissions()
                        return@addOnCompleteListener
                    }
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            Toast.makeText(context, "Geofence added",
                                Toast.LENGTH_SHORT)
                                .show()
                            Log.e("Add Geofence", geofence.requestId)
                            //_viewModel.geofenceActivated()
                        }
                        addOnFailureListener {
                            Toast.makeText(context, R.string.geofences_not_added,
                                Toast.LENGTH_SHORT).show()
                            if ((it.message != null)) {
                                Log.w("Geofence error add", it.message!!)
                            }
                        }
                    }
                }
            }


//             2) save the reminder to the local db
            var reminderDataToSave = ReminderDataItem(title,description,location,latitude,longitude)
            _viewModel.validateAndSaveReminder(reminderDataToSave)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
