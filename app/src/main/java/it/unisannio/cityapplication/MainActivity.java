package it.unisannio.cityapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;
import java.util.List;

import it.unisannio.cityapplication.dto.RouteDTO;
import it.unisannio.cityapplication.dto.StationDTO;

public class MainActivity extends AppCompatActivity {

    private final String baseURI = "http://10.0.2.2:8094/api/routes";
    private final String TAG = "Map";
    private List<RouteDTO> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        routes = new ArrayList<RouteDTO>();



        new StationsRestTask().execute();

    }

    public class StationsRestTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            ClientResource clientResource = new ClientResource(baseURI);
            Gson gson = new Gson();
            String gsonResponse = null;

            try {
                gsonResponse = clientResource.get().getText();
                if(clientResource.getStatus().getCode() == 200) {
                    routes = gson.fromJson(gsonResponse, new TypeToken<ArrayList<RouteDTO>>() {}.getType());

                    return 1;
                } else {
                    return -1;
                }

            } catch(ResourceException | IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer res) {
            if(res == 1) {

                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                startActivity(intent);

            }

        }
    }
}