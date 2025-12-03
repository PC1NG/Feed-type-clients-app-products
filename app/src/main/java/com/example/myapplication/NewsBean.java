package com.example.myapplication;

import java.util.List;
public class NewsBean {
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_SINGLE_IMAGE = 1;
    public static final int TYPE_THREE_IMAGES = 2;
    public static final int TYPE_VIDEO = 3;

    public int type; // 0:纯文, 1:单图, 2:三图
    public String title;
    public String author;
    public String comment;
    public List<String> images;
    public String duration; // 视频时长

    // 构造函数

    public NewsBean() {
    }

}
