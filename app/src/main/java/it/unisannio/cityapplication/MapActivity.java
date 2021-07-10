package it.unisannio.cityapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unisannio.cityapplication.dto.RouteDTO;
import it.unisannio.cityapplication.dto.StationDTO;
import it.unisannio.cityapplication.exception.StationException;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private final String TAG = "Map";
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private UiSettings mUiSettings;

    private List<Marker> stationMarkers;
    private Map<String, List<StationDTO>> stationsOnRoutes;
    private List<StationDTO> stations;
    private List<RouteDTO> routes;
    private StationDTO source;
    private StationDTO destination;
    private Marker sourceMarker;
    private Marker destinationMarker;

    private StationDTO getStationByMarker(Marker marker) {
        for (StationDTO s : stations)
            if (checkCoordinates(marker.getPosition(), s))
                return s;

        throw new StationException();

    }


    private boolean checkCoordinates(LatLng latLng, StationDTO s) {
        return latLng.latitude == s.getLatitude() && latLng.longitude == s.getLongitude();
    }

    private List<String> getAssociateRoute(Marker marker) {

        List<String> routeStrings = new ArrayList<>();

        for (RouteDTO r : routes) {
            for (StationDTO s : r.getStations()) {
                if (checkCoordinates(marker.getPosition(), s)) {
                    routeStrings.add(r.getId());
                }
            }
        }
        return routeStrings;

    }

    private ArrayList<Marker> getMarkersByStations(ArrayList<StationDTO> stationsR) {
        ArrayList<Marker> markers = new ArrayList<>();

        for (StationDTO s : stationsR)
            for (Marker m : stationMarkers)
                if (checkCoordinates(m.getPosition(), s))
                    markers.add(m);
        return markers;
    }

    private boolean checkRoutes(Marker sourceMarker, Marker destinationMarker) {
        List<String> sourceMarkerStrings = getAssociateRoute(sourceMarker);
        List<String> destinationMarkerStrings = getAssociateRoute(destinationMarker);

        for (String s : sourceMarkerStrings) {
            for (String d : destinationMarkerStrings) {
                if (s.equals(d))
                    return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        /*routes = new ArrayList<>();
        RouteDTO r1 = new RouteDTO();
        r1.setId("Prima");

        StationDTO s1 = new StationDTO();
        s1.setNodeId(1);
        s1.setLatitude(40d);
        s1.setLongitude(40d);
        StationDTO s2 = new StationDTO();
        s2.setNodeId(2);
        s2.setLatitude(42d);
        s2.setLongitude(42d);
        StationDTO s3 = new StationDTO();
        s3.setNodeId(3);
        s3.setLatitude(44d);
        s3.setLongitude(44d);
        List<StationDTO> l1 = new ArrayList<StationDTO>();
        l1.add(s1);
        l1.add(s2);
        l1.add(s3);
        r1.setStations(l1);

        RouteDTO r2 = new RouteDTO();
        r2.setId("Seconda");

        StationDTO s12 = new StationDTO();
        s12.setNodeId(3);
        s12.setLatitude(44d);
        s12.setLongitude(44d);
        StationDTO s22 = new StationDTO();
        s22.setNodeId(4);
        s22.setLatitude(39d);
        s22.setLongitude(39d);
        StationDTO s32 = new StationDTO();
        s32.setNodeId(4);
        s32.setLatitude(38d);
        s32.setLongitude(38d);
        List<StationDTO> l2 = new ArrayList<StationDTO>();
        l2.add(s12);
        l2.add(s22);
        l2.add(s32);
        r2.setStations(l2);

        routes.add(r1);
        routes.add(r2);*/

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        source = null;
        destination = null;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);

        Marker marker = null;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(routes.get(routes.size() / 2).getStations().get(0).getLatitude(),
                        routes.get(routes.size() / 2).getStations().get(0).getLongitude()), 9F));


        stationMarkers = new ArrayList<Marker>();
        stations = new ArrayList<StationDTO>();
        stationsOnRoutes = new HashMap<String, List<StationDTO>>();

        for (RouteDTO r : routes) {
            stationsOnRoutes.put(r.getId(), r.getStations());
            for (StationDTO s : r.getStations()) {
                if (!stations.contains(s)) {
                    stations.add(s);
                    marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(s.getLatitude(), s.getLongitude())));
                    //marker.setAlpha(0.6f);
                    stationMarkers.add(marker);
                }
            }
        }

        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setMapToolbarEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                List<String> routeStrings = getAssociateRoute(marker);

                ArrayList<StationDTO> stationsByRoute = new ArrayList<>();
                for (String r : routeStrings)
                    stationsByRoute.addAll(stationsOnRoutes.get(r));

                ArrayList<Marker> markersToColor = getMarkersByStations(stationsByRoute);

                for (Marker m : markersToColor)
                    m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                marker.setAlpha(0.3f);

                if (source == null && destination == null) {
                    sourceMarker = marker;
                    source = getStationByMarker(sourceMarker);

                    //Snackbar.make(findViewById(android.R.id.content), source.getNodeId().toString(), Snackbar.LENGTH_LONG).show();


                } else if (source != null && destination == null) {

                    destinationMarker = marker;
                    destination = getStationByMarker(destinationMarker);

                    if (checkRoutes(sourceMarker, destinationMarker) && !destination.equals(source)) {
                        AlertDialog title = new AlertDialog.Builder(MapActivity.this)
                                .setTitle(getResources().getString(R.string.confirm_title))
                                .setMessage(getResources().getString(R.string.source_info)
                                        .concat(source.getNodeId().toString())
                                        .concat("\n")
                                        .concat(getResources().getString(R.string.destination_info))
                                        .concat(destination.getNodeId().toString()))
                                .setIcon(R.drawable.ic_baseline_directions_car_24)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // action to sì --> SERVER
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        source = null;
                                        destination = null;
                                        sourceMarker = null;
                                        destinationMarker = null;
                                        for (Marker m : stationMarkers) {
                                            m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                            m.setAlpha(1f);
                                        }
                                    }
                                }).show();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.rejected_proposal), Snackbar.LENGTH_LONG).show();
                        source = null;
                        destination = null;
                        sourceMarker = null;
                        destinationMarker = null;
                        for (Marker m : stationMarkers) {
                            m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            m.setAlpha(1f);
                        }
                    }

                }

                return true;
            }
        });

    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("Map", latLng.toString());
    }

}