package com.example.myapplication.cache;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.model.NewsBean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 新闻数据缓存管理器
 * 使用 SharedPreferences 存储 JSON 数据
 * 网络请求失败时可从缓存读取
 */
public class NewsCacheManager {

    private static final String PREF_NAME = "news_cache";
    private static final String KEY_PREFIX = "cache_";
    private static final String KEY_TIMESTAMP_PREFIX = "timestamp_";
    
    // 缓存有效期：1小时
    private static final long CACHE_EXPIRE_TIME = 60 * 60 * 1000;

    private final SharedPreferences prefs;
    private final Gson gson;

    public NewsCacheManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * 保存数据到缓存
     * @param key 缓存key（如频道名）
     * @param data 新闻数据列表
     */
    public void saveToCache(String key, List<NewsBean> data) {
        if (data == null || data.isEmpty()) return;
        
        String json = gson.toJson(data);
        prefs.edit()
                .putString(KEY_PREFIX + key, json)
                .putLong(KEY_TIMESTAMP_PREFIX + key, System.currentTimeMillis())
                .apply();
    }

    /**
     * 从缓存读取数据
     * @param key 缓存key
     * @return 缓存的数据，如果没有或已过期返回null
     */
    public List<NewsBean> getFromCache(String key) {
        String json = prefs.getString(KEY_PREFIX + key, null);
        if (json == null) return null;

        try {
            Type listType = new TypeToken<List<NewsBean>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从缓存读取数据（忽略过期时间，用于网络失败时兜底）
     */
    public List<NewsBean> getFromCacheIgnoreExpire(String key) {
        String json = prefs.getString(KEY_PREFIX + key, null);
        if (json == null) return null;

        try {
            Type listType = new TypeToken<List<NewsBean>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查缓存是否有效（未过期）
     */
    public boolean isCacheValid(String key) {
        long timestamp = prefs.getLong(KEY_TIMESTAMP_PREFIX + key, 0);
        return System.currentTimeMillis() - timestamp < CACHE_EXPIRE_TIME;
    }

    /**
     * 检查是否有缓存（不管是否过期）
     */
    public boolean hasCache(String key) {
        return prefs.contains(KEY_PREFIX + key);
    }

    /**
     * 清除指定key的缓存
     */
    public void clearCache(String key) {
        prefs.edit()
                .remove(KEY_PREFIX + key)
                .remove(KEY_TIMESTAMP_PREFIX + key)
                .apply();
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        prefs.edit().clear().apply();
    }

    /**
     * 获取缓存时间戳
     */
    public long getCacheTimestamp(String key) {
        return prefs.getLong(KEY_TIMESTAMP_PREFIX + key, 0);
    }
}
