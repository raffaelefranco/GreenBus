package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.greenbusapplication.dto.SessionDTO;
import it.unisannio.greenbusapplication.dto.RegisterDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.service.GreenBusService;
import it.unisannio.greenbusapplication.util.ConstantValues;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SIGN_IN_ACTIVITY";
    private static final String sharedPreferencesName = "GreenBusApplication";
    private static String baseUrl;
    private SharedPreferences sharedPreferences;
    private EditText firstname;
    private EditText lastname;
    private EditText email;
    private EditText username;
    private EditText password;
    private Button signIn;

    private List<RouteDTO> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        firstname = (EditText) findViewById(R.id.firstname);
        lastname = (EditText) findViewById(R.id.lastname);
        email = (EditText) findViewById(R.id.email);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        signIn = (Button) findViewById(R.id.sign_in);
        sharedPreferences = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE);
        baseUrl = ConstantValues.localAddress + ConstantValues.baseApi;

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstname.getText().toString().length() != 0 && lastname.getText().toString().length() != 0
                        && email.getText().toString().length() != 0 && username.getText().toString().length() != 0
                        && password.getText().toString().length() != 0)
                    signInTask(firstname.getText().toString(), lastname.getText().toString(), email.getText().toString(), username.getText().toString(), password.getText().toString());
                else
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.sign_in_failed), Snackbar.LENGTH_LONG).show();
            }
        });

    }

    private void signInTask(String firstname, String lastname, String email, String username, String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            GreenBusService registerService = retrofit.create(GreenBusService.class);

            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstname(firstname);
            registerDTO.setLastname(lastname);
            registerDTO.setEmail(email);
            registerDTO.setUsername(username);
            registerDTO.setPassword(password);

            Call<SessionDTO> call = registerService.getTokenForSignIn(registerDTO);
            Response<SessionDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            Response<SessionDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.registration_successful), Snackbar.LENGTH_LONG).show();

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Log.e(TAG, e.getMessage());
                            }

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(getResources().getString(R.string.jwt), String.valueOf(finalResponse.body().getJwt())).apply();
                            Intent intent = new Intent(SignInActivity.this, PassengerMapActivity.class);
                            intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                            startActivity(intent);
                        }

                    });
                    thread.start();

                } else {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.sign_in_failed), Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

}