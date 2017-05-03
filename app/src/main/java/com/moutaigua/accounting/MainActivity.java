package com.moutaigua.accounting;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.xmlbeans.impl.soap.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity{

    final static public String WORKBOOK_DICTIONARY_DROPBOX = "/喵喵喵/美国分账本.xlsx";
    final static public String WORKBOOK_DICTIONARY_LOCAL = "/temp.xlsx";
    final static public String SHAREPREF_NAME = "accounting";
    final static public String INTENT_MERCHANT_TAG = "TransactionMerchant";
    final static public String INTENT_MONEY_TAG = "TransactionAmount";
    final static public String INTENT_NOTIFI_ID_TAG = "NotificationId";
    final static public String INTENT_EMAIL_ID_TAG = "EmailId";
    final static public String INTENT_EMAIL_FROM_TAG = "EmailFrom";
    final static private int REQUEST_PERMISSIONS_EXTERNAL_STORAGE = 1;


    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    private final String LOG_TAG = "Accounting_Main";
    private final int DELAY_TIME_IN_SECOND = 3;
    private Toast toast;
    private DropboxHandler dropboxHandler;
    private FirebaseHandler firebaseHandler;
    private GmailHandler gmailHandler;
    private Calendar calendar;
    private Button submit_btn;
    private EditText dateText;
    private EditText timeText;
    private EditText whereText;
    private AutoCompleteTextView providerText;
    private EditText moneyText;
    private EditText noteText;
    private TextView dropboxStatusText;
    private TextView wacaiStatusText;
    private Spinner buyerSpinner;
    private ToggleButton mouPayTgBtn;



    private String buyer;
    private boolean isFromNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNotificationListenerService();
        localPermissionRequest();
        checkGooglePlayServices();

        isFromNotification = false;
        firebaseHandler = new FirebaseHandler();
        firebaseHandler.syncServiceProviders();

        dropboxHandler = new DropboxHandler(this);
        Button dropbox_logio_btn = (Button) findViewById(R.id.btn_dropbox);
        dropboxHandler.attachLoginButton(dropbox_logio_btn);
        dropboxHandler.loginButtonRefresh();
        dropbox_logio_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dropboxHandler.loginButtonClick();
            }
        });

        gmailHandler = new GmailHandler(this);
        gmailHandler.initGmailAccount();
        gmailHandler.startListeningToReports();

        submit_btn = (Button) findViewById(R.id.btn_submit);
        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( !isReadyToSubmit() ){
                    return;
                }
                // Dropbox Update
                if( !buyerSpinner.getSelectedItem().toString().equalsIgnoreCase("M") ) {
                    dropboxStatusText.setText(getString(R.string.status_updating));
                    DropboxHandler.AsyncCallBack asyncCallBack = new DropboxHandler.AsyncCallBack() {
                        @Override
                        public void callback() {
                            Log.i("Dropbox", "Download the file successfully");
                            ExcelHandler excelHandler = new ExcelHandler();
                            excelHandler.open(WORKBOOK_DICTIONARY_LOCAL);
                            ExcelHandler.Transaction transaction = new ExcelHandler.Transaction();
                            transaction.date = dateText.getText().toString();
                            transaction.location = whereText.getText().toString();
                            transaction.what = providerText.getText().toString();
                            transaction.buyer = buyer;
                            transaction.payer = mouPayTgBtn.isChecked() ? "M" : "W";
                            transaction.money = Float.valueOf(moneyText.getText().toString());
                            transaction.note = noteText.getText().toString();
                            excelHandler.record(transaction);
                            excelHandler.close();
                            dropboxHandler.updateFile(WORKBOOK_DICTIONARY_DROPBOX, WORKBOOK_DICTIONARY_LOCAL);
                            Log.i("Dropbox", "Dropbox is updated!");
                            dropboxStatusText.setText(getString(R.string.status_complete));
                        }
                    };
                    dropboxHandler.getFile(WORKBOOK_DICTIONARY_DROPBOX, WORKBOOK_DICTIONARY_LOCAL, asyncCallBack);
                }
                // Firebase Update
                FirebaseHandler.ServiceProvider provider = new FirebaseHandler.ServiceProvider();
                provider.location = whereText.getText().toString();
                provider.name = providerText.getText().toString();
                FirebaseHandler firebaseHandler = new FirebaseHandler();
                firebaseHandler.addServiceProvider(provider);
                // WaCai Update
                WaCaiHandler waCaiHandler = new WaCaiHandler(MainActivity.this);
                WaCaiHandler.Item newItem = new WaCaiHandler.Item();
                newItem.serviceProvider = providerText.getText().toString();
                newItem.datetime = waCaiHandler.getFormattedDate(dateText.getText().toString())
                        + " "
                        + waCaiHandler.getFormattedTime(timeText.getText().toString());
                newItem.note = noteText.getText().toString();
                wacaiStatusText.setText(getString(R.string.status_updating));
                WaCaiHandler.AsyncCallBack asyncCallBack = new WaCaiHandler.AsyncCallBack() {
                    @Override
                    public void callback() {
                        Log.i("Wacai", "Wacai is updated!");
                        wacaiStatusText.setText(getString(R.string.status_complete));
                        if( isFromNotification ) {
                            toast = Toast.makeText(
                                        MainActivity.this,
                                        "The app will be closed after " + String.valueOf(DELAY_TIME_IN_SECOND) + " seconds",
                                        Toast.LENGTH_SHORT
                            );
                            toast.show();
                            delayedFinish();
                        } else {
                            delayedClear();
                        }
                    }
                };
                if( buyerSpinner.getSelectedItem().toString().equalsIgnoreCase("M")) {
                    newItem.money = Float.valueOf(moneyText.getText().toString());
                    waCaiHandler.addItem(newItem, asyncCallBack);
                } else if (buyerSpinner.getSelectedItem().toString().equalsIgnoreCase("WM")) {
                    newItem.money = Float.valueOf(moneyText.getText().toString()) / 2;
                    waCaiHandler.addItem(newItem, asyncCallBack);
                }
            }
        });

        calendar = Calendar.getInstance();
        dateText = (EditText) findViewById(R.id.editxt_date);
        String date_format = "MM/dd/yyyy";
        SimpleDateFormat date_sdf = new SimpleDateFormat(date_format, Locale.US);
        dateText.setText( date_sdf.format(calendar.getTime()) );

        timeText = (EditText) findViewById(R.id.editxt_time);
        String time_format = "HH:mm:ss";
        SimpleDateFormat time_sdf = new SimpleDateFormat(time_format, Locale.US);
        timeText.setText( time_sdf.format(calendar.getTime()) );

        whereText = (EditText) findViewById(R.id.editxt_location);
        whereText.setText("Chicago");

        providerText = (AutoCompleteTextView) findViewById(R.id.textview_provider);
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                firebaseHandler.getProviderNameArrayList());
        providerText.setThreshold(1);
        providerText.setAdapter(providerAdapter);
        providerAdapter.notifyDataSetChanged();

        buyerSpinner = (Spinner) findViewById(R.id.spinner_buyer);
        buyer = "WM";
        ArrayList<String> categories = new ArrayList<>();
        categories.add("WM");
        categories.add("W");
        categories.add("M");
        ArrayAdapter<String> buyerAdapter = new ArrayAdapter<>(this,  R.layout.spinner_textview, categories);
        buyerSpinner.setAdapter(buyerAdapter);
        buyerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                buyer = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mouPayTgBtn = (ToggleButton) findViewById(R.id.toggle_whopay);
        mouPayTgBtn.setChecked(true);

        moneyText = (EditText) findViewById(R.id.editxt_money);
        Intent intent = getIntent();
        String moneyStr = intent.getStringExtra(INTENT_MONEY_TAG);
        if( moneyStr!=null ){
            isFromNotification = true;
            moneyText.setText(moneyStr);
            String merchantStr = intent.getStringExtra(INTENT_MERCHANT_TAG);
            providerText.setText( WordUtils.capitalizeFully(merchantStr.toLowerCase()));
            providerText.setThreshold(1);
            // to cancel the notification
            int notifi_id = intent.getIntExtra(INTENT_NOTIFI_ID_TAG, 0);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(notifi_id);
            // notify service to mark the email read
            String email_id = intent.getStringExtra(INTENT_EMAIL_ID_TAG);
            String email_from = intent.getStringExtra(INTENT_EMAIL_FROM_TAG);
            Intent serviceIntent = new Intent(this, GmailReportService.class);
            serviceIntent.putExtra(GmailReportService.INTENT_KEY_EMAIL_TO_MARK_FROM, GmailReportService.INTENT_VALUE_EMAIL_TO_MARK_FROM_GMAIL);
            serviceIntent.putExtra(GmailReportService.INTENT_KEY_EMAIL_TO_MARK_ID, email_id);
            startService(serviceIntent);
            Log.i(LOG_TAG, "Merch: "+merchantStr);
            Log.i(LOG_TAG, "Money: "+moneyStr);
            Log.i(LOG_TAG, "Id: "+email_id);
            Log.i(LOG_TAG, "From: "+email_from);
            Log.i(LOG_TAG, "Notifi_id: "+notifi_id);
        }

        noteText = (EditText) findViewById(R.id.editxt_note);
        dropboxStatusText = (TextView) findViewById(R.id.txt_status_dropbox);
        wacaiStatusText = (TextView) findViewById(R.id.txt_status_wacai);


        Button btn_exit = (Button) findViewById(R.id.btn_exit);
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }





    protected void onResume() {
        super.onResume();
        dropboxHandler.saveAccessTokenPair();
        dropboxHandler.loginButtonRefresh();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        firebaseHandler.stopSyncServiceProviders();
    }


    public Toast getToast(){
        return toast;
    }

    public void setToast(Toast temp_toast){
        toast = temp_toast;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    toast.makeText(this,
                            "This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.",
                            Toast.LENGTH_LONG).show();
                } else {
                    gmailHandler.startListeningToReports();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        gmailHandler.saveSelectedAccount(accountName);
                        gmailHandler.startListeningToReports();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    gmailHandler.startListeningToReports();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "The permission has been granted", Toast.LENGTH_SHORT);
                } else {
                    Toast.makeText(this, "The permission has not been granted", Toast.LENGTH_SHORT);
                    submit_btn.setEnabled(false);
                }
                break;
            case REQUEST_PERMISSION_GET_ACCOUNTS:
                EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
                break;
        }
    }


    private void localPermissionRequest(){
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) +
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.GET_ACCOUNTS)) {
            } else {
                String[] PERMISSION_LIST = new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                        Manifest.permission.GET_ACCOUNTS
                };

                ActivityCompat.requestPermissions(MainActivity.this,
                        PERMISSION_LIST,
                        REQUEST_PERMISSIONS_EXTERNAL_STORAGE);
            }
        }
    }

    private boolean isReadyToSubmit() {
        if( whereText.getText().toString().isEmpty()
                || providerText.getText().toString().isEmpty()
                || moneyText.getText().toString().isEmpty() ){
            toast = Toast.makeText(this, "Required fields should not be empty", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
        if( buyerSpinner.getSelectedItem().toString().equalsIgnoreCase("W")
                && !mouPayTgBtn.isChecked() ) {
            toast = Toast.makeText(this, "No need to record", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
        if( !buyerSpinner.getSelectedItem().toString().equalsIgnoreCase("M")
                && !dropboxHandler.isDropboxLinked() ){
            toast = Toast.makeText(this, "Please login Dropbox first!", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
        return true;
    }

    private void delayedFinish() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };
        handler.postDelayed(runnable, 1000 * DELAY_TIME_IN_SECOND);
    }

    private void delayedClear() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                moneyText.setText("");
                noteText.setText("");
                dropboxStatusText.setText(getString(R.string.status_initial));
                wacaiStatusText.setText(getString(R.string.status_initial));
            }
        };
        handler.postDelayed(runnable, 1000 * DELAY_TIME_IN_SECOND);
    }

    // Notification Listener Service
    private void initNotificationListenerService() {
        if( !isNotificationListenerEnabled() ){
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
        restartNotificationListenerService();
    }

    private boolean isNotificationListenerEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void restartNotificationListenerService() {
        if( !isNotificationListenerServiceRunning() ) {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, TransactionListener.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(this, TransactionListener.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
    }

    private boolean isNotificationListenerServiceRunning() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (packageNames.contains(getPackageName())) {
            return true;
        }
        return false;
    }


    // Google Play Service and Google Account
    private void checkGooglePlayServices(){
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }






}
