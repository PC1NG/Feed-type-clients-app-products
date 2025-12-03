package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvHistory, rvGuess;
    private SearchWordAdapter historyAdapter, guessAdapter;

    // 历史记录数据
    private List<String> historyList = new ArrayList<>();
    // 猜你想搜数据 (模拟)
    private List<String> guessList = Arrays.asList(
            "成都市市长王凤朝", "中纪委11月打下6虎",
            "日本战争准备曝光", "妻子婚内与人同居",
            "王励勤任新职", "香港大埔火灾遇难",
            "吃猪蹄对人体好吗", "特朗普与共和党",
            "迈巴赫S560图片", "流感进入快速上升期"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 沉浸式状态栏
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        initView();
        initData();
    }

    private void initView() {
        etSearch = findViewById(R.id.et_search);
        rvHistory = findViewById(R.id.rv_history);
        rvGuess = findViewById(R.id.rv_guess);

        // 1. 设置返回按钮
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 2. 设置搜索按钮点击
        findViewById(R.id.tv_search_btn).setOnClickListener(v -> performSearch());

        // 3. 监听键盘上的"搜索"键
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // 4. 删除历史记录
        findViewById(R.id.iv_delete_history).setOnClickListener(v -> {
            historyList.clear();
            saveHistory(); // 清空本地存储
            historyAdapter.notifyDataSetChanged();
        });

        // 5. 初始化列表 (使用 Grid 2列)
        initRecyclerView(rvHistory);
        initRecyclerView(rvGuess);
    }

    private void initRecyclerView(RecyclerView rv) {
        // 设置为 2 列网格布局
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        // 解决 ScrollView 嵌套 RecyclerView 滑动冲突
        rv.setNestedScrollingEnabled(false);
    }

    private void initData() {
        // 加载历史记录
        loadHistory();

        // 设置适配器
        historyAdapter = new SearchWordAdapter(historyList);
        guessAdapter = new SearchWordAdapter(guessList);

        rvHistory.setAdapter(historyAdapter);
        rvGuess.setAdapter(guessAdapter);
    }

    // 执行搜索逻辑
    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 添加到历史记录
        if (historyList.contains(keyword)) {
            historyList.remove(keyword);
        }
        historyList.add(0, keyword);
        // 最多保存 10 条
        if (historyList.size() > 10) {
            historyList.remove(historyList.size() - 1);
        }

        // 2. 刷新 UI 并保存
        historyAdapter.notifyDataSetChanged();
        saveHistory();

        // 3. 清空输入框 (可选)
        etSearch.setText("");
        Toast.makeText(this, "正在搜索: " + keyword, Toast.LENGTH_SHORT).show();
    }

    // SharedPreferences 本地存储逻辑
    private void saveHistory() {
        SharedPreferences sp = getSharedPreferences("search_history", Context.MODE_PRIVATE);
        // 简单粗暴：用逗号拼接字符串保存
        StringBuilder sb = new StringBuilder();
        for (String s : historyList) {
            sb.append(s).append(",");
        }
        sp.edit().putString("history_str", sb.toString()).apply();
    }

    private void loadHistory() {
        SharedPreferences sp = getSharedPreferences("search_history", Context.MODE_PRIVATE);
        String str = sp.getString("history_str", "");
        if (!TextUtils.isEmpty(str)) {
            String[] arr = str.split(",");
            historyList.clear();
            Collections.addAll(historyList, arr);
        }
    }

    // 内部适配器类
    class SearchWordAdapter extends RecyclerView.Adapter<SearchWordAdapter.VH> {
        private List<String> mList;
        public SearchWordAdapter(List<String> list) { this.mList = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_word, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String word = mList.get(position);
            holder.tv.setText(word);
            // 点击词条直接搜索
            holder.itemView.setOnClickListener(v -> {
                etSearch.setText(word);
                performSearch();
            });
        }

        @Override public int getItemCount() { return mList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            public VH(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv_word);
            }
        }
    }
}
