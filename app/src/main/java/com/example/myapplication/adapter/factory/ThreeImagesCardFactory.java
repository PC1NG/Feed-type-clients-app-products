package com.example.myapplication.adapter.factory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.R;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.adapter.viewholder.ThreeImagesViewHolder;
import com.example.myapplication.model.NewsBean;

/**
 * 三图卡片
 */
public class ThreeImagesCardFactory implements CardViewHolderFactory {

    @Override
    public int getViewType() {
        return NewsBean.TYPE_THREE_IMAGES;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_news_three_images;
    }

    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new ThreeImagesViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_three_images, parent, false)
        );
    }

    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new ThreeImagesViewHolder(preloadedView);
    }
}
