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
                headers.put("Cookie", "_jzqckmp=1; JSESSIONID=0A4620BAC7696E7A0EF7556104C310CD; access_token=140fd0bae10f4e8a8848e89fc43fb681; refresh_token=\"\"; wctk=140fd0bae10f4e8a8848e89fc43fb681; JSESSIONID=0A4620BAC7696E7A0EF7556104C310CD; _jzqa=1.3278237867087935500.1495126089.1495153819.1495157167.6; _jzqc=1; _jzqx=1.1495128784.1495157167.1.jzqsr=wacai%2Ecom|jzqct=/.-; Hm_lvt_0311e2d8c20d5428ab91f7c14ba1be08=1495126099; Hm_lpvt_0311e2d8c20d5428ab91f7c14ba1be08=1495157167; _jzqb=1.1.10.1495157167.1; _qzja=1.167105144.1495126089874.1495153818740.1495157166864.1495154583525.1495157166864..0.0.16.6; _qzjb=1.1495157166863.1.0.0.0; _qzjc=1; _qzjto=16.6.0");
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
                params.put("outgo.money", item.money);
                params.put("outgo.subtypeid", item.categoryCode); // 交通
//                params.put("outgo.tradeTgt.id", "5eef7cf7b9d04c16a6ce69250e08f676");// id of the provider name (unknown)
                params.put("outgo.tradeTgt.name", item.serviceProvider);
                params.put("outgo.accountid", "f6e2aa6ca8ad45978cfd1ef445047867"); // 在美国（账本）
                params.put("outgo.outgodate", item.datetime);
                params.put("outgo.projectid", "1"); // 日常
                params.put("outgo.members[0].memberid", "1"); //自己
                params.put("outgo.members[0].sharemoney", item.money);
                params.put("outgo.comment", item.note);
                params.put("outgo.reimburse", "0");
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(ctxt);
        requestQueue.add(stringRequest);
    }

    static class Item {
        public String money;
        public String serviceProvider;
        public String datetime;
        public String categoryCode;
        public String note;
    }



}
