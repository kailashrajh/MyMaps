package com.example.mymaps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Created by User on 10/2/2017.
 */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener
{
    private static final String TAG = "MapsActivity";
    //Constants
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    
    //widgets
    private ImageView mGps;
    private ImageView mSave;
    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private String searchString;
    private GeoPoint mUserLocation;
    private GeoPoint mUserDestintaion;
    private Marker mTrip;
    private LatLng mLatLng;
    private ArrayList<PolylineData> mPolylineData = new ArrayList<>();
    private UserTrips mUserTrips;
    private FirebaseFirestore mDb;
    
    
    private void saveUserTrip()
    {
        if(mUserTrips != null)
        {
            DocumentReference tripRef = mDb
                    .collection(getString(R.string.collections_trips))
                    .document(FirebaseAuth.getInstance().getUid());
            
            tripRef.set(mUserTrips).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if(task.isSuccessful())
                    {
                        Log.d(TAG, "onComplete: Saved user trip");
                        
                    }
                }
            });
            Toast.makeText(this,"Trip has been saved to database",Toast.LENGTH_SHORT).show();
        }
    }
    
    private void getUserDetails()
    {
        if(mUserTrips == null)
        {
            mUserTrips = new UserTrips();
        }
        
        User user = ((UserClient)getApplication()).getUser();
        mUserTrips.setUser(user);
        
        getDeviceLocation();
    }
    public void onMapReady(GoogleMap googleMap)
    {
        Toast.makeText(this, "Map is Ready", LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        
        if (mLocationPermissionsGranted)
        {
            getDeviceLocation();
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setOnPolylineClickListener(this);
    
            // Initialize Places.
            Places.initialize(getApplicationContext(), new Constants().getGoogle_maps_api());
    
            init();
            autocompletePlaces();
            getUserDetails();
            getDeviceLocation();
        }
    }
    
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //mSearchText = (EditText) findViewById(R.id.input_search);
        mGps = findViewById(R.id.ic_gps);
        mSave = findViewById(R.id.ic_save_trip);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mDb = FirebaseFirestore.getInstance();
        getLocationPermission();
    
    }
    
    private void autocompletePlaces()
    {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
    
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener()
        {
            @Override
            public void onPlaceSelected(@NonNull Place place)
            {
                Log.d(TAG, "onPlaceSelected: "+ place.getName() + ", " + place.getId());
                searchString = place.getName();
                geoLocate();
            }
    
            @Override
            public void onError(@NonNull Status status)
            {
                Log.d(TAG, "onError: An error occurred" + status);
            }
        });
    }
    
    private void init()
    {
        Log.d(TAG, "init: initializing");
        
        mGps.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(TAG, "onClick: getting device location");
                getDeviceLocation();
            }
        });
        
        mSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getUserDetails();
                saveUserTrip();
                Log.d(TAG, "onClick: Saving trip to database");
            }
        });
        
        hideSoftKeyboard();
    }
    
    private void geoLocate()
    {
        Log.d(TAG, "geoLocate: geolocating");
    
        
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        
        List<Address> list = new ArrayList<>();
        try
        {
            list = geocoder.getFromLocationName(searchString, 1);
        }
        catch (IOException e)
        {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }
        
        if(list.size() > 0)
        {
            Address address = list.get(0);
            
            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            
            mLatLng = new LatLng(address.getLatitude(),address.getLongitude());
            mUserDestintaion = new GeoPoint(address.getLatitude(),address.getLongitude());
            moveCamera(mLatLng,DEFAULT_ZOOM,address.getAddressLine(0));
           
            mUserTrips.setGeoPointDestination(mUserDestintaion);
        }
    }
    
    private void getDeviceLocation()
    {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        
        
        
        try
        {
            if(mLocationPermissionsGranted)
            {
                
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener()
                {
                    @Override
                    public void onComplete(@NonNull Task task)
                    {
                        if(task.isSuccessful())
                        {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            mUserLocation = new GeoPoint(currentLocation.getLatitude(),currentLocation.getLongitude());
                            moveCamera(new LatLng(mUserLocation.getLatitude(), mUserLocation.getLongitude()),
                                    DEFAULT_ZOOM,"My Location");
                            
                            
                            
                        }
                        else
                        {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
        catch (SecurityException e)
        {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }
    
    private void moveCamera(LatLng latLng, float zoom, String title)
    {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    
        if(!title.equals("My Location"))
        {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mTrip = mMap.addMarker(options);
            calculateDirections(mTrip);
        }
        
        hideSoftKeyboard();
    }
    
    private void initMap()
    {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        
        mapFragment.getMapAsync(MapsActivity.this);
        
    }
    
    private void getLocationPermission()
    {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;
        
        switch(requestCode)
        {
            case LOCATION_PERMISSION_REQUEST_CODE:
                {
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++)
                    {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                    
                }
            }
        }
    }
    
    private void hideSoftKeyboard()
    {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    
    
    private void calculateDirections(Marker marker){
        Log.d(TAG, "calculateDirections: calculating directions.");
        try
        {
            GeoApiContext  mGeoApiContext = new GeoApiContext.Builder().apiKey(new Constants().getServer_api()).build();
            com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                    marker.getPosition().latitude,
                    marker.getPosition().longitude
            );
            DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);
    
            directions.alternatives(true);
            directions.origin(
                    new com.google.maps.model.LatLng(
                            mUserLocation.getLatitude(),
                            mUserLocation.getLongitude()
                    )
            );
            Log.d(TAG, "calculateDirections: destination: " + destination.toString());
            directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result)
                {
                    Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                    Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                    Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                    Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                    
                    addPolylinesToMap(result);
                }
        
                @Override
                public void onFailure(Throwable e) {
                    Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );
            
                }
            });
        
        }
        catch (BootstrapMethodError error)
        {
            Log.d(TAG, "calculateDirections: An error occurred "+ error.getMessage());
        }
        finally
        {
            mUserTrips.setGeoPointUser(mUserLocation);
            mUserTrips.setTimestamp(null);
        }
        
    }
    
    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run()
            {
                Log.d(TAG, "run: result routes: " + result.routes.length);
                if(mPolylineData.size()>0)
                {
                    for(PolylineData polylineData: mPolylineData)
                    {
                     polylineData.getPolyline().remove();
                    }
                    mPolylineData.clear();
                    mPolylineData = new ArrayList<>();
                }
                double duration = 9999999;
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());
                    
                    List<LatLng> newDecodedPath = new ArrayList<>();
                    
                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

//                        Log.d(TAG, "run: latlng: " + latLng.toString());
                        
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(MapsActivity.this,R.color.darkGrey));
                    polyline.setClickable(true);
                    mPolylineData.add(new PolylineData(polyline,route.legs[0]));
                    
                    double tempDuration = route.legs[0].duration.inSeconds;
                    if(tempDuration < duration)
                    {
                        duration = tempDuration;
                        onPolylineClick(polyline);
                    }
                    
                }
            }
        });
    }
    
    @Override
    public void onPolylineClick(Polyline polyline)
    {
        int index = 0;
        
        for(PolylineData polylineData: mPolylineData)
        {
            index++;
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.blue1));
                polylineData.getPolyline().setZIndex(1);
                
                LatLng endLocation = new LatLng(polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng);
                
                Marker marker = mMap.addMarker(new MarkerOptions()
                .position(endLocation)
                .title("Trip: # " +index)
                .snippet("Duration: "+polylineData.getLeg().duration));
                marker.showInfoWindow();
            }
            else
            {
                polylineData.getPolyline().setColor(ContextCompat.getColor(this, R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }
    }
}