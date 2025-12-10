package com.example.myapplication.adapter.factory;

import android.view.View;
import android.view.ViewGroup;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;

/**
 * 卡片ViewHolder接口
 * 实现此接口可以扩展新的卡片类型
 */
public interface CardViewHolderFactory {

    /**
     * 获取此对应的ViewType
     */
    int getViewType();

    /**
     * 获取此对应的布局ID
     */
    int getLayoutId();

    /**
     * 创建ViewHolder
     */
    BaseViewHolder createViewHolder(ViewGroup parent);

    /**
     * 使用预加载的View创建ViewHolder
     */
    BaseViewHolder createViewHolder(View preloadedView);
}
