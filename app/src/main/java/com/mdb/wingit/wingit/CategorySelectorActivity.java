package com.mdb.wingit.wingit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class CategorySelectorActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private FirebaseAuth mAuth;
    private GoogleApiClient client;
    private int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    static LatLng current; // current location in lat and long
    static String currentName = "";
    static int indexPlace = 0;
    private static AdventureAdapter adapter;
    private static ArrayList<Adventure> adventures;
    static ArrayList<String> otherFive = new ArrayList<>();
    static ArrayList<Place> currentLocations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set up tabbed view with fragments
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        //Connect to Places API
        client = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API).build();
        client.connect();

        adventures = new ArrayList<Adventure>();
        getCurrentLocations();

        mAuth = FirebaseAuth.getInstance();
    }

    /** Get list of likely places for user's current location from Places API */
    private void getCurrentLocations() {
        //Check permissions to access user's location
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        }

        currentLocations = new ArrayList<>();
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(client, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                double likelihood = 0;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i("Place", placeLikelihood.getPlace().getName().toString());
                    currentLocations.add(placeLikelihood.getPlace());
                    Log.i("Likelihood", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                }
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    if (placeLikelihood.getLikelihood() > likelihood) {
                        likelihood = placeLikelihood.getLikelihood();
                        indexPlace = currentLocations.indexOf(placeLikelihood.getPlace());
                    }
                }
                if (currentLocations.size() != 0) {
                    for (int i = 0; i < currentLocations.size(); i++) {
                        otherFive.add(currentLocations.get(i).getName().toString());
                    }
                    current = currentLocations.get(indexPlace).getLatLng();
                    currentName = currentLocations.get(indexPlace).getName().toString();
                    StartOptions.updateLocation();
                }
                likelyPlaces.release();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class StartOptions extends Fragment implements View.OnClickListener {

        static TextView location;
        // FirebaseDatabase database;
        private static DatabaseReference mDatabase;
        private static FirebaseUser user;
        // private AdventureList adventures;
        //DatabaseReference db = database.getReference().child("adventures");
        // static String adventureKey;
        private static Adventure adventure;
        private static String date;
        private Calendar calendar;
        private ConstraintLayout bg;

        public static StartOptions newInstance(int page) {
            Bundle args = new Bundle();
            args.putInt("pagenumber", page);
            StartOptions fragment = new StartOptions();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_start_options, container, false);

            CardView food = (CardView) v.findViewById(R.id.food);
            CardView activity = (CardView) v.findViewById(R.id.activity);
            location = (TextView) v.findViewById(R.id.location2);
            bg = (ConstraintLayout) v.findViewById(R.id.start_options_constraint);

            mDatabase = FirebaseDatabase.getInstance().getReference();
            adventure = new Adventure();
            user = FirebaseAuth.getInstance().getCurrentUser();
            date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            location.setText("Loading...");
            calendar = Calendar.getInstance();

            if (calendar.HOUR_OF_DAY > 6 && calendar.HOUR_OF_DAY < 18) {
                bg.setBackgroundResource(R.drawable.rochesterdayleft);
            } else {
                bg.setBackgroundResource(R.drawable.rochesternightleft);
            }

            food.setOnClickListener(this);
            activity.setOnClickListener(this);

            return v;
        }

        public static void updateLocation() {
            location.setText("Location: " + currentName);
        }

        //Intent intent = new Intent(getActivity(), CarouselActivity.class);

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.food:
                    //String key = db.push().getKey();
                    // TODO: Create adventure and add to database
                    // createAdventure();

                    Intent foodIntent = new Intent(getActivity(), CarouselActivity.class);
                    foodIntent.putExtra("food", true);
                    foodIntent.putExtra("current", current);
                    // foodIntent.putExtra("adventureKey", adventureKey);
                    startActivity(foodIntent);
                    break;
                case R.id.activity:
                    //String key1 = db.push().getKey();
                    // TODO: Create adventure and add to database
                    // createAdventure();

                    Intent activityIntent = new Intent(getActivity(), CarouselActivity.class);
                    activityIntent.putExtra("food", false);
                    activityIntent.putExtra("current", current);
                    // activityIntent.putExtra("adventureKey", adventureKey);
                    startActivity(activityIntent);
                    break;
            }
        }

        // TODO: Put Adventure into DB
        public static void createAdventure() {
            // Create a new adventure and add to db
            adventure.setStartloc(currentName);
            adventure.setDate(date);
            final DatabaseReference adventureDB = mDatabase.child("Adventures").push();
            adventureDB.setValue(adventure);
            adventures.add(adventure);
            adapter.notifyDataSetChanged();

            // Add adventure to user's adventurelist
            final String uid = user.getUid();
            DatabaseReference userAdventureList = mDatabase.child("Users").child(uid);
            userAdventureList.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    user.addAdventureKey(adventureDB.getKey());
                    mDatabase.child("Users").child(uid).child("adventureKeysList").setValue(user.getAdventureKeysList());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    public static class AdventureLog extends Fragment {

        private RecyclerView rv;
        private static DatabaseReference mDatabase;
        private ArrayList<String> adventureKeys = new ArrayList<>();
        private CoordinatorLayout bg;
        private Calendar calendar;
        private CardView noAdventuresCard;
        private TextView noAdventures;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            // adventureList = new AdventureList();
            // adventures = adventureList.getArrayList();

            View view = inflater.inflate(R.layout.activity_adventure_log, container, false);
            rv = (RecyclerView) view.findViewById(R.id.adventureLogRv);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            bg = (CoordinatorLayout) view.findViewById(R.id.activity_adventure_log);
            calendar = Calendar.getInstance();
            noAdventuresCard = (CardView) view.findViewById(R.id.noAdventuresCard);
            noAdventures = (TextView) view.findViewById(R.id.noAdventures);
            mDatabase = FirebaseDatabase.getInstance().getReference();

            if (calendar.HOUR_OF_DAY > 6 && calendar.HOUR_OF_DAY < 18) {
                bg.setBackgroundResource(R.drawable.rochesterdayright);
            } else {
                bg.setBackgroundResource(R.drawable.rochesternightright);
            }

            // Get the Adventure keys from the current User
            DatabaseReference adventureKeyDB = mDatabase.child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            adventureKeyDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);

                    Log.i("USER", user.getName());
                    if (user.getAdventureKeysList() != null && user.getAdventureKeysList().size() != 0) {
                        noAdventuresCard.setVisibility(View.GONE);
                        noAdventures.setVisibility(View.GONE);
                        adventureKeys = user.getAdventureKeysList();

                        // Get the correct Adventures with the Adventure keys
                        DatabaseReference adventuresDB = mDatabase.child("Adventures");
                        adventuresDB.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                                    Log.i("AdventureData", dsp.toString());
                                    if (adventureKeys.contains(dsp.getKey())) {
                                        Adventure ad = dsp.getValue(Adventure.class);
                                        adventures.add(ad);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            adapter = new AdventureAdapter(getContext(), adventures);
            rv.setAdapter(adapter);
            return view;
        }
    }

    //Set up tabbed view with StartOptions and AdventureLog
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    StartOptions tab1 = new StartOptions();
                    return tab1;
                case 1:
                    AdventureLog tab2 = new AdventureLog();
                    return tab2;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Begin Adventure";
                case 1:
                    return "My Past Trips";
            }
            return null;
        }
    }

}
