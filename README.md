# APKExport

一款现代、轻量的 Android 应用导出与安装包管理工具。本仓库是 [leftshine/APKExport](https://github.com/leftshine/APKExport) 的持续维护版本。

## 功能

- 使用 Jetpack Compose 与 Material 3 重构界面，亮色和暗色主题跟随系统
- 导出、分享已安装应用，支持搜索、排序和批量操作
- 用户应用与系统应用混合展示，可分别筛选并记忆选择
- 扫描共享存储中的本地安装包，支持 APK、APKS、XAPK、APKM
- 查看、安装、分享、重命名和删除本地安装包
- 使用系统文件选择器设置导出位置
- 自定义导出文件名，支持应用名、包名、版本名和版本号占位符
- 支持简体中文、繁体中文和英文
- 内置应用更新检查与下载校验
- 适配 Android 16，Target SDK 37

## 界面

应用采用三个可点击、可左右滑动切换的底栏页面：

- **导出**：管理已安装的用户应用和系统应用
- **本地安装包**：扫描并管理共享存储中的安装包
- **设置**：导出位置、文件名格式、主题、更新、帮助与关于

首次进入“本地安装包”页面时，应用会申请“所有文件访问权限”，用于扫描共享存储中的完整安装包列表。用户需要在系统设置中手动授权。

## 构建

环境要求：

- Android Studio 或兼容的 Gradle 环境
- JDK 17
- Android SDK 37

```shell
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug
```

构建结果位于 `app/build/outputs/apk/`。

Release 签名信息从本地 `.signing/keystore.properties` 读取，签名文件与凭据不会纳入版本控制。

## 相关项目

- [APK 批量安装器](https://optool.daxiaamu.com/super_adb)：PC 端批量安装应用，支持 APK、XAPK、APKM、APKS 等多种格式

## 致谢

- 维护者：[大侠阿木](https://github.com/daxiaamu)
- 原作者及原项目：[leftshine/APKExport](https://github.com/leftshine/APKExport)

## 许可证

沿用原项目许可证，详见 [LICENSE](LICENSE)。
