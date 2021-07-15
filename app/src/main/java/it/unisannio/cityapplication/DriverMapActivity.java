package it.unisannio.cityapplication;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.unisannio.cityapplication.dto.RouteDTO;
import it.unisannio.cityapplication.dto.TripNotificationDTO;
import it.unisannio.cityapplication.dto.TripRequestDTO;
import it.unisannio.cityapplication.util.PermissionUtils;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DriverMapActivity extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    private GoogleMap map;
    private UiSettings mUiSettings;
    private List<RouteDTO> routes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }

    /*private void start(String ott, Integer sourceNode, Integer destinationNode) {
        Gson gson = new Gson();
        Request request = new Request.Builder().url("ws://10.0.2.2:8080/api/city/notifications?ticket="+ott).build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                TripNotificationDTO tripNotificationDTO = null;
                if(text.contains("tripId"))
                    tripNotificationDTO = gson.fromJson(text, TripNotificationDTO.class);

                if(tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.APPROVED))
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_ok), Snackbar.LENGTH_LONG).show();
                else if (tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.REJECTED))
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_failed), Snackbar.LENGTH_LONG).show();

            }
        };

        TripRequestDTO tripRequestDTO = new TripRequestDTO();
        tripRequestDTO.setOsmidSource(sourceNode);
        tripRequestDTO.setOsmidDestination(destinationNode);

        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        ws.send(gson.toJson(tripRequestDTO, TripRequestDTO.class));

    }*/


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMyLocationClickListener(this);
        enableMyLocation();

        /*Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .add(
                        new LatLng(-35.016, 143.321),
                        new LatLng(-34.747, 145.592),
                        new LatLng(-34.364, 147.891),
                        new LatLng(-33.501, 150.217),
                        new LatLng(-32.306, 149.248),
                        new LatLng(-32.491, 147.309)));*/

        mUiSettings = map.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);
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
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            }
        }
        else {
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
}
