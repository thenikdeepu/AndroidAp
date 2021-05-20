package com.ownmyway.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.ownmyway.App;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.Trip;
import com.ownmyway.model.UserLocation;
import com.ownmyway.R;
import com.ownmyway.views.UIErrorHandler;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.rtchagas.pingplacepicker.PingPlacePicker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

/**
 * Main Rider activity for building and creating a trip. Activity generates a form that uses the
 * Google search api to gather user locations. When form is submitted, new Trip instance is created
 * and added to Firebase.
 * TODO: MVC Updating and Error Handling.
 */


public class TripBuilderActivity extends AppCompatActivity implements UIErrorHandler, Observer {
    // Trip tings
    Trip tripRequest;
    UserLocation currentUserLoc, startLoc, endLoc;
    Geocoder geocoder;
    double minimumFareOffering;
    double riderFareOffering;
    int choosingBtnID;
    double GEOFENCE_DETECTION_TOLERANCE;
    Button button;

    // UI
    ConstraintLayout tripSubmissionLayout;
    TextView startPointTextView;
    TextView endPointTextView;
    EditText fareOfferingEditText;
    public CircularProgressButton submitTripBtn;

    // AutoComplete Places API
    String apiKey;
    int REQUEST_PLACE_PICKER = 1;

    /**onCreate method will create TripBuilderActivity when it is run
     * @param savedInstanceState will get a previous saved state if available*/
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trip_builder_layout);
        App.getModel().addObserver(this);
        apiKey = App.getAPIKey(this, false);
        Places.initialize(this,apiKey);
button=findViewById(R.id.selectEndPointBtn);
//button.setOnClickListener(new View.OnClickListener() {
//    @Override
//    public void onClick(View v) {
//        List<Place.Field> fieldList=Arrays.asList(Place.Field.ADDRESS,Place.Field.LAT_LNG,Place.Field.NAME);
//        Intent intent=new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,fieldList).build(TripBuilderActivity.this);
//        startActivityForResult(intent,100);
//    }
//});
        // Set start location parameters from previous map activity
        double[] currentLatLong = getIntent().getDoubleArrayExtra("currentLatLong");
        GEOFENCE_DETECTION_TOLERANCE = getIntent().getDoubleExtra("GEOFENCE_DETECTION_TOLERANCE", 0.040);
        assert currentLatLong != null;
        currentUserLoc = new UserLocation(currentLatLong[0], currentLatLong[1]);
        startLoc = new UserLocation(currentLatLong[0], currentLatLong[1]);

        // Initialize locations
        try {
            geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    currentLatLong[0],
                    currentLatLong[1],
                    1 // represent max location result returned, recommended 1-5
            );
            currentUserLoc.setAddress(addresses.get(0).getAddressLine(0));
            startLoc.setAddress(addresses.get(0).getAddressLine(0));
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Instantiate TextViews/EditText
        startPointTextView = findViewById(R.id.startPointTextView);
        startPointTextView.setText("Set to your location: \n" + startLoc.getAddress());
        fareOfferingEditText = findViewById(R.id.fareOfferingEditText);
        endPointTextView = findViewById(R.id.endPointTextView);
//        endPointTextView.setText(endLoc.getAddress());




        // Specify the types of place data to return.

        // Instantiate submit button
        submitTripBtn = findViewById(R.id.submitTripBtn);

        // Disengage submission UI elements
        tripSubmissionLayout = findViewById(R.id.tripSubmissionLayout);
        tripSubmissionLayout.setVisibility(View.VISIBLE);
    }


    @SuppressLint("SetTextI18n")
    public void handleClearStartPtBtn(View v) {
        startLoc = new UserLocation(currentUserLoc.getLatitude(), currentUserLoc.getLongitude());
        startLoc.setAddress(currentUserLoc.getAddress());
        if (endLoc == null) {
            tripSubmissionLayout.setVisibility(View.VISIBLE);
        } else {
            recalculateFareOffering();
        }
        startPointTextView.setText("Set to your location: \n" + currentUserLoc.getAddress());
    }

    @SuppressLint("SetTextI18n")
    public void handleClearEndPtBtn(View v) {
        endLoc = null;
        tripSubmissionLayout.setVisibility(View.VISIBLE);
        endPointTextView.setText(currentUserLoc.getAddress());
    }

    public void engagePlacePicker(View v) {
        PingPlacePicker.IntentBuilder builder = new PingPlacePicker.IntentBuilder();
        builder.setAndroidApiKey("AIzaSyDT17Dnst2miFN2FLvUr40MtVhHcH_Kovs").setMapsApiKey("AIzaSyDT17Dnst2miFN2FLvUr40MtVhHcH_Kovs");

        // If you want to set a initial location rather then the current device location.
        // NOTE: enable_nearby_search MUST be true.z
        // builder.setLatLng(new LatLng(37.4219999, -122.0862462))

        try {
            Intent placeIntent = builder.build(this);
            startActivityForResult(placeIntent, REQUEST_PLACE_PICKER);
            choosingBtnID = v.getId();
        }
        catch (Exception ex) {
            // Google Play services is not available...
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 100) && (resultCode == RESULT_OK)) {


            Place place = PingPlacePicker.getPlace(data);
            if (place != null) {
                final LatLng latLng = place.getLatLng();
                assert latLng != null;
                switch (choosingBtnID) {
                    case R.id.selectStartPointBtn:
                        if (startLoc == null) {
                            startLoc = new UserLocation(latLng.latitude, latLng.longitude);
                            startLoc.setAddress(place.getAddress());

                        } else {
                            startLoc.setLatitude(latLng.latitude);
                            startLoc.setLongitude(latLng.longitude);
                            startLoc.setAddress(place.getAddress());
                        }
                        startPointTextView.setText(place.getAddress());

                        break;
                    case R.id.selectEndPointBtn:
                        if (endLoc == null) {
                            endLoc = new UserLocation(latLng.latitude, latLng.longitude);
                            endLoc.setAddress(place.getAddress());
                        } else {
                            endLoc.setLatitude(latLng.latitude);
                            endLoc.setLongitude(latLng.longitude);
                            endLoc.setAddress(place.getAddress());
                        }
//                        endPointTextView.setText(place.getAddress());
endPointTextView.setText(place.getAddress());
                        break;
                }
            }

                if (startLoc != null && endLoc != null) {
                    recalculateFareOffering();
                    tripSubmissionLayout.setVisibility(View.VISIBLE);
                } else {
                    tripSubmissionLayout.setVisibility(View.VISIBLE);
                }
            }
        }



    /**Used to recalculate ride fare*/
    @SuppressLint("SetTextI18n")
    private void recalculateFareOffering() {
        minimumFareOffering = startLoc.distancePriceEstimate(endLoc);
        riderFareOffering = minimumFareOffering;
        fareOfferingEditText.setText(Double.toString(riderFareOffering));
    }


    public void handleTripSubmitBtn(View v) {
        submitTripBtn.startAnimation();

        // Edge case: Check if rider is already at location
//        if (currentUserLoc.distanceTo(endLoc) <= GEOFENCE_DETECTION_TOLERANCE) {
//            Toast.makeText(getBaseContext(), "You are already here...", Toast.LENGTH_SHORT).show();
//            this.finish();
//            return;
//        }

        String username = App.getModel().getSessionUser().getUsername();
        double offeredFare = Double.parseDouble(fareOfferingEditText.getText().toString().trim());
        if (offeredFare >= minimumFareOffering) {
            riderFareOffering = offeredFare;

            String riderID = App.getAuthDBManager().getCurrentUserID();
            tripRequest = new Trip(
                    riderID,
                    offeredFare,
                    startLoc,
                    endLoc,
                    username
            );
            App.getController().createNewTrip(tripRequest, this, submitTripBtn);
            Toast.makeText(getBaseContext(), "Submitting Trip...", Toast.LENGTH_SHORT).show();
        } else {
            // Remind user of minimum cost+
            recalculateFareOffering();
            fareOfferingEditText.setError(
                    "Please enter an amount higher than or equal to: \n" + minimumFareOffering);
            submitTripBtn.revertAnimation();
        }
    }


    /**update function updates the activity
     * @param o,arg are the observable and the object*/
    @Override
    public void update(Observable o, Object arg) {
    }

    /**onDestroy closes the activity when it needs to be closed*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        // THIS CODE SHOULD BE IMPLEMENTED IN EVERY VIEW
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }

    /**onError handles errors in the TripBuilderActivity
     * @param e is the error called*/
    @Override
    public void onError(Error e) {
        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
