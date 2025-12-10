package com.example.myapplication.adapter;

import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.adapter.factory.*;
import com.example.myapplication.adapter.preload.LayoutPreloader;
import com.example.myapplication.adapter.viewholder.BaseViewHolder;
import com.example.myapplication.model.NewsBean;
import java.util.List;

/**
 * 新闻列表Adapter
 * 支持插件式扩展卡片类型
 * 集成布局预加载优化
 */
public class NewsAdapter extends RecyclerView.Adapter<BaseViewHolder> {

    private List<NewsBean> mData;
    private OnItemLongClickListener longClickListener;

    // 卡片工厂注册表
    private final SparseArray<CardViewHolderFactory> factoryRegistry = new SparseArray<>();
    
    // 布局预加载器
    private LayoutPreloader layoutPreloader;

    public NewsAdapter(List<NewsBean> data) {
        this.mData = data;
        // 注册默认卡片类型
        registerDefaultFactories();
    }

    /**
     * 设置布局预加载器
     */
    public void setLayoutPreloader(LayoutPreloader preloader) {
        this.layoutPreloader = preloader;
    }

    /**
     * 注册默认的卡片
     */
    private void registerDefaultFactories() {
        registerCardFactory(new TextCardFactory());
        registerCardFactory(new ThreeImagesCardFactory());
        registerCardFactory(new VideoCardFactory());
        registerCardFactory(new GridCardFactory());
        registerCardFactory(new VideoGridCardFactory());
    }

    /**
     * 注册新的卡片（支持扩展）
     */
    public void registerCardFactory(CardViewHolderFactory factory) {
        factoryRegistry.put(factory.getViewType(), factory);
    }

    // 长按监听接口
    public interface OnItemLongClickListener {
        void onLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < mData.size()) {
            mData.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mData.size() - position);
        }
    }

    public void setNewData(List<NewsBean> newData) {
        this.mData = newData;
        notifyDataSetChanged();
    }

    public void addData(List<NewsBean> moreData) {
        int startPos = this.mData.size();
        this.mData.addAll(moreData);
        notifyItemRangeInserted(startPos, moreData.size());
    }

    @Override
    public int getItemViewType(int position) {
        NewsBean item = mData.get(position);
        // 双列模式
        if (item.span == NewsBean.SPAN_DOUBLE) {
            if (item.type == NewsBean.TYPE_VIDEO) {
                return VideoGridCardFactory.VIEW_TYPE_VIDEO_GRID;
            }
            return GridCardFactory.VIEW_TYPE_GRID;
        }
        // 单列模式按type返回
        return item.type;
    }

    public int getSpanSize(int position) {
        if (position < 0 || position >= mData.size()) {
            return NewsBean.SPAN_SINGLE;
        }
        return mData.get(position).span;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardViewHolderFactory factory = factoryRegistry.get(viewType);
        if (factory == null) {
            factory = factoryRegistry.get(NewsBean.TYPE_TEXT);
        }
        
        // 尝试使用预加载的View
        if (layoutPreloader != null) {
            int layoutId = factory.getLayoutId();
            View preloadedView = layoutPreloader.getPreloadedView(layoutId, parent);
            return factory.createViewHolder(preloadedView);
        }
        
        // 没有预加载器，使用默认方式创建
        return factory.createViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        NewsBean news = mData.get(position);
        holder.bind(news);

        // 长按监听
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longClickListener.onLongClick(pos);
                }
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }
}
