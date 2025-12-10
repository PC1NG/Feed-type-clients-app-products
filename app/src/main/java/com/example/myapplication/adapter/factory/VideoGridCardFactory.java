package com.example.myapplication.adapter.factory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.R;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.adapter.viewholder.VideoGridViewHolder;

/**
 * 双列视频卡片
 */
public class VideoGridCardFactory implements CardViewHolderFactory {

    public static final int VIEW_TYPE_VIDEO_GRID = 101;

    @Override
    public int getViewType() {
        return VIEW_TYPE_VIDEO_GRID;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_news_video_grid;
    }

    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new VideoGridViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news_video_grid, parent, false)
        );
    }

    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new VideoGridViewHolder(preloadedView);
    }
}
