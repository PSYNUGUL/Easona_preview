package com.example.map_navigation;


import static android.app.PendingIntent.getActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//API request imports
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;


public class MapsActivity extends AppCompatActivity implements
                                                        OnMapReadyCallback,
                                                        GoogleMap.OnPolylineClickListener {

    // The map object
    private GoogleMap mMap;

    // The direction object
    private GeoApiContext mGeoApiContext = null;

    // Origin Destination
    private LatLng origin = new LatLng(43.076472, -89.401192);      // CoLib
    private LatLng dest = new LatLng(43.083640, -89.477645);        // A Random Friend

    // Jay's code
    private Marker currentMarker;
    private LocationManager locationManager;
    private MyLocationListener myLocationListener;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String NWS_API_BASE_URL = "https://api.weather.gov/";
    private static String forecastURL = null;
    private static String forecastHourlyURL = null;
    private static String gridpointURL = null;
    private NetworkRequestHandler requestHandler;
    DatabaseReference mDatabase;

    // random object
    private Random random = new Random();
    private static final int TRANSPARENCY_MAX = 50;

    private final ArrayList<BitmapDescriptor> images = new ArrayList<>();

    private GroundOverlay groundOverlay;
    private SeekBar transparencyBar;

    private int currentEntry = 0;


    // BroadcastReceiver to handle location updates
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("location-update")) {
                double latitude = intent.getDoubleExtra("latitude", 0.0);
                double longitude = intent.getDoubleExtra("longitude", 0.0);
                origin = new LatLng(latitude, longitude);
                updateMarkerPosition(latitude, longitude);

                // Recalculate the direction based on the updated origin
                calculateDirection(origin.latitude, origin.longitude, dest.latitude, dest.longitude);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // TODO: Merge
        // Instantiate the GeoApiContext object, enable the app to communicate with the Direction API
        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.google_maps_api_key))
                    .build();
        }

        FirebaseApp.initializeApp(this);
        myLocationListener = new MyLocationListener(this);
        requestHandler = NetworkRequestHandler.getInstance(this);
        mDatabase = FirebaseDatabase.getInstance().getReference("recordings");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Check and request location permission
        checkLocationPermission();
    }
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            // Perform location-related tasks
            enableMyLocation();
        } else {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocation() {
        if (mMap != null) {
            try {
                // Enable the My Location layer if the permission is granted
                mMap.setMyLocationEnabled(true);

                // Optionally, you can customize the UI settings related to My Location
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException e) {
                // Handle exception if permission is revoked at runtime
                e.printStackTrace();
            }
        }
    }

    // new method to implement destination update

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions()
                .position(dest)
                .title("My Destination"));

        // Enable the polyline clicking functionality
        mMap.setOnPolylineClickListener(this);

        // Start the debug run
        // Note: The origin marker is not added here, as it will be updated dynamically
        calculateDirection(origin.latitude, origin.longitude, dest.latitude, dest.longitude);
        images.clear();
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image0));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image1));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image2));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image3));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image4));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image5));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image6));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image7));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image8));
        images.add(BitmapDescriptorFactory.fromResource(R.drawable.solid_color_image9));

        groundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(images.get(0))
                .position(origin, 2500f, 2500f));
        assert groundOverlay != null;
        groundOverlay.setTransparency(1f);

//        random.nextInt(10);
        groundCreationGrid();
    }

    public void updateMarkerPosition(double latitude, double longitude) {
        if (mMap != null) {
            LatLng newLocation = new LatLng(latitude, longitude);
            if (currentMarker == null) {
                currentMarker = mMap.addMarker(new MarkerOptions().position(newLocation).title("Current Location"));
            } else {
                currentMarker.setPosition(newLocation);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 50));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle location permission result if needed
    }

    // Method to construct the forecast URL based on latitude and longitude
    private String getData(double latitude, double longitude) {
        return NWS_API_BASE_URL + "points/" + latitude + "," + longitude;
    }

    // Call this method with the constructed forecast URLs (the one linked to the forecastHourly section under "properties")
    public void makeNetworkRequest(double latitude, double longitude, Location location) {
        String dataURL = getData(latitude, longitude);
        Map<String, String> headers = new HashMap<>();
        Log.d("URL for API: ", dataURL);
        headers.put("User-Agent", "Your Custom User-Agent String");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                dataURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Extract forecast and forecastHourly URLs
                            forecastURL = response.getJSONObject("properties").optString("forecast");
                            forecastHourlyURL = response.getJSONObject("properties").optString("forecastHourly");
                            gridpointURL = response.getJSONObject("properties").optString("forecastGridData");
                            // Log the URLs
                            Log.d("Forecast URL: ", forecastURL);
                            Log.d("ForecastHourly URL: ", forecastHourlyURL);
                            Log.d("Gridpoint URL: ", gridpointURL);

                            // Check if URLs are not empty and fetch data
                            if (forecastURL != null && !forecastURL.isEmpty()) {
                                requestHandler.fetchDataFromForecastURL(forecastURL);
                            } else {
                                Log.e("Forecast URL", "Forecast URL is empty or null");
                            }

                            if (forecastHourlyURL != null && !forecastHourlyURL.isEmpty()) {
                                requestHandler.fetchDataFromForecastHourlyURL(forecastHourlyURL);
                            } else {
                                Log.e("ForecastHourly URL", "ForecastHourly URL is empty or null");
                            }

                            if (gridpointURL != null && !gridpointURL.isEmpty()) {
                                requestHandler.fetchDataFromGridpointURL(gridpointURL, location);
                            } else {
                                Log.e("gridpoint URL", "gridpoint URL is empty or null");
                            }
                        } catch (Exception e) {
                            Log.e("Forecast URL", "Error parsing forecast URL: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Forecast URL", "Error fetching weather data: " + error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }


    /**
     * Calculate the direction using Google's Direction API
     *
     * See <a href="https://developers.google.com/maps/documentation/directions/start"> Directions API
     * </a> for documentation
     *
     * @param originLat latitude of origin
     * @param originLng longitude of origin
     * @param destLat latitude of destination
     * @param destLng longitude of origin
     */
    private void calculateDirection(double originLat, double originLng, double destLat, double destLng) {
        String TAG = "calculateDirection";

        // Make a new Direction API Request
        DirectionsApiRequest directionsApiRequest = new DirectionsApiRequest(mGeoApiContext);

        // Enable calculating alternative routes (all possible routes)
        directionsApiRequest.alternatives(true);

        // Set origin
        com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(originLat, originLng);
        directionsApiRequest.origin(origin);

        // Set destination and calculate the route
        // The callback method will be triggered as soon as the result of calculation came back
        com.google.maps.model.LatLng dest = new com.google.maps.model.LatLng(destLat, destLng);
        directionsApiRequest.destination(dest).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                // More info on the DirectionsResult object:
                // https://developers.google.com/maps/documentation/directions/start

                // Print the route got from the API
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                // Draw the route on the map
                drawRoutesOnMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "Failed to get direction: " + e.getMessage());
            }
        });

    }

    /**
     * Draw the routes using poly-lines on the map
     * This method should be called after the Directions API done calculating routes
     *
     * @param result route information calculated and returned by the DirectionsApiRequest
     * @see {@link #calculateDirection(double, double, double, double)}
     */
    private void drawRoutesOnMap(final DirectionsResult result) {
        String TAG = "drawRoutesOnMap";

        // Post on the main thread because the Google Map "mMap" is on the main thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                // Get all available routes returned by the Directions API
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());

                    // Get all waypoints along the route
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());
                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }

                    // Create the polyline (route) on the map
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getActivity(), R.color.darkGrey));
                    // TODO: set color
//                    polyline.setColor();
                    polyline.setClickable(true);

                }
            }
        });
    }

    /**
     * Used to handle when the route is pressed
     * @param polyline
     */
    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        // When this polyline is clicked, set the color to blue and make it in front of all other
        // polyline
        polyline.setColor(ContextCompat.getColor(getActivity(), R.color.blue));
        polyline.setZIndex(1);
    }

    private Context getActivity() {
        return this;
    }

    private void showToast(String toastText) {
        Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_SHORT);
        toast.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Start listening for location updates when the activity is resumed
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 20000, 0, myLocationListener);
            Log.d("location is updating", "True");
        }
        // Register the BroadcastReceiver for location updates
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdateReceiver,
                new IntentFilter("location-update"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop listening for location updates when the activity is paused
        locationManager.removeUpdates(myLocationListener);
        // Unregister the BroadcastReceiver when the activity is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);
    }

    public LatLng addToLatLng(LatLng ll, double north, double east) {
        // Assumes everything is in km
        double currentLat = ll.latitude;
        double currentLng = ll.longitude;

        // // Earth radius in kilometers
        // double earthRadius = 6378.0;

        // // Convert latitude and longitude from degrees to radians
        // double currentLatRad = Math.toRadians(currentLat);
        // double currentLngRad = Math.toRadians(currentLng);

        // // Calculate new latitude
        // double newLatRad = currentLatRad + (north / earthRadius);

        // // Calculate new longitude
        // double newLngRad = currentLngRad + (east / earthRadius) / Math.cos(currentLatRad);

        // // Convert back to degrees
        // double newLat = Math.toDegrees(newLatRad);
        // double newLng = Math.toDegrees(newLngRad);

        // return new LatLng(newLat, newLng);


        // Taken from: https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
        double earth = 6378.137;
        double pi = Math.PI;
        double m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
        double new_latitude = currentLat + (north * m);

        m = (1 / ((2 * pi / 360) * earth)) / 1000;  //1 meter in degree
        double new_longitude = currentLng + (east * m) / Math.cos(currentLat * (pi / 180));

        return new LatLng(new_latitude, new_longitude);
    }

    public void stuffGroundOverlayCreation(int n, int e, int pic) {
        GroundOverlay groundOverlay1 = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(images.get(pic))
                .position(addToLatLng(origin, n * 2500d, e * 2500d), 2500f, 2500f));
        assert groundOverlay != null;
        groundOverlay1.setTransparency(0.60f);
    }

    public void groundCreationColumn(int x) {
        stuffGroundOverlayCreation(0, x, random.nextInt(3));
        stuffGroundOverlayCreation(1, x, random.nextInt(3));
        stuffGroundOverlayCreation(2, x, random.nextInt(3));
        stuffGroundOverlayCreation(3, x, random.nextInt(3));
        stuffGroundOverlayCreation(4, x, random.nextInt(3));
        stuffGroundOverlayCreation(5, x, random.nextInt(3));
        stuffGroundOverlayCreation(6, x, random.nextInt(3));
        stuffGroundOverlayCreation(7, x, random.nextInt(3));
        stuffGroundOverlayCreation(-1, x, random.nextInt(3));
        stuffGroundOverlayCreation(-2, x, random.nextInt(3));
        stuffGroundOverlayCreation(-3, x, random.nextInt(3));
        stuffGroundOverlayCreation(-4, x, random.nextInt(3));
        stuffGroundOverlayCreation(-5, x, random.nextInt(3));
        stuffGroundOverlayCreation(-6, x, random.nextInt(3));
        stuffGroundOverlayCreation(-7, x, random.nextInt(3));

        stuffGroundOverlayCreation(8, x, random.nextInt(3));
        stuffGroundOverlayCreation(-8, x, random.nextInt(3));
        stuffGroundOverlayCreation(9, x, random.nextInt(3));
        stuffGroundOverlayCreation(-9, x, random.nextInt(3));
        stuffGroundOverlayCreation(10, x, random.nextInt(3));
        stuffGroundOverlayCreation(-10, x, random.nextInt(3));
        stuffGroundOverlayCreation(11, x, random.nextInt(3));
        stuffGroundOverlayCreation(-11, x, random.nextInt(3));
//        stuffGroundOverlayCreation(0, x, 0);
//        stuffGroundOverlayCreation(1, x, 0);
//        stuffGroundOverlayCreation(2, x, 0);
//        stuffGroundOverlayCreation(3, x, 0);
//        stuffGroundOverlayCreation(4, x, 0);
//        stuffGroundOverlayCreation(5, x, 0);
//        stuffGroundOverlayCreation(6, x, 0);
//        stuffGroundOverlayCreation(7, x, 0);
//        stuffGroundOverlayCreation(-1, x, 0);
//        stuffGroundOverlayCreation(-2, x, 0);
//        stuffGroundOverlayCreation(-3, x, 0);
//        stuffGroundOverlayCreation(-4, x, 0);
//        stuffGroundOverlayCreation(-5, x, 0);
//        stuffGroundOverlayCreation(-6, x, 0);
//        stuffGroundOverlayCreation(-7, x, 0);
    }

    public void groundCreationGrid() {
        groundCreationColumn(0);
        groundCreationColumn(1);
        groundCreationColumn(-1);
        groundCreationColumn(2);
        groundCreationColumn(-2);
        groundCreationColumn(3);
        groundCreationColumn(-3);
        groundCreationColumn(4);
        groundCreationColumn(-4);
        groundCreationColumn(5);
        groundCreationColumn(-5);
        groundCreationColumn(6);
        groundCreationColumn(-6);
        groundCreationColumn(7);
        groundCreationColumn(-7);
        groundCreationColumn(8);
        groundCreationColumn(-8);
    }
}



