# ARAM Mayhem Assistant (海克斯大乱斗信息差助手)

英雄联盟海克斯大乱斗（Hextech ARAM）信息差助手 Android APP。

## 技术栈

### Android 客户端
- **语言**: Java 21
- **UI**: XML Layout + ViewBinding + Material Design
- **架构**: MVVM (ViewModel + LiveData + Repository)
- **DI**: Hilt (Dagger)
- **网络**: Retrofit2 + OkHttp4 + Gson
- **本地存储**: Room Database + EncryptedSharedPreferences
- **图片**: Glide
- **路由**: Navigation Component

### 后端服务
- **语言**: Java 21
- **框架**: Spring Boot 3.3.x
- **ORM**: MyBatis Plus 3.5.x
- **安全**: Spring Security + JWT (jjwt)
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **构建**: Maven

## 项目结构

### Android (`D:\androidProjects\ARAM_Mayhem_Assistant\`)
```
├── app/                    主应用模块
├── core-common/            公共模块 (Constants, Result, Tier)
├── core-network/           网络层 (Retrofit, OkHttp)
├── core-data/              数据层 (Room, SharedPreferences)
├── core-ui/                通用 UI 组件
├── feature-hero/           英雄模块
├── feature-augment/        强化符文模块
├── feature-community/      玩法社区模块
├── feature-profile/        个人中心模块
├── feature-bulletin/       公告模块
└── navigation/             导航图
```

### 后端 (`D:\ideaProjects\aram-server\`)
```
src/main/java/com/aram/mayhem/
├── controller/    控制器层
├── service/       业务层
├── mapper/        MyBatis Mapper 接口
├── entity/        实体类
├── config/        配置类
├── security/      安全/JWT
└── common/        公共工具
```

## 环境要求

- JDK 21
- Android Studio (Hedgehog+)
- MySQL 8.0+
- Redis
- Maven 3.9+

## 本地部署

### 1. 启动后端
```bash
cd D:\ideaProjects\aram-server
start-server.bat
```

### 2. 构建 Android APK
```bash
cd D:\androidProjects\ARAM_Mayhem_Assistant
gradlew assembleDebug
```

### 3. 安装到手机
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
