/**
 * 
 */
package com.lenovo.vcs.medialoader.test;

import java.io.File;

import com.lenovo.vcs.medialoader.R;
import com.lenovo.vcs.medialoader.cache.naming.MediaFileNameGenerator;
import com.lenovo.vcs.medialoader.core.MediaLoader;
import com.lenovo.vcs.medialoader.core.MediaLoaderConfiguration;
import com.lenovo.vcs.medialoader.core.listener.MediaLoadingListener;
import com.lenovo.vcs.medialoader.core.listener.MediaLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.FailReason;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

/**
 * @author xubin
 *
 */
public class TestMediaLoadActivity extends Activity {
    
    private final static String LOG_TAG = TestMediaLoadActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blank);
        initMediaLoader();
        final VideoView videoView = (VideoView) this.findViewById(R.id.video_view);
        String url1 = "http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/MediaCoder/MediaCoder_test7_1m9s_XVID_VBR_161kbps_176x144_25fps_MPEG2.5Layer3_CBR_16kbps_Stereo_8000Hz.mp4";
        String url2 = "http://download.wavetlan.com/SVV/Media/HTTP/MP4/ConvertedFiles/MediaCoder/MediaCoder_test3_1m10s_MPEG4SP_VBR_516kbps_320x240_25fps_MPEG1Layer3_CBR_320kbps_Stereo_44100Hz.mp4";
        MediaLoader.getInstance().loadMedia(url1, new MediaLoadingListener() {

            @Override
            public void onLoadingStarted(String mediaUri) {
                // TODO Auto-generated method stub
                Log.d(LOG_TAG, "start load:" + mediaUri);
            }

            @Override
            public void onLoadingFailed(String mediaUri, FailReason failReason) {
                Log.d(LOG_TAG, "fail load:" + mediaUri);
                
            }

            @Override
            public void onLoadingComplete(String mediaUri, File localFile) {
                Log.d(LOG_TAG, "complete load:" + mediaUri );
                if (localFile != null) {
                    Log.d(LOG_TAG, " local path:" + localFile.getAbsolutePath());
                    //videoView.setVideoPath(localFile.getAbsolutePath());
                    //videoView.start();
                   //videoView.requestFocus();
                }
            }

            @Override
            public void onLoadingCancelled(String mediaUri) {
                // TODO Auto-generated method stub
                
            }
            
        }, new MediaLoadingProgressListener() {

            @Override
            public void onProgressUpdate(String imageUri, int current, int total) {
                
                Log.d(LOG_TAG, "on progress, current: " + current + " total:" + total);
            }
            
        });
        
        
        MediaLoader.getInstance().loadMedia(url2, new MediaLoadingListener() {

            @Override
            public void onLoadingStarted(String mediaUri) {
                // TODO Auto-generated method stub
                Log.d(LOG_TAG, "start load:" + mediaUri);
            }

            @Override
            public void onLoadingFailed(String mediaUri, FailReason failReason) {
                Log.d(LOG_TAG, "fail load:" + mediaUri);
                
            }

            @Override
            public void onLoadingComplete(String mediaUri, File localFile) {
                Log.d(LOG_TAG, "complete load:" + mediaUri );
                if (localFile != null) {
                    final String localFileCopy = localFile.getAbsolutePath();
                    Log.d(LOG_TAG, " local path:" + localFile.getAbsolutePath());
                    TestMediaLoadActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            videoView.setVideoPath(localFileCopy);
                            videoView.start();
                           //videoView.requestFocus();
                        }
                    });
                    
                }
            }

            @Override
            public void onLoadingCancelled(String mediaUri) {
                // TODO Auto-generated method stub
                
            }
            
        }, new MediaLoadingProgressListener() {

            @Override
            public void onProgressUpdate(String imageUri, int current, int total) {
                
                Log.d(LOG_TAG, "on progress, current: " + current + " total:" + total);
            }
            
        });
        
    }
    
    public void initMediaLoader() {
        MediaLoaderConfiguration config =  new MediaLoaderConfiguration.Builder(this)
                    .diskCacheFileCount(100).diskCacheSize(50 * 1000 * 1000)
                    .build();
        MediaLoader.getInstance().init(config);
        
    }
}
