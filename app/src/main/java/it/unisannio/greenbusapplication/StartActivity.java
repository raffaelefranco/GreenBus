package it.unisannio.greenbusapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.service.GreenBusService;
import it.unisannio.greenbusapplication.util.Values;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StartActivity extends AppCompatActivity {

    private final static String TAG = "START_ACTIVITY";
    private static String baseUrl;
    private List<RouteDTO> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseUrl = Values.localAddress + Values.baseApi;
        routes = new ArrayList<RouteDTO>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                getInitialInfoTask();
            }
        });
        thread.start();
    }

    private void getInitialInfoTask() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            GreenBusService greenBusService = retrofit.create(GreenBusService.class);
            Call<List<RouteDTO>> call = greenBusService.getRoutes();
            Response<List<RouteDTO>> response = null;

            try {
                response = call.execute();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            Response<List<RouteDTO>> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                    intent.putExtra(getResources().getString(R.string.routes), (Serializable) finalResponse.body());
                    startActivity(intent);
                } else {
                    Log.e(TAG, String.valueOf(finalResponse.code()));
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.server_problem), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}