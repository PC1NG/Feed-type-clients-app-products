package com.example.myapplication.model;

import java.util.List;

/**
 * 新闻数据模型
 */
public class NewsBean {
    // 卡片类型常量
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_SINGLE_IMAGE = 1;
    public static final int TYPE_THREE_IMAGES = 2;
    public static final int TYPE_VIDEO = 3;

    // 排版模式常量
    public static final int SPAN_SINGLE = 2; // 单列（占满2格）
    public static final int SPAN_DOUBLE = 1; // 双列（占1格）

    public int type;
    public int span = SPAN_SINGLE;
    public String title;
    public String author;
    public String comment;
    public List<String> images;
    public String duration;

    public NewsBean() {
    }
}
