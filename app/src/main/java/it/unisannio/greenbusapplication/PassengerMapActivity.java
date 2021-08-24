package it.unisannio.greenbusapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.StationDTO;
import it.unisannio.greenbusapplication.dto.TicketDTO;
import it.unisannio.greenbusapplication.dto.TripNotificationDTO;
import it.unisannio.greenbusapplication.dto.TripRequestDTO;
import it.unisannio.greenbusapplication.exception.StationException;
import it.unisannio.greenbusapplication.service.GreenBusService;
import it.unisannio.greenbusapplication.util.ConstantValues;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PassengerMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private static final String TAG = "PASSENGER_MAP_ACTIVITY";
    private static final String sharedPreferencesName = "GreenBusApplication";
    private static String baseUrl;
    private SharedPreferences sharedPreferences;
    private GoogleMap googleMap;
    private SupportMapFragment supportMapFragment;
    private UiSettings uiSettings;
    private OkHttpClient okHttpClient;

    private List<Marker> stationMarkers;
    private Marker sourceMarker;
    private Marker destinationMarker;

    private List<StationDTO> stations;
    private Map<String, List<StationDTO>> stationsOnRoutes;
    private List<RouteDTO> routes;
    private StationDTO source;
    private StationDTO destination;

    private boolean checkCoordinates(LatLng latLng, StationDTO s) {
        return latLng.latitude == s.getPosition().getLatitude() && latLng.longitude == s.getPosition().getLongitude();
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
    private StationDTO getStationByMarker(Marker marker) {
        for (StationDTO s : stations)
            if (checkCoordinates(marker.getPosition(), s))
                return s;

        throw new StationException();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_map);

        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        baseUrl = ConstantValues.localAddress + ConstantValues.baseApi;

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        okHttpClient = new OkHttpClient();
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
        source = null;
        destination = null;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnMapClickListener(this);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(routes.get(routes.size() / 2).getStations().get(0).getPosition().getLatitude(),
                        routes.get(routes.size() / 2).getStations().get(0).getPosition().getLongitude()), 11F));


        Marker marker = null;
        stationMarkers = new ArrayList<Marker>();
        stations = new ArrayList<StationDTO>();
        stationsOnRoutes = new HashMap<String, List<StationDTO>>();

        for (RouteDTO r : routes) {
            stationsOnRoutes.put(r.getId(), r.getStations());
            for (StationDTO s : r.getStations()) {
                if (!stations.contains(s)) {
                    stations.add(s);
                    marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(s.getPosition().getLatitude(), s.getPosition().getLongitude())));
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_marker));
                    //marker.setAlpha(0.6f);
                    stationMarkers.add(marker);
                }
            }
        }

        uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                List<String> routeStrings = getAssociateRoute(marker);

                ArrayList<StationDTO> stationsByRoute = new ArrayList<>();
                for (String r : routeStrings)
                    stationsByRoute.addAll(stationsOnRoutes.get(r));

                ArrayList<Marker> markersToColor = getMarkersByStations(stationsByRoute);

                for (Marker m : markersToColor)
                    m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.green_marker));

                marker.setAlpha(0.3f);

                if (source == null && destination == null) {
                    sourceMarker = marker;
                    source = getStationByMarker(sourceMarker);

                } else if (source != null && destination == null) {

                    destinationMarker = marker;
                    destination = getStationByMarker(destinationMarker);

                    if (checkRoutes(sourceMarker, destinationMarker) && !destination.equals(source)) {
                        AlertDialog title = new AlertDialog.Builder(PassengerMapActivity.this)
                                .setTitle(getResources().getString(R.string.confirm_proposal))
                                .setMessage(getResources().getString(R.string.pick_up_point)
                                        .concat(" Station ")
                                        .concat(source.getNodeId().toString())
                                        .concat("\n")
                                        .concat(getResources().getString(R.string.release_point))
                                        .concat(" Station ")
                                        .concat(destination.getNodeId().toString()))
                                .setIcon(R.drawable.ic_bus)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        for (Marker m : stationMarkers) {
                                            m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_marker));
                                            m.setAlpha(1f);
                                        }
                                        getTicketTask(source.getNodeId(), destination.getNodeId());
                                        source = null;
                                        destination = null;
                                        sourceMarker = null;
                                        destinationMarker = null;
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        source = null;
                                        destination = null;
                                        sourceMarker = null;
                                        destinationMarker = null;
                                        for (Marker m : stationMarkers) {
                                            m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_marker));
                                            m.setAlpha(1f);
                                        }
                                    }
                                }).show();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.incorrect_proposal), Snackbar.LENGTH_LONG).show();
                        source = null;
                        destination = null;
                        sourceMarker = null;
                        destinationMarker = null;
                        for (Marker m : stationMarkers) {
                            m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_marker));
                            m.setAlpha(1f);
                        }
                    }

                }

                return true;
            }
        });

    }

    private void getTicketTask(Integer sourceNode, Integer destinationNode) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            GreenBusService greenBusService = retrofit.create(GreenBusService.class);

            String authType = getResources().getString(R.string.authType);
            String jwt = sharedPreferences.getString(getResources().getString(R.string.jwt), null);
            Call<TicketDTO> call = greenBusService.getTicket(authType.concat(" ").concat(jwt));
            Response<TicketDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Response<TicketDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    start(finalResponse.body().getOneTimeTicket(), sourceNode, destinationNode);
                }
            });
        });

    }

    private void start(String ott, Integer sourceNode, Integer destinationNode) {
        Gson gson = new Gson();
        String urlString = ConstantValues.webSocketAddress
                .concat(ConstantValues.baseApi)
                .concat("notifications")
                .concat("?ticket=")
                .concat(ott);
        Request request = new Request.Builder().url(urlString).build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);

                if(text.contains("status"))
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_accepted), Snackbar.LENGTH_LONG).show();

                TripNotificationDTO tripNotificationDTO = null;
                if(text.contains("tripId")) {
                    tripNotificationDTO = gson.fromJson(text, TripNotificationDTO.class);
                    if (tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.APPROVED))
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.vehicle_found, tripNotificationDTO.getVehicleLicensePlate(), tripNotificationDTO.getPickUpNodeId()), Snackbar.LENGTH_LONG).show();

                    else if (tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.REJECTED))
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_rejected), Snackbar.LENGTH_LONG).show();
                }
            }
        };

        TripRequestDTO tripRequestDTO = new TripRequestDTO();
        tripRequestDTO.setOsmidSource(sourceNode);
        tripRequestDTO.setOsmidDestination(destinationNode);

        WebSocket ws = okHttpClient.newWebSocket(request, listener);
        okHttpClient.dispatcher().executorService().shutdown();

        ws.send(gson.toJson(tripRequestDTO, TripRequestDTO.class));

    }

    @Override
    public void onBackPressed() {
        AlertDialog title = new AlertDialog.Builder(PassengerMapActivity.this)
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
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, latLng.toString());
    }

}