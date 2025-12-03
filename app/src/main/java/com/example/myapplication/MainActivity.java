package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import androidx.recyclerview.widget.GridLayoutManager;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    // 记录当前正在显示的文件名，默认为推荐
    // ⚠️ 请确保你的 assets 目录下有 news_recommend.json 这个文件
    private String currentFileName = "news_recommend.json";

    // 内存中的数据缓存
    private List<NewsBean> currentDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // === 沉浸式状态栏代码 (保持你之前的设置) ===
        android.view.Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // 1. 初始化界面控件和监听器
        initView();

        // 2. 首次进入，加载默认数据 (推荐)
        loadDataFromFile(currentFileName);
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
                // 获取当前卡片的数据
                int type = adapter.getItemViewType(position);

                // 规则：如果是"单图"模式(Type=1)，我们让它变成双列混排（只占1格）
                // 其他模式（纯文、三图、视频）保持单列全宽（占2格）
                if (type == NewsBean.TYPE_SINGLE_IMAGE) {
                    return 1; // 占一半宽度
                } else {
                    return 2; // 占满全宽
                }
            }
        });

        recyclerView.setLayoutManager(gridLayoutManager);

        // 初始化 adapter
        adapter = new NewsAdapter(currentDataList);
        recyclerView.setAdapter(adapter);

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
            // 模拟网络延迟 1秒
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 核心逻辑：重新读取当前频道文件
                List<NewsBean> freshData = getNewsFromAssets(currentFileName);
                if (freshData != null) {
                    // 模拟更新：打乱顺序，假装是新新闻
                    Collections.shuffle(freshData);

                    // 更新 UI
                    currentDataList.clear();
                    currentDataList.addAll(freshData);
                    adapter.setNewData(currentDataList);

                    Toast.makeText(MainActivity.this, "推荐成功", Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false); // 停止转圈
            }, 1000);
        });

        // 初始化 加载更多 (滑动到底部监听)
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        // === D. 初始化 TabLayout (点击切换) ===
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
        loadDataFromFile(currentFileName);

        // 3. 切换后自动回到顶部
        recyclerView.scrollToPosition(0);
    }


    //从文件读取数据并更新 Adapter

    private void loadDataFromFile(String fileName) {
        List<NewsBean> data = getNewsFromAssets(fileName);
        if (data != null && !data.isEmpty()) {
            currentDataList.clear();
            currentDataList.addAll(data);
            adapter.setNewData(currentDataList);
            recyclerView.post(() -> checkExposure());
        } else {
            // 如果文件不存在或没数据
            Toast.makeText(this, "暂无内容: " + fileName, Toast.LENGTH_SHORT).show();
        }
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

    //读取 Assets JSON 的底层方法
    private List<NewsBean> getNewsFromAssets(String fileName) {
        try {
            InputStreamReader isr = new InputStreamReader(getAssets().open(fileName), "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            Gson gson = new Gson();
            Type listType = new TypeToken<List<NewsBean>>() {
            }.getType();
            return gson.fromJson(sb.toString(), listType);
        } catch (Exception e) {
            e.printStackTrace();
            // 如果文件找不到，打印错误日志
            return null;
        }
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

    private void checkExposure() {
        if (recyclerView == null) return;
        androidx.recyclerview.widget.GridLayoutManager layoutManager =
                (androidx.recyclerview.widget.GridLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int firstPos = layoutManager.findFirstVisibleItemPosition();
        int lastPos = layoutManager.findLastVisibleItemPosition();

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
    }
}

