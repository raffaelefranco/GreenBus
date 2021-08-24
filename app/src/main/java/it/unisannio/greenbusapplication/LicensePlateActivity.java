package it.unisannio.greenbusapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import it.unisannio.greenbusapplication.dto.RouteDTO;

public class LicensePlateActivity extends AppCompatActivity {

    private static final String TAG = "LICENSE_PLATE_ACTIVITY";
    private EditText licensePlate;
    private Button go;

    private String oneTimeTicket;
    private List<RouteDTO> routes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_plate);

        Intent fromCaller = getIntent();
        routes = (ArrayList<RouteDTO>) fromCaller.getSerializableExtra(getResources().getString(R.string.routes));
        oneTimeTicket = (String) fromCaller.getSerializableExtra(getResources().getString(R.string.oneTimeTicket));

        licensePlate = (EditText) findViewById(R.id.license_plate);
        go = (Button) findViewById(R.id.go);

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (licensePlate.getText().toString().length() != 0) {
                    Intent intent = new Intent(LicensePlateActivity.this, DriverMapActivity.class);
                    intent.putExtra(getResources().getString(R.string.routes), (Serializable) routes);
                    intent.putExtra(getResources().getString(R.string.oneTimeTicket), (Serializable) oneTimeTicket);
                    intent.putExtra(getResources().getString(R.string.license_place), (Serializable) licensePlate.getText().toString());
                    startActivity(intent);
                } else
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.input_error), Toast.LENGTH_LONG).show();

            }
        });

    }
}