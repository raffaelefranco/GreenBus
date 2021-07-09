package it.unisannio.cityapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import it.unisannio.cityapplication.dto.StationDTO;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private UiSettings mUiSettings;
    private Marker myMarker;
    private ArrayList<StationDTO> stations;
    private final String TAG = "Map";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent fromCaller = getIntent();

        stations = (ArrayList<StationDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        Log.d(TAG, stations.get(0).getLatitude().toString());
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);


        for (StationDTO s : stations) {

            LatLng latLng = new LatLng(s.getLatitude(), s.getLongitude());

            myMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if(s.getNodeId()>50) {
                myMarker.setAlpha(0.1f);
            }
            myMarker.getPosition();
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(stations.get(0).getLatitude(), stations.get(0).getLongitude()), 8F));

        mUiSettings = mMap.getUiSettings();

        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng position = marker.getPosition();
                Toast.makeText(
                        MapActivity.this,
                        "Lat " + position.latitude + " "
                                + "Long " + position.longitude,
                        Toast.LENGTH_LONG).show();
                Log.d("Map", position.toString());
                return true;
            }
        });
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("Map", latLng.toString());

    }

}