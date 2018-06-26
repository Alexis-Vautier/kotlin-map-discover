package com.locationspaces.alexis.locationplaces.apis

import android.location.Location
import java.net.URL

class GoogleApis {
    private val GOOGLE_API_KEY = "AIzaSyDFmxkuY9Dh-vQJUFyOFAvneMmKY5QANik"

    fun urlForNearbyLocations(lastUserLocation: Location): URL {
        var urlPath = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        urlPath += "?location=" + lastUserLocation.latitude.toString() + "," + lastUserLocation.longitude.toString()
        urlPath += "&radius=2000"
        urlPath += "&key=" + GOOGLE_API_KEY

        return URL(urlPath)
    }

    fun urlForReferencePicture(path: String): URL {
        var urlPath = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=150"
        urlPath += "&photoreference=" + path
        urlPath += "&key=" + GOOGLE_API_KEY

        return URL(urlPath)
    }
}



