package com.moutaigua.accounting;

import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import pub.devrel.easypermissions.AfterPermissionGranted;

/**
 * Created by mou on 5/3/17.
 */

public class ActivityMain extends FragmentActivity {

    public static final String SHAREPREF_NAME = "accounting";
    public static final String SHAREPREF_GMAIL_ACCOUNT_SELECTED = "gmail_account_selected";
    public static final int REQUEST_PERMISSIONS = 1;
    public static final int REQUEST_GMAIL_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    public static final int REQUEST_CHOOSE_LCOAL_FILE = 10004;
    private final long REPORT_LISTENER_PERIOD = 1000 * 10;  //30 sec
    private final String LOG_TAG = "Accounting_Main";

    private Toast toast;
    private PagerSlidingTabStrip tabs;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNotificationListenerService();
        localPermissionRequest();
        checkGooglePlayServices();

        initEmailAccounts();
        startEmailReportListener();

        //uploadServiceProvider();


        viewPager = (ViewPager) findViewById(R.id.main_viewpager);
        initViewPager(viewPager);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.main_tabs);
        tabs.setViewPager(viewPager);
        setupCurrentPager();




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                    startEmailReportListener();
                }
                break;
            case REQUEST_GMAIL_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        saveSelectedAccount(accountName);
                        startEmailReportListener();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    startEmailReportListener();
                }
                break;
            case REQUEST_CHOOSE_LCOAL_FILE:
                if (resultCode == RESULT_OK) {
                    Uri selectedfile = data.getData();
                }
                break;
        }
    }

    /***** Prerequisite *****/

    // permissions

    private void localPermissionRequest(){
        if (ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) +
                ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) +
                ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.GET_ACCOUNTS) +
                ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.ACCESS_FINE_LOCATION) +
                ContextCompat.checkSelfPermission(ActivityMain.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.GET_ACCOUNTS) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(ActivityMain.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                String[] PERMISSION_LIST = new String[] {
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                        android.Manifest.permission.GET_ACCOUNTS,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(ActivityMain.this,
                        PERMISSION_LIST,
                        REQUEST_PERMISSIONS);
            }
        }
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






    /***** Email Accounts *****/

    public void initEmailAccounts(){
        initGmailAccount();
    }

    @AfterPermissionGranted(ActivityMain.REQUEST_PERMISSION_GET_ACCOUNTS)
    public void initGmailAccount(){
        GoogleAccountCredential mCredential = GoogleAccountCredential
                .usingOAuth2(getApplicationContext(), Arrays.asList(GmailReportService.GMAIL_AUTH_SCOPES))
                .setBackOff(new ExponentialBackOff());
        String accountName = getSharedPreferences(ActivityMain.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getString(SHAREPREF_GMAIL_ACCOUNT_SELECTED, null);
        if (accountName == null) {
            startActivityForResult(mCredential.newChooseAccountIntent(), ActivityMain.REQUEST_GMAIL_ACCOUNT_PICKER);
        } else {
            mCredential.setSelectedAccountName(accountName);
        }
    }

    public void saveSelectedAccount(String account_name) {
        SharedPreferences pref = getSharedPreferences(ActivityMain.SHAREPREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SHAREPREF_GMAIL_ACCOUNT_SELECTED, account_name);
        editor.commit();
    }


    /***** Report Listening Service *****/

    // start a repeating background service to listen to email reports
    private void startEmailReportListener() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                REPORT_LISTENER_PERIOD, pIntent);
    }


    /***** ViewPager *****/

    private void initViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentPending(), "Pending");
        adapter.addFragment(new FragmentManual(), "Manual");
        adapter.addFragment(new Fragment(), "Records");
        adapter.addFragment(new FragmentSetting(), "Setting");
        viewPager.setAdapter(adapter);
    }

    private void setupCurrentPager(){
        FirebaseHandler handler = new FirebaseHandler();
        handler.getTransactions(new FirebaseHandler.TransactionCallback() {
            @Override
            public void onResult(@NonNull ArrayList<Transaction> transList) {
                if( transList.size()>0 ){
                    viewPager.setCurrentItem(0);
                } else {
                    viewPager.setCurrentItem(1);
                }
            }
        });
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final ArrayList<Fragment> mFragmentList;
        private final ArrayList<String> mFragmentTitleList;

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
            mFragmentList = new ArrayList<>();
            mFragmentTitleList = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


}
