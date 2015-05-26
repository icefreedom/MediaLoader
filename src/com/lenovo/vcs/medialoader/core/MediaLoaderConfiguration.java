/**
 * 
 */
package com.lenovo.vcs.medialoader.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import android.content.Context;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.utils.L;

/**
 * @author xubin
 *
 */
public final class MediaLoaderConfiguration {

    final Executor taskExecutor;
    
    final int threadPoolSize;
    final int threadPriority;
    final QueueProcessingType tasksProcessingType;
    
    final DiskCache diskCache;
    final ImageDownloader downloader;
    final ImageDownloader networkDeniedDownloader;
    final ImageDownloader slowNetworkDownloader;
    
    private MediaLoaderConfiguration(final Builder builder) {
        
        taskExecutor = builder.taskExecutor;

        threadPoolSize = builder.threadPoolSize;
        threadPriority = builder.threadPriority;
        tasksProcessingType = builder.tasksProcessingType;
        diskCache = builder.diskCache;

        downloader = builder.downloader;

        networkDeniedDownloader = new NetworkDeniedImageDownloader(downloader);
        slowNetworkDownloader = new SlowNetworkImageDownloader(downloader);

        L.writeDebugLogs(builder.writeLogs);
    }
    
    public static MediaLoaderConfiguration createDefault(Context context) {
        return new Builder(context).build();
    }
    
    public static class Builder {

        private static final String WARNING_OVERLAP_DISK_CACHE_PARAMS = "diskCache(), diskCacheSize() and diskCacheFileCount calls overlap each other";
        private static final String WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR = "diskCache() and diskCacheFileNameGenerator() calls overlap each other";
        private static final String WARNING_OVERLAP_EXECUTOR = "threadPoolSize(), threadPriority() and tasksProcessingOrder() calls "
                + "can overlap taskExecutor() and taskExecutorForCachedImages() calls.";
        
        
        /** {@value} */
        public static final int DEFAULT_THREAD_POOL_SIZE = 3;
        /** {@value} */
        public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2;
        /** {@value} */
        public static final QueueProcessingType DEFAULT_TASK_PROCESSING_TYPE = QueueProcessingType.FIFO;
        
        
        private Context context;
        
        private Executor taskExecutor = null;
        
        private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        private int threadPriority = DEFAULT_THREAD_PRIORITY;
        private QueueProcessingType tasksProcessingType = DEFAULT_TASK_PROCESSING_TYPE;
        
        private DiskCache diskCache = null;
        private FileNameGenerator diskCacheFileNameGenerator = null;
        private ImageDownloader downloader = null;
        
        private long diskCacheSize = 0;
        private int diskCacheFileCount = 0;
        
        private boolean writeLogs = false;
        
        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }
        
        public Builder taskExecutor(Executor executor) {
            if (threadPoolSize != DEFAULT_THREAD_POOL_SIZE || threadPriority != DEFAULT_THREAD_PRIORITY || tasksProcessingType != DEFAULT_TASK_PROCESSING_TYPE) {
                L.w(WARNING_OVERLAP_EXECUTOR);
            }

            this.taskExecutor = executor;
            return this;
        }
        
        public Builder threadPoolSize(int threadPoolSize) {
            if (taskExecutor != null) {
                L.w(WARNING_OVERLAP_EXECUTOR);
            }

            this.threadPoolSize = threadPoolSize;
            return this;
        }
        
        public Builder threadPriority(int threadPriority) {
            if (taskExecutor != null) {
                L.w(WARNING_OVERLAP_EXECUTOR);
            }

            if (threadPriority < Thread.MIN_PRIORITY) {
                this.threadPriority = Thread.MIN_PRIORITY;
            } else {
                if (threadPriority > Thread.MAX_PRIORITY) {
                    this.threadPriority = Thread.MAX_PRIORITY;
                } else {
                    this.threadPriority = threadPriority;
                }
            }
            return this;
        }
        
        public Builder tasksProcessingOrder(QueueProcessingType tasksProcessingType) {
            if (taskExecutor != null) {
                L.w(WARNING_OVERLAP_EXECUTOR);
            }

            this.tasksProcessingType = tasksProcessingType;
            return this;
        }
        
        public Builder diskCacheSize(int maxCacheSize) {
            if (maxCacheSize <= 0) throw new IllegalArgumentException("maxCacheSize must be a positive number");

            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS);
            }

            this.diskCacheSize = maxCacheSize;
            return this;
        }
        
        public Builder diskCacheFileCount(int maxFileCount) {
            if (maxFileCount <= 0) throw new IllegalArgumentException("maxFileCount must be a positive number");

            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS);
            }

            this.diskCacheFileCount = maxFileCount;
            return this;
        }
        
        public Builder diskCacheFileNameGenerator(FileNameGenerator fileNameGenerator) {
            if (diskCache != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR);
            }

            this.diskCacheFileNameGenerator = fileNameGenerator;
            return this;
        }
        
        public Builder diskCache(DiskCache diskCache) {
            if (diskCacheSize > 0 || diskCacheFileCount > 0) {
                L.w(WARNING_OVERLAP_DISK_CACHE_PARAMS);
            }
            if (diskCacheFileNameGenerator != null) {
                L.w(WARNING_OVERLAP_DISK_CACHE_NAME_GENERATOR);
            }

            this.diskCache = diskCache;
            return this;
        }
        
        public Builder imageDownloader(ImageDownloader imageDownloader) {
            this.downloader = imageDownloader;
            return this;
        }
        
        public Builder writeDebugLogs() {
            this.writeLogs = true;
            return this;
        }
        
        
        private void initEmptyFieldsWithDefaultValues() {
            if (taskExecutor == null) {
                taskExecutor = DefaultConfigurationFactory
                        .createExecutor(threadPoolSize, threadPriority, tasksProcessingType);
            } 
            
            if (diskCache == null) {
                if (diskCacheFileNameGenerator == null) {
                    diskCacheFileNameGenerator = DefaultConfigurationFactory.createFileNameGenerator();
                }
                diskCache = DefaultConfigurationFactory
                        .createDiskCache(context, diskCacheFileNameGenerator, diskCacheSize, diskCacheFileCount);
            }
            
            if (downloader == null) {
                downloader = new BaseImageDownloader(context, 5 * 1000, 5 * 60 * 1000);
            }
            
        }
        
        public MediaLoaderConfiguration build() {
            initEmptyFieldsWithDefaultValues();
            return new MediaLoaderConfiguration(this);
        }

    }
    
    /**
     * Decorator. Prevents downloads from network (throws {@link IllegalStateException exception}).<br />
     * In most cases this downloader shouldn't be used directly.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.0
     */
    private static class NetworkDeniedImageDownloader implements ImageDownloader {

        private final ImageDownloader wrappedDownloader;

        public NetworkDeniedImageDownloader(ImageDownloader wrappedDownloader) {
            this.wrappedDownloader = wrappedDownloader;
        }

        @Override
        public InputStream getStream(String imageUri, Object extra) throws IOException {
            switch (Scheme.ofUri(imageUri)) {
                case HTTP:
                case HTTPS:
                    throw new IllegalStateException();
                default:
                    return wrappedDownloader.getStream(imageUri, extra);
            }
        }
    }
    
    
    /**
     * Decorator. Handles <a href="http://code.google.com/p/android/issues/detail?id=6066">this problem</a> on slow networks
     * using {@link com.nostra13.universalimageloader.core.assist.FlushedInputStream}.
     *
     * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
     * @since 1.8.1
     */
    private static class SlowNetworkImageDownloader implements ImageDownloader {

        private final ImageDownloader wrappedDownloader;

        public SlowNetworkImageDownloader(ImageDownloader wrappedDownloader) {
            this.wrappedDownloader = wrappedDownloader;
        }

        @Override
        public InputStream getStream(String imageUri, Object extra) throws IOException {
            InputStream imageStream = wrappedDownloader.getStream(imageUri, extra);
            switch (Scheme.ofUri(imageUri)) {
                case HTTP:
                case HTTPS:
                    return new FlushedInputStream(imageStream);
                default:
                    return imageStream;
            }
        }
    }

    
}
