/**
 * 
 */
package com.lenovo.vcs.medialoader.core;

import java.util.concurrent.locks.ReentrantLock;

import com.lenovo.vcs.medialoader.core.listener.MediaLoadingListener;
import com.lenovo.vcs.medialoader.core.listener.MediaLoadingProgressListener;

/**
 * @author xubin
 *
 */
public class MediaLoadingInfo {
    final String uri;
    final ReentrantLock loadFromUriLock;
    final MediaLoadingListener mediaLoadingListener;
    final MediaLoadingProgressListener mediaLoadingProgressListener;
    
    public MediaLoadingInfo(String uri, ReentrantLock loadFromUriLock, 
            MediaLoadingListener mediaLoadingListener, MediaLoadingProgressListener mediaLoadingProgressListener) {
        this.uri = uri;
        this.loadFromUriLock = loadFromUriLock;
        this.mediaLoadingListener = mediaLoadingListener;
        this.mediaLoadingProgressListener = mediaLoadingProgressListener;
    }
}
