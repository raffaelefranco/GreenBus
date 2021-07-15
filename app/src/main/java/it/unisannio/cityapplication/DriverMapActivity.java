package it.unisannio.cityapplication;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.cityapplication.dto.NextStationDTO;
import it.unisannio.cityapplication.dto.NextStationRequestDTO;
import it.unisannio.cityapplication.dto.RouteDTO;
import it.unisannio.cityapplication.dto.StationDTO;
import it.unisannio.cityapplication.dto.TicketDTO;
import it.unisannio.cityapplication.dto.TripNotificationDTO;
import it.unisannio.cityapplication.dto.TripRequestDTO;
import it.unisannio.cityapplication.dto.internal.Coordinate;
import it.unisannio.cityapplication.service.CityService;
import it.unisannio.cityapplication.util.PermissionUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DriverMapActivity extends AppCompatActivity implements GoogleMap.OnMyLocationChangeListener, OnMyLocationButtonClickListener, OnMyLocationClickListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private final String TAG = "DriverMap";
    public static final String prefName = "CityApplication";
    private static String baseURI;
    private SharedPreferences preferences;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    private GoogleMap map;
    private UiSettings mUiSettings;
    private List<RouteDTO> routes;
    private OkHttpClient client;
    private Location myLocation;
    private Polyline polyline;
    private String ticket;
    private WebSocket ws = null;
    private NextStationDTO nextStationDTO = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        preferences = getSharedPreferences(prefName, MODE_PRIVATE);
        baseURI = getString(R.string.local) + "/api/city/";

        Intent fromCaller = getIntent();
        ticket = (String) fromCaller.getSerializableExtra(getResources().getString(R.string.ticket));
        client = new OkHttpClient();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Gson gson = new Gson();
        Request request = new Request.Builder().url("ws://10.0.2.2:8080/api/city/drivers?ticket=" + ticket + "&licensePlate=FZ215RY").build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);

                nextStationDTO = gson.fromJson(text, NextStationDTO.class);
                if (nextStationDTO.getMinPath() != null) {

                    LatLng latLng = new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude());

                    Handler mainHandler = new Handler(getMainLooper());
                    NextStationDTO finalNextStationDTO = nextStationDTO;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            map.addMarker(new MarkerOptions().position(latLng));

                            for (Coordinate coordinate : finalNextStationDTO.getMinPath()) {
                                map.addPolyline(new PolylineOptions().clickable(true).
                                        add(new LatLng(coordinate.getLatitude(), coordinate.getLongitude())));
                            }
                        }

                    });
                }
            }


        };

        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        map.setOnMyLocationChangeListener(this);
        enableMyLocation();

        mUiSettings = map.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);

    }

    private boolean toBeClose(LatLng latLng1, LatLng latLng2) {
        return distance(latLng1.latitude, latLng1.longitude, latLng2.latitude, latLng2.longitude) < 10000d;
    }

    public Double distance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(lat2 - lat1);
        double lngDiff = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return distance * meterConversion;
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (map != null) {
                map.setMyLocationEnabled(true);
            }
        } else {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            }
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            permissionDenied = false;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog title = new AlertDialog.Builder(DriverMapActivity.this)
                .setTitle(getResources().getString(R.string.confirm_exit))
                .setIcon(R.drawable.ic_baseline_directions_car_24)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    @Override
    public void onMyLocationChange(@NonNull @NotNull Location location) {
        Gson gson = new Gson();
        if(toBeClose(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude()))) {
            ws.send(gson.toJson(new NextStationRequestDTO(nextStationDTO.getNextStation())));
        }
    }
}
