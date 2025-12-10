package com.example.myapplication.adapter.viewholder;

import android.view.View;
import android.widget.TextView;
import com.example.myapplication.R;
import com.example.myapplication.model.NewsBean;

/**
 * 纯文字卡片ViewHolder
 */
public class TextViewHolder extends BaseViewHolder {

    private final TextView tvTitle;
    private final TextView tvAuthor;

    public TextViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvAuthor = itemView.findViewById(R.id.tv_author);
    }

    @Override
    public void bind(NewsBean news) {
        tvTitle.setText(news.title);
        tvAuthor.setText(news.author);
    }
}
