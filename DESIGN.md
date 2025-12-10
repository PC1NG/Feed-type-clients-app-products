# 新闻列表应用设计文档

## 项目概述

本项目是一个类似今日头条的新闻信息流Android应用，支持多种卡片类型展示、频道切换、视频自动播放等功能，并实现了多维度的性能优化。

---

## 已实现功能

### 一、功能点扩展

#### 1. 本地数据缓存

**实现类**: `NewsCacheManager`, `NewsRepository`

**功能描述**: 网络请求失败时，自动使用本地缓存数据进行展示

**技术方案**:
- 使用 SharedPreferences 存储 JSON 数据
- 缓存有效期设置为1小时
- 网络优先策略：成功时更新缓存，失败时读取缓存
- 下拉刷新时强制从网络获取

**核心代码路径**:
- `app/src/main/java/com/example/myapplication/cache/NewsCacheManager.java`
- `app/src/main/java/com/example/myapplication/repository/NewsRepository.java`

---

#### 2. 视频自动播放与停止

**实现类**: `VideoViewHolder`, `VideoGridViewHolder`, `BaseViewHolder`

**功能描述**: 类似抖音搜索单列结果页，视频滑入可见区域自动播放，滑出自动停止

**技术方案**:
- 使用倒计时显示模拟播放器（简化方案）
- 曝光检测：视频卡片露出超过50%时触发自动播放
- 同一时间只有一个视频播放（选择可见度最高的）
- 切换Tab时停止所有视频播放

**核心代码路径**:
- `app/src/main/java/com/example/myapplication/adapter/viewholder/BaseViewHolder.java`
- `app/src/main/java/com/example/myapplication/adapter/viewholder/VideoViewHolder.java`
- `app/src/main/java/com/example/myapplication/adapter/viewholder/VideoGridViewHolder.java`

---

### 二、架构设计

#### 1. 整体架构清晰合理

**模块划分**:

```
app/src/main/java/com/example/myapplication/
├── MainActivity.java          # 主页面控制
├── SearchActivity.java        # 搜索页面
├── adapter/
│   ├── NewsAdapter.java       # 列表适配器
│   ├── factory/               # 卡片工厂类
│   │   ├── CardViewHolderFactory.java
│   │   ├── TextCardFactory.java
│   │   ├── ThreeImagesCardFactory.java
│   │   ├── VideoCardFactory.java
│   │   ├── GridCardFactory.java
│   │   └── VideoGridCardFactory.java
│   ├── viewholder/            # ViewHolder实现
│   │   ├── BaseViewHolder.java
│   │   ├── TextViewHolder.java
│   │   ├── ThreeImagesViewHolder.java
│   │   ├── VideoViewHolder.java
│   │   ├── GridViewHolder.java
│   │   └── VideoGridViewHolder.java
│   └── preload/               # 预加载组件
│       ├── LayoutPreloader.java
│       ├── CardPrerenderer.java
│       └── VideoPreloader.java
├── cache/
│   └── NewsCacheManager.java  # 缓存管理
├── repository/
│   └── NewsRepository.java    # 数据仓库
└── model/
    └── NewsBean.java          # 数据模型
```

---

#### 2. 卡片样式支持插件式扩展

**实现接口**: `CardViewHolderFactory`

**功能描述**: 可通过新增一个卡片类，再注册到Feed的方式扩展新的样式类型

**已实现卡片类型**:

| 工厂类 | 卡片类型 | ViewType | 说明 |
|--------|----------|----------|------|
| TextCardFactory | TYPE_TEXT | 0 | 纯文字卡片 |
| ThreeImagesCardFactory | TYPE_THREE_IMAGES | 2 | 三图卡片 |
| VideoCardFactory | TYPE_VIDEO | 3 | 单列视频卡片 |
| GridCardFactory | VIEW_TYPE_GRID | 100 | 双列图文卡片 |
| VideoGridCardFactory | VIEW_TYPE_VIDEO_GRID | 101 | 双列视频卡片 |

**扩展方式**:

```java
// 1. 实现 CardViewHolderFactory 接口
public class NewCardFactory implements CardViewHolderFactory {
    
    @Override
    public int getViewType() { 
        return NEW_TYPE; 
    }
    
    @Override
    public int getLayoutId() { 
        return R.layout.item_new; 
    }
    
    @Override
    public BaseViewHolder createViewHolder(ViewGroup parent) {
        return new NewViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_new, parent, false)
        );
    }
    
    @Override
    public BaseViewHolder createViewHolder(View preloadedView) {
        return new NewViewHolder(preloadedView);
    }
}

// 2. 注册到 Adapter
adapter.registerCardFactory(new NewCardFactory());
```

---

### 三、性能优化

#### 1. 图片预加载

**实现位置**: `MainActivity.preloadImages()`

**功能描述**: 滑动停止时，提前加载即将显示的图片

**技术方案**:
- 使用 `Glide.preload()` 预加载后续5个item的图片
- 滑动时暂停图片加载 `Glide.pauseRequests()`
- 停止后恢复 `Glide.resumeRequests()`

---

#### 2. 视频预加载

**实现类**: `VideoPreloader`

**功能描述**: 提前加载即将显示的视频封面和数据

**技术方案**:
- 使用线程池 `Executors.newFixedThreadPool(2)` 异步执行预加载
- `LruCache` 缓存已预加载的位置，避免重复加载
- 预加载后续3个视频卡片的封面图

**核心代码路径**:
- `app/src/main/java/com/example/myapplication/adapter/preload/VideoPreloader.java`

---

#### 3. XML异步预加载

**实现类**: `LayoutPreloader`

**功能描述**: 在后台线程预加载布局，减少主线程压力

**技术方案**:
- 使用 `AsyncLayoutInflater` 异步inflate布局
- `SparseArray<Queue<View>>` 实现View缓存池，按layoutId分类
- 每种布局预加载2个备用
- 取用后自动补充预加载

**核心代码路径**:
- `app/src/main/java/com/example/myapplication/adapter/preload/LayoutPreloader.java`

---

#### 4. 卡片预渲染

**实现类**: `CardPrerenderer`

**功能描述**: 在空闲时提前触发即将显示的卡片渲染

**技术方案**:
- 检测滑动状态 `setScrolling(boolean)`
- 只在空闲时执行预渲染
- 延迟50ms确保滑动完全停止
- 预渲染后续5个item

**核心代码路径**:
- `app/src/main/java/com/example/myapplication/adapter/preload/CardPrerenderer.java`

---

#### 5. 预渲染时机控制

**功能描述**: 避免预渲染影响滑动流畅性

**技术方案**:
- `CardPrerenderer.setScrolling()` 跟踪滑动状态
- 滑动中延迟预渲染任务 `postDelayed(task, 100)`
- 滑动停止后才执行预渲染

---

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                           UI 层                                  │
├─────────────────────────────────────────────────────────────────┤
│  MainActivity                                                    │
│  ├── 页面控制                                                    │
│  ├── 滑动监听                                                    │
│  ├── 曝光检测 (checkExposure)                                    │
│  └── 视频自动播放控制 (handleVideoAutoPlay)                       │
│                                                                  │
│  NewsAdapter                                                     │
│  ├── 列表适配                                                    │
│  ├── 工厂模式分发                                                │
│  └── 布局预加载集成                                              │
│                                                                  │
│  ViewHolder 系列                                                 │
│  ├── BaseViewHolder (基类 + 自动播放接口)                        │
│  ├── VideoViewHolder (单列视频 + 自动播放)                       │
│  └── VideoGridViewHolder (双列视频 + 自动播放)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          数据层                                  │
├─────────────────────────────────────────────────────────────────┤
│  NewsRepository                                                  │
│  ├── 网络请求 (模拟)                                             │
│  ├── 缓存策略管理                                                │
│  └── 回调通知                                                    │
│                                                                  │
│  NewsCacheManager                                                │
│  ├── SharedPreferences 存储                                      │
│  ├── 缓存有效期判断                                              │
│  └── JSON 序列化/反序列化                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         预加载层                                 │
├─────────────────────────────────────────────────────────────────┤
│  LayoutPreloader          │ XML布局异步预加载                    │
│  CardPrerenderer          │ 卡片预渲染 + 时机控制                │
│  VideoPreloader           │ 视频封面预加载                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          工厂层                                  │
├─────────────────────────────────────────────────────────────────┤
│  CardViewHolderFactory (接口)                                    │
│  ├── TextCardFactory                                             │
│  ├── ThreeImagesCardFactory                                      │
│  ├── VideoCardFactory                                            │
│  ├── GridCardFactory                                             │
│  └── VideoGridCardFactory                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 核心类说明

| 类名 | 路径 | 职责 |
|------|------|------|
| MainActivity | MainActivity.java | 页面控制、滑动监听、曝光检测、视频自动播放 |
| NewsAdapter | adapter/NewsAdapter.java | 列表适配器、工厂模式集成、布局预加载集成 |
| BaseViewHolder | adapter/viewholder/BaseViewHolder.java | ViewHolder基类、自动播放接口定义 |
| VideoViewHolder | adapter/viewholder/VideoViewHolder.java | 单列视频卡片、自动播放实现 |
| VideoGridViewHolder | adapter/viewholder/VideoGridViewHolder.java | 双列视频卡片、自动播放实现 |
| CardViewHolderFactory | adapter/factory/CardViewHolderFactory.java | 卡片工厂接口 |
| LayoutPreloader | adapter/preload/LayoutPreloader.java | XML异步预加载 |
| CardPrerenderer | adapter/preload/CardPrerenderer.java | 卡片预渲染时机控制 |
| VideoPreloader | adapter/preload/VideoPreloader.java | 视频封面预加载 |
| NewsCacheManager | cache/NewsCacheManager.java | 本地缓存管理 |
| NewsRepository | repository/NewsRepository.java | 数据仓库、网络/缓存策略 |
| NewsBean | model/NewsBean.java | 新闻数据模型 |

---

## 依赖库

| 库名 | 版本 | 用途 |
|------|------|------|
| RecyclerView | 1.4.0 | 列表展示 |
| Glide | 4.15.1 | 图片加载和预加载 |
| Gson | 2.10.1 | JSON解析 |
| AsyncLayoutInflater | 1.0.0 | 异步布局加载 |
| SwipeRefreshLayout | 1.1.0 | 下拉刷新 |
| Material | 1.13.0 | TabLayout等UI组件 |

---

## 数据模型

### NewsBean

```java
public class NewsBean {
    // 卡片类型常量
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_SINGLE_IMAGE = 1;
    public static final int TYPE_THREE_IMAGES = 2;
    public static final int TYPE_VIDEO = 3;

    // 排版模式常量
    public static final int SPAN_SINGLE = 2;  // 单列（占满2格）
    public static final int SPAN_DOUBLE = 1;  // 双列（占1格）

    public int type;           // 卡片类型
    public int span;           // 排版模式
    public String title;       // 标题
    public String author;      // 作者
    public String comment;     // 评论数
    public List<String> images; // 图片列表
    public String duration;    // 视频时长
}
```

---

## 功能实现总结

| 功能 | 状态 | 说明 |
|------|------|------|
| 本地数据缓存 | ✅ 已实现 | 网络失败时使用缓存 |
| 视频自动播放 | ✅ 已实现 | 滑入播放，滑出停止 |
| 插件式卡片扩展 | ✅ 已实现 | Factory模式 |
| 图片预加载 | ✅ 已实现 | Glide预加载 |
| 视频预加载 | ✅ 已实现 | 封面图预加载 |
| XML异步预加载 | ✅ 已实现 | AsyncLayoutInflater |
| 卡片预渲染 | ✅ 已实现 | 空闲时预渲染 |
| 预渲染时机控制 | ✅ 已实现 | 避免影响滑动 |
