package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.cityapplication.dto.JWTTokenDTO;
import it.unisannio.cityapplication.dto.LoginDTO;
import it.unisannio.cityapplication.service.CityService;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Login";
    public static final String prefName = "CityApplication";
    private SharedPreferences preferences;
    private static String baseURI;
    private EditText username;
    private EditText password;
    private Button login;
    private Button signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        baseURI = getString(R.string.local) + "/api/city/";

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login);
        signIn = (Button) findViewById(R.id.sign_in);

        preferences = getSharedPreferences(prefName, MODE_PRIVATE);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (username.getText().toString().length() != 0 && password.getText().toString().length() != 0)
                    loginTask(username.getText().toString(), password.getText().toString());
                else
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.login_failed), Snackbar.LENGTH_LONG).show();
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        });

    }

    private void loginTask(String username, String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURI)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            CityService cityService = retrofit.create(CityService.class);
            Call<JWTTokenDTO> call = cityService.getTokenForLogin(new LoginDTO(username, password));
            Response<JWTTokenDTO> response = null;

            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Response<JWTTokenDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("jwt", String.valueOf(finalResponse.body().getJwt())).apply();
                    Intent intent = new Intent(LoginActivity.this, SocketActivity.class);
                    startActivity(intent);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.login_failed), Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public void onBackPressed() {

    }
}