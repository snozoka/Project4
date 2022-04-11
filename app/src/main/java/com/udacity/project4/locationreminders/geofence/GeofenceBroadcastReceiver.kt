package com.udacity.project4.locationreminders.geofence

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment.Companion.ACTION_GEOFENCE_EVENT

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

//TODO: implement the onReceive method to receive the geofencing events at the background
       if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceStatusCodes
                    .getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

           val geofenceTransition = geofencingEvent.geofenceTransition

           // Test that the reported transition was of interest.
           if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
               Log.v(TAG, context.getString(R.string.geofence_entered))
               GeofenceTransitionsJobIntentService.enqueueWork(context, intent)

               // Get the geofences that were triggered. A single event can trigger
               // multiple geofences.
               val triggeringGeofences = geofencingEvent.triggeringGeofences

//               // Get the transition details as a String.
//               val geofenceTransitionDetails = getGeofenceTransitionDetails(
//                   this,
//                   geofenceTransition,
//                   triggeringGeofences
//               )

               val fenceId = when {
                   geofencingEvent.triggeringGeofences.isNotEmpty() ->
                       geofencingEvent.triggeringGeofences[0].requestId
                   else -> {
                       Log.e(TAG, "No Geofence Trigger Found! There are no reminders!")
                       return
                   }
               }
           }
        }
    }
}
//private const val TAG = "GeofenceReceiver"