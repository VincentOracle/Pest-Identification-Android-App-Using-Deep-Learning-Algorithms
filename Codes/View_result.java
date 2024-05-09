package com.example.pestify;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.EditText;
import android.widget.TextView;

import com.example.pestify.R;

public class View_result extends AppCompatActivity {
    EditText ed1,ed2;
    TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_result);

        tv1=(TextView) findViewById(R.id.resid);
        String res=getIntent().getStringExtra("result");
        String desc=getIntent().getStringExtra("desc");

        String finattxt="\n\nRecognized Pest: "+res+"\n============================\n\nDescription\n============================\n"+desc;

        tv1.setText(finattxt);

        tv1.setMovementMethod(new ScrollingMovementMethod());
    }
}