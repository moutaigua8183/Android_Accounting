package com.moutaigua.accounting;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by mou on 1/27/17.
 */

public class FirebaseHandler {


    private final String LOG_TAG = "FirebaseDatabase";
    public static final String SERVICE_PROVIDER_TABLE = "ServiceProviders";
    public static final String SERVICE_PROVIDER_CHILD_LOCATION = "locations";
    public static final String TRANSACTION_TABLE = "Transactions";
    public static final String TRANSACTION_CHILD_TIME_IN_MS = "time_in_millseconds";
    public static final String TRANSACTION_CHILD_TIME_IN_TEXT = "time_in_text";
    public static final String TRANSACTION_CHILD_CATEGORY = "category";
    public static final String TRANSACTION_CHILD_MONEY = "money";
    public static final String TRANSACTION_CHILD_PROVIDER = "provider";
    public static final String TRANSACTION_CHILD_CITY = "city";
    public static final String TRANSACTION_CHILD_GPS_LONGITUDE = "gps_longitude";
    public static final String TRANSACTION_CHILD_GPS_LATITUDE = "gps_latitude";
    public static final String TRANSACTION_CHILD_REPORT_SOURCE = "report_source";
    public static final String CATEGORY_TABLE = "Category";
    public static final String CATEGORY_CHILD_INDEX = "index";
    public static final String CATEGORY_CHILD_CODE = "code";
    public static final String SHARE_TABLE = "Share";
    public static final String SHARE_CHILD_TIME_IN_MS = "time_in_millseconds";
    public static final String SHARE_CHILD_TIME_IN_TEXT = "time_in_text";
    public static final String SHARE_CHILD_CATEGORY_NAME = "category_name";
    public static final String SHARE_CHILD_MONEY = "money";
    public static final String SHARE_CHILD_PROVIDER_NAME = "provider_name";
    public static final String SHARE_CHILD_CITY = "city";
    public static final String SHARE_CHILD_NOTE = "note";
    public static final String SHARE_CHILD_TYPE = "type";
    public static final String SHARE_CHILD_SEPERATE = "seperate";


    private DatabaseReference databaseRef;
    private ChildEventListener serviceProviderListener;
    private ArrayList<ServiceProvider> providerList;
    private ArrayList<Transaction.TransactionCategory> categoryList; // complete list
    private ArrayList<String> categoryNameList;                      // used for spinner
    private ChildEventListener transactionListener;
    private ChildEventListener shareListener;
    private ArrayList<Transaction> shareList;

    public FirebaseHandler() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
        providerList = new ArrayList<>();
        categoryList = new ArrayList<>();
        categoryNameList = new ArrayList<>();
        shareList = new ArrayList<>();
    }


    /**** interface ****/

    public interface TransactionCallback {
        void onResult(@NonNull ArrayList<Transaction> transList);
    }

    public interface TransactionListener {
        void onTransactionAdded(Transaction transaction);
        void onTransactionRemoved(Transaction transaction);
    }

    public interface ResultCallback {
        void onSuccess();
        void onFail();
    }

    public interface SingleCallback {
        void onResult();
    }



    /**** Service Provider ****/

    public static class ServiceProvider {
        private String name;
        private ArrayList<Pair<Double, Double>> locationList;

        public ServiceProvider() {
            name = null;
            locationList = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ArrayList<Pair<Double, Double>> getLocationList() {
            return locationList;
        }
    }

    public void syncServiceProviders(){
        serviceProviderListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                ServiceProvider provider = new ServiceProvider();
                provider.setName(dataSnapshot.getKey().toString());
                Iterator<DataSnapshot> iter = dataSnapshot.child(SERVICE_PROVIDER_CHILD_LOCATION).getChildren().iterator();
                while( iter.hasNext() ){
                    DataSnapshot eachLocation = iter.next();
                    String[] gpsArray = eachLocation.getValue().toString().split(";");
                    double longitude = Double.parseDouble( gpsArray[0] );
                    double latitude = Double.parseDouble( gpsArray[1] );
                    Pair<Double, Double> gpsPair = new Pair<>(longitude,latitude);
                    provider.getLocationList().add( gpsPair );
                }
                providerList.add(provider);
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
        databaseRef.child(SERVICE_PROVIDER_TABLE).addChildEventListener(serviceProviderListener);
    }

    public void stopSyncServiceProviders(){
        databaseRef.removeEventListener(serviceProviderListener);
        this.providerList.clear();
    }

    public void addServiceProvider(final ServiceProvider provider, final boolean isAddLastLocation, @Nullable final ResultCallback callback ) {
        Query query = databaseRef.child(SERVICE_PROVIDER_TABLE).child(provider.getName());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> newGPS = new HashMap<>();
                String prefix = "/"+SERVICE_PROVIDER_TABLE+"/"+provider.getName()+"/"+SERVICE_PROVIDER_CHILD_LOCATION+"/";
                String gpsPair;
                if( dataSnapshot.getValue()==null ) { // new provider
                    if( !isAddLastLocation ) {
                        gpsPair = "0;0";
                        newGPS.put(prefix + "0", gpsPair);
                        Log.i(LOG_TAG, "A new service provider has been added without its gps location.");
                    } else {
                        int lastLocIndex = provider.getLocationList().size() - 1;
                        gpsPair = String.valueOf(provider.getLocationList().get(lastLocIndex).first)
                                    + ";"
                                    + String.valueOf(provider.getLocationList().get(lastLocIndex).second);
                        newGPS.put(prefix + lastLocIndex, gpsPair);
                        Log.i(LOG_TAG, "A new service provider has been added with its gps location.");
                    }
                    databaseRef.updateChildren(newGPS);
                    if( callback!=null ){
                        callback.onSuccess();
                    }
                } else { // service provider exists
                    // add a new GPS location to an existing provider
                    if(!isAddLastLocation){
                        Log.i(LOG_TAG, "No need to update the service provider");
                        if(callback!=null) {
                            callback.onFail();
                        }
                    } else {
                        Log.i(LOG_TAG, "A new location is added to the service provider");
                        int lastLocIndex = provider.getLocationList().size() - 1;
                        gpsPair = String.valueOf(provider.getLocationList().get(lastLocIndex).first)
                                + ";"
                                + String.valueOf(provider.getLocationList().get(lastLocIndex).second);
                        newGPS.put(prefix + lastLocIndex, gpsPair);
                        databaseRef.updateChildren(newGPS);
                        if( callback!=null){
                            callback.onSuccess();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public ArrayList<ServiceProvider> getProviderList(){
        return providerList;
    }


    /**** Category ****/

    public void downloadCategory(final SingleCallback callback){
        Query query = databaseRef.child(CATEGORY_TABLE).orderByChild(CATEGORY_CHILD_INDEX);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if( dataSnapshot.getValue()!=null ) {
                    Iterator<DataSnapshot> allCategoryNodes = dataSnapshot.getChildren().iterator();
                    while( allCategoryNodes.hasNext() ){
                        DataSnapshot eachTranNode = allCategoryNodes.next();
                        Transaction.TransactionCategory cate = new Transaction.TransactionCategory();
                        cate.setName( eachTranNode.getKey().toString());
                        cate.setCode( eachTranNode.child(CATEGORY_CHILD_CODE).getValue().toString());
                        categoryList.add(cate);
                        categoryNameList.add(cate.getName());
                    }
                }
                callback.onResult();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public ArrayList<Transaction.TransactionCategory> getCategoryList(){
        return categoryList;
    }

    public ArrayList<String> getCategoryNameList() {
        return  categoryNameList;
    }

    public void addCategory(final Transaction.TransactionCategory cate, final ResultCallback callback){
        Query query = databaseRef.child(CATEGORY_TABLE).child(cate.getName());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if( dataSnapshot.getValue()==null ) { // not exist, meaning 'cate' is new
                    String prefix = "/" + CATEGORY_TABLE + "/" + cate.getName() + "/";
                    HashMap<String, Object> newCate = new HashMap<>();
                    newCate.put(prefix+CATEGORY_CHILD_CODE, cate.getCode());
                    newCate.put(prefix+CATEGORY_CHILD_INDEX, cate.getIndex());
                    databaseRef.updateChildren(newCate);
                    Log.i(LOG_TAG, "New category has been added");
                    callback.onSuccess();
                } else {
                    Log.i(LOG_TAG, "Such category has already existed");
                    callback.onFail();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




    }


    /**** Transaction ****/

    public void addTransaction(Transaction trans){
        HashMap<String, Object> newProvider = new HashMap<>();
        String prefix = "/"+TRANSACTION_TABLE+"/"+trans.getReportId()+"/";
        newProvider.put(prefix+TRANSACTION_CHILD_REPORT_SOURCE, trans.getReportSource());
        newProvider.put(prefix+TRANSACTION_CHILD_TIME_IN_MS, trans.getLongTime());
        newProvider.put(prefix+TRANSACTION_CHILD_TIME_IN_TEXT, trans.getTextTime());
        newProvider.put(prefix+TRANSACTION_CHILD_MONEY, trans.getMoney());
        newProvider.put(prefix+TRANSACTION_CHILD_PROVIDER, trans.getProviderName());
        newProvider.put(prefix+TRANSACTION_CHILD_CITY, trans.getCity());
        newProvider.put(prefix+TRANSACTION_CHILD_GPS_LONGITUDE, trans.getGpsLongitude());
        newProvider.put(prefix+TRANSACTION_CHILD_GPS_LATITUDE, trans.getGpsLatitude());
//        newProvider.put(prefix+TRANSACTION_CHILD_CATEGORY, trans.getCategory());
//        newProvider.put(prefix+TRANSACTION_CHILD_NOTE, trans.getNote());
//        newProvider.put(prefix+TRANSACTION_CHILD_TYPE, trans.getType());
//        newProvider.put(prefix+TRANSACTION_CHILD_SEPERATE, trans.getSeperate());
        databaseRef.updateChildren(newProvider);
        Log.i(LOG_TAG, "A new transaction has been added.");
    }

    public void getTransactions(final TransactionCallback callback){
        Query query = databaseRef.child(TRANSACTION_TABLE);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Transaction> transList = new ArrayList<>();
                if( dataSnapshot.getValue()!=null ) {
                    Iterator<DataSnapshot> allTranNodes = dataSnapshot.getChildren().iterator();
                    while( allTranNodes.hasNext() ){
                        DataSnapshot eachTranNode = allTranNodes.next();
                        Transaction trans = new Transaction();
                        trans.setTextTime(eachTranNode.child(TRANSACTION_CHILD_TIME_IN_TEXT).getValue().toString());
                        trans.setLongTime(Long.parseLong(eachTranNode.child(TRANSACTION_CHILD_TIME_IN_MS).getValue().toString()));
                        trans.setProviderName(eachTranNode.child(TRANSACTION_CHILD_PROVIDER).getValue().toString());
                        trans.setMoney(eachTranNode.child(TRANSACTION_CHILD_MONEY).getValue().toString());
                        trans.setReportId(eachTranNode.getKey().toString());
                        trans.setCity(eachTranNode.child(TRANSACTION_CHILD_CITY).getValue().toString());
                        trans.setGpsLongitude( Double.valueOf(eachTranNode.child(TRANSACTION_CHILD_GPS_LONGITUDE).getValue().toString()) );
                        trans.setGpsLatitude( Double.valueOf(eachTranNode.child(TRANSACTION_CHILD_GPS_LATITUDE).getValue().toString()) );
                        trans.setReportSource(eachTranNode.child(TRANSACTION_CHILD_REPORT_SOURCE).getValue().toString());
                        transList.add(trans);
                    }
                }
                callback.onResult(transList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void deleteTransaction(String transID){
        databaseRef.child(TRANSACTION_TABLE).child(transID).removeValue();
    }

    public void startListenNewTransactions(final TransactionListener listener){
        transactionListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Transaction trans = new Transaction();
                trans.setTextTime(dataSnapshot.child(TRANSACTION_CHILD_TIME_IN_TEXT).getValue().toString());
                trans.setLongTime(Long.parseLong(dataSnapshot.child(TRANSACTION_CHILD_TIME_IN_MS).getValue().toString()));
                trans.setProviderName(dataSnapshot.child(TRANSACTION_CHILD_PROVIDER).getValue().toString());
                trans.setMoney(dataSnapshot.child(TRANSACTION_CHILD_MONEY).getValue().toString());
                trans.setReportId(dataSnapshot.getKey().toString());
                trans.setCity(dataSnapshot.child(TRANSACTION_CHILD_CITY).getValue().toString());
                trans.setGpsLongitude( Double.valueOf(dataSnapshot.child(TRANSACTION_CHILD_GPS_LONGITUDE).getValue().toString()) );
                trans.setGpsLatitude( Double.valueOf(dataSnapshot.child(TRANSACTION_CHILD_GPS_LATITUDE).getValue().toString()) );
                trans.setReportSource(dataSnapshot.child(TRANSACTION_CHILD_REPORT_SOURCE).getValue().toString());
                listener.onTransactionAdded(trans);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Transaction trans = new Transaction();
                trans.setTextTime(dataSnapshot.child(TRANSACTION_CHILD_TIME_IN_TEXT).getValue().toString());
                trans.setLongTime(Long.parseLong(dataSnapshot.child(TRANSACTION_CHILD_TIME_IN_MS).getValue().toString()));
                trans.setProviderName(dataSnapshot.child(TRANSACTION_CHILD_PROVIDER).getValue().toString());
                trans.setMoney(dataSnapshot.child(TRANSACTION_CHILD_MONEY).getValue().toString());
                trans.setReportId(dataSnapshot.getKey().toString());
                trans.setCity(dataSnapshot.child(TRANSACTION_CHILD_CITY).getValue().toString());
                trans.setGpsLongitude( Double.valueOf(dataSnapshot.child(TRANSACTION_CHILD_GPS_LONGITUDE).getValue().toString()) );
                trans.setGpsLatitude( Double.valueOf(dataSnapshot.child(TRANSACTION_CHILD_GPS_LATITUDE).getValue().toString()) );
                trans.setReportSource(dataSnapshot.child(TRANSACTION_CHILD_REPORT_SOURCE).getValue().toString());
                listener.onTransactionRemoved(trans);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseRef.child(TRANSACTION_TABLE).addChildEventListener(transactionListener);
    }

    public void stopListenNewTransactions(){
        databaseRef.removeEventListener(transactionListener);
    }


    /**** Share ****/

    public void syncShare(final SingleCallback callback){
        shareListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if( dataSnapshot.getValue()!=null ) {
                    Transaction trans = new Transaction();
                    trans.setLongTime( Long.valueOf(dataSnapshot.child(SHARE_CHILD_TIME_IN_MS).getValue().toString()) );
                    trans.setTextTime( dataSnapshot.child(SHARE_CHILD_TIME_IN_TEXT).getValue().toString() );
                    trans.setMoney( dataSnapshot.child(SHARE_CHILD_MONEY).getValue().toString() );
                    trans.setProviderName( dataSnapshot.child(SHARE_CHILD_PROVIDER_NAME).getValue().toString() );
                    trans.setCity( dataSnapshot.child(SHARE_CHILD_CITY).getValue().toString() );
                    Transaction.TransactionCategory cate = new Transaction.TransactionCategory();
                    cate.setName( dataSnapshot.child(SHARE_CHILD_CATEGORY_NAME).getValue().toString() );
                    trans.setCategory( cate );
                    trans.setNote( dataSnapshot.child(SHARE_CHILD_NOTE).getValue().toString() );
                    trans.setType( dataSnapshot.child(SHARE_CHILD_TYPE).getValue().toString() );
                    trans.setSeperate( Integer.valueOf(dataSnapshot.child(SHARE_CHILD_SEPERATE).getValue().toString()) );
                    // ascendingly ordered by time_in_ms
                    int index = 0;
                    for (; index < shareList.size(); ++index) {
                        if (shareList.get(index).getLongTime() >= trans.getLongTime()) {
                            shareList.add(index, trans);
                            break;
                        }
                    }
                    if( index==shareList.size() ){
                        shareList.add(trans);
                    }
                    callback.onResult();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if( dataSnapshot.getValue()!=null ){
                    long transTime = Long.valueOf( dataSnapshot.child(SHARE_CHILD_TIME_IN_MS).getValue().toString() );
                    for( int i = 0; i < shareList.size(); ++i ){
                        if( shareList.get(i).getLongTime()==transTime ){
                            shareList.remove(i);
                            break;
                        }
                    }
                    callback.onResult();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseRef.child(SHARE_TABLE).addChildEventListener(shareListener);
    }

    public void stopShare(){
        databaseRef.removeEventListener(shareListener);
        this.shareList.clear();
    }

    public ArrayList<Transaction> getShareList() {
        return shareList;
    }

    public void addShare(Transaction trans){
        HashMap<String, Object> newProvider = new HashMap<>();
        String prefix = "/"+SHARE_TABLE+"/"+trans.getLongTime()+"/";
        newProvider.put(prefix+SHARE_CHILD_TIME_IN_MS, trans.getLongTime());
        newProvider.put(prefix+SHARE_CHILD_TIME_IN_TEXT, trans.getTextTime());
        newProvider.put(prefix+SHARE_CHILD_MONEY, trans.getMoney());
        newProvider.put(prefix+ SHARE_CHILD_PROVIDER_NAME, trans.getProviderName());
        newProvider.put(prefix+SHARE_CHILD_CITY, trans.getCity());
        newProvider.put(prefix+ SHARE_CHILD_CATEGORY_NAME, trans.getCategory().getName());
        newProvider.put(prefix+SHARE_CHILD_NOTE, trans.getNote());
        newProvider.put(prefix+SHARE_CHILD_TYPE, trans.getType());
        newProvider.put(prefix+SHARE_CHILD_SEPERATE, trans.getSeperate());
        databaseRef.updateChildren(newProvider);
        Log.i(LOG_TAG, "A new transaction has been added.");
    }

    public void deleteShare(Transaction trans) {
        String shareId = String.valueOf(trans.getLongTime());
        databaseRef.child(SHARE_TABLE).child(shareId).removeValue();
    }



}
