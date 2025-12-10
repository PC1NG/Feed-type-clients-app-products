package com.example.myapplication.adapter.factory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.R;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.adapter.viewholder.GridViewHolder;

/**
 * 双列卡片
 */
public class GridCardFactory implements CardViewHolderFactory {

    public static final int VIEW_TYPE_GRID = 100;

    @Override
    public int getViewType() {
        return VIEW_TYPE_GRID;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_news_grid;
    }

    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new GridViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_grid, parent, false)
        );
    }

    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new GridViewHolder(preloadedView);
    }
}
