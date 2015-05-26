/**
 * 
 */
package com.lenovo.vcs.medialoader.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;

import com.lenovo.vcs.medialoader.core.listener.MediaLoadingListener;
import com.lenovo.vcs.medialoader.core.listener.MediaLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FailReason.FailType;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.utils.IoUtils;
import com.nostra13.universalimageloader.utils.IoUtils.CopyListener;
import com.nostra13.universalimageloader.utils.L;

/**
 * @author xubin
 *
 */
public final class LoadMediaTask implements Runnable, CopyListener {
    
    private final MediaLoaderEngine engine;
    private final Handler handler;
    private final MediaLoaderConfiguration configuration;
    
    private final ImageDownloader downloader;
    private final ImageDownloader networkDeniedDownloader;
    private final ImageDownloader slowNetworkDownloader;
    
    private final MediaLoadingInfo mediaLoadingInfo;
    
    final String uri;
    final MediaLoadingListener listener;
    final MediaLoadingProgressListener progressListener;
    
    private LoadedFrom loadedFrom = LoadedFrom.NETWORK;
    
    public LoadMediaTask(MediaLoaderEngine engine, MediaLoadingInfo mediaLoadingInfo, Handler handler) {
        this.engine = engine;
        this.handler = handler;
        this.mediaLoadingInfo = mediaLoadingInfo;
        
        this.configuration = engine.configuration;
        this.downloader = configuration.downloader;
        this.networkDeniedDownloader = configuration.networkDeniedDownloader;
        this.slowNetworkDownloader = configuration.slowNetworkDownloader;
        
        this.uri = mediaLoadingInfo.uri;
        this.listener = mediaLoadingInfo.mediaLoadingListener;
        this.progressListener = mediaLoadingInfo.mediaLoadingProgressListener;
    }


    @Override
    public boolean onBytesCopied(int current, int total) {
        return fireProgressEvent(current, total);
    }
    private boolean fireProgressEvent(final int current, final int total) {
        if (isTaskInterrupted()) return false;
        if (progressListener != null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    progressListener.onProgressUpdate(uri,  current, total);
                }
            };
            runTask(r, false, handler, engine);
        }
        return true;
    }

    @Override
    public void run() {
        if (waitIfPaused()) return;
        
        ReentrantLock loadFromUriLock = mediaLoadingInfo.loadFromUriLock;
        L.d("start media load task: %s", uri);
        if (loadFromUriLock.isLocked()) {
            L.d("uri locked: %s", uri);
        }
        
        loadFromUriLock.lock();
        try {
            tryLoadMediaFile();
            checkTaskInterrupted();
        } catch (TaskCancelledException e) {
            fireCancelEvent();
            return;
        } finally {
            loadFromUriLock.unlock();
        }
    }
    private File tryLoadMediaFile() throws TaskCancelledException{
        File mediaFile = null;
        try {
        mediaFile = configuration.diskCache.get(uri);
        
        if (mediaFile != null && mediaFile.exists()) {
            L.d("load media file from disk,  local path: %s, uri: %s", mediaFile.getAbsoluteFile(), uri);
            loadedFrom = LoadedFrom.DISC_CACHE;
        } else {
            L.d("try to load from network: %s", uri);
            loadedFrom = LoadedFrom.NETWORK;
            boolean loaded = downloadMedia();
            
            if (loaded) {
                mediaFile = configuration.diskCache.get(uri); 
            }
        }
        } catch (IllegalStateException e) {
            fireFailEvent(FailType.NETWORK_DENIED, null);
        } catch (IOException e) {
            L.e(e);
            fireFailEvent(FailType.IO_ERROR, e);
        } catch (OutOfMemoryError e) {
            L.e(e);
            fireFailEvent(FailType.OUT_OF_MEMORY, e);
        } catch (Throwable e) {
            L.e(e);
            fireFailEvent(FailType.UNKNOWN, e);
        } finally {
            listener.onLoadingComplete(uri, mediaFile);
        }

        
        return mediaFile;
    }
    private boolean waitIfPaused() {
        AtomicBoolean pause = engine.getPause();
        if (pause.get()) {
            synchronized (engine.getPauseLock()) {
                if (pause.get()) {

                    try {
                        engine.getPauseLock().wait();
                    } catch (InterruptedException e) {

                        return true;
                    }

                }
            }
        }
        return false;
    }
    
    private boolean downloadMedia() throws IOException {
        InputStream is = getDownloader().getStream(uri, null);
        if (is == null) {
            L.e("error: fail to download media: %s" , uri);
            return false;
        } else {
            try {
                return configuration.diskCache.save(uri, is, this);
            } finally {
                IoUtils.closeSilently(is);
            }
        }
    }
    
    private ImageDownloader getDownloader() {
        ImageDownloader d;
        if (engine.isNetworkDenied()) {
            d = networkDeniedDownloader;
        } else if (engine.isSlowNetwork()) {
            d = slowNetworkDownloader;
        } else {
            d = downloader;
        }
        return d;
    }
    
    class TaskCancelledException extends Exception {
    }
    private void fireCancelEvent() {
        if (isTaskInterrupted()) return;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                listener.onLoadingCancelled(uri);
            }
        };
        runTask(r, false, handler, engine);
    }
    
    private void fireFailEvent(final FailType failType, final Throwable failCause) {
        if (isTaskInterrupted()) return;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                
                listener.onLoadingFailed(uri,  new FailReason(failType, failCause));
            }
        };
        runTask(r, false, handler, engine);
    }
    
    private boolean isTaskInterrupted() {
        if (Thread.interrupted()) {
            return true;
        }
        return false;
    }
    
    String getLoadingUri() {
        return uri;
    }

    static void runTask(Runnable r, boolean sync, Handler handler, MediaLoaderEngine engine) {
        if (sync) {
            r.run();
        } else if (handler == null) {
            engine.fireCallback(r);
        } else {
            handler.post(r);
        }
    }
    
    private void checkTaskInterrupted() throws TaskCancelledException {
        if (isTaskInterrupted()) {
            throw new TaskCancelledException();
        }
    }

}
