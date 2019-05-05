package com.example.mymaps;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;


public class UserTrips
{
    private GeoPoint mGeoPointDestination;
    private @ServerTimestamp Date timestamp;
    private User mUser;
    private GeoPoint mGeoPointUser;
    
    public UserTrips()
    {
    
    }
    
    public GeoPoint getGeoPointUser()
    {
        return mGeoPointUser;
    }
    
    public void setGeoPointUser(GeoPoint geoPointUser)
    {
        mGeoPointUser = geoPointUser;
    }
    
    public GeoPoint getGeoPointDestination()
    {
        return mGeoPointDestination;
    }
    
    public void setGeoPointDestination(GeoPoint geoPointDestination)
    {
        mGeoPointDestination = geoPointDestination;
    }
    
    public Date getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }
    
    public User getUser()
    {
        return mUser;
    }
    
    public void setUser(User user)
    {
        mUser = user;
    }
    
    
}
