package com.example.myapplication.adapter.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.myapplication.R;
import com.example.myapplication.model.NewsBean;

/**
 * 双列卡片ViewHolder
 */
public class GridViewHolder extends BaseViewHolder {

    private final TextView tvTitle;
    private final TextView tvAuthor;
    private final TextView tvLike;
    private final ImageView imgCover;

    public GridViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvAuthor = itemView.findViewById(R.id.tv_author);
        tvLike = itemView.findViewById(R.id.tv_like);
        imgCover = itemView.findViewById(R.id.img_cover);
    }

    @Override
    public void bind(NewsBean news) {
        tvTitle.setText(news.title);
        tvAuthor.setText(news.author);
        tvLike.setText(news.comment != null ? news.comment : "");
        if (news.images != null && !news.images.isEmpty()) {
            loadImage(imgCover, news.images.get(0));
        }
    }
}
