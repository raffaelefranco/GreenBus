package it.unisannio.greenbusapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import it.unisannio.greenbusapplication.dto.NextStationDTO;
import it.unisannio.greenbusapplication.dto.NextStationRequestDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.StationDTO;
import it.unisannio.greenbusapplication.dto.internal.Coordinate;
import it.unisannio.greenbusapplication.util.PermissionUtils;
import it.unisannio.greenbusapplication.util.Values;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DriverMapActivity extends AppCompatActivity implements GoogleMap.OnMyLocationChangeListener, OnMyLocationButtonClickListener, OnMyLocationClickListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "DRIVER_MAP_ACTIVITY";
    private static final String sharedPreferencesName = "GreenBusApplication";
    private static boolean permissionDenied;
    private static boolean isClose;
    private static String baseUrl;
    private SharedPreferences sharedPreferences;
    private GoogleMap googleMap;
    private Polyline polyline;

    private OkHttpClient okHttpClient;
    private WebSocket webSocket = null;

    private List<RouteDTO> routes;
    private NextStationDTO nextStationDTO = null;

    private boolean toBeClose(LatLng latLng1, LatLng latLng2) {
        return distance(latLng1.latitude, latLng1.longitude, latLng2.latitude, latLng2.longitude) < 300d;
    }

    private Double distance(double lat1, double lng1, double lat2, double lng2) {
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
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
        } else {
            PermissionUtils.requestPermission(this, 1, Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        baseUrl = Values.localAddress + Values.baseApi;
        okHttpClient = new OkHttpClient();

        Intent fromCaller = getIntent();
        String oneTimeTicket = (String) fromCaller.getSerializableExtra(getResources().getString(R.string.oneTimeTicket));
        String licensePlate = (String) fromCaller.getSerializableExtra(getResources().getString(R.string.license_place));
        routes = (List<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);

        Gson gson = new Gson();
        String urlString = Values.webSocketAddress
                .concat(Values.baseApi)
                .concat("drivers")
                .concat("?ticket=")
                .concat(oneTimeTicket)
                .concat("&licensePlate=")
                .concat(licensePlate);

        Request request = new Request.Builder().url(urlString).build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);
                nextStationDTO = gson.fromJson(text, NextStationDTO.class);
                isClose = false;

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
                            if (polyline != null)
                                polyline.remove();
                            polyline = googleMap.addPolyline(new PolylineOptions()
                                    .color(R.color.purple_500)
                                    .clickable(true)
                                    .add(latLngs.toArray(new LatLng[latLngs.size()])));

                            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                NotificationChannel notificationChannel = new NotificationChannel(Values.channelId, Values.channelName, NotificationManager.IMPORTANCE_HIGH);
                                notificationChannel.enableLights(true);
                                notificationManager.createNotificationChannel(notificationChannel);
                            }
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(DriverMapActivity.this, Values.channelId)
                                    .setSmallIcon(R.drawable.green_marker)
                                    .setContentTitle(getResources().getString(R.string.go))
                                    .setContentText(getResources().getString(R.string.new_route))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            manager.notify(0, builder.build());
                        }

                    });
                }
            }
        };

        webSocket = okHttpClient.newWebSocket(request, listener);
        okHttpClient.dispatcher().executorService().shutdown();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMyLocationButtonClickListener(this);
        this.googleMap.setOnMyLocationClickListener(this);
        this.googleMap.setOnMyLocationChangeListener(this);
        enableMyLocation();

        List<Marker> stationMarkers = new ArrayList<Marker>();
        List<StationDTO> stations = new ArrayList<StationDTO>();
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(routes.get(routes.size() / 2).getStations().get(0).getPosition().getLatitude(),
                        routes.get(routes.size() / 2).getStations().get(0).getPosition().getLongitude()), 11F));

        Marker marker = null;
        for (RouteDTO r : routes) {
            for (StationDTO s : r.getStations()) {
                if (!stations.contains(s)) {
                    stations.add(s);
                    marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(s.getPosition().getLatitude(), s.getPosition().getLongitude())));
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.green_marker));
                    stationMarkers.add(marker);
                }
            }
        }

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);

    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, getResources().getString(R.string.location_button), Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this,
                getResources().getString(R.string.current_location)
                        .concat(" ")
                        .concat(String.valueOf(location.getLatitude()))
                        .concat(",")
                        .concat(String.valueOf(location.getLongitude())), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
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
        Gson gson = new Gson();
        if (!isClose && nextStationDTO != null && toBeClose(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude()))) {
            webSocket.send(gson.toJson(new NextStationRequestDTO(nextStationDTO.getNextStation())));
            isClose = true;
        } else if (nextStationDTO != null && !toBeClose(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(nextStationDTO.getNextStation().getPosition().getLatitude(), nextStationDTO.getNextStation().getPosition().getLongitude()))) {
            isClose = false;
        }
    }
}
