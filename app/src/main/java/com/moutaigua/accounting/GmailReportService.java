package com.moutaigua.accounting;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by mou on 2/11/17.
 */

public class GmailReportService extends IntentService {

    public static final String SHAREPREF_GMAIL_ACCOUNT_SELECTED = "gmail_account_selected";
    public static final String SHAREPREF_NOTICED_EMAIL_ID = "noticed_email_id";
    public static final String SHAREPREF_NOTIFICATION_ID = "notification_id";
    public static final String INTENT_KEY_EMAIL_TO_MARK_FROM = "email_from";
    public static final String INTENT_KEY_EMAIL_TO_MARK_ID = "email_id";
    public static final String INTENT_VALUE_EMAIL_TO_MARK_FROM_GMAIL = "gmail";
    public static final String[] GMAIL_AUTH_SCOPES = {
//            GmailScopes.GMAIL_LABELS,
            GmailScopes.MAIL_GOOGLE_COM,
//            GmailScopes.GMAIL_METADATA,
//            GmailScopes.GMAIL_MODIFY,
//            GmailScopes.GMAIL_READONLY
    };
    private final String GMAIL_USER_ID = "me";
    private final String GMAIL_QUERY = "label:INBOX is:unread ";
    private final int GMAIL_MESSAGE_HEADER_SENDER = 10;
    private final int GMAIL_MESSAGE_HEADER_SUBJECT = 14;

    private final String DISCOVER_REPORT_SENDER = "service.discover.com";
    private final String DISCOVER_REPORT_SUBJECT = "Your purchase exceeds the amount you set";
    private final String DISCOVER_REPORT_MONEY_PREFIX_TAG = "$";
    private final String DISCOVER_REPORT_MONEY_SUFFIX_TAG = "Date:";
    private final String DISCOVER_REPORT_MERCHANT_PREFIX_TAG = "Merchant:";
    private final String DISCOVER_REPORT_MERCHANT_SUFFIX_TAG = "Amount:";


    private final String LOG_TAG = "GmailReportService";

    private Gmail gmail;
    private int notificationId;
    private Set<String> noticedId;


    public GmailReportService() {
        super("GmailReportService");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        // google account init
        String accountName = getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getString(SHAREPREF_GMAIL_ACCOUNT_SELECTED, null);
        if (accountName == null) {
            Log.i(LOG_TAG, "No gmail accouont has been chosen.");
            stopSelf();
        } else {
            initGmailService(accountName);
        }
        // old unread reports list
        noticedId = getNoticedEmailId();
        // notification id init
        notificationId = getNotificationId();
    }




    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "Service starts");
        Bundle bundle = intent.getExtras();
        if( bundle!=null ){
            String email_id = bundle.getString(INTENT_KEY_EMAIL_TO_MARK_ID);
            markGmailEmailRead(email_id);
            for(String eachId : noticedId){
                if( eachId.equalsIgnoreCase(email_id) ){
                    noticedId.remove(eachId);
                    return;
                }
            }
        } else {
            Log.i(LOG_TAG, "Scanning...");
            ArrayList<Message> unreadEmails = getUnreadEmails();
            updateNoticedId(unreadEmails);
            ArrayList<Message> unnoticedEmails = getUnnoticedEmails(unreadEmails);
            ArrayList<ReportSummary> summaries = getSummaryToNotify(unnoticedEmails);
            for (ReportSummary eachSummary : summaries) {
                sendConfirmNotification(eachSummary);
                Log.i(LOG_TAG, eachSummary.getProvider() + " charges you $" + eachSummary.getMoney());
                notificationId = (notificationId + 1) % 100;
                noticedId.add(eachSummary.getEmailId());
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        saveNoticedEmailId();
        saveNotificationId();
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

    private Set<String> getNoticedEmailId(){
        String idsStr = getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getString(SHAREPREF_NOTICED_EMAIL_ID, null);
        if( idsStr!=null ) {
            Log.i(LOG_TAG, "Get: "+idsStr);
            String[] ids = idsStr.split(",");
            return new HashSet<>(Arrays.asList(ids));
        } else {
            return new HashSet<>();
        }
    }

    private void saveNoticedEmailId() {
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE).edit();
        String idsStr = "";
        for(String each: noticedId){
            idsStr += each + ",";
        }
        editor.putString(SHAREPREF_NOTICED_EMAIL_ID, idsStr);
        editor.apply();
        Log.i(LOG_TAG, "Save: "+idsStr);
    }

    private int getNotificationId(){
        return  getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE)
                .getInt(SHAREPREF_NOTIFICATION_ID, 0);
    }

    private void saveNotificationId(){
        SharedPreferences.Editor editor = getSharedPreferences(MainActivity.SHAREPREF_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(SHAREPREF_NOTIFICATION_ID, notificationId);
        editor.apply();
    }


    /**** Get Unread ****/

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

    private void updateNoticedId(ArrayList<Message> unreadEmails) {
        Iterator<String> iter = noticedId.iterator();
        while(iter.hasNext()){
            String eachNoticedId = iter.next();
            Boolean hasRead = true;
            for(Message eachUnread : unreadEmails){
                if( eachUnread.getId().equalsIgnoreCase(eachNoticedId) ){
                    hasRead = false;
                    break;
                }
            }
            if( hasRead ){
                iter.remove();
            }
        }
    }

    private ArrayList<Message> getUnnoticedEmails(ArrayList<Message> unreadEmails) {
        ArrayList<Message> list = new ArrayList<>();
        for(int i=0; i < unreadEmails.size(); ++i) {
            Boolean isUnnoticed = true;
            for(String id : noticedId){
                if( unreadEmails.get(i).getId().equalsIgnoreCase(id) ){
                    isUnnoticed = false;
                    break;
                }
            }
            if( isUnnoticed ){
                list.add(unreadEmails.get(i));
            }
        }
        return list;
    }

    private ArrayList<ReportSummary> getSummaryToNotify(ArrayList<Message> undetectedUnreadEmail){
        ArrayList<ReportSummary> summaries = new ArrayList<>();
        for(Message eachSummary : undetectedUnreadEmail){
            if( eachSummary.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_SENDER).getValue().equalsIgnoreCase(DISCOVER_REPORT_SENDER)
                    && eachSummary.getPayload().getHeaders().get(GMAIL_MESSAGE_HEADER_SUBJECT).getValue().equalsIgnoreCase(DISCOVER_REPORT_SUBJECT) ) {
                String fullBody = StringUtils.newStringUtf8( Base64.decodeBase64(eachSummary.getPayload().getParts().get(0).getBody().getData()) );
                String provider = getDiscoverTransMerchant(fullBody);
                String money = getDiscoverTransMoney(fullBody);
                String id = eachSummary.getId();
                ReportSummary summary = new ReportSummary();
                summary.setProvider(provider);
                summary.setMoney(money);
                summary.setEmailId(id);
                summary.setEmailPlatform(INTENT_VALUE_EMAIL_TO_MARK_FROM_GMAIL);
                summaries.add(summary);
                sendConfirmNotification(summary);
            }
        }
        return summaries;
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

    private void sendConfirmNotification(ReportSummary summary) {
        Intent recordIntent = new Intent(GmailReportService.this, MainActivity.class);
        recordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        recordIntent.putExtra(MainActivity.INTENT_MERCHANT_TAG, summary.getProvider());
        recordIntent.putExtra(MainActivity.INTENT_MONEY_TAG, summary.getMoney());
        recordIntent.putExtra(MainActivity.INTENT_EMAIL_ID_TAG, summary.getEmailId());
        recordIntent.putExtra(MainActivity.INTENT_EMAIL_FROM_TAG, summary.getEmailPlatform());
        recordIntent.putExtra(MainActivity.INTENT_NOTIFI_ID_TAG, notificationId);
        PendingIntent recordPendingIntent = PendingIntent.getActivity(GmailReportService.this, 0, recordIntent, PendingIntent.FLAG_ONE_SHOT);

        // building notification
        Drawable drawable = getDrawable(R.drawable.icon);
        Bitmap largeIcon = ((BitmapDrawable)drawable).getBitmap();
        String contentText = summary.getProvider() + " has charged you $" + summary.getMoney() + "\n"
                + getResources().getString(R.string.notifi_large_content_suffix);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(GmailReportService.this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(getResources().getString(R.string.notifi_title))
                        .setContentText(getResources().getString(R.string.notifi_small_content))
                        .setStyle( new NotificationCompat.BigTextStyle().bigText(contentText) )
                        .setLargeIcon(largeIcon)
                        .addAction(R.drawable.btn_add, "add", recordPendingIntent)
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setPriority(Notification.PRIORITY_DEFAULT);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(notificationId, mBuilder.build());
    }



    /**** Mark Read or Noticed ****/

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



//    private void sendCreateNotification() {
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.icon)
//                        .setContentTitle(getResources().getString(R.string.notifi_title))
//                        .setContentText("The app has started listening to transactions.")
//                        .setAutoCancel(true)
//                        .setWhen(System.currentTimeMillis())
//                        .setPriority(Notification.PRIORITY_DEFAULT);
//        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        manager.notify(0, mBuilder.build());
//    }


    private class ReportSummary implements Serializable {
        private String provider;
        private String money;
        private String emailPlatform;
        private String emailId;


        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getMoney() {
            return money;
        }

        public void setMoney(String money) {
            this.money = money;
        }

        public String getEmailPlatform() {
            return emailPlatform;
        }

        public void setEmailPlatform(String emailPlatform) {
            this.emailPlatform = emailPlatform;
        }

        public String getEmailId() {
            return emailId;
        }

        public void setEmailId(String emailId) {
            this.emailId = emailId;
        }
    }



}
