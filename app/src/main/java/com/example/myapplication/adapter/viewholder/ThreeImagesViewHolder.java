package com.example.myapplication.adapter.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.myapplication.R;
import com.example.myapplication.model.NewsBean;

/**
 * 三图卡片ViewHolder
 */
public class ThreeImagesViewHolder extends BaseViewHolder {

    private final TextView tvTitle;
    private final TextView tvAuthor;
    private final ImageView img1;
    private final ImageView img2;
    private final ImageView img3;

    public ThreeImagesViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvAuthor = itemView.findViewById(R.id.tv_author);
        img1 = itemView.findViewById(R.id.img_1);
        img2 = itemView.findViewById(R.id.img_2);
        img3 = itemView.findViewById(R.id.img_3);
    }

    @Override
    public void bind(NewsBean news) {
        tvTitle.setText(news.title);
        tvAuthor.setText(news.author);
        if (news.images != null && news.images.size() >= 3) {
            loadImage(img1, news.images.get(0));
            loadImage(img2, news.images.get(1));
            loadImage(img3, news.images.get(2));
        }
    }
}
