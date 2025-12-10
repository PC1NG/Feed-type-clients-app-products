package com.example.myapplication.adapter.viewholder;

import android.view.View;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication.model.NewsBean;

/**
 * ViewHolder基类，提供通用方法
 */
public abstract class BaseViewHolder extends RecyclerView.ViewHolder {

    public BaseViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * 绑定数据到ViewHolder
     */
    public abstract void bind(NewsBean news);

    /**
     * 加载图片的通用方法
     */
    protected void loadImage(ImageView view, String path) {
        String imagePath;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            imagePath = path;
        } else {
            imagePath = "file:///android_asset/images/" + path;
        }
        Glide.with(view.getContext()).load(imagePath).centerCrop().into(view);
    }

    /**
     * 是否支持自动播放
     */
    public boolean isAutoPlayable() {
        return false;
    }

    /**
     * 开始自动播放
     */
    public void startAutoPlay() {
        // 默认空实现
    }

    /**
     * 停止自动播放
     */
    public void stopAutoPlay() {
        // 默认空实现
    }
}
