package com.magdy.gpstracker

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.magdy.gpstracker.Constant.LOCATION_PERMISSION

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    lateinit var map: SupportMapFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        map = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        map.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                for (location in result.locations) {
                    // Update UI With New Locations . ..
                    drawUserLocation(location)
                }

            }
        }
        // Determine whether your app was already granted the permission
        if (isLocationPermissionGranted()) {
            // call your function
            showUserLocation()
        } else {
            showRationaleToUser()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // the response of request Permission from user after show dialog ..
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                showUserLocation()
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Toast.makeText(
                    this,
                    "Sorry , Can't Find Driver To Access Your Location ",
                    Toast.LENGTH_LONG
                ).show()
            }

        }


    @SuppressLint("MissingPermission")
    private fun showUserLocation() {
        satisfySettingToStartTracking()
    }

    private fun satisfySettingToStartTracking() {
        //parameters determine the level of accuracy for location requests.
        locationRequest.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        //check whether the current location settings are satisfied:
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        // All location settings are satisfied. The client can initialize
        task.addOnSuccessListener { locationSettings ->
            // start Tracking User Location
            startUserLocationTracking()

        }
            // Location settings are not satisfied, but this can be fixed
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(
                            this@MainActivity,
                            200
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
            }
    }


    @SuppressLint("MissingPermission")
    private fun startUserLocationTracking() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback, Looper.getMainLooper()
        )
    }

    private fun showRationaleToUser() {
        if (shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
            // show dialog to user explain why wee need this permission
            showDialogToUser(message = "App Need Access Your Location !",
                title = "GPsTracker", posActionName = "Ok", posAction = { dialog, which ->
                    requestPermissionFromUser()
                    dialog.dismiss()
                }, negActionName = "No", negAction = { dialog, which ->
                    dialog.dismiss()
                })
        } else {
            // request Permission without show Rationale
            requestPermissionFromUser()
        }
    }

    private fun requestPermissionFromUser() {
        requestPermissionLauncher.launch(LOCATION_PERMISSION)
    }

    fun showDialogToUser(
        title: String? = null,
        message: String? = null,
        posActionName: String? = null,
        negActionName: String? = null,
        posAction: DialogInterface.OnClickListener? = null,
        negAction: DialogInterface.OnClickListener? = null,
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title!!)
        builder.setMessage(message!!)
        builder.setPositiveButton(posActionName!!, posAction)
        builder.setNegativeButton(negActionName!!, negAction)
        builder.show()
    }

    // to check if location Permission is granted or not
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED // allowed to use this permission
    }

    var googleMap: GoogleMap? = null
    var userMarker: Marker? = null

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    private fun drawUserLocation(location: Location?) {

        val latLng = LatLng(location!!.latitude, location!!.longitude)
        val markerOption = MarkerOptions().position(latLng)

        googleMap?.addCircle(
            CircleOptions().center(
                LatLng(
                    location!!.latitude,
                    location!!.longitude
                )
            ).radius(10.0)

        )
        // ما تضيفش ماركر تاني الا لو مفيش واحد تاني
        if (userMarker == null) {
            userMarker = googleMap?.addMarker(markerOption)
            // طب لو مش ب null
        } else {
            userMarker?.position = LatLng(location!!.latitude, location!!.longitude)
        }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
    }
}




















