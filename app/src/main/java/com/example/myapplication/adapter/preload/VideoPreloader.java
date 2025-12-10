package com.example.myapplication.adapter.preload;

import android.content.Context;
import android.util.LruCache;

import com.bumptech.glide.Glide;
import com.example.myapplication.model.NewsBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 视频预加载器
 * 提前加载视频封面图和模拟视频数据预缓存
 */
public class VideoPreloader {

    private final Context context;
    private final ExecutorService executor;
    
    // 已预加载的视频位置缓存
    private final LruCache<Integer, Boolean> preloadedCache = new LruCache<>(30);
    
    // 正在预加载的位置（防止重复预加载）
    private final Set<Integer> loadingSet = new HashSet<>();
    
    // 预加载数量
    private static final int PRELOAD_COUNT = 3;

    public interface PreloadCallback {
        void onPreloadComplete(int position);
    }

    public VideoPreloader(Context context) {
        this.context = context;
        this.executor = Executors.newFixedThreadPool(2);
    }

    /**
     * 预加载即将显示的视频
     * @param dataList 数据列表
     * @param lastVisiblePosition 最后可见位置
     */
    public void preloadVideos(List<NewsBean> dataList, int lastVisiblePosition) {
        preloadVideos(dataList, lastVisiblePosition, null);
    }

    /**
     * 预加载即将显示的视频（带回调）
     */
    public void preloadVideos(List<NewsBean> dataList, int lastVisiblePosition, PreloadCallback callback) {
        int startPos = lastVisiblePosition + 1;
        int endPos = Math.min(startPos + PRELOAD_COUNT, dataList.size());

        for (int i = startPos; i < endPos; i++) {
            final int position = i;
            NewsBean item = dataList.get(i);
            
            // 只预加载视频类型
            if (item.type != NewsBean.TYPE_VIDEO) continue;
            
            // 检查是否已预加载或正在加载
            if (isPreloaded(position) || isLoading(position)) continue;

            // 标记为正在加载
            markLoading(position);

            // 异步预加载
            executor.execute(() -> {
                try {
                    // 1. 预加载视频封面图
                    preloadCoverImage(item);
                    
                    // 2. 模拟视频数据预缓存（实际项目中这里可以预下载视频首帧或部分数据）
                    simulateVideoDataPreload(item);
                    
                    // 标记为已预加载
                    markPreloaded(position);
                    
                    // 回调通知
                    if (callback != null) {
                        callback.onPreloadComplete(position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    removeLoading(position);
                }
            });
        }
    }

    /**
     * 预加载视频封面图
     */
    private void preloadCoverImage(NewsBean item) {
        if (item.images != null && !item.images.isEmpty()) {
            String coverUrl = item.images.get(0);
            String imagePath = coverUrl.startsWith("http") ? coverUrl : "file:///android_asset/images/" + coverUrl;
            
            // 使用Glide预加载封面图到内存
            try {
                Glide.with(context)
                        .load(imagePath)
                        .preload();
            } catch (Exception e) {
                // 忽略预加载失败
            }
        }
    }

    /**
     * 模拟视频数据预缓存
     * 实际项目中可以：
     * 1. 预下载视频首帧
     * 2. 预缓存视频前几秒数据
     * 3. 预初始化播放器
     */
    private void simulateVideoDataPreload(NewsBean item) {
        try {
            // 模拟预加载耗时（实际项目中这里是真实的视频数据预加载）
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查是否已预加载
     */
    public boolean isPreloaded(int position) {
        return preloadedCache.get(position) != null;
    }

    /**
     * 检查是否正在加载
     */
    private synchronized boolean isLoading(int position) {
        return loadingSet.contains(position);
    }

    /**
     * 标记为正在加载
     */
    private synchronized void markLoading(int position) {
        loadingSet.add(position);
    }

    /**
     * 移除加载标记
     */
    private synchronized void removeLoading(int position) {
        loadingSet.remove(position);
    }

    /**
     * 标记为已预加载
     */
    private void markPreloaded(int position) {
        preloadedCache.put(position, true);
    }

    /**
     * 清除预加载缓存
     */
    public void clearCache() {
        preloadedCache.evictAll();
        synchronized (this) {
            loadingSet.clear();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        clearCache();
        executor.shutdown();
    }
}
