//package com.example.map_navigation;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.location.LocationManager;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.GroundOverlay;
//import com.google.android.gms.maps.model.GroundOverlayOptions;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import com.google.maps.DirectionsApiRequest;
//import com.google.maps.GeoApiContext;
//import com.google.maps.PendingResult;
//import com.google.maps.internal.PolylineEncoding;
//import com.google.maps.model.DirectionsResult;
//import com.google.maps.model.DirectionsRoute;
//
//import java.util.ArrayList;
//import java.util.List;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//import java.util.HashMap;
//import java.util.Map;
//
//
////API request imports
//import com.android.volley.Request;
//import com.android.volley.Response;
//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//// Stores a GroundOverlay including the position, value
//public class GradientGroundOverlay {
//    public LatLng latlng;
//    // values range from 0 - 10 (highest/hottest = 0)
//    public int val;
//
//    public GroundOverlay groundOverlay;
//
//
//    public GradientGroundOverlay(LatLng latlng, int val) {
//        this.latlng = latlng;
//        this.val = val;
//
//        // forecasts = new ArrayList<>();
//
//        double latitude = this.latlng.latitude;
//        double longitude = this.latlng.longitude;
//
//        // Create the actual radar square
//        groundOverlay = MapsActivity.mMap.addGroundOverlay(new GroundOverlayOptions()
//                .image(MapsActivity.images.get(this.val))
//                .position(addToLatLng(latlng, latitude, longitude), 2500f, 2500f));
//        assert groundOverlay != null;
//        groundOverlay.setTransparency(0.60f);
//    }
//
//    public void changeValAndColor(int val) {
//        this.val = val;
//        groundOverlay.setImage(MapsActivity.images.get(val));
//    }
//
//    public void changeTransparency(float transparency) {
//        if (transparency >= 0 && transparency <= 0) {
//            groundOverlay.setTransparency(transparency);
//        }
//    }
//
//    public LatLng addToLatLng(LatLng ll, double north, double east) {
//        // Assumes everything is in km
//        double currentLat = ll.latitude;
//        double currentLng = ll.longitude;
//
//        // Taken from: https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
//        double earth = 6378.137;
//        double pi = Math.PI;
//        double m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
//        double new_latitude = currentLat + (north * m);
//
//        m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
//        double new_longitude = currentLng + (east * m) / Math.cos(currentLat * (pi / 180));
//
//        return new LatLng(new_latitude, new_longitude);
//    }
//}
//
//// This class stores GroupGradientGroundOverlay of a certain region
//class GroupGradientGroundOverlay {
//    public ArrayList<GradientGroundOverlay> ggoList;
//    public LatLng here;
//    public int size;
//    public HashMap<LatLng, Integer> hm;
//    public int hour;
//    // Size will make a square of size 2*size sides
//
//    public GroupGradientGroundOverlay(LatLng here, int size, int hour) {
//        this.here = here;
//        this.hour = hour;
//        hm = new HashMap<>();
//        ggoList = new ArrayList<>();
//
//            // TODO: We need to create a new object into add
//
//        ggoList.add(new GradientGroundOverlay(here, getVal(here, hour)));
//        double lat = here.latitude;
//        double lng = here.longitude;
//
//        for (int i = 0; i < size; ++i) {
//            LatLng directlyNorth = addToLatLng(here, i * 2500, 0);
//            ggoList.add(new GradientGroundOverlay(directlyNorth, getVal(directlyNorth, hour)));
//
//            LatLng directlySouth = addToLatLng(here, i * -2500, 0);
//            ggoList.add(new GradientGroundOverlay(directlySouth, getVal(directlySouth, hour)));
//
//            for (int j = 0; j < size; ++j) {
//                LatLng northEast = addToLatLng(here, i * 2500, j * 2500);
//                ggoList.add(new GradientGroundOverlay(northEast, getVal(northEast, hour)));
//
//                LatLng northWest = addToLatLng(here, i * 2500, j * -2500);
//                ggoList.add(new GradientGroundOverlay(northWest, getVal(northWest, hour)));
//
//                LatLng southEast = addToLatLng(here, i * -2500, j * 2500);
//                ggoList.add(new GradientGroundOverlay(southEast, getVal(southEast, hour)));
//
//                LatLng southWest = addToLatLng(here, i * -2500, j * -2500);
//                ggoList.add(new GradientGroundOverlay(southWest, getVal(southWest, hour)));
//            }
//        }
//    }
//
//    public int getVal(LatLng latlng, int hour) {
//        // Team up with Jae and have it properly retrieve the value
//        Integer val = hm.get(latlng);
//        if (val != null) {
//            return (int) val;
//        }
//
//
//        // Do something
//        // SO consult with Jae to make it properly return some value
//        // Also if it takes say over 10ms, just continue and have it return 0, since it
//        // might mean the API calls have stalled
//        // Consulting with Jae found the database layout:
//        // Recordings DB
//            // forecast_data DB
//            // locations DB
//            // temperature forecast data DB
//                // Various hashes for each entry DB
//                    // Location data
//                    // Temperature data
////        There is hash entries at the
////        temperature forecast database. Meaning if we brute force check the location,
////        then (for example the presentation which was 20x20) we would need 400 / 2
////        average (or 400 at worst case when there's no entry for it) calls/checks
////        per location to see if it exists in the database. Meaning if we had N squares
////        to check, it would require roughly O(N^2) just to see if the data already exists
////        in the database.
////                -Maybe to counter this we can just export everything out of the database
////                and into the hashmap, but then is there some filter that exists out there
////                that filters out duplicates/old data entries. Maybe we can have it look through
////                the validTime to see if its old or new, but then that would make it still
////                have a time complexity of O(N^2).
//        //TODO: Adjust code so that it fits with Jae's new database layout
//
//        // Return 0 for now so it make it all red
////        return 0;
//        return hour;
//    }
//
//    // Some updater so in case the API calls stalled
//    public void updateRadar() {
//        for (GradientGroundOverlay ggo : ggoList) {
//            ggo.changeValAndColor(getVal(ggo.latlng, hour));
//        }
//    }
//
//    public LatLng addToLatLng(LatLng ll, double north, double east) {
//        // Assumes everything is in km
//        double currentLat = ll.latitude;
//        double currentLng = ll.longitude;
//
//        // Taken from: https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
//        double earth = 6378.137;
//        double pi = Math.PI;
//        double m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
//        double new_latitude = currentLat + (north * m);
//
//        m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
//        double new_longitude = currentLng + (east * m) / Math.cos(currentLat * (pi / 180));
//
//        return new LatLng(new_latitude, new_longitude);
//    }
//}
//
//
//
//
//
//
//
//
//
