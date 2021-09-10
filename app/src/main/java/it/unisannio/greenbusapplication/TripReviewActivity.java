package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import it.unisannio.greenbusapplication.dto.ConfirmationDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.TicketDTO;
import it.unisannio.greenbusapplication.dto.TripNotificationDTO;
import it.unisannio.greenbusapplication.dto.TripRequestDTO;

import it.unisannio.greenbusapplication.dto.internal.TripDTO;
import it.unisannio.greenbusapplication.service.GreenBusService;
import it.unisannio.greenbusapplication.util.Values;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TripReviewActivity extends AppCompatActivity {

    private static final String TAG = "PASSENGER_MAP_ACTIVITY";
    private static final String sharedPreferencesName = "GreenBusApplication";
    private static String baseUrl;
    private static String urlString;
    private static Gson gson;

    private SharedPreferences sharedPreferences;
    private ListView tripList;
    private ArrayAdapter adapter;
    private Button cancel;

    private WebSocket webSocket;
    private WebSocketListener webSocketListener;
    private OkHttpClient okHttpClient;

    private Integer source;
    private Integer destination;
    private ArrayList<String> list;
    private ArrayList<TripDTO> trips;
    private String oneTimeTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_review);

        Intent fromCaller = getIntent();
        source = (Integer) fromCaller.getSerializableExtra(getResources().getString(R.string.source_id));
        destination = (Integer) fromCaller.getSerializableExtra(getResources().getString(R.string.destination_id));

        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        gson = new Gson();
        baseUrl = Values.localAddress + Values.baseApi;
        urlString = Values.webSocketAddress
                .concat(Values.baseApi)
                .concat("notifications")
                .concat("?ticket=");

        if(oneTimeTicket == null) {
            getOneTimeTicketTask(source, destination);
        }

        okHttpClient = new OkHttpClient();
        webSocketListener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);

                if (text.contains(Values.STATUS)) {
                    ConfirmationDTO confirmationDTO = null;
                    confirmationDTO = gson.fromJson(text, ConfirmationDTO.class);

                    if (text.contains(Values.MULTI_PATHS)) {
                        Handler mainHandler = new Handler(getMainLooper());
                        ConfirmationDTO finalConfirmationDTO = confirmationDTO;
                        trips = (ArrayList<TripDTO>) finalConfirmationDTO.getTrips();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                for(TripDTO t : finalConfirmationDTO.getTrips()) {
                                    list.add("- ".concat(getResources().getString(R.string.source))
                                            .concat(" ")
                                            .concat(getResources().getString(R.string.station))
                                            .concat(" ")
                                            .concat(String.valueOf(t.getSource()))
                                            .concat(" → ")
                                            .concat(getResources().getString(R.string.destination))
                                            .concat(" ")
                                            .concat(getResources().getString(R.string.station))
                                            .concat(" ")
                                            .concat(String.valueOf(t.getDestination())));
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
                    } else
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_accepted), Snackbar.LENGTH_LONG).show();
                }

                if (text.contains(Values.TRIP)) {
                    TripNotificationDTO tripNotificationDTO = null;
                    tripNotificationDTO = gson.fromJson(text, TripNotificationDTO.class);
                    if (tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.APPROVED)) {
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.vehicle_found, tripNotificationDTO.getVehicleLicensePlate(), tripNotificationDTO.getPickUpNodeId()), Snackbar.LENGTH_LONG).show();

                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            NotificationChannel notificationChannel = new NotificationChannel(Values.channelId, Values.channelName, NotificationManager.IMPORTANCE_HIGH);
                            notificationChannel.enableLights(true);
                            notificationManager.createNotificationChannel(notificationChannel);
                        }
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(TripReviewActivity.this, Values.channelId)
                                .setSmallIcon(R.drawable.green_marker)
                                .setContentTitle(getResources().getString(R.string.vehicle_found_title))
                                .setContentText(getResources().getString(R.string.vehicle_found_description, tripNotificationDTO.getVehicleLicensePlate(), tripNotificationDTO.getPickUpNodeId()))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        manager.notify(0, builder.build());
                    } else if (tripNotificationDTO != null && tripNotificationDTO.getStatus().equals(TripNotificationDTO.Status.REJECTED))
                        Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_rejected), Snackbar.LENGTH_LONG).show();
                }
            }
        };

        tripList = (ListView) findViewById(R.id.trip_list);

        cancel = (Button) findViewById(R.id.cancel);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TripReviewActivity.this, PassengerMapActivity.class);
                startActivity(intent);
            }
        });

        list = new ArrayList<>();

        adapter = new ArrayAdapter(getApplicationContext(), R.layout.trip_list_item, list);
        tripList.setAdapter(adapter);

        tripList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                sendRequest(trips.get(position).getSource(), trips.get(position).getDestination());
            }
        });
    }

    private void getOneTimeTicketTask(Integer sourceNode, Integer destinationNode) {

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
                    oneTimeTicket = finalResponse.body().getOneTimeTicket();
                    sendRequest(sourceNode, destinationNode);
                } else {
                    Log.e(TAG, String.valueOf(finalResponse.code()));
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.server_problem), Toast.LENGTH_LONG).show();
                }
            });
        });

    }

    private void sendRequest(Integer sourceNode, Integer destinationNode) {
        TripRequestDTO tripRequestDTO = new TripRequestDTO();
        tripRequestDTO.setOsmidSource(sourceNode);
        tripRequestDTO.setOsmidDestination(destinationNode);

        if(webSocket == null) {
            urlString = urlString.concat(oneTimeTicket);
            Request request = new Request.Builder().url(urlString).build();
            webSocket = okHttpClient.newWebSocket(request, webSocketListener);
            okHttpClient.dispatcher().executorService().shutdown();
        }

        webSocket.send(gson.toJson(tripRequestDTO, TripRequestDTO.class));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TripReviewActivity.this, PassengerMapActivity.class);
        startActivity(intent);
        finish();
    }

}