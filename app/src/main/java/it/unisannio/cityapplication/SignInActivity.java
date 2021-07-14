package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unisannio.cityapplication.dto.JWTTokenDTO;
import it.unisannio.cityapplication.dto.LoginDTO;
import it.unisannio.cityapplication.dto.RegisterDTO;
import it.unisannio.cityapplication.service.CityService;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignIn";
    private static String baseURI;
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
        baseURI = getString(R.string.local) + "/api/city/";

        firstname = (EditText) findViewById(R.id.firstname);
        lastname = (EditText) findViewById(R.id.lastname);
        email = (EditText) findViewById(R.id.email);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);

        signIn = (Button) findViewById(R.id.sign_in);

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstname.getText().toString().length() != 0 && lastname.getText().toString().length() != 0
                        && email.getText().toString().length() != 0 && username.getText().toString().length() != 0
                        && password.getText().toString().length() != 0)
                    signInTask(firstname.getText().toString(), lastname.getText().toString(), email.getText().toString(), username.getText().toString(), password.getText().toString());
                else
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.register_failed), Snackbar.LENGTH_LONG).show();
            }
        });

    }

    private void signInTask(String firstname, String lastname, String email, String username, String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURI)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            CityService registerService = retrofit.create(CityService.class);

            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstname(firstname);
            registerDTO.setLastname(lastname);
            registerDTO.setEmail(email);
            registerDTO.setUsername(username);
            registerDTO.setPassword(password);

            Call<JWTTokenDTO> call = registerService.getTokenForSignIn(registerDTO);
            Response<JWTTokenDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Response<JWTTokenDTO> finalResponse = response;
            handler.post(() -> {
                if (finalResponse.code() == 200) {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.register_success), Snackbar.LENGTH_LONG).show();

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
                            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                            startActivity(intent);
                        }

                    });
                    thread.start();

                } else {
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.register_failed), Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

}