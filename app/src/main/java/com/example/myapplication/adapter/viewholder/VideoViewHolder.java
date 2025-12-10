package com.example.myapplication.adapter.viewholder;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.myapplication.R;
import com.example.myapplication.model.NewsBean;

/**
 * 视频卡片ViewHolder（单列）
 * 支持自动播放：滑入可见区域自动播放，滑出自动停止
 */
public class VideoViewHolder extends BaseViewHolder {

    private final TextView tvTitle;
    private final TextView tvAuthor;
    private final TextView tvDuration;
    private final TextView tvCountdown;
    private final ImageView imgCover;
    private final ImageView btnPlay;
    private CountDownTimer timer;
    private boolean isPlaying = false;

    public VideoViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvAuthor = itemView.findViewById(R.id.tv_author);
        tvDuration = itemView.findViewById(R.id.tv_duration);
        tvCountdown = itemView.findViewById(R.id.tv_countdown);
        imgCover = itemView.findViewById(R.id.img_cover);
        btnPlay = itemView.findViewById(R.id.btn_play);

        btnPlay.setOnClickListener(v -> startPlayback());
    }

    @Override
    public void bind(NewsBean news) {
        stopPlayback();
        tvTitle.setText(news.title);
        tvAuthor.setText(news.author);
        tvDuration.setText(news.duration);
        if (news.images != null && !news.images.isEmpty()) {
            loadImage(imgCover, news.images.get(0));
        }
    }

    @Override
    public boolean isAutoPlayable() {
        return true;
    }

    @Override
    public void startAutoPlay() {
        if (!isPlaying) {
            startPlayback();
        }
    }

    @Override
    public void stopAutoPlay() {
        stopPlayback();
    }

    private void startPlayback() {
        if (isPlaying) return;
        isPlaying = true;
        btnPlay.setVisibility(View.GONE);
        tvCountdown.setVisibility(View.VISIBLE);
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("播放中: " + (millisUntilFinished / 1000) + "s");
            }
            public void onFinish() {
                resetUI();
            }
        }.start();
    }

    public void stopPlayback() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isPlaying = false;
        resetUI();
    }

    private void resetUI() {
        isPlaying = false;
        tvCountdown.setVisibility(View.GONE);
        btnPlay.setVisibility(View.VISIBLE);
    }
}
