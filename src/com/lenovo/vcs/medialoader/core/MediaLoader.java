/**
 * 
 */
package com.lenovo.vcs.medialoader.core;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.lenovo.vcs.medialoader.core.listener.MediaLoadingListener;
import com.lenovo.vcs.medialoader.core.listener.MediaLoadingProgressListener;
import com.nostra13.universalimageloader.utils.L;

/**
 * @author xubin
 *
 */
public class MediaLoader {
    public static final String TAG = MediaLoader.class.getSimpleName();
    
    private MediaLoaderConfiguration  configuration;
    private MediaLoaderEngine engine;
    
    private volatile static MediaLoader instance;
    
    private Handler handler;
    
    public static MediaLoader getInstance() {
        if (instance == null) {
            synchronized (MediaLoader.class) {
                if (instance == null) {
                    instance = new MediaLoader();
                }
            }
            
        }
        return instance;
    }
    
    protected MediaLoader() {
        
    }
    
    public synchronized void init(MediaLoaderConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("MeidaLoaderConfiguration can't be initialized with null");
        }
        if (this.configuration == null) {
            L.d("init config");
            engine = new MediaLoaderEngine(configuration);
            this.configuration = configuration;
        } else {
            L.w("dupicate to initialize config");
        }
    }
    
    public void loadMedia(String uri, MediaLoadingListener listener, MediaLoadingProgressListener progressListener) {
        checkConfiguration();
        
        if (TextUtils.isEmpty(uri)) {
            listener.onLoadingStarted(uri);
            listener.onLoadingComplete(uri, null);
            return;
        }
        
        listener.onLoadingStarted(uri);
        
        MediaLoadingInfo mediaLoadingInfo = new MediaLoadingInfo(uri, engine.getLockForUri(uri), listener, progressListener);
        LoadMediaTask task = new LoadMediaTask(engine, mediaLoadingInfo, defineHandler());
        engine.submit(task);
    }
    
    private void checkConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("MediaLoader not initialized");
        }
    }
    
    private Handler defineHandler() {
        if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
            handler = new Handler();
        }
        return handler;
    }
}
