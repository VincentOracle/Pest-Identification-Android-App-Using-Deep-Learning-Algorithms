package com.example.pestify;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Loginpage extends AppCompatActivity {

    EditText ed1, ed2;
    Button b1;
    TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginpage);

        ed1 = findViewById(R.id.unm);
        ed2 = findViewById(R.id.pass);
        b1 = findViewById(R.id.button);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uname = ed1.getText().toString();
                String pass = ed2.getText().toString();
                Log.e("uname pass-->", uname + " " + pass);

                if (uname.equals("admin") && pass.equals("admin")) {
                    Toast.makeText(Loginpage.this, "Login Successful!", Toast.LENGTH_LONG).show();
                    Intent i1 = new Intent(Loginpage.this, MainActivity.class);
                    startActivity(i1);
                } else {
                    Toast.makeText(Loginpage.this, "Invalid Credentials!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
