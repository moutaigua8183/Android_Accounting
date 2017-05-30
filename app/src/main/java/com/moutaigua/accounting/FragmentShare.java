package com.moutaigua.accounting;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mou on 5/27/17.
 */

public class FragmentShare extends Fragment {

    private final String LOG_TAG = "Fragment_Share";

    private FirebaseHandler firebaseHandler;
    private ListView listView;
    private ShareListAdapter adapter;


    public FragmentShare() {
        firebaseHandler = new FirebaseHandler();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView = (ListView) getActivity().findViewById(R.id.fragment_share_listview);
        adapter = new ShareListAdapter(getActivity(), R.layout.share_recyclerview_each_item, firebaseHandler.getShareList());
        listView.setAdapter(adapter);


    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseHandler.SingleCallback callback = new FirebaseHandler.SingleCallback() {
            @Override
            public void onResult() {
                adapter.notifyDataSetChanged();
            }
        };
        firebaseHandler.syncShare(callback);
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseHandler.stopShare();
    }


    public class ShareListAdapter extends ArrayAdapter {

        private Context ctxt;
        private ArrayList<Transaction> data;
        private int mLayoutResourceId;


        public ShareListAdapter(@NonNull Context context, @LayoutRes int resource, ArrayList<Transaction> list) {
            super(context, resource);
            this.data = list;
            this.ctxt = context;
            this.mLayoutResourceId = resource;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Transaction getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private class ViewHolder {
            public TextView txtViewTime;
            public TextView txtViewCategoryName;
            public TextView txtViewType;
            public TextView txtViewCity;
            public TextView txtViewSeparate;
            public TextView txtViewMoney;
            public TextView txtViewProviderName;
            public TextView txtViewNote;
            public ImageButton imgBtnDelete;

            public ViewHolder(View view) {
                this.txtViewTime = (TextView) view.findViewById(R.id.share_listview_each_item_time);
                this.txtViewCategoryName = (TextView) view.findViewById(R.id.share_listview_each_item_category);
                this.txtViewType = (TextView) view.findViewById(R.id.share_listview_each_item_type);
                this.txtViewCity = (TextView) view.findViewById(R.id.share_listview_each_item_city);
                this.txtViewSeparate = (TextView) view.findViewById(R.id.share_listview_each_item_seperate);
                this.txtViewMoney = (TextView) view.findViewById(R.id.share_listview_each_item_money);
                this.txtViewProviderName = (TextView) view.findViewById(R.id.share_listview_each_item_provider);
                this.txtViewNote = (TextView) view.findViewById(R.id.share_listview_each_item_note);
                this.imgBtnDelete = (ImageButton) view.findViewById(R.id.share_listview_each_item_delete);
            }
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = ((Activity) ctxt).getLayoutInflater();
                view = inflater.inflate(mLayoutResourceId, viewGroup, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }
            else {
                holder = (ViewHolder) view.getTag();
            }
            Transaction trans = getItem(i);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd EEE HH:mm");
            String txtTime = sdf.format(new Date(trans.getLongTime()));
            holder.txtViewTime.setText(txtTime);
            holder.txtViewCategoryName.setText(trans.getCategory().getName());
            holder.txtViewType.setText(trans.getType());
            holder.txtViewCity.setText(trans.getCity());
            holder.txtViewSeparate.setText(String.valueOf(trans.getSeperate()) + " People");
            holder.txtViewMoney.setText("$"+trans.getMoney());
            holder.txtViewProviderName.setText(trans.getProviderName());
            if( trans.getNote().isEmpty() ) {
                holder.txtViewNote.setVisibility(View.GONE);
            } else {
                holder.txtViewNote.setText(trans.getNote());
            }
            holder.imgBtnDelete.setTag(i);
            holder.imgBtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.myDialogStyle));
                    mBuilder.setMessage(R.string.fragment_share_delete_alert)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    int viewIndex = (int) view.getTag();
                                    firebaseHandler.deleteShare(data.get(viewIndex));
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
            return view;
        }
    }



}
