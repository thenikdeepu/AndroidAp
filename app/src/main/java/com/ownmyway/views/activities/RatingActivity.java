package com.ownmyway.views.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.R;

import java.util.Observable;
import java.util.Observer;

public class RatingActivity extends AppCompatActivity implements Observer {
    private String driverID;

    /**
     * Renders a view with 3 buttons. One to rate thumbs up, one to rate thumbs down, and one
     * to skip.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        App.getModel().addObserver(this);
        driverID =  getIntent().getStringExtra("driverID");
    }

    /**
     * if we click thumbs up/down, call controller method to adjust rating to the driver
     * Otherwise(we clicked skip) go back to the map activity where the trip is finished
     * @param v The button view that is pressed
     */
    public void handleRatingClick(View v){
        if(v.getId()==R.id.buttonThumbsUp){
            ApplicationController.updateDriverRating(this,
                    driverID,true);
            Toast.makeText(this, "Driver rating updated!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        else if(v.getId()== R.id.buttonThumbsDown){
            ApplicationController.updateDriverRating(this,
                    driverID,false);
            Toast.makeText(this, "Driver rating updated!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
        else{
            this.finish();
        }
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    /** onDestroy method destructs activity if it is closed down **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        // THIS CODE SHOULD BE IMPLEMENTED IN EVERY VIEW
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }
}
