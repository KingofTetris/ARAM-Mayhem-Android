# ARAM Mayhem Assistant - Android客户端

> 海克斯大乱斗信息差助手 - Android 原生应用

[![GitHub](https://img.shields.io/badge/github-KingofTetris/ARAM--Mayhem--Android-blue?logo=github)
[![License](https://img.shields.io/badge/license-MIT-green)
[![Java Version](https://img.shields.io/badge/java-21-orange)
[![Min API](https://img.shields.io/badge/minSdk-26-green)

## 项目概述

**ARAM Mayhem Android** 是专为《英雄联盟》海克斯大乱斗模式玩家打造的移动端信息查询和社区分享App，提供英雄数据、符文搭配、玩法策略分享等功能。

### 主要特性

- 📊 **英雄数据查询**：完整英雄信息、强度分级(Tier)、英雄修饰效果
- 💎 **强化符文浏览**：Hextech ARAM 专属强化效果、最佳搭配
- 🎮 **玩法策略社区**：玩家分享出装、玩法策略、投票评价
- 📱 **原生流畅体验**：Java 21 + Hilt + MVVM架构，高性能
- 🔒 **安全认证**：JWT 登录、加密本地存储

## 技术栈

| 技术 | 版本/选型 | 用途 |
|------|----------|------|
| **编程语言** | Java 21 | 核心开发语言 |
| **UI框架** | XML Layout + ViewBinding + Material Design 3 | 界面构建 |
| **架构模式** | MVVM (ViewModel + LiveData + Repository | 代码组织 |
| **依赖注入** | Hilt 2.51.1 | DI容器 |
| **网络请求** | Retrofit 2.11.0 + OkHttp 4.12.0 | API通信 |
| **本地存储** | Room 2.6.1 + EncryptedSharedPreferences | 数据持久化 |
| **图片加载** | Glide 4.16.0 | 图片加载与缓存 |
| **路由导航** | Navigation Component 2.7.7 | 页面路由 |
| **Gradle** | 8.7 (AGP 8.5.2) | 构建工具 |

## 项目结构

本项目采用多模块架构设计，实现清晰的关注点分离：

```
ARAM_Mayhem_Assistant/
├── app/                        # 主应用模块
│   ├── src/main/
│   │   ├── java/com/aram/mayhem/
│   │   │   ├── MainActivity.java
│   │   │   └── MayhemApplication.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── menu/
│   │   │   ├── navigation/
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── core-common/              # 公共基础模块
│   └── src/main/java/com/aram/mayhem/common/
│       ├── Constants.java
│       ├── Result.java
│       └── Tier.java
├── core-network/             # 网络层模块
│   └── src/main/
├── core-data/               # 数据层模块
│   └── src/main/
├── core-ui/                  # UI组件库
│   └── src/main/
├── feature-hero/             # 英雄列表模块
│   └── src/main/
├── feature-augment/          # 强化符文模块
│   └── src/main/
├── feature-community/        # 玩法社区模块
│   └── src/main/
├── feature-profile/          # 个人资料模块
│   └── src/main/
├── feature-bulletin/         # 公告模块
│   └── src/main/
├── navigation/               # 导航配置
│   └── src/main/
├── gradle/wrapper/
├── build.gradle
├── gradle.properties
├── settings.gradle
└── local.properties (git忽略)
```

## 环境要求

### 开发环境

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **JDK**: 21 (项目已配置 Gradle JDK 21
- **Android SDK**:
  - compileSdk: 34
  - minSdk: 26 (Android 8.0)
  - targetSdk: 34
- **Gradle**: 8.7
- **Git**: 2.43.0+

### 测试设备/模拟器

- Android 8.0 (API 26) 或更高版本
- 建议使用物理设备测试性能

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/KingofTetris/ARAM-Mayhem-Android.git
cd ARAM-Mayhem-Android
```

### 2. 配置后端

本项目依赖后端API，请确保后端服务正常运行：

- 后端仓库：https://github.com/KingofTetris/ARAM-Mayhem-Server
- 默认API地址：`http://10.0.2.2:8080` (模拟器访问)
- 本地开发修改：`core-network/src/main/res/values/api_config.xml`

### 3. 构建项目

#### 使用 Android Studio

1. 打开 Android Studio
2. File → Open → 选择项目目录
3. 等待 Gradle 同步完成
4. 点击 "Sync Project with Gradle Files"
5. 连接设备或启动模拟器
6. 点击 Run 按钮 (▶️)

#### 使用命令行

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

### 4. APK 输出位置

```
app/build/outputs/apk/
├── debug/
│   └── app-debug.apk
└── release/
    └── app-release.apk
```

## 功能说明

### 已实现功能

- ✅ **多模块架构**：清晰的代码组织与关注点分离
- ✅ **底部导航**：5大功能入口
- ✅ **基础UI框架**：Material Design 3 设计风格
- ✅ **Hilt依赖注入**：完善的DI配置
- ✅ **公共工具**：Result、Tier枚举、常量定义

### 待实现功能

- ⏳ **英雄模块**：英雄列表、英雄详情、强度显示
- ⏳ **强化符文模块**：强化列表、分类筛选
- ⏳ **玩法社区**：发帖、查看、评论、投票
- ⏳ **用户系统**：注册/登录、个人资料
- ⏳ **公告系统**：系统公告展示
- ⏳ **本地缓存**：Room数据库实现
- ⏳ **网络层**：Retrofit + OkHttp 配置

## 开发指南

### 模块依赖关系

```
app (主应用)
├── core-common (所有模块依赖)
├── core-network (core-common)
├── core-data (core-common + core-network)
├── core-ui (core-common)
├── feature-* (core-common + core-ui + core-network + core-data)
└── navigation (core-common + core-ui + all features)
```

### 代码规范

1. **命名规范**
   - 类名：大驼峰 (PascalCase)
   - 方法/变量：小驼峰 (camelCase)
   - 资源：小写下划线 (snake_case)

2. **提交信息**
   - 遵循 Conventional Commits
   - `feat: 新增功能`
   - `fix: 修复bug`
   - `refactor: 代码重构`
   - `docs: 文档更新`

3. **架构原则**
   - UI层：只处理UI逻辑
   - ViewModel层：处理状态管理
   - Repository层：数据获取与处理
   - 网络/数据层：通过Repository访问

### 常见问题

**Q: Gradle 同步失败怎么办？**
A: 检查 JDK 版本是否为 21，清理缓存 `./gradlew clean 并重新同步

**Q: 如何修改API地址？**
A: 在 `core-network` 模块中配置 `ApiClient 或修改网络配置文件

## 贡献指南

我们欢迎所有形式的贡献！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某个功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2026 KingofTetris

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

## 致谢

- 拳头游戏 (Riot Games) - 《英雄联盟》
- 所有开源库作者

---

## 联系方式

- 项目问题：[GitHub Issues](https://github.com/KingofTetris/ARAM-Mayhem-Android/issues)
- 邮箱：1204066670@qq.com
- 关联项目：
  - [项目工作区](https://github.com/KingofTetris/ARAM-Mayhem-Assistant)
  - [后端服务](https://github.com/KingofTetris/ARAM-Mayhem-Server)
