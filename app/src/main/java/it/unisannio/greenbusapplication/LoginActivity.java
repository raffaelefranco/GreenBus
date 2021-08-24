package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import it.unisannio.greenbusapplication.dto.LoginDTO;
import it.unisannio.greenbusapplication.dto.RouteDTO;
import it.unisannio.greenbusapplication.dto.TicketDTO;
import it.unisannio.greenbusapplication.service.GreenBusService;
import it.unisannio.greenbusapplication.util.ConstantValues;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Login";
    private static final String prefName = "CityApplication";
    private static final String ROLE_PASSENGER = "ROLE_PASSENGER";
    private static final String ROLE_DRIVER = "ROLE_DRIVER";
    private SharedPreferences preferences;
    private static String baseUrl;
    private List<RouteDTO> routes;
    private EditText username;
    private EditText password;
    private Button login;
    private Button signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        baseUrl = ConstantValues.localAddress + ConstantValues.baseApi;

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.login);
        signIn = (Button) findViewById(R.id.sign_in);

        preferences = getSharedPreferences(prefName, MODE_PRIVATE);

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));

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
                intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                startActivity(intent);
            }
        });

    }

    private void loginTask(String username, String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            GreenBusService greenBusService = retrofit.create(GreenBusService.class);
            Call<SessionDTO> call = greenBusService.getTokenForLogin(new LoginDTO(username, password));
            Response<SessionDTO> response = null;

            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Response<SessionDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("jwt", String.valueOf(finalResponse.body().getJwt())).apply();
                    if(finalResponse.body().getRoles().contains(ROLE_DRIVER)) {
                        ticketTask();
                    }
                    else if(finalResponse.body().getRoles().contains(ROLE_PASSENGER)) {
                       Intent intent = new Intent(LoginActivity.this, UserMapActivity.class);
                        intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                        startActivity(intent);
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.login_failed), Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    private void ticketTask() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            GreenBusService greenBusService = retrofit.create(GreenBusService.class);

            String typeAuth = "Bearer ";
            String jwt = preferences.getString("jwt", null);

            Call<TicketDTO> call = greenBusService.getTicket(typeAuth.concat(jwt));

            retrofit2.Response<TicketDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            retrofit2.Response<TicketDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    Intent intent = new Intent(LoginActivity.this, DriverMapActivity.class);
                    intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                    intent.putExtra(getResources().getString(R.string.ticket), (Serializable) finalResponse.body().getOneTimeTicket());
                    startActivity(intent);
                }
            });
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog title = new AlertDialog.Builder(LoginActivity.this)
                .setTitle(getResources().getString(R.string.confirm_exit))
                .setIcon(R.drawable._minibus)
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
}