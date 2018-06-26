package com.locationspaces.alexis.locationplaces.model

import org.json.JSONObject


class GooglePlace constructor(var id: String,
                              var name: String,
                              var imagePath: String,
                              var imageReferencePath: String,
                              var latitude: Double,
                              var longitude: Double) {

}

class GooglePlaceHelper {
    fun createGooglePlacesJson(json: String): MutableList<GooglePlace> {
        var poisList = mutableListOf<GooglePlace>()

        val jsonObj = JSONObject(json)

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
        return poisList
    }
}

