package com.dcastalia.googlemapsdirection;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dcastalia.googlemapsdirection.model.EndLocation_;
import com.dcastalia.googlemapsdirection.model.GoogleMapDirection;
import com.dcastalia.googlemapsdirection.model.Route;
import com.dcastalia.googlemapsdirection.model.StartLocation_;
import com.dcastalia.googlemapsdirection.model.Step;
import com.dcastalia.googlemapsdirection.retrofit.ApiService;
import com.dcastalia.googlemapsdirection.retrofit.RetroClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener ,
        LocationListener{

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 10001;
    private GoogleMap mMap;
    private TextView txtTravelDuration, txtTravelDistance, txtTravelMode;
    private EditText etDestination;
    private Button btnGo;
    private RadioGroup rgMode;
    private RadioButton rbWalking, rbBicyle, rbDriving, tnMode;
    private Context context;
    private String destination, transactionMode, currentLocation;
    private final String SERVER_API_KEY = "AIzaSyDREG26YdpKf_klZZZFV-KtXuQByjqFOlk";
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private Polyline polyline;
    private ArrayList<Polyline> polylineArrayList = new ArrayList<>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        findViewsById();
        context = MapsActivity.this;
        transactionMapFragment();
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               goWork();
            }
        });

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
        }
    }

    private void goWork() {
        if (Utils.chekcGPSEnable(context)) {
            if (Utils.isNetworkAvailable(context)) {
                getTheDirection();
            } else {
                Toast.makeText(context, "Please Enable Wifi or Mobile Data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Utils.showGPSDialog(context);
        }
    }


    private void transactionMapFragment() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getTheDirection() {

        //Get the user input data .
        int selectedId = rgMode.getCheckedRadioButtonId();
        tnMode = findViewById(selectedId);
        transactionMode = tnMode.getText().toString();
        destination = etDestination.getText().toString();

        /** User input is not valid than no request will be serve ** , user must input transactionMode & destiantion **/
        if (!transactionMode.equals("") && !destination.equals("")) {
            Utils.showProgressDialog(context);
            ApiService apiService = RetroClient.getApiService();
            Call<GoogleMapDirection> call = apiService.getDirection(currentLocation, destination, transactionMode, SERVER_API_KEY);

            call.enqueue(new Callback<GoogleMapDirection>() {
                @Override
                public void onResponse(Call<GoogleMapDirection> call, Response<GoogleMapDirection> response) {
                    Utils.hideProgressDialog();
                    if (response.code() == 200) {
                        GoogleMapDirection googleMapDirection = response.body();
                        if (googleMapDirection.getStatus().equals("OK")) {
                            processDirectionData(googleMapDirection);
                        } else {
                            Toast.makeText(MapsActivity.this, "Status not ok.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                public void onFailure(Call<GoogleMapDirection> call, Throwable t) {
                    Utils.hideProgressDialog();
                    Toast.makeText(MapsActivity.this, "Fail , Request can not process. ", Toast.LENGTH_SHORT).show();
                }
            });


        } else {
            Toast.makeText(context, "Enter Destination & Transaction Mode", Toast.LENGTH_SHORT).show();
        }

    }

    private void processDirectionData(GoogleMapDirection googleMapDirection) {
        if(googleMapDirection!=null){
            String distance = googleMapDirection.getRoutes().get(0).getLegs().get(0).getDistance().getText();
            String duration = googleMapDirection.getRoutes().get(0).getLegs().get(0).getDuration().getText();
            txtTravelMode.setText(transactionMode);
            txtTravelDistance.setText(distance);
            txtTravelDuration.setText(duration);
            Route route = googleMapDirection.getRoutes().get(0);

            drawTheRouteToMap(route);
        }
    }

    private void drawTheRouteToMap(Route route) {
        /** remove previous polyLine if has any **/
        if(polylineArrayList.size()>0){
          for(int i=0 ; i<polylineArrayList.size(); i++){
              polylineArrayList.get(i).remove();
          }
        }
        for(int i=0 ; i<route.getLegs().size(); i++ ){
            List<Step> steps = route.getLegs().get(i).getSteps();
            for(int z=0 ;z<steps.size(); z++){
                StartLocation_ startLocation_ = steps.get(z).getStartLocation();
                EndLocation_ endLocation_ = steps.get(z).getEndLocation();
                drawPolyLine(new LatLng(startLocation_.getLat(),startLocation_.getLng()),new LatLng(endLocation_.getLat(),endLocation_.getLng()) );
            }

        }


    }

    public void drawPolyLine(LatLng startLatLng , LatLng endLatLng){
        // Add PolyLine Into Map , We can Custom the PolyLine **/
        polyline = this.mMap.addPolyline(  new PolylineOptions()
                .add(startLatLng, endLatLng)
                .width(10)
                .color(Color.GREEN));

        polylineArrayList.add(polyline);



    }



    private void findViewsById() {
        txtTravelDistance = findViewById(R.id.txt_travel_distance);
        txtTravelDuration = findViewById(R.id.txt_travel_time);
        txtTravelMode = findViewById(R.id.txt_travel_mode);
        etDestination = findViewById(R.id.et_place);
        btnGo = findViewById(R.id.btn_go);
        rgMode = findViewById(R.id.radio_group);
        rbWalking = findViewById(R.id.rb_walking);
        rbBicyle = findViewById(R.id.rb_bi_cycle);
        rbDriving = findViewById(R.id.rb_driving);
    }
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentLocation = String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
      /*  mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);*/
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
