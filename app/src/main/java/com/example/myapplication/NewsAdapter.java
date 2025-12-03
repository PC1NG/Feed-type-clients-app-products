package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<NewsBean> mData;
    private OnItemLongClickListener longClickListener;

    // 构造函数
    public NewsAdapter(List<NewsBean> data) {
        this.mData = data;
    }

    // 设置长按监听接口
    public interface OnItemLongClickListener {
        void onLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    // 删除条目方法
    public void removeItem(int position) {
        if (position >= 0 && position < mData.size()) {
            mData.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mData.size() - position);
        }
    }

    // 刷新数据方法
    public void setNewData(List<NewsBean> newData) {
        this.mData = newData;
        notifyDataSetChanged();
    }

    // 加载更多方法
    public void addData(List<NewsBean> moreData) {
        int startPos = this.mData.size();
        this.mData.addAll(moreData);
        notifyItemRangeInserted(startPos, moreData.size());
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == NewsBean.TYPE_THREE_IMAGES) {
            return new ThreeImagesHolder(inflater.inflate(R.layout.item_news_three_images, parent, false));
        } else if (viewType == NewsBean.TYPE_VIDEO) {
            return new VideoHolder(inflater.inflate(R.layout.item_news_video, parent, false));
        } else {
            // 默认处理为纯文（包括单图类型，如果你的单图布局还没写，暂时用纯文代替）
            return new TextHolder(inflater.inflate(R.layout.item_news_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NewsBean news = mData.get(position);

        if (holder instanceof ThreeImagesHolder) {
            ThreeImagesHolder th = (ThreeImagesHolder) holder;
            th.tvTitle.setText(news.title);
            th.tvAuthor.setText(news.author);
            if (news.images != null && news.images.size() >= 3) {
                loadImage(th.img1, news.images.get(0));
                loadImage(th.img2, news.images.get(1));
                loadImage(th.img3, news.images.get(2));
            }
        } else if (holder instanceof VideoHolder) {
            VideoHolder vh = (VideoHolder) holder;
            vh.stopPlayback(); // 重置播放状态
            vh.tvTitle.setText(news.title);
            vh.tvAuthor.setText(news.author);
            vh.tvDuration.setText(news.duration);
            if (news.images != null && !news.images.isEmpty()) {
                loadImage(vh.imgCover, news.images.get(0));
            }
        } else if (holder instanceof TextHolder) {
            TextHolder th = (TextHolder) holder;
            th.tvTitle.setText(news.title);
            th.tvAuthor.setText(news.author);
        }

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

    // 加载图片
    private void loadImage(ImageView view, String fileName) {
        String assetPath = "file:///android_asset/images/" + fileName;
        Glide.with(view.getContext()).load(assetPath).centerCrop().into(view);
    }

    //ViewHolder 类定义

    static class TextHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor;
        public TextHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
        }
    }

    static class ThreeImagesHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor;
        ImageView img1, img2, img3;
        public ThreeImagesHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            img1 = itemView.findViewById(R.id.img_1);
            img2 = itemView.findViewById(R.id.img_2);
            img3 = itemView.findViewById(R.id.img_3);
        }
    }

    static class VideoHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvDuration, tvCountdown;
        ImageView imgCover, btnPlay;
        private android.os.CountDownTimer timer;

        public VideoHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            imgCover = itemView.findViewById(R.id.img_cover);
            btnPlay = itemView.findViewById(R.id.btn_play);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);

            btnPlay.setOnClickListener(v -> {
                btnPlay.setVisibility(View.GONE);
                tvCountdown.setVisibility(View.VISIBLE);
                if (timer != null) timer.cancel();
                timer = new android.os.CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        tvCountdown.setText("播放中: " + (millisUntilFinished / 1000) + "s");
                    }
                    public void onFinish() {
                        resetUI();
                    }
                }.start();
            });
        }

        public void stopPlayback() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            resetUI();
        }

        private void resetUI() {
            tvCountdown.setVisibility(View.GONE);
            btnPlay.setVisibility(View.VISIBLE);
        }
    }
}