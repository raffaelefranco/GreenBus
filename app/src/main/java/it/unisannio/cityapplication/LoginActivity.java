package it.unisannio.cityapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private EditText username;
    private EditText password;
    private Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        login = (Button) findViewById(R.id.button);

        login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // check credential with server
                // if ok intent
                username.getText();
                password.getText();
                // else
                Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.login_failed), Snackbar.LENGTH_LONG).show();
            }
        });

    }
}