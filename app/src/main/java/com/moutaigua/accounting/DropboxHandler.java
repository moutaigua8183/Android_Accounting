package com.moutaigua.accounting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by mou on 1/25/17.
 */

public class DropboxHandler {

    final static private String APP_KEY = "ix7ltus2kmklg3w";
    final static private String APP_SECRET = "esfjq59v5m769qb";
    final static private String SHAREPREF_DROPBOX_ACCESSTOKEN = "dropbox_accessToken";

    private Context ctxt;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private Button loginButton;




    public DropboxHandler(Context context){
        ctxt = context;
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        SharedPreferences pref = ctxt.getSharedPreferences(MainActivity1.SHAREPREF_NAME, Context.MODE_PRIVATE);
        String accessToken = pref.getString(SHAREPREF_DROPBOX_ACCESSTOKEN, null);
        if( accessToken!=null ){
            mDBApi.getSession().setOAuth2AccessToken(accessToken);
        }
    }


    public interface AsyncCallBack {
        void callback();
    }


    public void loginButtonClick(){
        if(isDropboxLinked()){
            dropboxLogOut();
        } else {
            dropboxLogIn();
        }
        loginButtonRefresh();
    }

    private void dropboxLogIn(){
        SharedPreferences pref = ctxt.getSharedPreferences(MainActivity1.SHAREPREF_NAME, Context.MODE_PRIVATE);
        String accessToken = pref.getString(SHAREPREF_DROPBOX_ACCESSTOKEN, null);
        if( accessToken==null ) {
            mDBApi.getSession().startOAuth2Authentication(ctxt);
        } else {
            mDBApi.getSession().setOAuth2AccessToken(accessToken);
        }
    }

    private void dropboxLogOut(){
        mDBApi.getSession().unlink();
    }

    public boolean isDropboxLinked() {
        return mDBApi.getSession().isLinked();
    }

    public void saveAccessTokenPair(){
        if (mDBApi!=null && mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                SharedPreferences pref = ctxt.getSharedPreferences(MainActivity1.SHAREPREF_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(SHAREPREF_DROPBOX_ACCESSTOKEN, accessToken);
                editor.commit();
            } catch (IllegalStateException e) {
                Log.i("Dropbox_AuthLog", "Error authenticating", e);
            }
        }
    }

    public void attachLoginButton(Button button){
        loginButton = button;
    }

    public void loginButtonRefresh(){
        if( isDropboxLinked() ){
            loginButton.setText(R.string.dropbox_logout);
        } else {
            loginButton.setText(R.string.dropbox_login);
        }
    }

    public void getFile(String online_rel_addr, String local_file_name, AsyncCallBack callBack){
        String[] params = new String[]{online_rel_addr, local_file_name};
        DownloadFile downloadProc = new DownloadFile(callBack);
        downloadProc.execute(params);
    }

    public void updateFile(String online_rel_addr, String local_file_name){
        String[] params = new String[]{online_rel_addr, local_file_name};
        OverwriteFile uploadProc = new OverwriteFile();
        uploadProc.execute(params);
    }





    private class DownloadFile  extends AsyncTask<String, Void, Void> {

        private AsyncCallBack asyncCallBack;

        public DownloadFile(AsyncCallBack callBack){
            asyncCallBack = callBack;
        }


        @Override
        protected Void doInBackground(String... strings) {
            String online_rel_addr = strings[0];
            String local_file_name = strings[1];
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), local_file_name);
            try {
                FileOutputStream outputStream = new FileOutputStream(file, false);
                DropboxAPI.DropboxFileInfo info = mDBApi.getFile(online_rel_addr, null, outputStream, null);
                Log.i("Dropbox_Log", "The file's rev is: " + info.getMetadata().rev);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (DropboxException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            asyncCallBack.callback();
        }
    }

    private class OverwriteFile extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String online_rel_addr = strings[0];
            String local_file_name = strings[1];
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), local_file_name);
            try {
                FileInputStream inputStream = new FileInputStream(file);
                DropboxAPI.Entry response = mDBApi.putFileOverwrite(online_rel_addr, inputStream, file.length(), null);
                inputStream.close();
                Log.i("Dropbox_Log", "The file's rev is: " + response.rev);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (DropboxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}



