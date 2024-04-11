package com.example.map_navigation;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkRequestHandler {
    private static NetworkRequestHandler instance;
    private RequestQueue requestQueue;
    private Context context;
    List<Object[]> testTemperatureDataList = new ArrayList<>();

    // Add a HashSet for fast lookup of locations
    private Set<String> locationSet = new HashSet<>();

    private WeatherForecast myWeather = new WeatherForecast(
            testTemperatureDataList, //list
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
            "null",  // weather
            "null",  // coverage
            0,     // probPrecipitation
            0.0,    // amtRain
            "null" // time
    );

    private NetworkRequestHandler(Context context) {
        this.context = context.getApplicationContext();
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    // Modify getInstance to take a Context parameter
    public static synchronized NetworkRequestHandler getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkRequestHandler(context);
        }
        return instance;
    }

    // Fetch data from the gridpoint URL
    void fetchDataFromGridpointURL(String gridpointURL, Location location) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Your Custom User-Agent String");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                gridpointURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Process gridpoint data
                            Log.d("Gridpoint Data", response.toString());

                            // Extract and process other data as needed
                            extractAndProcessTemperature(response);

                            // Call pushWeatherDataToFirebase to trigger clean-up and record new data
                            pushWeatherDataToFirebase(location);
                        } catch (Exception e) {
                            Log.e("Gridpoint Data", "Error parsing gridpoint data: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Gridpoint Data", "Error fetching gridpoint data: " + error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };

        // Add the request to the RequestQueue
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    //TODO: Further implement these methods for the future development
    // Fetch data from the forecast URL
    void fetchDataFromForecastURL(String forecastURL) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Your Custom User-Agent String");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                forecastURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Process forecast data
                            Log.d("Forecast Data", response.toString());
                            // Extract and process other data as needed
                        } catch (Exception e) {
                            Log.e("Forecast Data", "Error parsing forecast data: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Forecast Data", "Error fetching forecast data: " + error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };

        // Add the request to the RequestQueue
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    //TODO: Further implement these methods for the future development
    // Fetch data from the forecastHourly URL
    void fetchDataFromForecastHourlyURL(String forecastHourlyURL) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Your Custom User-Agent String");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                forecastHourlyURL,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Process forecastHourly data
                            Log.d("ForecastHourly Data", response.toString());
                            // Extract and process other data as needed
                        } catch (Exception e) {
                            Log.e("ForecastHourly Data", "Error parsing forecastHourly data: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("ForecastHourly Data", "Error fetching forecastHourly data: " + error.toString());
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };

        // Add the request to the RequestQueue
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }
    // New method to extract and return sorted temperature data
    private List<Object[]> extractTemperatureData(JSONArray temperatureValues) throws JSONException {
        List<Object[]> temperatureDataList = new ArrayList<>();

        for (int i = 0; i < temperatureValues.length(); i++) {
            JSONObject temperatureValue = temperatureValues.getJSONObject(i);

            // Extract temperature data
            double value = temperatureValue.getDouble("value");
            String validTime = temperatureValue.getString("validTime");

            // Add to the list as an Object array
            temperatureDataList.add(new Object[]{value, validTime});
        }

        return temperatureDataList;
    }

    // Data extraction method from the gridPoint URL data
    private void extractAndProcessTemperature(JSONObject gridpointData) {
        try {
            //

            // Extract temperature values array
            JSONObject temperatureObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("temperature");

            JSONArray temperatureValues = temperatureObject.getJSONArray("values");
            //Get the timeperiod

            // Get the first temperature value
            if (temperatureValues.length() > 0) {
                JSONObject firstTemperatureValue = temperatureValues.getJSONObject(0);
                myWeather.gridTemp = firstTemperatureValue.getDouble("value");
                myWeather.time = firstTemperatureValue.getString("validTime");
                // Process the first temperature value as needed
                Log.d("Temperature Value", "First Temperature Value: " + myWeather.gridTemp);
            } else {
                Log.w("Temperature Value", "No temperature values found.");
            }
// Extract and sort temperature values

            // Get the time period
            myWeather.time = temperatureValues.getJSONObject(0).getString("validTime");

            // Extract and sort temperature values
            myWeather.temperatureDataList = extractTemperatureData(temperatureValues);

            // Sort the temperature data by validTime
            Collections.sort(myWeather.temperatureDataList, (a, b) -> ((String) a[1]).compareTo((String) b[1]));
            // Process the sorted temperature data as needed
            for (Object[] temperatureData : myWeather.temperatureDataList) {
                double temperature = (double) temperatureData[0];
                String validTime = (String) temperatureData[1];

                // Do something with the temperature data...
                Log.d("Temperature Data", "Temperature: " + temperature + ", Valid Time: " + validTime);
            }

            // Extract dewpoint values array
            JSONObject dewpointObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("dewpoint");
            JSONArray dewpointValues = dewpointObject.getJSONArray("values");

            // Get the first dewpoint value
            if (dewpointValues.length() > 0) {
                JSONObject firstDewpointValue = dewpointValues.getJSONObject(0);
                myWeather.dewpoint = firstDewpointValue.getDouble("value");

                // Process the first dewpoint value as needed
                Log.d("Dewpoint Value", "First Dewpoint Value: " + myWeather.dewpoint);
            } else {
                Log.w("Dewpoint Value", "No dewpoint values found.");
            }

            // Get the maxTemperature value
            JSONObject maxTemperatureObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("maxTemperature");
            JSONArray maxTemperatureValues = maxTemperatureObject.getJSONArray("values");

            // Get the first maxTemperature value
            if (maxTemperatureValues.length() > 0) {
                JSONObject firstMaxTempValue = maxTemperatureValues.getJSONObject(0);
                myWeather.maxTemperature = firstMaxTempValue.getDouble("value");

                // Process the first dewpoint value as needed
                Log.d("Max Temperature Value", "First Max Temperature Value: " + myWeather.maxTemperature);
            } else {
                Log.w("Max Temperature Value", "No Max Temperature values found.");
            }

            // Get the minTemperature value
            JSONObject minTemperatureObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("minTemperature");
            JSONArray minTemperatureValues = minTemperatureObject.getJSONArray("values");

            // Get the first minTemperature value
            if (minTemperatureValues.length() > 0) {
                JSONObject firstMinTempValue = minTemperatureValues.getJSONObject(0);
                myWeather.minTemperature = firstMinTempValue.getDouble("value");

                // Process the first min temperature value as needed
                Log.d("Min Temperature Value", "First Min Temperature Value: " + myWeather.minTemperature);
            } else {
                Log.w("Min Temperature Value", "No Min Temperature values found.");
            }

            // Get the relativeHumidity
            JSONObject relativeHumidityObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("relativeHumidity");
            JSONArray relativeHumidityValues = relativeHumidityObject.getJSONArray("values");

            // Get the first relativeHumidity value
            if (relativeHumidityValues.length() > 0) {
                JSONObject firstRelHumValue = relativeHumidityValues.getJSONObject(0);
                myWeather.relativeHumidity = firstRelHumValue.getDouble("value");

                // Process the first dewpoint value as needed
                Log.d("relativeHumidity Value", "First relativeHumidity Value: " + myWeather.relativeHumidity);
            } else {
                Log.w("relativeHumidity Value", "No relativeHumidity values found.");
            }

            // Get the apparentTemperature
            JSONObject apparentTemperatureObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("apparentTemperature");
            JSONArray apparentTemperatureValues = apparentTemperatureObject.getJSONArray("values");

            // Get the first apparentTemperature value
            if (apparentTemperatureValues.length() > 0) {
                JSONObject firstAppTempValue = apparentTemperatureValues.getJSONObject(0);
                myWeather.apparentTemperature = firstAppTempValue.getDouble("value");

                // Process the first apparentTemperature value as needed
                Log.d("apparentTemperature Value", "First apparentTemperature Value: " + myWeather.apparentTemperature);
            } else {
                Log.w("apparentTemperature Value", "No apparentTemperature values found.");
            }

            // Get the windChill
            JSONObject windChillObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("windChill");
            JSONArray windChillValues = windChillObject.getJSONArray("values");

            // Get the first windChill value
            if (windChillValues.length() > 0) {
                JSONObject firstWindChillValue = windChillValues.getJSONObject(0);
                myWeather.windChill = firstWindChillValue.getDouble("value");

                // Process the first apparentTemperature value as needed
                Log.d("windChill Value", "First windChill Value: " + myWeather.windChill);
            } else {
                Log.w("windChill Value", "No windChill values found.");
            }

            // Get the skyCover
            JSONObject skyCoverObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("skyCover");
            JSONArray skyCoverValues = skyCoverObject.getJSONArray("values");

            // Get the first skyCover value
            if (skyCoverValues.length() > 0) {
                JSONObject firstSkyCoverValue = skyCoverValues.getJSONObject(0);
                myWeather.skyCover = firstSkyCoverValue.getDouble("value");

                // Process the first skyCover value as needed
                Log.d("skyCover Value", "First skyCover Value: " + myWeather.skyCover);
            } else {
                Log.w("skyCover Value", "No skyCover values found.");
            }

            // Get the wind direction
            JSONObject windDirectionObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("windDirection");
            JSONArray windDirectionValues = windDirectionObject.getJSONArray("values");

            // Get the first wind direction value
            if (windDirectionValues.length() > 0) {
                JSONObject firstWindDirectionValue = windDirectionValues.getJSONObject(0);
                myWeather.windDirection = firstWindDirectionValue.getInt("value");

                // Process the first wind direction value as needed
                Log.d("wind direction Value", "First wind direction Value: " + myWeather.windDirection);
            } else {
                Log.w("wind direction Value", "No wind direction values found.");
            }

            // Get the wind speed
            JSONObject windSpeedObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("windSpeed");
            JSONArray windSpeedValues = windSpeedObject.getJSONArray("values");

            // Get the first wind speed value
            if (windSpeedValues.length() > 0) {
                JSONObject firstWindSpeedValue = windSpeedValues.getJSONObject(0);
                myWeather.windSpeed = firstWindSpeedValue.getDouble("value");

                // Process the first wind speed value as needed
                Log.d("wind speed Value", "First wind speed Value: " + myWeather.windSpeed);
            } else {
                Log.w("wind speed Value", "No wind speed values found.");
            }

            // Get the wind gust
            JSONObject windGustObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("windGust");
            JSONArray windGustValues = windGustObject.getJSONArray("values");

            // Get the first wind gust value
            if (windGustValues.length() > 0) {
                JSONObject firstWindGustValue = windGustValues.getJSONObject(0);
                myWeather.windGust = firstWindGustValue.getDouble("value");

                // Process the first wind gust value as needed
                Log.d("wind gust Value", "First wind gust Value: " + myWeather.windGust);
            } else {
                Log.w("wind gust Value", "No wind gust values found.");
            }

            // Get the weather
            JSONObject weatherObject = gridpointData.getJSONObject("properties").getJSONObject("weather");
            JSONArray valuesArray = weatherObject.getJSONArray("values");

            // Iterate through the items in the "values" array
            for (int i = 0; i < valuesArray.length(); i++) {
                // Get the current object from the "values" array
                JSONObject currentValueObject = valuesArray.getJSONObject(i);

                // Check if the "value" array exists in the current object
                if (currentValueObject.has("value")) {
                    JSONArray valueArray = currentValueObject.getJSONArray("value");

                    // Check if there are any objects in the "value" array
                    if (valueArray.length() > 0) {
                        // Get the first object from the "value" array
                        JSONObject valueObject = valueArray.getJSONObject(0);

                        // Access specific properties from the "value" object
                        // Weather information (sunny, rainy, etc.)
                        if (!valueObject.isNull("weather")) {
                            myWeather.weather = valueObject.getString("weather") + "(skipped " + i +"section)";
                            Log.d("Weather Summary", "The weather is: " + myWeather.weather + "(skipped " + i + "section)");
                            // No need to break the loop for "weather"
                        } else {
                            Log.w("Weather Summary", "No weather summary found for item " + i);
                        }

                        // Coverage information
                        if (!valueObject.isNull("coverage")) {
                            myWeather.coverage = valueObject.getString("coverage") + "(skipped " + i +"section)";
                            Log.d("Weather Coverage", "The chance of given weather is: " + myWeather.coverage);
                            break; // Exit the loop if "coverage" is found
                        } else {
                            Log.w("Weather Coverage", "No weather coverage found for item " + i);
                        }
                    }
                }
            }


            // Get the probability of precipitation
            JSONObject probPrecipObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("probabilityOfPrecipitation");
            JSONArray probPrecipValues = probPrecipObject.getJSONArray("values");

            // Get the first probability of precipitation value
            if (probPrecipValues.length() > 0) {
                JSONObject firstProbPrecipValue = probPrecipValues.getJSONObject(0);
                myWeather.probPrecipitation = firstProbPrecipValue.getInt("value");

                // Process the first probability of precipitation value as needed
                Log.d("probability of precipitation Value", "First probability of precipitation Value: " + myWeather.probPrecipitation);
            } else {
                Log.w("probability of precipitation Value", "No probability of precipitation values found.");
            }

            // Get the amount of precipitation
            JSONObject amtRainObject = gridpointData.getJSONObject("properties")
                    .getJSONObject("quantitativePrecipitation");
            JSONArray amtRainValues = amtRainObject.getJSONArray("values");

            // Get the first amount of precipitation value
            if (amtRainValues.length() > 0) {
                JSONObject firsAmtRainValue = amtRainValues.getJSONObject(0);
                myWeather.amtRain = firsAmtRainValue.getDouble("value");

                // Process the first amount of precipitation value as needed
                Log.d("amount of precipitation Value", "First amount of precipitation Value: " + myWeather.amtRain);
            } else {
                Log.w("amount of precipitation Value", "No amount of precipitation values found.");
            }


        } catch (Exception e) {
            Log.e("Forecast data", "Error extracting values: " + e.getMessage());
        }
    }


    // Method for converting Celsius to Farenheit
    private double convertCtoF(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    //TODO: Further implement these methods for the future development
    // For easier implementation considering the project deadline, decided to just move on with 1hr data for every region in the office provided (MKX,,)
    public void recordToFirebase(DatabaseReference database, Location location) {
        Map<String, Object> coordinate = new HashMap<>();
        //  Map<String, Object> forecastDaily = new HashMap<>();
        // Map<String, Object> forecastHourly = new HashMap<>();
        Map<String, Object> forecast = new HashMap<>();
        Map<String, Object> tempData = new HashMap<>();
        // Add timestamp to the forecast data
        long timestamp = System.currentTimeMillis();
        forecast.put("timestamp", timestamp);
        tempData.put("timestamp", timestamp);
        coordinate.put("timestamp", timestamp);

        LatLng place = new LatLng(location.getLatitude(), location.getLongitude());
        tempData.put("Temp at specific time: ", convertTemperatureDataList(myWeather.temperatureDataList));
        tempData.put("Location: ", place);

        coordinate.put("latitude", location.getLatitude());
        coordinate.put("longitude", location.getLongitude());
        forecast.put ("time for the temperature: " , myWeather.time);
        forecast.put("temperature (°C)", myWeather.gridTemp);
        forecast.put("temperature (°F)", convertCtoF(myWeather.gridTemp));
        forecast.put("dewpoint (°C)", myWeather.dewpoint);
        forecast.put("dewpoint (°F)", convertCtoF(myWeather.dewpoint));
        forecast.put("maxTemperature (°C)", myWeather.maxTemperature);
        forecast.put("maxTemperature (°F)", convertCtoF(myWeather.maxTemperature));
        forecast.put("minTemperature (°C)", myWeather.minTemperature);
        forecast.put("minTemperature (°F)", convertCtoF(myWeather.minTemperature));
        forecast.put("relative Humidity (%)", myWeather.relativeHumidity);
        forecast.put("apparent temperature (°C)", myWeather.apparentTemperature);
        forecast.put("apparent temperature (°F)", convertCtoF(myWeather.apparentTemperature));
        forecast.put("wind chill temperature (°C)", myWeather.windChill);
        forecast.put("wind chill temperature (°F)", convertCtoF(myWeather.windChill));
        forecast.put("sky cover percentage", myWeather.skyCover);
        forecast.put("wind direction (in degrees)", myWeather.windDirection);
        forecast.put("wind gust speed (km per h)", myWeather.windGust);
        forecast.put("brief weather summary", myWeather.weather);
        forecast.put("chance of the weather", myWeather.coverage);
        forecast.put("probability of precipitation", myWeather.probPrecipitation);
        forecast.put("amount of rain", myWeather.amtRain);

        // Store data to Firebase
        database.child("locations").push().setValue(coordinate);
        database.child("forecast_data").push().setValue(forecast);
        database.child("temperature forecast data").push().setValue(tempData);

    }

    // Wrapper method to push weather forecast data to Firebase
    public void pushWeatherDataToFirebase(Location location) {
        // Get the Firebase database reference
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Check if the location already exists in the set
        String locationKey = getLocationKey(location);
        if (!locationSet.contains(locationKey)) {
            // Location does not exist, proceed with database operations
            // ...

            // Add the location to the set
            locationSet.add(locationKey);

            // Clean up outdated records for each section before recording new data
            cleanUpData(database.child("forecast_data"));
            cleanUpData(database.child("locations"));
            cleanUpData(database.child("temperature forecast data"));

            // Record weather data to Firebase
            recordToFirebase(database, location);
        } else {
            // Log statement indicating that data didn't update because location key is the same
            Log.d("Location Update", "Data not updated because location key is the same: " + locationKey);
        }
    }
    // Helper method to get a unique key for a location
    private String getLocationKey(Location location) {
        return location.getLatitude() + "," + location.getLongitude();
    }

    // Helper method to clean up outdated records in a specific section
    private void cleanUpData(DatabaseReference sectionRef) {
        // Set the threshold age to 10 seconds
        long THRESHOLD_AGE = 300 * 1000; // 300 seconds in milliseconds

        long currentTime = System.currentTimeMillis();
        long lowerLimitTimestamp = currentTime - THRESHOLD_AGE;

        sectionRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Iterate through the records within the section and remove outdated ones
                for (DataSnapshot recordSnapshot : dataSnapshot.getChildren()) {
                    // Retrieve timestamp value
                    Long timestampValue = recordSnapshot.child("timestamp").getValue(Long.class);

                    // Check if timestampValue is not null before proceeding
                    if (timestampValue != null) {
                        long recordTimestamp = timestampValue.longValue();

                        // Check if the record is outdated
                        if (recordTimestamp < lowerLimitTimestamp) {
                            recordSnapshot.getRef().removeValue();
                        }
                    } else {
                        // Handle the case where timestampValue is null
                        Log.e("Clean-Up", "Timestamp value is null for recordSnapshot: " + recordSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors during clean-up for the section
                Log.e("Clean-Up", "Error during clean-up for section " + sectionRef.getKey() + ": " + databaseError.getMessage());
            }
        });
    }




    // Convert temperatureDataList to a list of maps for Firebase storage
    private List<Map<String, Object>> convertTemperatureDataList(List<Object[]> temperatureDataList) {
        List<Map<String, Object>> convertedList = new ArrayList<>();

        for (Object[] temperatureData : temperatureDataList) {
            double temperature = (double) temperatureData[0];
            String validTime = (String) temperatureData[1];


            Map<String, Object> temperatureMap = new HashMap<>();
            temperatureMap.put("temperature", temperature);
            temperatureMap.put("validTime", validTime);


            convertedList.add(temperatureMap);
        }

        return convertedList;
    }
    // Method to retrieve weather data from Firebase
    public void retrieveWeatherDataFromFirebase() {
        // Get the Firebase database reference
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Reference to the "forecast_data" node
        DatabaseReference forecastDataRef = database.child("forecast_data");

        // Attach a listener to read the data at "forecast_data" node
        forecastDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method will be called whenever data at "forecast_data" changes
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Retrieve each forecast data entry
                    Map<String, Object> forecastEntry = (Map<String, Object>) snapshot.getValue();

                    // You can now use the forecastEntry Map to access individual data points
                    // For example:
                    Object temperatureCelsius = forecastEntry.get("temperature (°C)");
                    Object temperatureFahrenheit = forecastEntry.get("temperature (°F)");

                    // Do something with the retrieved data...
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
                Log.e("Firebase", "Error reading forecast data: " + databaseError.getMessage());
            }
        });
    }


}
