package com.example.myapplication.adapter.preload;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import com.example.myapplication.R;
import java.util.LinkedList;
import java.util.Queue;

/**
 * XML布局异步预加载器
 * 在后台线程预加载布局，减少主线程压力
 */
public class LayoutPreloader {

    private final Context context;
    private final AsyncLayoutInflater asyncInflater;
    
    // 预加载的View缓存池，按layoutId分类
    private final SparseArray<Queue<View>> viewCache = new SparseArray<>();
    
    // 需要预加载的布局
    private static final int[] PRELOAD_LAYOUTS = {
        R.layout.item_news_text,
        R.layout.item_news_three_images,
        R.layout.item_news_grid,
        R.layout.item_news_video,
        R.layout.item_news_video_grid
    };
    
    // 每种布局预加载的数量
    private static final int PRELOAD_COUNT = 2;

    public LayoutPreloader(Context context) {
        this.context = context;
        this.asyncInflater = new AsyncLayoutInflater(context);
        
        // 初始化缓存队列
        for (int layoutId : PRELOAD_LAYOUTS) {
            viewCache.put(layoutId, new LinkedList<>());
        }
    }

    /**
     * 开始异步预加载所有布局
     */
    public void startPreload(ViewGroup parent) {
        for (int layoutId : PRELOAD_LAYOUTS) {
            for (int i = 0; i < PRELOAD_COUNT; i++) {
                preloadLayout(layoutId, parent);
            }
        }
    }

    /**
     * 异步预加载单个布局
     */
    private void preloadLayout(int layoutId, ViewGroup parent) {
        asyncInflater.inflate(layoutId, parent, (view, resid, p) -> {
            Queue<View> queue = viewCache.get(resid);
            if (queue != null && queue.size() < PRELOAD_COUNT * 2) {
                queue.offer(view);
            }
        });
    }

    /**
     * 获取预加载的View，如果没有则同步创建
     */
    public View getPreloadedView(int layoutId, ViewGroup parent) {
        Queue<View> queue = viewCache.get(layoutId);
        if (queue != null && !queue.isEmpty()) {
            View view = queue.poll();
            // 补充预加载
            preloadLayout(layoutId, parent);
            return view;
        }
        // 缓存中没有，同步创建
        return LayoutInflater.from(context).inflate(layoutId, parent, false);
    }

    /**
     * 检查是否有预加载的View可用
     */
    public boolean hasPreloadedView(int layoutId) {
        Queue<View> queue = viewCache.get(layoutId);
        return queue != null && !queue.isEmpty();
    }

    /**
     * 清理缓存
     */
    public void clear() {
        for (int i = 0; i < viewCache.size(); i++) {
            Queue<View> queue = viewCache.valueAt(i);
            if (queue != null) {
                queue.clear();
            }
        }
    }
}
