/**
 * 
 */
package com.lenovo.vcs.medialoader.core.listener;

/**
 * @author xubin
 *
 */
public interface MediaLoadingProgressListener {
    void onProgressUpdate(String imageUri, int current, int total);
}
