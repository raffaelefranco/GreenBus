package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

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
                    new SignInTask().execute(firstname.getText().toString(), lastname.getText().toString(), email.getText().toString(),
                            username.getText().toString(), password.getText().toString());
                else
                    Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.register_failed), Snackbar.LENGTH_LONG).show();
            }
        });

    }

    public class SignInTask extends AsyncTask<String, Void, Response<JWTTokenDTO>> {

        @Override
        protected Response<JWTTokenDTO> doInBackground(String... params) {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(baseURI)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            CityService registerService = retrofit.create(CityService.class);

            RegisterDTO registerDTO = new RegisterDTO();
            registerDTO.setFirstname(params[0]);
            registerDTO.setLastname(params[1]);
            registerDTO.setEmail(params[2]);
            registerDTO.setUsername(params[3]);
            registerDTO.setPassword(params[4]);

            Call<JWTTokenDTO> call = registerService.getTokenForSignIn(registerDTO);
            Response<JWTTokenDTO> response = null;
            try {
                response = call.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        @Override
        protected void onPostExecute(Response<JWTTokenDTO> response) {
            if (response.code() == 200) {
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

        }
    }


}