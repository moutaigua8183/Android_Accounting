package com.moutaigua.accounting;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by mou on 5/6/17.
 * This is for the implementation of pending fragment
 */

public class FragmentManual extends Fragment {


    private final String LOG_TAG = "Fragment_Manual";
    private final int DELAY_TIME_IN_SECOND = 3;

    private FirebaseHandler firebaseHandler;
    private LocationManager mLocationManager;
    private Transaction currTransaction;
    private ServiceProviderAdapter providerAdapter;

    private EditText editTime;
    private EditText editMoney;
    private EditText editSeperate;
    private AutoCompleteTextView editProvider;
    private CheckBox checkboxGPS;
    private Spinner spinnerCategory;
    private Spinner spinnerType;
    private EditText editCity;
    private EditText editNote;
    private TextView txtStatus;

    private boolean isWacaiUpdated;
    private boolean isFirebaseUpdated;


    public FragmentManual() {
        // Required empty public constructor
        firebaseHandler = new FirebaseHandler();
        currTransaction = new Transaction();
        isWacaiUpdated = false;
        isFirebaseUpdated = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupFirebaseListener();


        editTime = (EditText) getActivity().findViewById(R.id.fragment_manual_editxt_time);
        setTime(Calendar.getInstance().getTimeInMillis());
        editTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OnDateSetListener listener = new OnDateSetListener() {
                    @Override
                    public void onDateSet(TimePickerDialog timePickerView, long millseconds) {
                    setTime(millseconds);
                    }
                };
                long fiveYears = 1000 * 60 * 60 * 24L * 365 * 5;
                TimePickerDialog mDialogAll = new TimePickerDialog.Builder()
                        .setCallBack(listener)
                        .setCancelStringId("Cancel")
                        .setSureStringId("Confirm")
                        .setTitleStringId("TimePicker")
                        .setYearText("")
                        .setMonthText("")
                        .setDayText("")
                        .setHourText(" H")
                        .setMinuteText(" M")
                        .setCyclic(true)
                        .setMinMillseconds(System.currentTimeMillis() - fiveYears)
                        .setMaxMillseconds(System.currentTimeMillis() + fiveYears)
                        .setCurrentMillseconds(System.currentTimeMillis())
                        .setThemeColor(getResources().getColor(R.color.timepicker_dialog_bg))
                        .setType(Type.ALL)
                        .setWheelItemTextNormalColor(getResources().getColor(R.color.timetimepicker_default_text_color))
                        .setWheelItemTextSelectorColor(getResources().getColor(R.color.accountingPrimary))
                        .setWheelItemTextSize(15)
                        .build();
                mDialogAll.show(getActivity().getSupportFragmentManager(), "all");
            }
        });


        editMoney = (EditText) getActivity().findViewById(R.id.fragment_manual_editxt_money);
        editMoney.requestFocus();


        editSeperate = (EditText) getActivity().findViewById(R.id.fragment_manual_editxt_seperate);
        editSeperate.setText(String.valueOf(currTransaction.getSeperate()));
        editSeperate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b){
                    if(editSeperate.getText().toString().isEmpty()){
                        editSeperate.requestFocus();
                        Toast.makeText(getActivity(), "Cannot be empty", Toast.LENGTH_SHORT).show();
                    } else {
                        int num = Integer.valueOf(editSeperate.getText().toString());
                        if( num > 1 ){
                            spinnerType.setSelection(1); // can't be private
                        }
                    }
                }
            }
        });


        editProvider = (AutoCompleteTextView) getActivity().findViewById(R.id.fragment_manual_editxt_provider);
        providerAdapter = new ServiceProviderAdapter(getActivity(),
                R.layout.spinner_textview,
                firebaseHandler.getProviderList());
        editProvider.setThreshold(1);
        editProvider.setAdapter(providerAdapter);


        checkboxGPS = (CheckBox) getActivity().findViewById(R.id.fragment_manual_checkbox_gps);
        checkboxGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if( editProvider.getText().toString().isEmpty() ){
                    checkboxGPS.setChecked(false);
                } else {
                    if(b) {
                        retrieveGpsData();
                    }
                }
            }
        });


        spinnerCategory = (Spinner) getActivity().findViewById(R.id.fragment_manual_spinner_category);
        final ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.spinner_textview,
                firebaseHandler.getCategoryNameList());
        spinnerCategory.setAdapter(categoryAdapter);
        firebaseHandler.downloadCategory(new FirebaseHandler.SingleCallback() {
            @Override
            public void onResult() {
                categoryAdapter.notifyDataSetChanged();
            }
        });
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(adapterView.getSelectedItem().toString()
                        .equalsIgnoreCase("Gas")){
                    editNote.append("Miles: 1\nPrice: 2.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        spinnerType = (Spinner) getActivity().findViewById(R.id.fragment_manual_spinner_type);
        ArrayAdapter typeAdaper = ArrayAdapter.createFromResource(getActivity(), R.array.transaction_type, R.layout.spinner_textview);
        spinnerType.setAdapter(typeAdaper);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(adapterView.getSelectedItem().toString()
                        .equalsIgnoreCase(getString(R.string.transaction_type_private))
                        && !editSeperate.getText().toString().isEmpty()
                        && Integer.valueOf(editSeperate.getText().toString())>1 ){
                    // if type is private, seperate must be 1
                    editSeperate.setText("1");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        editCity = (EditText) getActivity().findViewById(R.id.fragment_manual_editxt_city);


        editNote = (EditText) getActivity().findViewById(R.id.fragment_manual_editxt_note);


        Button btnSubmit = (Button) getActivity().findViewById(R.id.fragment_manual_btn_submit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( !isReadyToSubmit() ){
                    return;
                }
                txtStatus.setText( "-- start\n" );
                // Service Provider Update
                if( !editProvider.getText().toString().isEmpty() ){
                    FirebaseHandler.ServiceProvider provider = getProviderService(editProvider.getText().toString());
                    if( provider==null ){
                        provider = new FirebaseHandler.ServiceProvider();
                        provider.setName(editProvider.getText().toString());
                    }
                    boolean isUnique = true;
                    if( checkboxGPS.isChecked() ){
                        Pair<Double, Double> gpsPair = new Pair<>(currTransaction.getGpsLongitude(), currTransaction.getGpsLatitude());
                        if( isFirstTimeLocUpdate(provider.getLocationList()) ) {
                            provider.getLocationList().set(0, gpsPair);
                        } else {
                            Location loc1 = new Location("");
                            loc1.setLongitude(gpsPair.first);
                            loc1.setLatitude(gpsPair.second);
                            for(Pair<Double, Double> eachGPSPair : provider.getLocationList()){
                                Location loc2 = new Location("");
                                loc2.setLongitude(eachGPSPair.first);
                                loc2.setLatitude(eachGPSPair.second);
                                float distanceInMeters = loc1.distanceTo(loc2);
                                if( distanceInMeters<1 ){
                                    isUnique =false;
                                    break;
                                }
                            }
                            if( isUnique ){
                                provider.getLocationList().add(gpsPair);
                            }
                        }
                    }
                    firebaseHandler.addServiceProvider(provider, checkboxGPS.isChecked() && isUnique, new FirebaseHandler.ResultCallback() {
                        @Override
                        public void onSuccess() { // if a new provider or a new location is added
                            isFirebaseUpdated = true;
                            txtStatus.append( "-- " + currTransaction.getProviderName() + " is updated\n" );
                            delayedExecute();
                        }

                        @Override
                        public void onFail() { // if no provider, no new provider or no new location is given
                            isFirebaseUpdated = true;
                            delayedExecute();
                        }

                    });
                } else {
                    isFirebaseUpdated = true;
                }
                // WaCai Update
                completeTransaction();
                if( !currTransaction.getType().equalsIgnoreCase(getString(R.string.transaction_type_unrelated)) ){
                    WaCaiHandler.Item transDataPackage = buildDataPackage(currTransaction);
                    WaCaiHandler waCaiHandler = new WaCaiHandler(getActivity());
                    WaCaiHandler.AsyncCallBack asyncCallBack = new WaCaiHandler.AsyncCallBack() {
                        @Override
                        public void callback() {
                            isWacaiUpdated = true;
                            Log.i(LOG_TAG, "Wacai is updated!");
                            txtStatus.append( "-- The transaction is added to Wacai\n" );
                            delayedExecute();
                        }
                    };
                    waCaiHandler.addItem(transDataPackage, asyncCallBack);
                } else {
                    isWacaiUpdated = true;
                }
                // Local record

            }
        });

        Button btn_exit = (Button) getActivity().findViewById(R.id.fragment_manual_btn_exit);
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        txtStatus = (TextView) getActivity().findViewById(R.id.fragment_manual_txt_status);


        emptyFields();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeFirebaseListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        txtStatus.setText("");
    }

    /***** Firebase Listen and download *****/

    private void setupFirebaseListener(){
        firebaseHandler.syncServiceProviders();
    }

    private void closeFirebaseListener(){
        firebaseHandler.stopSyncServiceProviders();
    }


    /***** interface init *****/

    private void emptyFields(){
        editMoney.setText("");
        editSeperate.setText("1");
        editProvider.setText("");
        checkboxGPS.setChecked(false);
        spinnerCategory.setSelection(0);
        spinnerType.setSelection(0);
        editCity.setText("");
        editNote.setText("");
    }

    private void delayedExecute() {
        if( isFirebaseUpdated && isWacaiUpdated ) {
            isFirebaseUpdated = false;
            isWacaiUpdated = false;
            txtStatus.append("-- end\n");
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "Fileds Clear");
                    currTransaction.clear();
                    emptyFields();
                }
            };
            handler.postDelayed(runnable, 1000 * DELAY_TIME_IN_SECOND);
        }
    }

    // when submitting

    private boolean isReadyToSubmit() {
        if( editMoney.getText().toString().isEmpty() ){
            Toast.makeText(getActivity(), "Enter an amount", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void completeTransaction() {
        String money = editMoney.getText().toString();
        currTransaction.setMoney(money);
        int seperate = Integer.valueOf(editSeperate.getText().toString());
        currTransaction.setSeperate(seperate);
        String providerName = editProvider.getText().toString();
        currTransaction.setProviderName(providerName);
        for(Transaction.TransactionCategory eachCate : firebaseHandler.getCategoryList()){
            String selected = spinnerCategory.getSelectedItem().toString();
            if( eachCate.getName().equalsIgnoreCase(selected)){
                currTransaction.setCategory(eachCate);
            }
        }
        String type = spinnerType.getSelectedItem().toString();
        currTransaction.setType(type);
        String city = editCity.getText().toString();
        currTransaction.setCity(city);
        String note = editNote.getText().toString();
        currTransaction.setNote(note);
    }

    private WaCaiHandler.Item buildDataPackage(Transaction trans) {
        WaCaiHandler.Item newItem = new WaCaiHandler.Item();
        float moneyFloat = Float.parseFloat(trans.getMoney()) / trans.getSeperate();
        newItem.money = String.valueOf(moneyFloat);
        newItem.serviceProvider = trans.getProviderName();
        newItem.datetime = trans.getTextTime();
        newItem.categoryCode = trans.getCategory().getCode();
        newItem.note = trans.getNote();
        return newItem;
    }

    // Service Provider

    private class ServiceProviderAdapter extends ArrayAdapter<String> {
        private final Context ctxt;
        private final int mLayoutResourceId;
        private final ArrayList<FirebaseHandler.ServiceProvider> rawData;
        private ArrayList<String> allData;
        private ArrayList<String> suggestions;


        public ServiceProviderAdapter(@NonNull Context context, @LayoutRes int resource, ArrayList<FirebaseHandler.ServiceProvider> list) {
            super(context, resource);
            this.rawData = list;
            this.ctxt = context;
            this.mLayoutResourceId = resource;
            this.suggestions = new ArrayList<>();
            this.allData = new ArrayList<>();
            for(FirebaseHandler.ServiceProvider provider : rawData){
                allData.add(provider.getName());
            }
        }

        public int getCount() {
            return allData.size();
        }

        public String getItem(int position) {
            return allData.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    LayoutInflater inflater = ((Activity) ctxt).getLayoutInflater();
                    convertView = inflater.inflate(mLayoutResourceId, parent, false);
                }
                String providerName = getItem(position);
                TextView txtView = (TextView) convertView.findViewById(R.id.spinner_txtview_txt);
                txtView.setText(providerName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                public String convertResultToString(Object resultValue) {
                    return (String) resultValue;
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    if (constraint != null) {
                        suggestions.clear();
                        for (FirebaseHandler.ServiceProvider eachProvider : rawData) {
                            if (eachProvider.getName().toLowerCase().startsWith(constraint.toString().toLowerCase())) {
                                suggestions.add(eachProvider.getName());
                            }
                        }
                        FilterResults filterResults = new FilterResults();
                        filterResults.values = suggestions;
                        filterResults.count = suggestions.size();
                        return filterResults;
                    } else {
                        return new FilterResults();
                    }
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    allData.clear();
                    if (results != null && results.count > 0) {
                        // avoids unchecked cast warning when using mDepartments.addAll((ArrayList<Department>) results.values);
                        ArrayList<String> list = (ArrayList<String>) results.values;
                        for(String eachName : list)
                        allData.add(eachName);
                    }
                    notifyDataSetChanged();
                }
            };
        }




    }

    private FirebaseHandler.ServiceProvider getProviderService(String providerName){
        for(FirebaseHandler.ServiceProvider eachProvider : firebaseHandler.getProviderList()){
            if( eachProvider.getName().equalsIgnoreCase(providerName) ){
                return eachProvider;
            }
        }
        return null;
    }

    private boolean isFirstTimeLocUpdate(ArrayList<Pair<Double,Double>> locations){
        double eps = 0.0000000001;
        return locations.size()==1
                && (locations.get(0).first < eps && locations.get(0).first > -eps)
                && (locations.get(0).second < eps && locations.get(0).second > -eps);
    }

    private void retrieveGpsData() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Location Permission Not Granted.");
            Toast.makeText(getActivity(), "Location Permission Not Granted", Toast.LENGTH_SHORT).show();
            return;
        }
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 1 * 60 * 60 * 1000) { // every hour
            String cityName = getCityName(location);
            if( cityName!=null ) {
                editCity.setText(cityName);
            }
            currTransaction.setGpsLongitude(location.getLongitude());
            currTransaction.setGpsLatitude(location.getLatitude());
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                String cityName = getCityName(location);
                                if( cityName!=null ) {
                                    editCity.setText(cityName);
                                }
                                currTransaction.setGpsLongitude(location.getLongitude());
                                currTransaction.setGpsLatitude(location.getLatitude());
                                mLocationManager.removeUpdates(this);
                            }
                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {

                        }

                        @Override
                        public void onProviderDisabled(String s) {

                        }
                    });
        }
    }

    @Nullable
    private String getCityName(Location location){
        Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses!=null && addresses.size() > 0) {
            return addresses.get(0).getLocality();
        } else {
            return null;
        }
    }

    // time

    public void setTime(long timeInMs){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String txtTime = sdf.format(new Date(timeInMs));
        editTime.setText(txtTime);  // local time
        currTransaction.setLongTime(timeInMs);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00")); //beijing time for wacai
        currTransaction.setTextTime(sdf.format(new Date(timeInMs)));
    }





}
