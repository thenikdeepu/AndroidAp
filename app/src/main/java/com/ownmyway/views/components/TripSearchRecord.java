package com.ownmyway.views.components;

import com.ownmyway.model.Trip;
import com.ownmyway.model.UserLocation;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Formatted Trip used for displaying trip information in the trip selection modal.
 */
public class TripSearchRecord implements Serializable {
    private String riderName;
    private String startAddress;
    private String endAddress;
    private LatLng startLatLng;
    private LatLng endLatLng;
    private String estimatedCost;
    private String distanceFromDriver;
    private String riderID;

    /**The following function is used assign user info for use in TripSearch views
     * @param t The stored trip information
     * @param driverLocation the location of the driver user*/
    public TripSearchRecord(Trip t, UserLocation driverLocation){
        this.riderName = t.getRiderUserName();
        this.startAddress = t.getStartUserLocation().getAddress();
        this.endAddress = t.getEndUserLocation().getAddress();
        this.startLatLng = t.getStartUserLocation().generateLatLng();
        this.endLatLng = t.getEndUserLocation().generateLatLng();
        this.estimatedCost = ((Double) t.getFareOffering()).toString();
        this.distanceFromDriver = ((Double) t.getStartUserLocation().distanceTo(driverLocation))
                .toString();
        this.riderID = t.getRiderID();
    }

    public String getRiderName(){return this.riderName;}
    public String getStartAddress(){return this.startAddress;}
    public String getEndAddress(){return this.endAddress;}
    public String getEstimatedCost(){return this.estimatedCost;}
    public String getDistanceFromDriver(){return this.distanceFromDriver;}
    public LatLng getStartLatLng(){return this.startLatLng;}
    public LatLng getEndLatLng(){return  this.endLatLng;}

    public String getRiderID() {
        return riderID;
    }
}
