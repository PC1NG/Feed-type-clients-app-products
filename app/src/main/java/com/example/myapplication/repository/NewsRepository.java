package com.example.myapplication.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.myapplication.cache.NewsCacheManager;
import com.example.myapplication.model.NewsBean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 新闻数据仓库
 * 统一管理数据获取：网络优先，失败时使用本地缓存
 */
public class NewsRepository {

    private final Context context;
    private final NewsCacheManager cacheManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    // 模拟网络请求失败的概率（用于测试缓存功能）
    private static final float NETWORK_FAIL_RATE = 0.3f;

    public interface DataCallback {
        void onSuccess(List<NewsBean> data, boolean fromCache);
        void onError(String message);
    }

    public NewsRepository(Context context) {
        this.context = context;
        this.cacheManager = new NewsCacheManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    /**
     * 获取新闻数据
     * 策略：模拟网络请求，成功则更新缓存，失败则使用缓存
     */
    public void fetchNews(String fileName, DataCallback callback) {
        executor.execute(() -> {
            // 模拟网络延迟
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 模拟网络请求（随机失败）
            boolean networkSuccess = new Random().nextFloat() > NETWORK_FAIL_RATE;

            if (networkSuccess) {
                // 网络成功：从assets读取（模拟网络返回）
                List<NewsBean> data = loadFromAssets(fileName);
                if (data != null) {
                    // 保存到缓存
                    cacheManager.saveToCache(fileName, data);
                    postSuccess(callback, data, false);
                } else {
                    // 读取失败，尝试缓存
                    tryLoadFromCache(fileName, callback, "数据加载失败");
                }
            } else {
                // 网络失败：使用缓存
                tryLoadFromCache(fileName, callback, "网络请求失败");
            }
        });
    }

    /**
     * 强制从缓存加载（离线模式）
     */
    public void fetchFromCacheOnly(String fileName, DataCallback callback) {
        executor.execute(() -> {
            List<NewsBean> cached = cacheManager.getFromCacheIgnoreExpire(fileName);
            if (cached != null && !cached.isEmpty()) {
                postSuccess(callback, cached, true);
            } else {
                postError(callback, "无缓存数据");
            }
        });
    }

    /**
     * 强制刷新（忽略缓存）
     */
    public void forceRefresh(String fileName, DataCallback callback) {
        executor.execute(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<NewsBean> data = loadFromAssets(fileName);
            if (data != null) {
                cacheManager.saveToCache(fileName, data);
                postSuccess(callback, data, false);
            } else {
                postError(callback, "刷新失败");
            }
        });
    }

    /**
     * 尝试从缓存加载
     */
    private void tryLoadFromCache(String fileName, DataCallback callback, String networkError) {
        List<NewsBean> cached = cacheManager.getFromCacheIgnoreExpire(fileName);
        if (cached != null && !cached.isEmpty()) {
            postSuccess(callback, cached, true);
        } else {
            postError(callback, networkError + "，且无本地缓存");
        }
    }

    /**
     * 从Assets加载JSON（模拟网络请求返回）
     */
    private List<NewsBean> loadFromAssets(String fileName) {
        try {
            InputStreamReader isr = new InputStreamReader(
                    context.getAssets().open(fileName), "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            Type listType = new TypeToken<List<NewsBean>>() {}.getType();
            return gson.fromJson(sb.toString(), listType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void postSuccess(DataCallback callback, List<NewsBean> data, boolean fromCache) {
        mainHandler.post(() -> callback.onSuccess(data, fromCache));
    }

    private void postError(DataCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * 检查是否有缓存
     */
    public boolean hasCache(String fileName) {
        return cacheManager.hasCache(fileName);
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cacheManager.clearAllCache();
    }
}
