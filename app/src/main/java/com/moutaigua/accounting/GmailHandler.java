package com.moutaigua.accounting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;

import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;

/**
 * Created by mou on 2/10/17.
 */

public class GmailHandler {

    private final String LOG_TAG = "GmailHandler";
    private final long PERIODICAL_LENGTH = 1000 * 30;  //30 sec
    private GoogleAccountCredential mCredential;
    private Context ctxt;



    public GmailHandler(Context context) {
        this.ctxt = context;
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential
                .usingOAuth2(ctxt.getApplicationContext(), Arrays.asList(GmailReportService.GMAIL_AUTH_SCOPES))
                .setBackOff(new ExponentialBackOff());
    }



    @AfterPermissionGranted(MainActivity.REQUEST_PERMISSION_GET_ACCOUNTS)
    public void initGmailAccount(){
        MainActivity parentActivity = (MainActivity) ctxt;
        String accountName = parentActivity.getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getString(GmailReportService.SHAREPREF_GMAIL_ACCOUNT_SELECTED, null);
        if (accountName == null) {
            parentActivity.startActivityForResult(mCredential.newChooseAccountIntent(), MainActivity.REQUEST_ACCOUNT_PICKER);
        } else {
            mCredential.setSelectedAccountName(accountName);
        }
    }

    public void saveSelectedAccount(String account_name) {
        SharedPreferences pref = ctxt.getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(GmailReportService.SHAREPREF_GMAIL_ACCOUNT_SELECTED, account_name);
        editor.commit();
    }



    public void startListeningToReports() {
        if( mCredential.getSelectedAccountName()!=null ) {
            MainActivity parentActivity = (MainActivity) ctxt;
            Intent intent = new Intent(parentActivity.getApplicationContext(), AlarmReceiver.class);
            // Create a PendingIntent to be triggered when the alarm goes off
            final PendingIntent pIntent = PendingIntent.getBroadcast(parentActivity, AlarmReceiver.REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
            long firstMillis = System.currentTimeMillis(); // alarm is set right away
            AlarmManager alarm = (AlarmManager) parentActivity.getSystemService(Context.ALARM_SERVICE);
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                    PERIODICAL_LENGTH, pIntent);

        }
    }

    public void stopListeningToReports() {
        MainActivity parentActivity = (MainActivity) ctxt;
        Intent intent = new Intent(parentActivity.getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(parentActivity, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) parentActivity.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }





}
