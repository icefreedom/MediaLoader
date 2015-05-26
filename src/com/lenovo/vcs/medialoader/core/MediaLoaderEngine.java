/**
 * 
 */
package com.lenovo.vcs.medialoader.core;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;

/**
 * @author xubin
 *
 */
class MediaLoaderEngine {
    final MediaLoaderConfiguration configuration;
    private Executor taskExecutor;
    private Executor taskDistributor;
    
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean networkDenied = new AtomicBoolean(false);
    private final AtomicBoolean slowNetwork = new AtomicBoolean(false);

    private final Object pauseLock = new Object();
    
    private final Map<String, ReentrantLock> uriLocks = new WeakHashMap<String, ReentrantLock>();
    
    MediaLoaderEngine(MediaLoaderConfiguration configuration) {
        this.configuration = configuration;
        this.taskExecutor = configuration.taskExecutor;
        
        this.taskDistributor = DefaultConfigurationFactory.createTaskDistributor();
    }
    
    void submit(final LoadMediaTask task) {
        taskDistributor.execute(new Runnable() {
            @Override
            public void run() {
                initExecutorsIfNeed();
                taskExecutor.execute(task);
            }
        });
    }
    
    private void initExecutorsIfNeed() {
        if (((ExecutorService) taskExecutor).isShutdown()) {
            taskExecutor = createTaskExecutor();
        }

    }

    private Executor createTaskExecutor() {
        return DefaultConfigurationFactory
                .createExecutor(configuration.threadPoolSize, configuration.threadPriority,
                configuration.tasksProcessingType);
    }
    
    void pause() {
        paused.set(true);
    }
    
    void resume() {
        paused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }
    
    void stop() {
        if (!((ExecutorService) taskExecutor).isShutdown()) {
            ((ExecutorService) taskExecutor).shutdownNow();
        }
        

        
        uriLocks.clear();
    }
    
    void fireCallback(Runnable r) {
        taskDistributor.execute(r);
    }

    ReentrantLock getLockForUri(String uri) {
        ReentrantLock lock = uriLocks.get(uri);
        if (lock == null) {
            lock = new ReentrantLock();
            uriLocks.put(uri, lock);
        }
        return lock;
    }

    AtomicBoolean getPause() {
        return paused;
    }

    Object getPauseLock() {
        return pauseLock;
    }

    boolean isNetworkDenied() {
        return networkDenied.get();
    }

    boolean isSlowNetwork() {
        return slowNetwork.get();
    }

}
