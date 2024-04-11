package com.example.map_navigation;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.VolleyError;

public class MyLocationListener implements LocationListener {

    private Context mContext;
    private MapsActivity myMaps;
    private WeatherForecast myWeatherData;
    private NetworkRequestHandler myRequest;

    public MyLocationListener(MapsActivity mapsActivity) {
        mContext = mapsActivity;
        myMaps = mapsActivity;
        myWeatherData = new WeatherForecast(
                null,
                0.0,  // gridTemp
                0.0,  // dewpoint
                0.0,  // maxTemperature
                0.0,  // minTemperature
                0.0,  // relativeHumidity
                0.0,  // apparentTemperature
                0.0,  // windChill
                0.0,  // skyCover
                0,   // windDirection
                0.0,  // windSpeed
                0.0,  // windGust
                "Test",  // weather
                "Test",  // coverage
                0,     // probPrecipitation
                0.0,     // amtRain
                "null" //time
        );
        myRequest = NetworkRequestHandler.getInstance(mContext);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Using modular LocationChange", "Good!");
        long timestamp = System.currentTimeMillis(); // or use location.getTime() if available

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Broadcast this data
        Intent intent = new Intent("location-update");
        intent.putExtra("longitude", longitude);
        intent.putExtra("latitude", latitude);
        LocalBroadcastManager.getInstance(myMaps).sendBroadcast(intent);

        // Update marker position
        myMaps.updateMarkerPosition(latitude, longitude);

        // Get data from NWS API
        myMaps.makeNetworkRequest(latitude, longitude, location);

        // Record the data to the firebase database
        myRequest.pushWeatherDataToFirebase(location);
    }
    // Implement other methods of LocationListener if needed
}
