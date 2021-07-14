package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import it.unisannio.cityapplication.dto.RouteDTO;
import it.unisannio.cityapplication.service.CityService;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "Map";
    private static String baseURI;
    private List<RouteDTO> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        baseURI = getString(R.string.local) + "/api/city/";
        routes = new ArrayList<RouteDTO>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }

                } finally {
                    finish();
                }
                stationsTask();
            }

        });
        thread.start();

    }

    private void stationsTask() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {

            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURI)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            CityService cityService = retrofit.create(CityService.class);
            Call<List<RouteDTO>> call = cityService.getRoutes();
            Response<List<RouteDTO>> response = null;

            try {
                response = call.execute();
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }

            Response<List<RouteDTO>> finalResponse = response;
            handler.post(() -> {

                if(finalResponse.code() == 200) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra(getResources().getString(R.string.routes), (Serializable) finalResponse.body());
                    startActivity(intent);

                }

            });
        });
    }
}