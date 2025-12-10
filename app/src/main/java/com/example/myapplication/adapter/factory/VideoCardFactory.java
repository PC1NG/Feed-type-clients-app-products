package com.example.myapplication.adapter.factory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.R;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.adapter.viewholder.VideoViewHolder;
import com.example.myapplication.model.NewsBean;

/**
 * 视频卡片（单列）
 */
public class VideoCardFactory implements CardViewHolderFactory {

    @Override
    public int getViewType() {
        return NewsBean.TYPE_VIDEO;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_news_video;
    }

    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new VideoViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_video, parent, false)
        );
    }

    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new VideoViewHolder(preloadedView);
    }
}
