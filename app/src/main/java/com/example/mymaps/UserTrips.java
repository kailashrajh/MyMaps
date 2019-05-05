package com.example.mymaps;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

public class UserTrips
{
    private GeoPoint mGeoPointUser;
    private GeoPoint mGeoPointDestination;
    private @ServerTimestamp String timestamp;
    private User mUser;
    
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
    
    public String getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp)
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
    
    @Override
    public String toString()
    {
        return "UserTrips{" +
                "mGeoPointUser=" + mGeoPointUser +
                ", mGeoPointDestination=" + mGeoPointDestination +
                ", timestamp='" + timestamp + '\'' +
                ", mUser=" + mUser +
                '}';
    }
}
