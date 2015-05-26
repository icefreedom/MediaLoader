/**
 * 
 */
package com.lenovo.vcs.medialoader.core.listener;

import java.io.File;

import com.nostra13.universalimageloader.core.assist.FailReason;

/**
 * @author xubin
 *
 */
public interface MediaLoadingListener {
    void onLoadingStarted(String mediaUri);
    void onLoadingFailed(String mediaUri, FailReason failReason);
    void onLoadingComplete(String mediaUri, File localFile);
    void onLoadingCancelled(String mediaUri);
}
