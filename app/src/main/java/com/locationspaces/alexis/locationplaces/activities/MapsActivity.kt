package com.locationspaces.alexis.locationplaces.activities

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.locationspaces.alexis.locationplaces.R
import com.locationspaces.alexis.locationplaces.apis.GoogleApis
import com.locationspaces.alexis.locationplaces.model.GooglePlace
import com.locationspaces.alexis.locationplaces.model.GooglePlaceHelper
import com.locationspaces.alexis.locationplaces.tools.CircleDrawer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var lastUserLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    //TODO : save the POIs in a database (or preferences)
    private var poisList = mutableListOf<GooglePlace>()

    /*private val navigationBarItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.action_map -> {

            }
            R.id.action_list -> {

            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastUserLocation = p0.lastLocation
            }
        }

        //TODO : finish the navigation bar
        //var navigationBar = this.bottom_navigation_menu as BottomNavigationView
        //navigationBar.setOnNavigationItemSelectedListener(navigationBarItemSelectedListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        initUserLocation()
    }

    // Check the location permission
    private fun initUserLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        grantedLocationAccess()
    }

    // The Location access is granted
    private fun grantedLocationAccess() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    lastUserLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

                    Thread({
                        loadNearbyPoints()
                        runOnUiThread({
                            placeMarkerOnMap()
                        })
                    }).start()

                } else {
                    Toast.makeText(this,
                            "Your location can't be displayed - Please, try again later.",
                            Toast.LENGTH_LONG).show()
                }
                startLocationUpdates()
            }
        }
    }

    private fun loadNearbyPoints() {
         with(GoogleApis().urlForNearbyLocations(lastUserLocation).openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "GET"

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    poisList = GooglePlaceHelper().createGooglePlacesJson(response.toString())
                }
         }
    }

    private fun placeMarkerOnMap() {
        poisList.forEach { place ->
            val markerOptions = MarkerOptions().position(LatLng(place.latitude, place.longitude))

            var urlPath: String
            if (place.imageReferencePath.equals("")) {
                urlPath = place.imagePath
                val url = URL(urlPath)

                Thread({
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    runOnUiThread({
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        mMap.addMarker(markerOptions)
                    })
                }).start()

            } else {
                Thread({
                    val bitmap = BitmapFactory.decodeStream(GoogleApis().urlForReferencePicture(place.imageReferencePath).openConnection().getInputStream())
                    runOnUiThread({
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(CircleDrawer().convertBitmapToCircle(bitmap, resources)))
                        mMap.addMarker(markerOptions)
                    })
                }).start()
            }
        }
    }

    // The Location access is declined
    private fun declineLocationAccess() {
        Toast.makeText(this,
                "Your location can't be displayed - The permission is required",
                Toast.LENGTH_LONG).show()
    }

    // Request permissions result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {

                if (grantResults.isEmpty() || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    declineLocationAccess()
                } else {
                    grantedLocationAccess()
                }
            }
        }
    }

    // Init location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapsActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }
}