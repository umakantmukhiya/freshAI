package com.app.plant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Result_activity extends AppCompatActivity {
    ExtendedFloatingActionButton callfab;
    TextView rpathogen , rremedy , rhost , rheadline ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_activity);
        rpathogen = findViewById(R.id.pathogen);
        rremedy = findViewById(R.id.remedy);
        rhost = findViewById(R.id.host);
        rheadline = findViewById(R.id.headline);

        callfab = findViewById(R.id.fab);
        callfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" +"18001801551"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        Intent intent = getIntent();
        String headline , pathogen ,host , remedy;
        headline = intent.getStringExtra("headline");
        pathogen = intent.getStringExtra("pathogen") ;
        host = intent.getStringExtra("host");
        remedy = intent.getStringExtra("remedy");



        rheadline.setText(headline);
        rhost.setText(host);
        rpathogen.setText(pathogen);
        rremedy.setText(remedy);
    }
}