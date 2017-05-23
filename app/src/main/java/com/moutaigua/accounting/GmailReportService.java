package com.moutaigua.accounting;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by mou on 2/11/17.
 * This is a service which can listen to the transaction reports in Gmail.
 */

public class GmailReportService extends IntentService {


    public static final String INTENT_KEY_EMAIL_TO_MARK_FROM = "email_from";
    public static final String INTENT_KEY_EMAIL_TO_MARK_ID = "email_id";
    public static final String INTENT_VALUE_EMAIL_TO_MARK_FROM_GMAIL = "gmail";
    public static final String REPORT_SOURCE = "gmail";
    public static final String[] GMAIL_AUTH_SCOPES = {
//            GmailScopes.GMAIL_LABELS,
            GmailScopes.MAIL_GOOGLE_COM,
//            GmailScopes.GMAIL_METADATA,
//            GmailScopes.GMAIL_MODIFY,
//            GmailScopes.GMAIL_READONLY
    };
    private final String GMAIL_USER_ID = "me";
    private final String GMAIL_QUERY = "label:INBOX is:unread ";
    private final int GMAIL_MESSAGE_HEADER_TIME = 10;
    private final int GMAIL_MESSAGE_HEADER_SENDER = 13;
    private final int GMAIL_MESSAGE_HEADER_SUBJECT = 17;

    private final String DISCOVER_REPORT_SENDER = "service.discover.com";
    private final String DISCOVER_REPORT_SUBJECT = "Your purchase exceeds the amount you set";
    private final String DISCOVER_REPORT_MONEY_PREFIX_TAG = "$";
    private final String DISCOVER_REPORT_MONEY_SUFFIX_TAG = "Date:";
    private final String DISCOVER_REPORT_MERCHANT_PREFIX_TAG = "Merchant:";
    private final String DISCOVER_REPORT_MERCHANT_SUFFIX_TAG = "Amount:";


    private final String LOG_TAG = "GmailReportService";
    private final int NOTIFICATION_ID = 7;

    private Gmail gmail;
    private FirebaseHandler firebaseHandler;
    private LocationManager mLocationManager;
    private String cityName;


    public GmailReportService() {
        super("GmailReportService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        String accountName = getSharedPreferences(ActivityMain.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getString(ActivityMain.SHAREPREF_GMAIL_ACCOUNT_SELECTED, null);
        if (accountName == null) {
            Log.i(LOG_TAG, "No gmail accouont has been chosen.");
            stopSelf();
        } else {
            initGmailService(accountName);
            retrieveCurrentCity();
            firebaseHandler = new FirebaseHandler();
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if( cityName==null ){
            // if location is not clear, this transaction will be short for information.
            // so this detection will be ignored.
            Log.i(LOG_TAG, "Since City is not detected, the service doesn\'t start.");
            stopSelf();
        } else {
            Log.i(LOG_TAG, "Service starts");
            Log.i(LOG_TAG, "Scanning...");
            ArrayList<Message> unreadEmails = getUnreadEmails();
            analyseDiscoverReports(unreadEmails);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Service ends");
    }


    private void initGmailService(String accountName) {
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(GMAIL_AUTH_SCOPES)).setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(accountName);
        HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, mCredential)
                .setApplicationName(getResources().getString(R.string.app_name))
                .build();
    }

    private void retrieveCurrentCity() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Location Permission Not Granted.");
            stopSelf();
            return;
        }
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() - 1 * 60 * 60 * 1000) { // every hour
            cityName = getCityName(location);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                cityName = getCityName(location);
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
        Log.i(LOG_TAG, "Transaction happens in " + cityName);
    }

    @Nullable
    private String getCityName(Location location){
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
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


    /**** Get Unread Discover Report ****/

    private void analyseDiscoverReports(ArrayList<Message> unreadEmails) {
        ArrayList<Transaction> transList = getDiscoverTransactions(unreadEmails);
        for (Transaction eachTrans : transList) {
            Log.i(LOG_TAG, eachTrans.getProvider() + " charges you $" + eachTrans.getMoney());
            firebaseHandler.addTransaction(eachTrans);
            markGmailEmailRead(eachTrans.getReportId());
        }
        if( transList.size()>0 ){
            // if there are new transactions available, send notification
            sendNotification();
        }
    }

    private ArrayList<Transaction> getDiscoverTransactions(ArrayList<Message> unreadEmails){
        ArrayList<Transaction> transList = new ArrayList<>();
        for(Message eachMail : unreadEmails){
            Log.i(LOG_TAG, eachMail.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_TIME).toString());
            if( eachMail.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_SENDER).getValue().equalsIgnoreCase(DISCOVER_REPORT_SENDER)
                    && eachMail.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_SUBJECT).getValue().equalsIgnoreCase(DISCOVER_REPORT_SUBJECT) ) {
                String fullBody = StringUtils.newStringUtf8( Base64.decodeBase64(eachMail.getPayload().getParts().get(0).getBody().getData()) );
                String provider = getDiscoverTransMerchant(fullBody);
                String money = getDiscoverTransMoney(fullBody);
                String id = eachMail.getId();
                String rawDate = eachMail.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_TIME).getValue(); // 2 May 2017 22:02:32 -0400
                Date date = null;
                String textDate = null;
                try {
                    date = new SimpleDateFormat("d MMM yyyy hh:mm:ss z", Locale.US).parse(rawDate);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                    textDate = sdf.format(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Transaction transaction = new Transaction();
                transaction.setTextTime(textDate);
                transaction.setLongTime(date.getTime());
                transaction.setProvider(provider);
                transaction.setMoney(money);
                transaction.setReportId(id);
                transaction.setCity(cityName);
                transaction.setReportSource(REPORT_SOURCE);
                transList.add(transaction);
            }
        }
        return transList;
    }

    private String getDiscoverTransMoney(String text) {
        int startIndex = text.indexOf(DISCOVER_REPORT_MONEY_PREFIX_TAG) + DISCOVER_REPORT_MONEY_PREFIX_TAG.length();
        int endIndex = text.indexOf(DISCOVER_REPORT_MONEY_SUFFIX_TAG, startIndex);
        String money = text.substring(startIndex, endIndex).trim();
        return money;
    }

    private String getDiscoverTransMerchant(String text) {
        int startIndex = text.indexOf(DISCOVER_REPORT_MERCHANT_PREFIX_TAG) + DISCOVER_REPORT_MERCHANT_PREFIX_TAG.length();
        int endIndex = text.indexOf(DISCOVER_REPORT_MERCHANT_SUFFIX_TAG, startIndex);
        String merchant = text.substring(startIndex, endIndex).trim();
        return merchant;
    }



    /**** Mark Read or Noticed ****/

    private ArrayList<Message> getUnreadEmails() {
        ArrayList<Message> messages = new ArrayList<>();
        try {
            ListMessagesResponse response = gmail.users().messages().list(GMAIL_USER_ID).setQ(GMAIL_QUERY).execute();
            while (response.getMessages() != null) {
                messages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = gmail.users().messages().list(GMAIL_USER_ID).setQ(GMAIL_QUERY).setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }
            for(int i=0; i < messages.size(); ++i) {
                Message fullMessage = gmail.users().messages().get(GMAIL_USER_ID, messages.get(i).getId()).execute();
                messages.set(i, fullMessage);
            }
        } catch (UserRecoverableAuthIOException userRecoverableException) {
            userRecoverableException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messages;
    }

    private void markGmailEmailRead(String email_id){
        try {
            List<String> labelsToRemove = new ArrayList<>();
            labelsToRemove.add("UNREAD");
            ModifyMessageRequest mods = new ModifyMessageRequest().setRemoveLabelIds(labelsToRemove);
            gmail.users().messages().modify(GMAIL_USER_ID, email_id, mods).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**** Notification ****/

    private void sendNotification() {
        Intent recordIntent = new Intent(this, ActivityMain.class);
        recordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent recordPendingIntent = PendingIntent.getActivity(GmailReportService.this, 0, recordIntent, PendingIntent.FLAG_ONE_SHOT);
        // building notification
        Drawable drawable = getDrawable(R.drawable.icon);
        Bitmap largeIcon = ((BitmapDrawable)drawable).getBitmap();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(GmailReportService.this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(getResources().getString(R.string.notifi_title))
                        .setContentText(getResources().getString(R.string.notifi_small_content))
                        .setContentIntent(recordPendingIntent)
                        .setLargeIcon(largeIcon)
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setPriority(Notification.PRIORITY_DEFAULT);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, mBuilder.build());
    }


}
