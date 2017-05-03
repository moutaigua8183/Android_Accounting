package com.moutaigua.accounting;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mou on 1/27/17.
 */

public class FirebaseHandler {


    private final String LOG_TAG = "FirebaseDatabase";
    public static final String SERVICE_PROVIDER_TABLE = "ServiceProviders";
    public static final String SERVICE_PROVIDER_LOCATION = "location";
    public static final String SERVICE_PROVIDER_FREQUENCY = "frequency";
    public static final String SERVICE_LOCATION_TABLE = "Locations";
    public static final String TRANSACTION_TABLE = "Transactions";

    private DatabaseReference databaseRef;
    private ChildEventListener serviceProviderListener;
    private ArrayList<ServiceProvider> providerArrayList;
    private ArrayList<String> providerNameArrayList;
    private ChildEventListener locationListener;
    private ArrayList<Location> locationArrayList;
    private ArrayList<String> locationNameArrayList;

    public FirebaseHandler() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
        providerArrayList = new ArrayList<>();
        providerNameArrayList = new ArrayList<>();
        locationNameArrayList = new ArrayList<>();
    }


    /**** Service Provider ****/

    public void syncServiceProviders(){
        serviceProviderListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ServiceProvider provider = new ServiceProvider();
                provider.location = dataSnapshot.child(SERVICE_PROVIDER_LOCATION).getValue().toString();
                provider.freqency = Integer.valueOf( dataSnapshot.child(SERVICE_PROVIDER_FREQUENCY).getValue().toString() );
                providerArrayList.add(provider);
                providerNameArrayList.add(dataSnapshot.getKey().toString());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                for (ServiceProvider eachProvider: providerArrayList) {
                    if( dataSnapshot.getKey().toString().equalsIgnoreCase(eachProvider.name) ){
                        eachProvider.freqency = Integer.valueOf( dataSnapshot.child(SERVICE_PROVIDER_FREQUENCY).getValue().toString() );
                        return;
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseRef.child(SERVICE_PROVIDER_TABLE).addChildEventListener(serviceProviderListener);
    }

    public void stopSyncServiceProviders(){
        databaseRef.removeEventListener(serviceProviderListener);
    }

    public void addServiceProvider(final ServiceProvider provider) {
        Query query = databaseRef.child(SERVICE_PROVIDER_TABLE).child(provider.name);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if( dataSnapshot.getValue()==null ) {
                    HashMap<String, Object> newProvider = new HashMap<>();
                    String prefix = "/"+SERVICE_PROVIDER_TABLE+"/"+provider.name+"/";
                    newProvider.put(prefix+SERVICE_PROVIDER_LOCATION, provider.location);
                    newProvider.put(prefix+SERVICE_PROVIDER_FREQUENCY, 1);
                    databaseRef.updateChildren(newProvider);
                    Log.i(LOG_TAG, "A new service provider has been added.");
                } else {
                    int cur_freq = Integer.valueOf(dataSnapshot.child(SERVICE_PROVIDER_FREQUENCY).getValue().toString());
                    databaseRef.child(SERVICE_PROVIDER_TABLE).child(provider.name).child(SERVICE_PROVIDER_FREQUENCY).setValue(cur_freq+1);
                    Log.i(LOG_TAG, "The service provider's frequency has been updated.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public ArrayList<ServiceProvider> getProviderArrayList(){
        return providerArrayList;
    }

    public ArrayList<String> getProviderNameArrayList() {
        return providerNameArrayList;
    }

    static class ServiceProvider {
        public String name;
        public String location;
        public int freqency;
    }

    /**** Location ****/

    public void syncLocations(){
        locationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Location location = new Location();
                location.name = dataSnapshot.getKey().toString();
                locationArrayList.add(location);
                locationNameArrayList.add(location.name);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseRef.child(SERVICE_LOCATION_TABLE).addChildEventListener(locationListener);
    }

    public void stopSyncLocations(){
        databaseRef.removeEventListener(locationListener);
    }

    public void addLocation(final Location location) {
        Query query = databaseRef.child(SERVICE_LOCATION_TABLE).child(location.name);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if( dataSnapshot.getValue()==null ) {
                    HashMap<String, Object> newLocation = new HashMap<>();
//                    String prefix = "/"+SERVICE_LOCATION_TABLE+"/"+location.name;
//                    newLocation.put(prefix+SERVICE_PROVIDER_LOCATION, provider.location);
//                    newLocation.put(prefix+SERVICE_PROVIDER_FREQUENCY, 1);
//                    databaseRef.updateChildren(newLocation);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public ArrayList<Location> getLocationArrayList(){
        return locationArrayList;
    }

    public ArrayList<String> getLocationNameArrayList() {
        return locationNameArrayList;
    }

    static class Location {
        public String name;
    }



}
