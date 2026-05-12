# 悬浮时间应用 (TimePop)

一个可以在Android手机屏幕上悬浮显示实时时间的应用，精确到毫秒级。

## 功能特点

- 🎯 **实时显示**：每10毫秒刷新一次时间显示
- ⏱️ **毫秒精度**：时间格式为 HH:mm:ss.SSS（例如：14:35:26.123）
- 🖱️ **可拖动**：悬浮时间框可以自由拖动到屏幕任意位置
- 🔒 **后台运行**：作为前台服务运行，保持稳定显示
- 🎨 **高可见性**：绿色字体配黑色半透明背景，带阴影效果

## 使用方法

### 安装APK
1. 将 `app/build/outputs/apk/debug/app-debug.apk` 文件传输到Android手机
2. 在手机上安装APK（可能需要允许"安装未知来源应用"）
3. 打开应用

### 权限设置
首次使用时，应用会请求以下权限：
- **悬浮窗权限**：必需，用于在屏幕上层显示时间
- **通知权限**（Android 13+）：必需，用于保持服务在后台运行

### 操作说明
- 点击"开始"按钮启动悬浮时间显示
- 点击"停止"按钮停止悬浮显示
- 拖动悬浮时间框可调整位置
- 服务会在通知栏显示一个常驻通知

## 技术架构

### 核心组件
- **MainActivity**: 主界面，处理权限请求和启动/停止服务
- **FloatingTimeService**: 前台服务，负责创建和管理悬浮窗口

### 关键实现
1. **悬浮窗口**：使用 `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` 在系统窗口层显示
2. **实时更新**：使用 `Timer` 每10毫秒更新一次时间显示
3. **触摸拖动**：通过重写 `OnTouchListener` 实现悬浮框拖动
4. **前台服务**：使用 `startForeground()` 确保服务稳定运行

## 项目结构

```
time_pop/
├── app/
│   ├── src/main/
│   │   ├── java/com/timepop/floatingtime/
│   │   │   ├── MainActivity.java          # 主界面
│   │   │   └── FloatingTimeService.java   # 悬浮时间服务
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # 主界面布局
│   │   │   │   └── floating_time.xml       # 悬浮窗口布局
│   │   │   ├── drawable/                   # 图标资源
│   │   │   └── values/                     # 字符串资源
│   │   └── AndroidManifest.xml             # 应用清单
│   └── build.gradle                        # 应用构建配置
├── build.gradle                            # 项目构建配置
├── settings.gradle                         # 项目设置
├── gradle.properties                       # Gradle属性
└── local.properties                         # 本地配置（SDK路径）
```

## 构建说明

### 环境要求
- Android SDK (API Level 34)
- Gradle 9.5.0 或更高版本
- JDK 17

### 构建命令
```bash
# 配置环境变量
export ANDROID_HOME=~/android-sdk

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

### 输出位置
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## 配置说明

### 环境变量配置
项目根目录下的 `android_env.sh` 文件包含了环境变量配置：

```bash
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/cmdline-tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0
```

### SDK安装说明
如果本地没有Android SDK，需要先安装：

1. 下载 Android Command Line Tools
2. 安装必要的SDK组件：
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## 注意事项

1. **权限拒绝**：如果拒绝悬浮窗权限，应用无法显示悬浮时间
2. **电池优化**：建议将应用加入电池优化白名单，防止被系统杀死
3. **Android版本**：最低支持Android 8.0（API 26）
4. **性能考虑**：每10ms刷新一次时间，可能对电池续航有一定影响

## 许可证

本应用仅供学习和交流使用。
