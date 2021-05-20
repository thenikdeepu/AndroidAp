package com.ownmyway.views.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.Trip;
import com.ownmyway.model.User;
import com.ownmyway.R;
import com.ownmyway.views.components.CustomTripList;
import com.ownmyway.views.components.TripSearchRecord;
import com.ownmyway.views.fragments.AcceptTripRequestFragment;
import com.ownmyway.views.UIErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

/**
 * Main Driver activity for searching for and selecting a trip. Activity loads trips and uses
 * the model to display a list of trips that are filtered based on the users current radius.
 * TODO: MVC Updating and Error Handling.
 */
public class TripSearchActivity extends AppCompatActivity implements UIErrorHandler, Observer,
        AcceptTripRequestFragment.OnFragmentInteractionListener {

    ListView tripSearchList;
    ArrayAdapter<TripSearchRecord> tripSearchRecordArrayAdapter;
    ArrayList<TripSearchRecord> tripDataList;
    private boolean showAcceptedPendingRides;

    /**onCreate method creates the view. It is used to populate TripSearchActivity
     * @param savedInstanceState calls the previous saved state if there is one*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trip_search_activity);
        tripSearchList = findViewById(R.id.trip_search_list);
        App.getModel().addObserver(this);

        tripDataList = new ArrayList<>();
        tripSearchRecordArrayAdapter = new CustomTripList(this, tripDataList);


        if (App.getModel().getSessionUser().getCurrentUserLocation() == null) {
            Toast.makeText(getBaseContext(), "Couldn't fetch your location. Try restarting the app.", Toast.LENGTH_SHORT).show();
            this.finish();
        } else {
            showAcceptedPendingRides = getIntent().getBooleanExtra("ShowAcceptedPendingRidesFlag", false);
            //Activate the custom array adapter (CustomTripList)
            tripSearchList.setAdapter(tripSearchRecordArrayAdapter);
            tripSearchList.setOnItemClickListener((parent, view, position, id) ->
                    new AcceptTripRequestFragment(tripDataList.get(position),
                            position,
                            TripSearchActivity.this,
                            showAcceptedPendingRides,
                            App.getAPIKey(this, true))
                            .show(getSupportFragmentManager(), "VIEW_RECORD"));

            if (showAcceptedPendingRides) {
                ApplicationController.getPendingTripsForDriver(this);
            } else {
                ApplicationController.getTripsForUser(this);
            }
        }
    }

    /**onError function is used to handle incoming UI errors in tripSearchActivity
     * @param e is the error called*/
    @Override
    public void onError(Error e) {
        // TODO: Handle Incoming UI Errors
    }

    /**Update function updates the view whenever changes are made
     * @param o,arg are used to ensure the correct updates are made*/
    @Override
    public void update(Observable o, Object arg) {
        transferTripDataFromListToArrayList();
    }

    /** Converts List from Application model to ArrayList**/
    private void transferTripDataFromListToArrayList() {
        ApplicationModel m = App.getModel();
        User sessionUser = m.getSessionUser();

        List<Trip> tripList = m.getSessionTripList();
        if (tripList != null && !showAcceptedPendingRides) {
            if (tripDataList != null) {
                tripDataList.clear();
            }

            for (Trip t : tripList) {
                Objects.requireNonNull(tripDataList).add(new TripSearchRecord(t, sessionUser.getCurrentUserLocation()));
            }

            if (tripSearchRecordArrayAdapter != null) {
                tripSearchRecordArrayAdapter.notifyDataSetChanged();
            }
        }

        List<Trip> driverAcceptedPendingRides = m.getDriverAcceptedPendingRides();
        if (driverAcceptedPendingRides != null && showAcceptedPendingRides) {
            if (tripDataList != null) {
                tripDataList.clear();
            }

            for (Trip t : driverAcceptedPendingRides) {
                Objects.requireNonNull(tripDataList).add(new TripSearchRecord(t, sessionUser.getCurrentUserLocation()));
            }

            if (tripSearchRecordArrayAdapter != null) {
                tripSearchRecordArrayAdapter.notifyDataSetChanged();
            }
        }
    }

    /**onAcceptPressed handles user interaction when the accept button is pressed
     * @param tripSearchRecord,position uses the position and trip search record to ensure the correct
     *      trip is accepted*/
    public void onAcceptPressed(TripSearchRecord tripSearchRecord, int position){
        Trip selectedTrip = App.getModel().getSessionTripList().get(position);
        ApplicationController.handleDriverTripSelect(selectedTrip);

        // Navigate back to main activity
        this.finish();
    }

    /**
     * OnDestroy handles activity when application is closed down*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }
}
