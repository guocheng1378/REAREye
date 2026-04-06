# REAREye 新组件开发计划

## ✅ 已完成

### 1. 卡片动画增强 (Card Motion Enhancements)
- [x] 滑动删除手势 — `SwipeDismissCard.kt`
- [x] 拖拽排序状态 — `DragReorderState.kt`
- [ ] 待集成到 CardManagerScreen（可选：已有 ArtStaggeredReveal 动画）

### 2. 模板市场 (Template Market)
- [x] 预置模板定义 — `RearTemplatePresets.kt` (8个模板: 时钟×2, 电量, 自定义文字, 步数, 天气, 音乐, 系统信息)
- [x] TemplateMarketScreen UI — `TemplateMarketScreen.kt`
- [x] 一键导入功能（写文件 + 注册Business + 创建Card）
- [x] 分类筛选 (Chip)
- [x] 配置集成 — ConfigRoute + ConfigType.Manager

### 3. 通知镜像 (Notification Mirror)
- [x] NotificationMirrorHook — `NotificationMirrorHook.kt`
- [x] Hook 拦截 addNotification / removeNotification
- [x] 应用过滤（allowlist/blocklist）
- [x] 通过 RearWidgetApiClient.postNotice 转发到副屏
- [x] 配置 UI — `NotificationMirrorConfigScreen.kt`
- [x] 集成到 SubscreenCenterScope

### 4. 系统状态组件 (System Status Widget)
- [x] 系统信息采集 Hook — `SystemStatusHook.kt`
- [x] 电池: level/status/temperature/health
- [x] 内存: used%/totalMB/availMB (从 /proc/meminfo 读取)
- [x] 存储: used%/totalGB/availGB (StatFs)
- [x] 运行时间: hours/minutes
- [x] CPU 使用率: 从 /proc/stat 读取
- [x] 30秒定时刷新 + 电池变化即时更新
- [x] 存储到 SharedPreferences (reareye_system_status)
- [x] 集成到 SubscreenCenterScope

### 5. 音乐可视化 (Music Visualizer)
- [x] 音频捕获 Hook — `MusicVisualizerHook.kt`
- [x] Hook AudioTrack.write (ByteArray/ShortArray/FloatArray)
- [x] 频谱分析（能量分布近似）
- [x] RMS + Peak 电平计算
- [x] 可配置频段数 (8/16/32)
- [x] 20FPS 定时发布到 SharedPreferences
- [x] 集成到 SubscreenCenterScope

## 集成清单
- [x] SubscreenCenterScope — 添加 3 个新 Hook
- [x] ConfigKeys — 添加 10+ 个新配置键
- [x] ConfigType.ManagerType — 添加 TEMPLATE_MARKET, NOTIFICATION_MIRROR
- [x] REAREyeConfig — 添加新配置分类
- [x] ConfigScreen — 添加新路由 (TemplateMarket, NotificationMirrorConfig)
- [x] strings.xml — 英文字符串
- [x] strings.xml (zh-rCN) — 中文翻译

## 文件清单

### 新增文件
```
app/src/main/java/hk/uwu/reareye/
├── hook/scopes/subscreencenter/modules/
│   ├── NotificationMirrorHook.kt     (9.3KB)
│   ├── SystemStatusHook.kt           (11.1KB)
│   └── MusicVisualizerHook.kt        (8.9KB)
├── repository/rearwidget/
│   └── RearTemplatePresets.kt        (14.4KB)
└── ui/components/
    ├── config/
    │   ├── TemplateMarketScreen.kt    (17.6KB)
    │   └── NotificationMirrorConfigScreen.kt (9.3KB)
    └── motion/
        ├── SwipeDismissCard.kt        (5.1KB)
        └── DragReorderState.kt        (4.2KB)
```

### 修改文件
```
├── hook/scopes/subscreencenter/SubscreenCenterScope.kt
├── ui/config/Config.kt
├── ui/config/ModuleConfig.kt
├── ui/screen/ConfigScreen.kt
├── res/values/strings.xml
└── res/values-zh-rCN/strings.xml
```
