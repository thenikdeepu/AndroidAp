package com.ownmyway.views.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.Driver;
import com.ownmyway.model.Trip;
import com.ownmyway.model.User;
import com.ownmyway.R;
import com.ownmyway.views.UIErrorHandler;

import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

import static com.ownmyway.model.User.TYPE.RIDER;

/** RequestStatusActivity is used to show status of trip requests, as well as driver's info
 */
public class RequestStatusActivity extends AppCompatActivity implements Observer, UIErrorHandler {
    private TextView statusTextView;
    private TextView startTextView;
    private TextView endTextView;
    private TextView usernameTextTextView;
    private TextView usernameTextView;
    private TextView driverRatingTextView;
    private Button viewContactButton;

    /**onCreate method will create the activity when called
     * @param savedInstanceState is a previous saved state for activity if available*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_status);
        App.getModel().addObserver(this);

        statusTextView = findViewById(R.id.txtRideStatus);
        startTextView = findViewById(R.id.txtStartLocation);
        endTextView = findViewById(R.id.txtEndLocation);

        usernameTextTextView = findViewById(R.id.txtRideStatusCurDriverTextTextView);
        usernameTextView = findViewById(R.id.txtRideStatusCurDriverUserName);
        driverRatingTextView =  findViewById(R.id.txtDriverRatingDisplay);
        viewContactButton = findViewById(R.id.viewContactButton);
        //initial form fill
        fillStatusForm();
    }

    /**Gets drivers info to be displayed*/
    @SuppressLint("SetTextI18n")
    public void getDriverInfo(String docID){
        App.getDbManager().getDriver(docID, (resultData, err) -> {
            if (resultData != null) {

                //this fetched driver is from the db
                Driver tmpDriver = (Driver) resultData.get("user");
                usernameTextView.setText(Objects.requireNonNull(tmpDriver).getUsername());
                driverRatingTextView.setText("Current Rating: " + tmpDriver.getRating() + " / 100.0");
            }
            else {
                Toast.makeText(this, err.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    /**Fills ride status form with correct user and trip data depending on which user is signed in*/
    public void fillStatusForm(){
        User curUser = App.getModel().getSessionUser();
        Trip trip = App.getModel().getSessionTrip();
        if(trip==null){
            Toast.makeText(this,
                    "no request made",Toast.LENGTH_SHORT).show();
            this.finish();
        }

        if(curUser.getType()==RIDER){  //rider opening the view
            statusTextView.setText(Objects.requireNonNull(trip).getStatus().toString());
            startTextView.setText(trip.getStartUserLocation().getAddress());
            endTextView.setText(trip.getEndUserLocation().getAddress());
            if(trip.getDriverID() != null){
                //TODO:: Error handling
                getDriverInfo(trip.getDriverID());
                usernameTextTextView.setVisibility(View.VISIBLE);
                usernameTextView.setVisibility(View.VISIBLE);
            }
            else{  //no driver accepted yet
                usernameTextTextView.setVisibility(View.INVISIBLE);
                usernameTextView.setVisibility(View.INVISIBLE);
            }
        } else {  //driver opening the view
            statusTextView.setText(Objects.requireNonNull(trip).getStatus().toString());
            startTextView.setText(trip.getStartUserLocation().getAddress());
            endTextView.setText(trip.getEndUserLocation().getAddress());
            usernameTextTextView.setVisibility(View.INVISIBLE);
            usernameTextView.setVisibility(View.INVISIBLE);
        }
        if (trip.getStatus() != Trip.STATUS.PENDING && trip.getStatus() != Trip.STATUS.COMPLETED) {
            viewContactButton.setVisibility(View.VISIBLE);
        } else {
            viewContactButton.setVisibility(View.INVISIBLE);
        }
    }

    public void handleViewContactButtonClick(View v) {
        // if Rider clicks this, then we want to load the drivers contact info with option to
        // call the driver, else load the riders contact information
        Trip trip = App.getModel().getSessionTrip();
        if (trip != null && trip.getDriverID() != null) {
            Intent contactIntent = new Intent(this, ContactViewerActivity.class);
            ApplicationController.handleViewContactInformation(this, contactIntent, trip.getRiderID(), trip.getDriverID());
        }
    }

    /**update will update the activity
     * @param o,arg are the observable and object used for update respectively*/
    @Override
    public void update(Observable o, Object arg) {
        fillStatusForm();
    }

    /**onDestroy handles destruction of activity when it is closed down*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        // THIS CODE SHOULD BE IMPLEMENTED IN EVERY VIEW
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }

    /**onError handles incoming errors
     * @param e is the incoming error*/
    @Override
    public void onError(Error e) {
        //TODO:: hand ui errors
    }
}