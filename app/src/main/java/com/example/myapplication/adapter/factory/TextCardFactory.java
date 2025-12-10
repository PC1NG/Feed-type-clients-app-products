package com.example.myapplication.adapter.factory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.R;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.adapter.viewholder.TextViewHolder;
import com.example.myapplication.model.NewsBean;

/**
 * 纯文字卡片
 */
public class TextCardFactory implements CardViewHolderFactory {

    @Override
    public int getViewType() {
        return NewsBean.TYPE_TEXT;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_news_text;
    }

    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new TextViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_text, parent, false)
        );
    }

    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new TextViewHolder(preloadedView);
    }
}
