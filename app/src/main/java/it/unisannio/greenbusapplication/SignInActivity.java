package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private static final String TAG = "SignIn";
    public static final String prefName = "CityApplication";
    private static String baseUrl;
    private List<RouteDTO> routes;
    private SharedPreferences preferences;
    private EditText firstname;
    private EditText lastname;
    private EditText email;
    private EditText username;
    private EditText password;
    private Button signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        baseUrl = ConstantValues.localAddress + ConstantValues.baseApi;

        firstname = (EditText) findViewById(R.id.firstname);
        lastname = (EditText) findViewById(R.id.lastname);
        email = (EditText) findViewById(R.id.email);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);

        preferences = getSharedPreferences(prefName, MODE_PRIVATE);

        signIn = (Button) findViewById(R.id.sign_in);

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
                e.printStackTrace();
            }
            Response<SessionDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.registration_successful), Snackbar.LENGTH_LONG).show();

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException interruptedException) {
                                    interruptedException.printStackTrace();
                                }

                            } finally {
                                finish();
                            }
                            SharedPreferences.Editor edit = preferences.edit();
                            edit.putString("jwt", String.valueOf(finalResponse.body().getJwt())).apply();
                            Intent intent = new Intent(SignInActivity.this, UserMapActivity.class);
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