package com.ownmyway.views.activities;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.User;
import com.ownmyway.views.UIErrorHandler;

import java.util.Observable;
import java.util.Observer;

/**
 * MainActivity. Used mostly as a router to check the current users login state and redirect
 * to either the Login or Main map activity (in case the user is already logged in)
 */
public class MainActivity extends AppCompatActivity implements Observer, UIErrorHandler {

    /**onCreate method creates the MainActivity when called
     * @param savedInstanceState is an previous saved state if applicable*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationModel m = App.getModel();
        m.addObserver(this);

        determineLoginStatus();
    }

    /**onDestroy method destructs activity when MainActivity is shut down*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {}

    /**determineLoginStatus is used to determine the login status of the user - it checks if the
     * user is logged in */
    private void determineLoginStatus() {
        if(App.getAuthDBManager().isLoggedIn()) {
            App.getAuthDBManager().getCurrentSessionUser((resultData, err) -> {
                if (resultData != null) {
                    User tmpUser = (User) resultData.get("user");
                    App.getModel().setSessionUser(tmpUser);
                    Intent i = new Intent(MainActivity.this, MapActivity.class);
                    ApplicationController.loadSessionTrip(i, this);  // now determine trip status
                }
                else {
                    Toast.makeText(this, err.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            this.finish();
        }
    }

    /**onError method handles errors if applicable
     * @param e is the error called*/
    @Override
    public void onError(Error e) {
        Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();

    }
}