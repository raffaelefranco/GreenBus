package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import it.unisannio.cityapplication.dto.TicketDTO;
import it.unisannio.cityapplication.dto.TripRequestDTO;
import it.unisannio.cityapplication.service.CityService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SocketActivity extends AppCompatActivity {

    private static final String TAG = "Socket";
    public static final String prefName = "CityApplication";
    private SharedPreferences preferences;
    private static String baseURI;
    private OkHttpClient client;
    private TextView output;

    private void start(String ott) {

        Request request = new Request.Builder().url("ws://10.0.2.2:8080/api/city/notifications?ticket="+ott).build();

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, text);
            }
        };

        TripRequestDTO tripRequestDTO = new TripRequestDTO();
        tripRequestDTO.setOsmidSource(500);
        tripRequestDTO.setOsmidDestination(30);
        Gson gson = new Gson();

        WebSocket ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();

        ws.send(gson.toJson(tripRequestDTO, TripRequestDTO.class));

        //WIP
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);

        client = new OkHttpClient();
        baseURI = getString(R.string.local) + "/api/city/";
        preferences = getSharedPreferences(prefName, MODE_PRIVATE);
        output = (TextView) findViewById(R.id.textView2);

        new TicketTask().execute();

    }

    public class TicketTask extends AsyncTask<String, Integer, retrofit2.Response<TicketDTO>> {

        @Override
        protected retrofit2.Response<TicketDTO> doInBackground(String... params) {

            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURI)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            CityService cityService = retrofit.create(CityService.class);

            String typeAuth = "Bearer ";
            String jwt = preferences.getString("jwt", null);

            Call<TicketDTO> call = cityService.getTicket(typeAuth.concat(jwt));

            retrofit2.Response<TicketDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(retrofit2.Response<TicketDTO> response) {
            if (response.code() == 200) {
                start(response.body().getOneTimeTicket());
            } else {
                // TO DO
            }
        }
    }
}