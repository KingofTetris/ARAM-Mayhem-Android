# ARAM Mayhem Assistant - Android 客户端

> 海克斯大乱斗信息差助手 - Android 原生应用

[![GitHub](https://img.shields.io/badge/github-KingofTetris/ARAM--Mayhem--Android-blue?logo=github)
[![License](https://img.shields.io/badge/license-MIT-green)
[![Java Version](https://img.shields.io/badge/java-21-orange)

## 快速开始

### 环境要求

- JDK 21
- Android Studio Hedgehog (2023.1.1)+
- Android SDK 34 / minSdk 26

### 构建

```bash
# 同步依赖
./gradlew

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

### 运行

1. 打开 Android Studio
2. Open → 选择项目目录
3. 等待 Gradle 同步完成
4. Run → 选择设备

> 首次运行需配置 Android SDK（API 34）

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 21 |
| Gradle | 8.7 |
| AGP | 8.5.2 |
| Hilt | 2.51.1 |
| Retrofit | 2.11.0 |
| Room | 2.6.1 |
| Glide | 4.16.0 |
| Navigation | 2.7.7 |

## 项目结构

```
├── app/              # 主入口
├── core-common/      # 公共工具
├── core-network/     # 网络层
├── core-data/        # 数据层
├── core-ui/          # UI组件
├── feature-hero/     # 英雄模块
├── feature-augment/  # 符文模块
├── feature-community/# 社区模块
├── feature-profile/  # 个人中心
└── feature-bulletin/ # 公告模块
```

## 配套项目

- [后端服务](https://github.com/KingofTetris/aram-server) - Spring Boot API 服务
- [详细入门指南](./小白入门指南.md) - 新手完整安装配置教程

## 许可证

MIT

## 作者

[KingofTetris](https://github.com/KingofTetris)
