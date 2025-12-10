package com.example.myapplication.adapter.preload;

import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.model.NewsBean;
import java.util.List;

/**
 * 卡片预渲染器
 * 在空闲时提前绑定数据，控制预渲染时机避免影响滑动流畅性
 */
public class CardPrerenderer {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 预渲染数据缓存（position -> 是否已预渲染）
    private final LruCache<Integer, Boolean> prerenderedCache = new LruCache<>(50);
    
    // 是否正在滑动
    private boolean isScrolling = false;
    
    // 预渲染任务
    private Runnable prerenderTask;

    /**
     * 设置滑动状态
     */
    public void setScrolling(boolean scrolling) {
        this.isScrolling = scrolling;
    }

    /**
     * 在空闲时执行预渲染
     * @param recyclerView RecyclerView实例
     * @param dataList 数据列表
     * @param lastVisiblePosition 最后可见位置
     */
    public void prerenderWhenIdle(RecyclerView recyclerView, List<NewsBean> dataList, int lastVisiblePosition) {
        // 取消之前的任务
        if (prerenderTask != null) {
            mainHandler.removeCallbacks(prerenderTask);
        }

        prerenderTask = () -> {
            // 如果正在滑动，延迟执行
            if (isScrolling) {
                mainHandler.postDelayed(prerenderTask, 100);
                return;
            }

            // 预渲染后面5个item
            int startPos = lastVisiblePosition + 1;
            int endPos = Math.min(startPos + 5, dataList.size());

            for (int i = startPos; i < endPos; i++) {
                // 检查是否已预渲染
                if (prerenderedCache.get(i) != null) {
                    continue;
                }

                // 标记为已预渲染
                prerenderedCache.put(i, true);

                // 触发RecyclerView的预取机制
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                if (holder instanceof BaseViewHolder) {
                    // ViewHolder已存在，数据已绑定
                    continue;
                }
                
                // 让RecyclerView知道这个位置即将需要
                recyclerView.smoothScrollBy(0, 1);
                recyclerView.smoothScrollBy(0, -1);
            }
        };

        // 延迟50ms执行，确保滑动完全停止
        mainHandler.postDelayed(prerenderTask, 50);
    }

    /**
     * 清除指定位置的预渲染缓存
     */
    public void invalidate(int position) {
        prerenderedCache.remove(position);
    }

    /**
     * 清除所有预渲染缓存
     */
    public void clearCache() {
        prerenderedCache.evictAll();
        if (prerenderTask != null) {
            mainHandler.removeCallbacks(prerenderTask);
        }
    }

    /**
     * 检查是否已预渲染
     */
    public boolean isPrerendered(int position) {
        return prerenderedCache.get(position) != null;
    }
}
