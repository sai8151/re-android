package com.android.ref;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final long MIN_TIME_INTERVAL = 10 * 100; // 10 seconds
    private static final float MIN_DISTANCE_INTERVAL = 1; // 10 meters

    private LocationManager locationManager;
    private WebView webView;
    TextView messageTextView;
    TextToSpeech tts;
    // Declare variables to store previous message and timestamp
    String previousMessage = "";
    long lastUpdateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        messageTextView=findViewById(R.id.messageTextView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript for Leaflet map

        // Load the HTML file containing the Leaflet map
        webView.loadUrl("file:///android_asset/leaflet_map.html");

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
// Inside your activity or fragment
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US); // Set desired language
                }
            }
        });

        // Check location permissions
        if (checkLocationPermissions()) {
            // Start location updates
            startLocationUpdates();
        } else {
            // Request location permissions
            requestLocationPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop location updates when the activity is destroyed
        stopLocationUpdates();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private boolean checkLocationPermissions() {
        // Check if location permissions are granted
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        // Request location permissions if not granted
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        // Check if location provider is enabled
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Request location updates every 3 seconds (3000 milliseconds) with no minimum distance requirement
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    3000, 0, locationListener);
        } else {
            Log.e("MainActivity", "GPS provider is not enabled");
        }
    }


    private void stopLocationUpdates() {
        // Stop location updates
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Handle location updates
            Log.d("LocationUpdate", "Latitude: " + location.getLatitude() +
                    ", Longitude: " + location.getLongitude());
            // Update the UI or perform any other tasks with the received location data
            updateLocationInWebView(location.getLatitude(), location.getLongitude());
            fetchAndDisplayMessage();
            // Update location in API
            updateLocationInAPI(location.getLatitude(), location.getLongitude());

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    @SuppressLint("JavascriptInterface")
    public void updateLocationInWebView(double latitude, double longitude) {
        // Call JavaScript function in WebView to update the marker position
        String jsFunction = String.format("updateMarkerPosition(%f, %f);", latitude, longitude);
        webView.post(() -> webView.evaluateJavascript(jsFunction, null));
    }


    private void updateLocationInAPI(double latitude, double longitude) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Create JSON object with user location data
                    JSONObject locationData = new JSONObject();
                    locationData.put("user_id", 1); // Replace with actual user ID
                    locationData.put("latitude", latitude);
                    locationData.put("longitude", longitude);

                    // Create connection to API endpoint
                    URL url = new URL("https://saikiranreddy.info/api/update_location.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // Write data to connection output stream
                    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                    writer.write(locationData.toString());
                    writer.flush();
                    writer.close();

                    // Get response code from the server
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Location updated successfully
                        Log.d("LocationUpdate", "Location updated in API successfully");
                    } else {
                        // Error updating location
                        Log.e("LocationUpdate", "Failed to update location in API. Response code: " + responseCode);
                    }

                    // Close connection
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, start location updates
                startLocationUpdates();
            } else {
                // Location permission denied, handle accordingly (e.g., show a message)
                Log.e("MainActivity", "Location permission denied");
            }
        }
    }

    // Method to fetch and display the message
    private void fetchAndDisplayMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://saikiranreddy.info/api/view_mes.php");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Update UI with fetched message
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String message = response.toString();
                            messageTextView.setText(message);

                            long currentTime = System.currentTimeMillis();
                            // Check if message is different or if it's been 30 seconds since last update
                            if (!message.equals(previousMessage) || (currentTime - lastUpdateTime) >= 30000) {
                                // Speak out the message
                                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
                                // Update previous message and timestamp
                                previousMessage = message;
                                lastUpdateTime = currentTime;
                            }
                        }
                    });

                    // Close connection
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Fetch and display message every 30 seconds
                try {
                    Thread.sleep(3000);
                    fetchAndDisplayMessage();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}
