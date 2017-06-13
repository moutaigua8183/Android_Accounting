package com.moutaigua.accounting;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by mou on 5/6/17.
 * This is for the implementation of pending fragment
 */

public class FragmentPending extends Fragment {


    private final String LOG_TAG = "Fragment_Pending";
    private final int DELAY_TIME_IN_SECOND = 3;

    private FirebaseHandler firebaseHandler;
    private Spinner pendingTransactionSpinner;
    private ArrayList<Transaction> pendingTransactionsList; // all the transactions on Firebase
    private ArrayList<String> transactionSpinnerMenu;      // corresponding spinner item for each transaction
    private ArrayAdapter<String> transactionAdapter;        // transaction spinner adaper
    private Transaction currTransaction;
    private ServiceProviderAdapter providerAdapter;

    private View detailBlock;
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
    private boolean isProviderUpdated;


    public FragmentPending() {
        // Required empty public constructor
        firebaseHandler = new FirebaseHandler();
        pendingTransactionsList = new ArrayList<>();
        transactionSpinnerMenu = new ArrayList<>();
        isWacaiUpdated = false;
        isProviderUpdated = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pending, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupFirebaseListener();
        initPendingTransactionSpinner();

        detailBlock = getActivity().findViewById(R.id.fragment_pending_layout_details);


        editMoney = (EditText) getActivity().findViewById(R.id.fragment_pending_editxt_money);


        editSeperate = (EditText) getActivity().findViewById(R.id.fragment_pending_editxt_seperate);
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


        editProvider = (AutoCompleteTextView) getActivity().findViewById(R.id.fragment_pending_editxt_provider);
        providerAdapter = new ServiceProviderAdapter(getActivity(),
                R.layout.spinner_textview,
                firebaseHandler.getProviderList());
        editProvider.setThreshold(1);
        editProvider.setAdapter(providerAdapter);


        checkboxGPS = (CheckBox) getActivity().findViewById(R.id.fragment_pending_checkbox_gps);
        checkboxGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if( editProvider.getText().toString().isEmpty() ){
                    checkboxGPS.setChecked(false);
                }
            }
        });


        spinnerCategory = (Spinner) getActivity().findViewById(R.id.fragment_pending_spinner_category);
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
                } else {
                    editNote.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        spinnerType = (Spinner) getActivity().findViewById(R.id.fragment_pending_spinner_type);
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


        editCity = (EditText) getActivity().findViewById(R.id.fragment_pending_editxt_city);


        editNote = (EditText) getActivity().findViewById(R.id.fragment_pending_editxt_note);


        Button btnSubmit = (Button) getActivity().findViewById(R.id.fragment_pending_btn_submit);
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
                            isProviderUpdated = true;
                            txtStatus.append( "-- " + currTransaction.getProviderName() + " is updated\n" );
                            executeIfAllUpdated();
                        }

                        @Override
                        public void onFail() { // if no provider, no new provider or no new location is given
                            isProviderUpdated = true;
                            executeIfAllUpdated();
                        }

                    });
                } else {
                    isProviderUpdated = true;
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
                            executeIfAllUpdated();
                        }
                    };
                    waCaiHandler.addItem(transDataPackage, asyncCallBack);
                } else {
                    isWacaiUpdated = true;
                }
                // Share Update
                if( !currTransaction.getType().equalsIgnoreCase(getString(R.string.transaction_type_private)) ){
                    firebaseHandler.addShare(currTransaction);
                    txtStatus.append( "-- new Share card is added\n" );
                }
            }
        });

        Button btn_delete = (Button) getActivity().findViewById(R.id.fragment_pending_btn_delete);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.myDialogStyle));
                mBuilder.setMessage(R.string.fragment_pending_delete_alert)
                        .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                firebaseHandler.deleteTransaction(currTransaction.getReportId());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }
        });

        Button btn_exit = (Button) getActivity().findViewById(R.id.fragment_pending_btn_exit);
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        txtStatus = (TextView) getActivity().findViewById(R.id.fragment_pending_txt_status);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeFirebaseListener();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if( isVisibleToUser && txtStatus!=null && editNote!=null ){
            txtStatus.setText("");
            editNote.setText("");
        }
    }


    /***** Firebase Listen and download *****/

    private void setupFirebaseListener(){
        firebaseHandler.startListenNewTransactions(new FirebaseHandler.TransactionListener() {
            @Override
            public void onTransactionAdded(Transaction transaction) {
                pendingTransactionsList.add(transaction);
                String transTime = new SimpleDateFormat("EEE HH:mm").format(new Date(transaction.getLongTime()));
                String spinnerMenuItem = transaction.getProviderName() + " (" + transTime + ")"; // walmart (Sat HH:mm)
                if( pendingTransactionSpinner.isEnabled() ){
                    transactionSpinnerMenu.add(spinnerMenuItem);
                } else { // if there was no valid rawData
                    pendingTransactionSpinner.setEnabled(true);
                    transactionSpinnerMenu.clear();
                    transactionSpinnerMenu.add(spinnerMenuItem);
                    detailBlock.setVisibility(View.VISIBLE);
                    initFields(transaction);
                    currTransaction = transaction;
                }
                transactionAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTransactionRemoved(Transaction transaction) {
                for(int i = 0; i< pendingTransactionsList.size(); ++i){
                    if( pendingTransactionsList.get(i).getReportId()
                            .equalsIgnoreCase(transaction.getReportId()) ){
                        pendingTransactionsList.remove(i);
                        transactionSpinnerMenu.remove(i);
                        break;
                    }
                }
                if( transactionSpinnerMenu.size()==0 ){
                    // if there is no more valie rawData
                    pendingTransactionSpinner.setEnabled(false);
                    transactionSpinnerMenu.add(getString(R.string.fragment_pending_transaction_menu_no_entry));
                    detailBlock.setVisibility(View.INVISIBLE);
                    currTransaction = null;
                }
                transactionAdapter.notifyDataSetChanged();
                pendingTransactionSpinner.setSelection(0, true);
            }
        });
        firebaseHandler.syncServiceProviders();
    }

    private void closeFirebaseListener(){
        firebaseHandler.stopListenNewTransactions();
        firebaseHandler.stopSyncServiceProviders();
    }


    /***** interface init *****/

    private void initPendingTransactionSpinner() {
        pendingTransactionSpinner = (Spinner) getActivity().findViewById(R.id.fragment_pending_spinner_transaction_list);
        transactionAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_textview, transactionSpinnerMenu);
        pendingTransactionSpinner.setAdapter(transactionAdapter);
        pendingTransactionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if( pendingTransactionsList.size()>0 ) {
                    currTransaction = pendingTransactionsList.get(i);
                    initFields(pendingTransactionsList.get(i));
                } else { // if there is no valid transaction
                    currTransaction = null;
                    emptyFields();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        pendingTransactionSpinner.setEnabled(false);
        transactionSpinnerMenu.add(getString(R.string.fragment_pending_transaction_menu_no_entry));
        transactionAdapter.notifyDataSetChanged();
    }

    private void initFields(Transaction trans){
        editMoney.setText(trans.getMoney());
        editSeperate.setText(String.valueOf(trans.getSeperate()));
        editProvider.setText("");
        checkboxGPS.setChecked(false);
        spinnerCategory.setSelection(0);
        spinnerType.setSelection(0);
        editCity.setText(trans.getCity());
        editNote.setText("");
    }

    private void emptyFields(){
        editMoney.setText("");
        editSeperate.setText("");
        editProvider.setText("");
        checkboxGPS.setChecked(false);
        spinnerCategory.setSelection(0);
        spinnerType.setSelection(0);
        editCity.setText("");
        editNote.setText("");
    }

    private void executeIfAllUpdated() {
        if( isProviderUpdated && isWacaiUpdated ) {
            isProviderUpdated = false;
            isWacaiUpdated = false;
            txtStatus.append("-- end (This transaction is deleting)\n");
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "deleted");
                    firebaseHandler.deleteTransaction(currTransaction.getReportId());
                }
            };
            handler.postDelayed(runnable, 1000 * DELAY_TIME_IN_SECOND);
        }
    }

    // when submitting

    private boolean isReadyToSubmit() {
        if( currTransaction==null ){
            Toast.makeText(getActivity(), "No valid transaction is selected", Toast.LENGTH_LONG).show();
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
                break;
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
        newItem.categoryCode = trans.getCategory().getCode();
        newItem.note = trans.getNote();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));      // beijing time for wacai
        String txtTime = sdf.format(new Date(trans.getLongTime()));
        newItem.datetime = txtTime;
        return newItem;
    }

    // AutoCompleteText

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
        double eps = 0.0000001;
        return locations.size()==1
                && (locations.get(0).first < eps && locations.get(0).first > -eps)
                && (locations.get(0).second < eps && locations.get(0).second > -eps);
    }





}
