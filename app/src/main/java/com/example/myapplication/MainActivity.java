package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.tabs.TabLayout;
import com.bumptech.glide.Glide;
import com.example.myapplication.adapter.NewsAdapter;
import com.example.myapplication.adapter.preload.CardPrerenderer;
import com.example.myapplication.adapter.preload.LayoutPreloader;
import com.example.myapplication.adapter.preload.VideoPreloader;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.model.NewsBean;
import com.example.myapplication.repository.NewsRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    // 记录当前正在显示的文件名，默认为推荐
    private String currentFileName = "news_recommend.json";

    // 内存中的数据缓存
    private List<NewsBean> currentDataList = new ArrayList<>();
    
    // 数据仓库（网络+缓存）
    private NewsRepository newsRepository;
    
    // 视频预加载器
    private VideoPreloader videoPreloader;
    
    // XML布局预加载器
    private LayoutPreloader layoutPreloader;
    
    // 卡片预渲染器
    private CardPrerenderer cardPrerenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 沉浸式状态栏代码
        android.view.Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // 1. 初始化数据仓库和预加载器
        newsRepository = new NewsRepository(this);
        videoPreloader = new VideoPreloader(this);
        layoutPreloader = new LayoutPreloader(this);
        cardPrerenderer = new CardPrerenderer();
        
        // 2. 初始化界面控件和监听器
        initView();

        // 3. 首次进入，加载默认数据 (推荐)
        loadDataFromRepository(currentFileName);
    }

    private void initView() {
        // === A. 初始化 RecyclerView (改为 GridLayoutManager) ===
        recyclerView = findViewById(R.id.recycler_view);

        // 设置为 2 列
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);

        // 核心：定义每张卡片占几列
        // span = 2 表示占满全屏（单列模式）
        // span = 1 表示占一半（双列模式）
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // 直接从数据中读取span值，由服务端控制排版
                return adapter.getSpanSize(position);
            }
        });

        recyclerView.setLayoutManager(gridLayoutManager);

        // === 性能优化配置 ===
        recyclerView.setHasFixedSize(true);           // 固定大小优化
        recyclerView.setItemViewCacheSize(10);        // 增加缓存数量
        gridLayoutManager.setItemPrefetchEnabled(true); // 开启预取

        // 初始化 adapter，并设置布局预加载器
        adapter = new NewsAdapter(currentDataList);
        adapter.setLayoutPreloader(layoutPreloader);
        recyclerView.setAdapter(adapter);
        
        // 启动布局预加载（在RecyclerView设置好后）
        recyclerView.post(() -> layoutPreloader.startPreload(recyclerView));

        adapter.setOnItemLongClickListener(position -> {
            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("确定要删除这条内容吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        adapter.removeItem(position);
                        Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        //初始化 SwipeRefreshLayout (下拉刷新)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_red_light); // 设置红色转圈

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 下拉刷新：强制从网络获取
            newsRepository.forceRefresh(currentFileName, new NewsRepository.DataCallback() {
                @Override
                public void onSuccess(List<NewsBean> data, boolean fromCache) {
                    // 模拟更新：打乱顺序，假装是新新闻
                    Collections.shuffle(data);
                    
                    currentDataList.clear();
                    currentDataList.addAll(data);
                    adapter.setNewData(currentDataList);
                    
                    Toast.makeText(MainActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        });

        // 初始化 加载更多 (滑动到底部监听) + 性能优化
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 滑动时暂停图片加载，停止后恢复 - 提升滑动流畅性
                // 更新卡片预渲染器的滑动状态
                cardPrerenderer.setScrolling(newState != RecyclerView.SCROLL_STATE_IDLE);
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Glide.with(MainActivity.this).resumeRequests();
                    // 停止滑动时预加载后面的图片和视频
                    preloadImages();
                    preloadVideos();
                    // 触发卡片预渲染
                    prerenderCards();
                } else {
                    Glide.with(MainActivity.this).pauseRequests();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // dy > 0 表示手指向下滑动

                checkExposure();
                if (dy > 0) {
                    int visibleItemCount = recyclerView.getChildCount();
                    int totalItemCount = recyclerView.getItemDecorationCount();
                    int firstVisibleItemPosition = recyclerView.getVerticalScrollbarPosition();

                    // 判断是否滑到底部
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount) {
                        loadMoreData();
                    }
                }
            }
        });

        //  D. 初始化 TabLayout (点击切换)
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            String[] tabs = {"关注", "推荐", "热榜", "北京", "发现", "视频"};
            for (String tab : tabs) {
                tabLayout.addTab(tabLayout.newTab().setText(tab));
            }

            // 监听点击事件
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    // 获取 Tab 的文字，调用切换方法
                    String tabText = tab.getText().toString();
                    switchContent(tabText);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });

            // 默认选中第2个
            if (tabLayout.getTabCount() > 1) {
                tabLayout.getTabAt(1).select();
            }
        }
        // 初始化搜索跳转
        // 找到顶部搜索栏
        android.view.View searchBar = findViewById(R.id.layout_search);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                // 跳转到 SearchActivity
                android.content.Intent intent = new android.content.Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            });
        }

    }

    //切换频道
    private void switchContent(String tabName) {
        // 切换前停止当前视频播放并清除预加载缓存
        stopAllVideoPlayback();
        videoPreloader.clearCache();
        cardPrerenderer.clearCache();
        
        // 1. 根据名字映射到对应的 JSON 文件名
        switch (tabName) {
            case "关注":
                currentFileName = "news_focus.json";
                break;
            case "热榜":
                currentFileName = "news_hot.json";
                break;
            case "北京":
                currentFileName = "news_Beijing.json";
                break;
            case "发现":
                currentFileName = "news_discovery.json";
                break;
            case "视频":
                currentFileName = "news_video.json";
                break;
            case "推荐":
            default:
                currentFileName = "news_recommend.json";
                break;
        }

        // 2. 读取新文件并刷新列表
        loadDataFromRepository(currentFileName);

        // 3. 切换后自动回到顶部
        recyclerView.scrollToPosition(0);
    }


    /**
     * 从数据仓库加载数据
     * 策略：网络优先，失败时使用本地缓存
     */
    private void loadDataFromRepository(String fileName) {
        // 显示加载中
        swipeRefreshLayout.setRefreshing(true);
        
        newsRepository.fetchNews(fileName, new NewsRepository.DataCallback() {
            @Override
            public void onSuccess(List<NewsBean> data, boolean fromCache) {
                currentDataList.clear();
                currentDataList.addAll(data);
                adapter.setNewData(currentDataList);
                recyclerView.post(() -> checkExposure());
                
                // 提示数据来源
                if (fromCache) {
                    Toast.makeText(MainActivity.this, "网络异常，已加载缓存数据", Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    //模拟加载更多
    private boolean isLoading = false;

    private void loadMoreData() {
        if (isLoading) return; // 防止重复触发
        isLoading = true;

        // 模拟 1秒 加载延迟
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 模拟逻辑：从当前数据里复制前3条，追加到末尾
            List<NewsBean> moreData = new ArrayList<>();
            if (currentDataList.size() > 0) {
                // 简单的算法：循环取数据
                for (int i = 0; i < 3; i++) {
                    // 防止越界，取余数
                    moreData.add(currentDataList.get(i % currentDataList.size()));
                }
            }

            if (moreData.size() > 0) {
                currentDataList.addAll(moreData); // 同时也更新内存数据
                adapter.addData(moreData); // 通知 Adapter 追加
                Toast.makeText(MainActivity.this, "加载了 " + moreData.size() + " 条新内容", Toast.LENGTH_SHORT).show();
            }

            isLoading = false;
        }, 1000);
    }

    /**
     * 图片预加载 - 提前加载即将显示的图片
     */
    private void preloadImages() {
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        // 预加载后面5个item的图片
        for (int i = lastVisible + 1; i <= lastVisible + 5 && i < currentDataList.size(); i++) {
            NewsBean item = currentDataList.get(i);
            if (item.images != null && !item.images.isEmpty()) {
                for (String url : item.images) {
                    String imagePath = url.startsWith("http") ? url : "file:///android_asset/images/" + url;
                    Glide.with(this).load(imagePath).preload();
                }
            }
        }
    }

    /**
     * 视频预加载 - 提前加载即将显示的视频封面和数据
     */
    private void preloadVideos() {
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        videoPreloader.preloadVideos(currentDataList, lastVisible, position -> {
            // 预加载完成回调（可选：打印日志）
            runOnUiThread(() -> logExposure("视频 " + position + " -> 📥 预加载完成"));
        });
    }

    /**
     * 卡片预渲染 - 在空闲时提前触发即将显示的卡片渲染
     */
    private void prerenderCards() {
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int lastVisible = layoutManager.findLastVisibleItemPosition();
        cardPrerenderer.prerenderWhenIdle(recyclerView, currentDataList, lastVisible);
    }

    // 测试工具：日志输出
    private android.widget.TextView tvConsole;

    private void logExposure(String msg) {
        if (tvConsole == null) tvConsole = findViewById(R.id.tv_console);
        if (tvConsole != null) {
            tvConsole.append("\n" + msg);
            // 自动滚动到底部
            ((android.view.View) tvConsole.getParent()).post(() ->
                    ((android.widget.ScrollView) tvConsole.getParent()).fullScroll(android.view.View.FOCUS_DOWN));
        }
    }

    // 曝光检测
    private java.util.Map<Integer, Integer> exposureStateMap = new java.util.HashMap<>();
    
    // 当前正在自动播放的视频位置，-1表示没有
    private int currentAutoPlayPosition = -1;

    private void checkExposure() {
        if (recyclerView == null) return;
        androidx.recyclerview.widget.GridLayoutManager layoutManager =
                (androidx.recyclerview.widget.GridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int firstPos = layoutManager.findFirstVisibleItemPosition();
        int lastPos = layoutManager.findLastVisibleItemPosition();

        // 用于记录最佳自动播放候选（露出比例最大的视频卡片）
        int bestAutoPlayPos = -1;
        float bestRatio = 0f;

        // 1. 检测可见区域内的 Item (处理露出)
        for (int i = firstPos; i <= lastPos; i++) {
            android.view.View view = layoutManager.findViewByPosition(i);
            if (view == null) continue;

            android.graphics.Rect globalRect = new android.graphics.Rect();
            boolean isVisible = view.getGlobalVisibleRect(globalRect);

            if (isVisible) {
                long visibleHeight = globalRect.height();
                long totalHeight = view.getHeight();
                if (totalHeight == 0) totalHeight = 1; // 防止除以0

                float ratio = (float) visibleHeight / totalHeight;

                // 获取旧状态
                Integer stateObj = exposureStateMap.get(i);
                int oldState = (stateObj == null) ? 0 : stateObj;

                int newState = oldState;

                // 获取标题用于打印
                String title = "";
                if (i >= 0 && i < currentDataList.size()) {
                    String fullTitle = currentDataList.get(i).title;
                    title = fullTitle.length() > 5 ? fullTitle.substring(0, 5) : fullTitle;
                }

                // 状态机流转：只有状态升级时才打印
                if (ratio > 0 && oldState == 0) {
                    newState = 1;
                    logExposure("item " + i + " [" + title + "] -> 🔴 开始露出");
                }
                // 加上 newState < 2 判断，防止 ratio 跳变时重复打印
                if (ratio >= 0.5f && oldState < 2) {
                    newState = 2;
                    logExposure("item " + i + " [" + title + "] -> 🟡 露出超过50%");
                }
                // 加上 newState < 3 判断
                if (ratio >= 0.99f && oldState < 3) { // 用 0.99 代替 1.0 防止浮点精度问题
                    newState = 3;
                    logExposure("item " + i + " [" + title + "] -> 🟢 完全展示");
                }

                // 只有状态发生改变时才更新 Map
                if (newState != oldState) {
                    exposureStateMap.put(i, newState);
                }

                // 检查是否为视频卡片，且露出超过50%，记录最佳候选
                if (ratio >= 0.5f) {
                    RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                    if (holder instanceof BaseViewHolder) {
                        BaseViewHolder baseHolder = (BaseViewHolder) holder;
                        if (baseHolder.isAutoPlayable() && ratio > bestRatio) {
                            bestRatio = ratio;
                            bestAutoPlayPos = i;
                        }
                    }
                }
            }
        }

        // 2. 检测消失的 Item (不在可见范围内的)
        // 使用迭代器安全删除，且只处理还在 Map 里的
        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> it = exposureStateMap.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = it.next();
            int pos = entry.getKey();
            int state = entry.getValue();

            // 如果当前位置已经不在可见范围内
            if (pos < firstPos || pos > lastPos) {
                // 只有当前状态不是 0 (说明之前露出过) 时，才打印消失
                if (state > 0) {
                    logExposure("item " + pos + " -> ⚫ 已消失");
                }
                // 彻底移除，防止重复检测
                it.remove();
            }
        }

        // 3. 处理视频自动播放逻辑
        handleVideoAutoPlay(bestAutoPlayPos);
    }

    /**
     * 处理视频自动播放
     * 规则：同一时间只有一个视频自动播放，选择露出比例最大且超过50%的视频
     */
    private void handleVideoAutoPlay(int bestAutoPlayPos) {
        // 如果最佳候选和当前播放的一样，不做处理
        if (bestAutoPlayPos == currentAutoPlayPosition) {
            return;
        }

        // 停止当前正在播放的视频
        if (currentAutoPlayPosition != -1) {
            RecyclerView.ViewHolder oldHolder = recyclerView.findViewHolderForAdapterPosition(currentAutoPlayPosition);
            if (oldHolder instanceof BaseViewHolder) {
                BaseViewHolder baseHolder = (BaseViewHolder) oldHolder;
                if (baseHolder.isAutoPlayable()) {
                    baseHolder.stopAutoPlay();
                    logExposure("视频 " + currentAutoPlayPosition + " -> ⏹ 自动停止");
                }
            }
        }

        // 开始播放新的视频
        if (bestAutoPlayPos != -1) {
            RecyclerView.ViewHolder newHolder = recyclerView.findViewHolderForAdapterPosition(bestAutoPlayPos);
            if (newHolder instanceof BaseViewHolder) {
                BaseViewHolder baseHolder = (BaseViewHolder) newHolder;
                if (baseHolder.isAutoPlayable()) {
                    baseHolder.startAutoPlay();
                    logExposure("视频 " + bestAutoPlayPos + " -> ▶ 自动播放");
                }
            }
        }

        currentAutoPlayPosition = bestAutoPlayPos;
    }

    /**
     * 停止所有视频播放（切换Tab或页面时调用）
     */
    private void stopAllVideoPlayback() {
        if (currentAutoPlayPosition != -1) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(currentAutoPlayPosition);
            if (holder instanceof BaseViewHolder) {
                ((BaseViewHolder) holder).stopAutoPlay();
            }
            currentAutoPlayPosition = -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放预加载器资源
        if (videoPreloader != null) {
            videoPreloader.release();
        }
        if (layoutPreloader != null) {
            layoutPreloader.clear();
        }
        if (cardPrerenderer != null) {
            cardPrerenderer.clearCache();
        }
    }
}

