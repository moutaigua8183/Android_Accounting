package com.moutaigua.accounting;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by mou on 1/31/17.
 */

public class TransactionListener extends NotificationListenerService {


    public static final int NOTIFICATION_ID = 0;
    private static final String LOG_TAG = "TransListener";
    private final String DISCOVER_REPORT_TITLE = "Discover Card";
    private final String DISCOVER_REPORT_CONTENT = "Your purchase exceeds the amount you set";
    private final String DISCOVER_REPORT_MONEY_PREFIX_TAG = "$";
    private final String DISCOVER_REPORT_MONEY_SUFFIX_TAG = "Date:";
    private final String DISCOVER_REPORT_MERCHANT_PREFIX_TAG = "Merchant:";
    private final String DISCOVER_REPORT_MERCHANT_SUFFIX_TAG = "Amount:";
    private final String BOA_REPORT_TITLE = "Bank of America";
    private final String BOA_REPORT_CONTENT = "Account Alert: Credit Card Transaction Over Specified Alert Limit";
    private final String BOA_REPORT_MONEY_PREFIX_TAG = "$";
    private final String BOA_REPORT_MONEY_SUFFIX_TAG = "Date:";
    private final String BOA_REPORT_MERCHANT_PREFIX_TAG = "Merchant:";
    private final String BOA_REPORT_MERCHANT_SUFFIX_TAG = "Amount:";



    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if( isTransReport(sbn) ) {
            CharSequence bigTextChar = sbn.getNotification().extras.getCharSequence("android.bigText");
            if(bigTextChar != null){
                String bigText = bigTextChar.toString();
                if (bigText.contains("Discover")) {
                    String[] transInfo = {getDiscoverTransMerchant(bigText), getDiscoverTransMoney(bigText)};
                    sendNotification(transInfo);
                } else if (bigText.contains("Bank of American")) {
                    sendNotification(new String[]{"Temp", "10.00"});
                }
            } else {
                Log.i(LOG_TAG, "No email content has been detected!!!");
            }
        }
    }


    private boolean isTransReport(StatusBarNotification sbn) {
        String title = sbn.getNotification().extras.getString("android.title", null);
        String text = sbn.getNotification().extras.getString("android.text", null);
        if (title==null || text==null) {
            return false;
        }
        writeToLog(sbn);
        return
                (title.equalsIgnoreCase(DISCOVER_REPORT_TITLE) && text.equalsIgnoreCase(DISCOVER_REPORT_CONTENT))
               || (title.equalsIgnoreCase(BOA_REPORT_TITLE) && text.equalsIgnoreCase(BOA_REPORT_CONTENT));
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

    private void sendNotification(String[] info) {
        Intent recordIntent = new Intent(this, MainActivity1.class);
        recordIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        recordIntent.putExtra(MainActivity1.INTENT_MERCHANT_TAG, info[0]);
        recordIntent.putExtra(MainActivity1.INTENT_MONEY_TAG, info[1]);
        PendingIntent recordPendingIntent = PendingIntent.getActivity(this, 0, recordIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // building notification
        Drawable drawable = getDrawable(R.drawable.icon);
        Bitmap largeIcon = ((BitmapDrawable)drawable).getBitmap();
        String contentText = info[0] + " has charged you $" + info[1] + "\n"
                + getResources().getString(R.string.notifi_large_content_suffix);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
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
        manager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void writeToLog(StatusBarNotification sbn){
        String title = sbn.getNotification().extras.getString("android.title", null);
        String text = sbn.getNotification().extras.getString("android.text", null);
        CharSequence bigTextChar = sbn.getNotification().extras.getCharSequence("android.bigText");
        CharSequence subTextChar = sbn.getNotification().extras.getCharSequence("android.subText");
        String bigText = null;
        String subText = null;
        if(bigTextChar != null){
            bigText = bigTextChar.toString();
        }
        if(subTextChar != null){
            subText = subTextChar.toString();
        }
        File log = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/accounting_log.txt");
        try {
            if (!log.exists()) {
                log.createNewFile();
            }
            FileOutputStream fOut = new FileOutputStream(log, true);
            String content = "Title: " + title + "\n";
            content += "Text:  " + text + "\n";
            content += "bigText: " + bigText + "\n";
            content += "subText: " + subText + "\n";
            content += "Time:  " + Calendar.getInstance().getTime().toString() + "\n";
            content += "*********\n\n";
            fOut.write(content.getBytes());
            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}

