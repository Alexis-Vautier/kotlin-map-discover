package com.locationspaces.alexis.locationplaces

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
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
import com.locationspaces.alexis.locationplaces.model.GooglePlace
import org.json.JSONObject
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

    private val GOOGLE_API_KEY = "AIzaSyDFmxkuY9Dh-vQJUFyOFAvneMmKY5QANik"


    private var poisList = mutableListOf<GooglePlace>()



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

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

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

        if (lastUserLocation != null) {
            //var params = URLEncoder.encode("location=", "UTF-8") + "=" + URLEncoder.encode(lastUserLocation.latitude.toString(), "UTF-8") + "," + URLEncoder.encode(lastUserLocation.longitude.toString(), "UTF-8")
            //reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8")
            // val mURL = URL("<Your API Link>")

            var urlPath = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
            urlPath += "?location=" +lastUserLocation.latitude.toString() +"," +lastUserLocation.longitude.toString()
            urlPath += "&radius=2000"
            urlPath += "&key=" +GOOGLE_API_KEY

            println(urlPath)
            val mURL = URL(urlPath)
            with(mURL.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "GET"

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }

                    val jsonObj = JSONObject(response.toString())

                    if (jsonObj.has("results")) {
                        val placeJson = jsonObj.getJSONArray("results")

                        for (i in 0 until placeJson!!.length() -1) {
                            val place = placeJson.getJSONObject(i)
                            if (place.has("geometry") and place.getJSONObject("geometry").has("location")) {
                                val placeLocationJson = place.getJSONObject("geometry").getJSONObject("location")
                                var photoRef = ""
                                if (place.has("photos")) {
                                    photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference")
                                }

                                poisList.add(GooglePlace(place.getString("id"), place.getString("name"), place.getString("icon"),photoRef, placeLocationJson.getDouble("lat"), placeLocationJson.getDouble("lng")))

                            }
                        }
                    }
                    println(poisList)
                }
            }
        }

        //https://maps.googleapis.com/maps/api/place/findplacefromtext/output?input=parameters

//https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=1500&key=AIzaSyAntuIe_aZ6DIKKlT45I9TgDRe8W9kmBs4

    }

    private fun placeMarkerOnMap() {
        poisList.forEach { place ->
            val markerOptions = MarkerOptions().position(LatLng(place.latitude, place.longitude))

            var urlPath = ""
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
                var urlPath = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=150"
                urlPath += "&photoreference=" +place.imageReferencePath
                urlPath += "&key=" +GOOGLE_API_KEY

                val url = URL(urlPath)

                Thread({
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    runOnUiThread({

                        val roundDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                        roundDrawable.isCircular = true

                        val output = Bitmap.createBitmap(roundDrawable.bitmap!!.width, roundDrawable.bitmap!!.height!!, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(output)

                        val paint = Paint()
                        val rect = Rect(0,0,bitmap.width, bitmap.height)
                        canvas.drawARGB(0,0,0,0)
                        canvas.drawCircle((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (bitmap.width / 2).toFloat(), paint)
                        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
                        canvas.drawBitmap(bitmap,rect,rect,paint)

                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(output))
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

    //https://maps.googleapis.com/maps/api/place/photo?
    // maxwidth=400&photoreference=CnRtAAAATLZNl354RwP_9UKbQ_5Psy40texXePv4oAlgP4qNEkdIrkyse7rPXYGd9D_Uj1rVsQdWT4oRz4QrYAJNpFX7rzqqMlZw2h2E2y5IKMUZ7ouD_SlcHxYq1yL4KbKUv3qtWgTK0A6QbGh87GB3sscrHRIQiG2RrmU_jF4tENr9wGS_YxoUSSDrYjWmrNfeEHSGSc3FyhNLlBU&key=YOUR_API_KEY

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
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MapsActivity,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }


}
