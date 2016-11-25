package com.mdb.wingit.wingit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

public class DetailScreen extends AppCompatActivity implements View.OnClickListener{

    Toolbar toolbar;
    CollapsingToolbarLayout toolbarLayout;
    ImageView imageView;
    TextView textView;
    Button nextActivityButton;
    Button endTripButton;
    FloatingActionButton fab;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_screen);

        dbRef = FirebaseDatabase.getInstance().getReference();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);

        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        nextActivityButton = (Button) findViewById(R.id.nextActivityButton);
        endTripButton = (Button) findViewById(R.id.endTripButton);
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.fab:
                Intent activityIntent = getIntent();
                //TODO: pass coordinate value of destination through intent
                //String coordinates = activityIntent.getStringExtra("coordinates");
                String coordinates = "20.5666,45.345";
                Intent mapsIntent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr=" + coordinates));
                startActivity(mapsIntent);
                break;

            case R.id.nextActivityButton:
                break;

            case R.id.endTripButton:
                break;

            default:
                break;
        }
    }
}
