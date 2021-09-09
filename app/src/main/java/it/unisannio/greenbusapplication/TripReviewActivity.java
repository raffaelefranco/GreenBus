package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;


import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.TripNotificationDTO;
import it.unisannio.greenbusapplication.dto.TripRequestDTO;

import it.unisannio.greenbusapplication.util.Values;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class TripReviewActivity extends AppCompatActivity {

    private static final String TAG = "PASSENGER_MAP_ACTIVITY";
    private static String baseUrl;
    private static String urlString;
    private static Gson gson;

    private ListView tripList;
    private ArrayAdapter adapter;
    private Button cancel;

    private WebSocket webSocket;
    private WebSocketListener webSocketListener;
    private OkHttpClient okHttpClient;

    private Integer source;
    private Integer destination;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_review);

        Intent fromCaller = getIntent();
        source = (Integer) fromCaller.getSerializableExtra(getResources().getString(R.string.source_id));
        destination = (Integer) fromCaller.getSerializableExtra(getResources().getString(R.string.destination_id));

        TripRequestDTO tripRequestDTO = new TripRequestDTO();
        tripRequestDTO.setOsmidSource(source);
        tripRequestDTO.setOsmidDestination(destination);
        webSocket.send(gson.toJson(tripRequestDTO, TripRequestDTO.class));

        gson = new Gson();
        baseUrl = Values.localAddress + Values.baseApi;
        urlString = Values.webSocketAddress
                .concat(Values.baseApi)
                .concat("notifications")
                .concat("?ticket=");

        okHttpClient = new OkHttpClient();
        webSocketListener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {

                Log.d(TAG, text);

                if (text.contains(Values.STATUS))
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.request_accepted), Snackbar.LENGTH_LONG).show();

                TripNotificationDTO tripNotificationDTO = null;
                if (text.contains(Values.TRIP)) {
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

        //adapter = new ArrayAdapter(getApplicationContext(), R.layout.trip_list_item, list);
        //tripList.setAdapter(adapter);
        //adapter.notifyDataSetChanged();

        tripList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendRequest(1,1);
            }
        });
    }

    /*private void sendRequest(Integer sourceNode, Integer destinationNode) {
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
    }*/
}