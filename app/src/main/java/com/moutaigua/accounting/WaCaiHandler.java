package com.moutaigua.accounting;

import android.content.Context;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mou on 1/28/17.
 */

public class WaCaiHandler {

    private final String LOG_TAG = "Wacai";

    private Context ctxt;
    private RequestQueue mRequestQueue;


    public WaCaiHandler(Context context) {
        ctxt = context;
        mRequestQueue = getRequestQueue();
    }

    private RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(ctxt.getApplicationContext());
        }
        return mRequestQueue;
    }


    public interface AsyncCallBack {
        void callback();
    }



    /**** WaCai Related ****/

    public void addItem(final Item item, final AsyncCallBack callBack) {
        String REQUEST_URL = "https://www.wacai.com/biz/outgo_save.action";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REQUEST_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(LOG_TAG, response.toString());
                        callBack.callback();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  headers = new HashMap<>();
                headers.put("Accept","*/*");
                headers.put("Accept-Encoding", "gzip, deflate, br");
                headers.put("Accept-Language", "zh-CN,zh;q=0.8");
                headers.put("Connection", "keep-alive");
//                headers.put("Content-Length", "0");
                headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                headers.put("Cookie", "_jzqx=1.1484871998.1484871998.1.jzqsr=google%2Ecom|jzqct=/.-; _jzqckmp=1; JSESSIONID=D3BA8F56F86256FF24ED6456B91583F3; Hm_lvt_0311e2d8c20d5428ab91f7c14ba1be08=1484871998; Hm_lpvt_0311e2d8c20d5428ab91f7c14ba1be08=1485844853; wctk=8b43f1f6675245429891e64b2e04c04a; _qzja=1.1688824358.1484871998369.1485658742416.1485840905320.1485844852562.1485845919193..0.0.19.3; _qzjb=1.1485840905320.15.0.0.0; _qzjc=1; _qzjto=11.0.0; _jzqa=1.2373817877101609500.1484871998.1485658743.1485840903.3; _jzqc=1; _jzqb=1.16.10.1485840903.1");
                headers.put("Host", "www.wacai.com");
                headers.put("Origin", "http://www.wacai.com");
                headers.put("Referer", "https://www.wacai.com/user/user.action");
                headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
                headers.put("X-Requested-With", "XMLHttpRequest");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("outgo.bookId", "e28832ee3e474da9a4e69c2a7a2f4c31"); //留学账本
                params.put("outgo.money", String.valueOf(item.money));
                params.put("outgo.subtypeid", "0c03adb739884765b3d1a4c271eb283a"); // 交通
//                params.put("outgo.tradeTgt.id", "5eef7cf7b9d04c16a6ce69250e08f676");//洗车
                params.put("outgo.tradeTgt.name", item.serviceProvider);
                params.put("outgo.accountid", "f6e2aa6ca8ad45978cfd1ef445047867"); // 在美国（账本）
                params.put("outgo.outgodate", item.datetime);
                params.put("outgo.projectid", "1"); // 日常
                params.put("outgo.members[0].memberid", "1"); //自己
                params.put("outgo.members[0].sharemoney", String.valueOf(item.money));
                params.put("outgo.comment", item.note);
                params.put("outgo.reimburse", "0");
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(ctxt);
        requestQueue.add(stringRequest);
    }


    public String getFormattedDate(String dateStr){
        String[] strs = dateStr.split("/");
        return strs[2] + "-" + strs[0] + "-" + strs[1];
    }

    public String getFormattedTime(String timeStr){
        String[] strs = timeStr.split(":");
        return strs[0] + ":" + strs[1];
    }

    static class Item {
        public String serviceProvider;
        public String datetime;
        public float money;
        public String note;
    }



}
