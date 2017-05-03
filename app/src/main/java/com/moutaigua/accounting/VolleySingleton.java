package com.moutaigua.accounting;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by mou on 2/11/17.
 */

public class VolleySingleton {


    private static VolleySingleton myInstance;
    private Context ctxt;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;


    private VolleySingleton(Context context) {
        ctxt = context;
        mRequestQueue = Volley.newRequestQueue(ctxt.getApplicationContext());
        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if( myInstance==null ){
            myInstance = new VolleySingleton(context);
        }
        return myInstance;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }




}
