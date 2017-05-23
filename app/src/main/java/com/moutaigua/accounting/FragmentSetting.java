package com.moutaigua.accounting;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import org.apache.poi.xwpf.usermodel.TOC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Created by mou on 5/6/17.
 */

public class FragmentSetting extends Fragment {


    private final String LOG_TAG = "Fragment_Setting";

    private FirebaseHandler firebaseHandler;

    private EditText editCategory;
    private Button btnAddCategory;
    private EditText editProvider;
    private Button btnUploadProvider;
    private Uri providersFileUri;


    public FragmentSetting() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        firebaseHandler = new FirebaseHandler();

        editCategory = (EditText) getActivity().findViewById(R.id.fragment_setting_editxt_category);
        btnAddCategory = (Button) getActivity().findViewById(R.id.fragment_setting_btn_add_category);
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( editCategory.getText().toString().isEmpty() ){
                    Toast.makeText(getActivity(), "Enter a valid name", Toast.LENGTH_SHORT).show();
                } else {
                    if( editCategory.getText().toString().isEmpty() ){
                        editCategory.requestFocus();
                        return;
                    }
                    Transaction.TransactionCategory category = new Transaction.TransactionCategory();
                    category.setName(editCategory.getText().toString());
                    firebaseHandler.addCategory(category, new FirebaseHandler.ResultCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getActivity(), "New category has been added", Toast.LENGTH_SHORT).show();
                            editCategory.setText("");
                        }

                        @Override
                        public void onFail() {
                            Toast.makeText(getActivity(), "Such category has already existed", Toast.LENGTH_SHORT).show();
                            editCategory.requestFocus();
                        }
                    });
                }
            }
        });


        editProvider = (EditText) getActivity().findViewById(R.id.fragment_setting_editxt_provider_file);
        editProvider.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    chooseFile();
                } else {
                    if(editProvider.getText().toString().isEmpty()){
                        providersFileUri = null;
                    }
                }
            }
        });
        btnUploadProvider = (Button) getActivity().findViewById(R.id.fragment_setting_btn_upload_provider);
        btnUploadProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( providersFileUri!=null ) {
                    Set<String> providerSet = openFile(providersFileUri);
                    if( providerSet.size()==0 ){
                        Toast.makeText(getActivity(), "This is an invalid, empty or unformatted file.", Toast.LENGTH_LONG).show();
                        editProvider.clearFocus();
                    } else {
                        uploadServiceProvider(providerSet);
                        Toast.makeText(getActivity(), "Service providers are updated", Toast.LENGTH_SHORT).show();
                        editProvider.setText("");
                    }
                }
            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onStop() {
        super.onStop();
        editProvider.setText("");
        editCategory.setText("");
        providersFileUri = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case ActivityMain.REQUEST_CHOOSE_LCOAL_FILE:
                if (resultCode == RESULT_OK) {
                    providersFileUri = data.getData();
                    editProvider.setText(providersFileUri.getPath());
                }
                break;
        }
    }


    /***** Local methods *****/

    // upload new service providers

    private void chooseFile(){
        Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a file"), ActivityMain.REQUEST_CHOOSE_LCOAL_FILE);
        startActivityForResult(intent, ActivityMain.REQUEST_CHOOSE_LCOAL_FILE);
    }

    private Set<String> openFile(Uri uri){
        Set<String> set = new HashSet<>();
        File file = new File(uri.getPath());
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader inputreader = new InputStreamReader(inputStream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line = buffreader.readLine();
            if( line==null || !line.equalsIgnoreCase(getString(R.string.new_provider_file_headline)) ){
                return set;
            }
            line = buffreader.readLine();
            while(line!=null) {
                String name = line.trim();
                if( !name.isEmpty() ) {
                    set.add(name);
                }
                line = buffreader.readLine();
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    public void uploadServiceProvider(Set<String> providerSet){
        FirebaseHandler handler = new FirebaseHandler();
        for(String each : providerSet){
            FirebaseHandler.ServiceProvider provider = new FirebaseHandler.ServiceProvider();
            provider.setName(each);
            handler.addServiceProvider(provider, null);
        }
    }






}
