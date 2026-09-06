# 严厉教练（LifeCoach）

一个原生 Kotlin + Jetpack Compose 的单机 Android 生活教练。它只让你回答教练卡片：回答自动入库，错过的问题下次打开自动补发。

## 在 GitHub 自动生成 APK

1. 新建 GitHub 仓库，把本目录所有文件推送到 `main`。
2. 打开仓库的 **Actions → Build APK → Run workflow**（或直接 push）。
3. 构建结束后下载 `lifecoach-debug-apk` artifact，解压并安装 `app-debug.apk`。

本工程使用 Android Gradle Plugin 8.9.2、Gradle 8.11.1、JDK 17、compile/target SDK 35，min SDK 26。GitHub Actions 会自动准备 Gradle；本地 Android Studio 直接打开工程即可。

## 第一次使用

在“设置”粘贴 DeepSeek API Key 并保存。没有 Key、断网、接口失败时仍可回答卡片、记录任务、看数据，AI 回复自动降级为本地短文案。Key 使用 Android Keystore 加密保存。

在华为/荣耀手机请按“通知与后台”提示开启自启动、后台活动，并锁定最近任务。应用同时使用精确闹钟、前台常驻通知和 WorkManager 30 分钟补发扫描；通知被系统拦截时，打开首页仍会按时序补回未回答卡片。

## 已实现

- 四个底部页面：今天、教练、数据、设置
- 六类问题卡：NUMBER、YESNO、SCALE、TEXT、CHOICE、MESSAGE；最多展示三张，可跳过并记录
- 每日三件事（最多三条）、完成切换、顺延次数和高风险标色
- catchUp 补发：今天及昨天、过期规则、唯一 slot 防重复
- DeepSeek `/chat/completions`，`deepseek-chat`，300 token 上限，离线降级
- AlarmManager 链式提醒、开机重排、前台服务、WorkManager 兜底
- 抽烟/完成率趋势、出门记录、睡眠×精神有效点计数
- JSON 导出、暗色 Material 3、大按钮和短震动友好的布局

真机通知权限、厂商电池策略和 DeepSeek 账户余额仍需在你的设备上验证；这些由 Android 系统和账户状态决定。

## 本地构建

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

若设备没有 Android SDK，请直接使用 GitHub Actions；Actions 会使用官方 Gradle 8.11.1 和 JDK 17，并输出可安装的 debug APK。
