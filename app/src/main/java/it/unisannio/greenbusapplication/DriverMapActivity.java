package it.unisannio.greenbusapplication;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import it.unisannio.greenbusapplication.dto.NextStationDTO;
import it.unisannio.greenbusapplication.dto.NextStationRequestDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.StationDTO;
import it.unisannio.greenbusapplication.dto.internal.Coordinate;
import it.unisannio.greenbusapplication.util.ConstantValues;
import it.unisannio.greenbusapplication.util.PermissionUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DriverMapActivity extends AppCompatActivity implements GoogleMap.OnMyLocationChangeListener, OnMyLocationButtonClickListener, OnMyLocationClickListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private final String TAG = "DriverMap";
    public static final String prefName = "CityApplication";
    private static String baseUrl;
    private SharedPreferences preferences;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    private GoogleMap map;
    private UiSettings mUiSettings;
    private List<RouteDTO> routes;
    private OkHttpClient client;
    private Location oldLocation;
    private Polyline polyline;
    private String ticket;
    private WebSocket ws = null;
    private NextStationDTO nextStationDTO = null;
    private List<Marker> stationMarkers;
    private List<StationDTO> stations;
    private static boolean isClose;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        preferences = getSharedPreferences(prefName, MODE_PRIVATE);
        baseUrl = ConstantValues.localAddress + ConstantValues.baseApi;

        Intent fromCaller = getIntent();
        ticket = (String) fromCaller.getSerializableExtra(getResources().getString(R.string.ticket));
        routes = (List<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));
        client = new OkHttpClient();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Gson gson = new Gson();
        Request request = new Request.Builder().url("ws://10.0.2.2:8080/api/city/drivers?ticket=" + ticket + "&licensePlate=FF222OO").build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);
                nextStationDTO = gson.fromJson(text, NextStationDTO.class);
                    if (nextStationDTO.getMinPath() != null) {


                    Handler mainHandler = new Handler(getMainLooper());
                    NextStationDTO finalNextStationDTO = nextStationDTO;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<LatLng> latLngs = new ArrayList<>();

                            for (Coordinate coordinate : finalNextStationDTO.getMinPath()) {
                                LatLng latLng = new LatLng(coordinate.getLatitude(), coordinate.getLongitude());
                                latLngs.add(latLng);
                            }
                            if(polyline != null)
                                polyline.remove();
                            polyline = map.addPolyline(new PolylineOptions()
                                    .color(R.color.purple_500)
                                    .clickable(true)
                                    .add(latLngs.toArray(new LatLng[latLngs.size()])));
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

        stationMarkers = new ArrayList<Marker>();
        stations = new ArrayList<StationDTO>();

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(routes.get(routes.size() / 2).getStations().get(0).getPosition().getLatitude(),
                        routes.get(routes.size() / 2).getStations().get(0).getPosition().getLongitude()), 11F));


        Marker marker = null;
        for (RouteDTO r : routes) {
            for (StationDTO s : r.getStations()) {
                if (!stations.contains(s)) {
                    stations.add(s);
                    marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(s.getPosition().getLatitude(), s.getPosition().getLongitude())));
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.green_marker));
                    //marker.setAlpha(0.6f);
                    stationMarkers.add(marker);
                }
            }
        }

        mUiSettings = map.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);

    }

    private boolean toBeClose(LatLng latLng1, LatLng latLng2) {
        return distance(latLng1.latitude, latLng1.longitude, latLng2.latitude, latLng2.longitude) < 300d;
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
                .setIcon(R.drawable.ic_bus)
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

        /*if(oldLocation != null && !toBeClose(new LatLng(oldLocation.getLatitude(), oldLocation.getLatitude()), new LatLng(location.getLatitude(), location.getLongitude()))) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(),
                            location.getLongitude()), 12F));
        }
        oldLocation=location;*/

        Gson gson = new Gson();
        if(!isClose && nextStationDTO != null && toBeClose(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude()))) {
            ws.send(gson.toJson(new NextStationRequestDTO(nextStationDTO.getNextStation())));
            isClose = true;
        } else if(!toBeClose(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude()))){
            isClose = false;
        }
    }
}
