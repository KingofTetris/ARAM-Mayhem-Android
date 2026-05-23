# ARAM Mayhem Assistant 离线模式测试步骤文档

> 版本：1.0.0
> 编制日期：2026-05-22
> 适用范围：Android 客户端离线模式功能验证

---

## 一、离线架构概述

### 1.1 离线优先策略

应用采用 **"缓存优先 + 网络回退"** 策略：

```
用户请求数据
    ↓
步骤1：从 Room 本地缓存读取 → 有数据则立即展示
    ↓
步骤2：检查 isOffline 标记 → 离线则跳过网络请求
    ↓
步骤3：发起 Retrofit 网络请求
    ↓ 成功                    ↓ 失败
更新 UI + 写入 Room 缓存    保持缓存数据（步骤1已展示）
```

### 1.2 已实现离线检测的模块

| 模块 | Fragment | 离线检测 | 离线提示 | 自动恢复 | Room缓存 |
|------|----------|---------|---------|---------|---------|
| 英雄 | HeroListFragment | ✅ ConnectivityManager | ✅ Snackbar | ✅ retry() | ✅ HeroEntity |
| 符文 | AugmentListFragment | ✅ ConnectivityManager | ✅ Snackbar | ✅ retry() | ✅ AugmentEntity |
| 社区 | CommunityFeedFragment | ✅ ConnectivityManager | ✅ Snackbar | ✅ retry() | ✅ StrategyEntity |
| 公告 | BulletinListFragment | ✅ ConnectivityManager | ✅ Snackbar | ✅ loadBulletins | ✅ BulletinEntity |
| 个人 | ProfileFragment | ✅ ConnectivityManager | ✅ Snackbar | ✅ loadUserProfile | ❌ 无本地缓存 |

### 1.3 关键代码位置

| 功能 | 文件路径 |
|------|---------|
| 网络状态监听 | `feature-hero/.../HeroListFragment.java` → `setupOfflineDetection()` |
| 离线标记同步 | `feature-hero/.../HeroListViewModel.java` → `setOffline()` |
| 缓存优先逻辑 | `feature-hero/.../HeroRepository.java` → `getHeroes()` |
| Room 数据库 | `core-data/.../AppDatabase.java` → `aram_mayhem_db` |
| Migration 策略 | `core-data/.../AppDatabaseMigrations.java` → v1~v6 |

---

## 二、测试前准备

### 2.1 环境要求

- [x] Android 模拟器已启动并连接 ADB
- [x] 后端服务已启动（http://localhost:8080）
- [x] 最新 APK 已安装到模拟器
- [x] 应用已至少在线运行一次（确保 Room 缓存有数据）

### 2.2 安装最新 APK

```bash
cd D:\androidProjects\ARAM_Mayhem_Assistant
.\gradlew :app:assembleDebug --no-daemon
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2.3 确认缓存数据已写入

1. 启动应用：`adb shell am start -n com.aram.mayhem/.MainActivity`
2. 依次浏览：英雄列表 → 符文列表 → 社区 → 公告 → 个人
3. 等待每个页面数据加载完成（约 3~5 秒）
4. 确认 Room 缓存已有数据：
   ```bash
   adb shell "run-as com.aram.mayhem sqlite3 /data/data/com.aram.mayhem/databases/aram_mayhem_db 'SELECT COUNT(*) FROM heroes;'"
   ```

---

## 三、离线模式测试步骤

> ⚠️ **重要提示**：不要使用 `adb shell svc wifi disable` 关闭模拟器 WiFi，这会导致 ADB 连接断开！
> 请使用模拟器内部的飞行模式开关来模拟离线。

### 3.1 方法一：模拟器飞行模式（推荐）

1. 在模拟器中，下拉通知栏
2. 点击 **飞行模式** 图标开启
3. 等待状态栏出现飞机图标
4. 开始执行测试用例
5. 测试完成后，再次点击飞行模式图标关闭

### 3.2 方法二：关闭后端服务

1. 在主机上停止后端服务（关闭 Spring Boot 进程）
2. 应用网络请求会超时失败，模拟离线效果
3. 此方法不会影响 ADB 连接
4. 测试完成后重新启动后端

### 3.3 方法三：真机测试

1. 在真机上安装 APK
2. 开启飞行模式
3. 执行测试用例
4. 关闭飞行模式验证恢复

---

## 四、测试用例

### TC-001：英雄列表 - 离线缓存展示

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览英雄列表，Room 缓存有数据 |
| 测试步骤 | 1. 开启飞行模式<br>2. 切换到英雄列表页<br>3. 观察页面展示 |
| 预期结果 | ✅ 显示之前缓存的英雄列表数据<br>✅ 底部出现 Snackbar："网络已断开，正在显示缓存数据" |
| 实际结果 | （待填写） |

### TC-002：英雄列表 - 下拉刷新离线

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启 |
| 测试步骤 | 1. 在英雄列表页下拉刷新<br>2. 观察页面行为 |
| 预期结果 | ✅ 保持显示缓存数据<br>✅ 不会出现崩溃或空白页面<br>✅ 网络请求失败后静默回退到缓存 |
| 实际结果 | （待填写） |

### TC-003：英雄搜索 - 离线搜索

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启，缓存有数据 |
| 测试步骤 | 1. 在搜索框输入英雄名称（如"亚托克斯"）<br>2. 观察搜索结果 |
| 预期结果 | ✅ 从本地缓存中筛选匹配的英雄<br>✅ 搜索结果即时展示 |
| 实际结果 | （待填写） |

### TC-004：符文列表 - 离线缓存展示

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览符文列表 |
| 测试步骤 | 1. 开启飞行模式<br>2. 切换到符文列表页<br>3. 观察页面展示 |
| 预期结果 | ✅ 显示之前缓存的符文列表数据<br>✅ 底部出现 Snackbar："网络已断开，正在显示缓存数据" |
| 实际结果 | （待填写） |

### TC-005：符文品质筛选 - 离线筛选

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启，缓存有数据 |
| 测试步骤 | 1. 点击品质筛选 Tab（如"棱彩"）<br>2. 观察筛选结果 |
| 预期结果 | ✅ 从本地缓存中筛选对应品质的符文<br>✅ 筛选结果即时展示 |
| 实际结果 | （待填写） |

### TC-006：社区玩法 - 离线缓存展示

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览社区攻略，Room 缓存有数据 |
| 测试步骤 | 1. 开启飞行模式<br>2. 切换到社区玩法页<br>3. 观察页面展示 |
| 预期结果 | ✅ 显示之前缓存的攻略列表数据<br>✅ 底部出现 Snackbar："网络已断开，正在显示缓存数据" |
| 实际结果 | （待填写） |

### TC-007：公告 - 离线缓存展示

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览公告列表，Room 缓存有数据 |
| 测试步骤 | 1. 开启飞行模式<br>2. 切换到公告页<br>3. 观察页面展示 |
| 预期结果 | ✅ 显示之前缓存的公告列表数据<br>✅ 底部出现 Snackbar："网络已断开，正在显示缓存数据" |
| 实际结果 | （待填写） |

### TC-008：个人中心 - 离线提示

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启 |
| 测试步骤 | 1. 切换到个人中心页<br>2. 观察页面展示 |
| 预期结果 | ✅ 底部出现 Snackbar："网络已断开，部分功能不可用"<br>⚠️ 个人中心无本地缓存，已登录用户资料保持显示 |
| 实际结果 | （待填写） |

---

## 五、网络恢复测试

### TC-009：英雄列表 - 网络恢复自动刷新

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启，英雄列表显示缓存数据 |
| 测试步骤 | 1. 关闭飞行模式<br>2. 观察英雄列表页行为 |
| 预期结果 | ✅ 底部出现 Snackbar："网络已恢复，正在刷新数据"<br>✅ 列表自动刷新为最新数据<br>✅ Room 缓存同步更新 |
| 实际结果 | （待填写） |

### TC-010：符文列表 - 网络恢复自动刷新

| 项目 | 内容 |
|------|------|
| 前置条件 | 飞行模式已开启，符文列表显示缓存数据 |
| 测试步骤 | 1. 关闭飞行模式<br>2. 观察符文列表页行为 |
| 预期结果 | ✅ 底部出现 Snackbar："网络已恢复"<br>✅ 列表自动刷新为最新数据 |
| 实际结果 | （待填写） |

---

## 六、数据持久化测试

### TC-011：应用重启 - 英雄数据保留

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览英雄列表，缓存有数据 |
| 测试步骤 | 1. 完全关闭应用：`adb shell am force-stop com.aram.mayhem`<br>2. 重新启动应用：`adb shell am start -n com.aram.mayhem/.MainActivity`<br>3. 切换到英雄列表页 |
| 预期结果 | ✅ 英雄列表立即显示缓存数据（无需等待网络请求）<br>✅ 数据与关闭前一致 |
| 实际结果 | （待填写） |

### TC-012：应用重启 - 符文数据保留

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态已浏览符文列表 |
| 测试步骤 | 1. 完全关闭应用<br>2. 重新启动应用<br>3. 切换到符文列表页 |
| 预期结果 | ✅ 符文列表立即显示缓存数据<br>✅ 数据与关闭前一致 |
| 实际结果 | （待填写） |

### TC-013：数据库升级 - Migration 不丢数据

| 项目 | 内容 |
|------|------|
| 前置条件 | Room 数据库版本为 v5 |
| 测试步骤 | 1. 确认数据库版本：<br>`adb shell "run-as com.aram.mayhem sqlite3 /data/data/com.aram.mayhem/databases/aram_mayhem_db 'PRAGMA user_version;'"`<br>2. 确认数据完整：<br>`adb shell "run-as com.aram.mayhem sqlite3 /data/data/com.aram.mayhem/databases/aram_mayhem_db 'SELECT COUNT(*) FROM heroes;'"` |
| 预期结果 | ✅ 数据库版本为 5<br>✅ heroes 表有数据<br>✅ augments 表有数据<br>✅ 未使用 fallbackToDestructiveMigration |
| 实际结果 | （待填写） |

---

## 七、边界场景测试

### TC-014：首次启动离线 - 无缓存数据

| 项目 | 内容 |
|------|------|
| 前置条件 | 应用全新安装，从未在线运行过 |
| 测试步骤 | 1. 卸载旧版：`adb uninstall com.aram.mayhem`<br>2. 安装新版：`adb install app-debug.apk`<br>3. 开启飞行模式<br>4. 启动应用 |
| 预期结果 | ✅ 应用不崩溃<br>⚠️ 英雄/符文列表显示空状态<br>📝 应显示友好的"暂无数据"提示 |
| 实际结果 | （待填写） |

### TC-015：网络请求超时

| 项目 | 内容 |
|------|------|
| 前置条件 | 在线状态，后端服务已停止 |
| 测试步骤 | 1. 关闭后端服务<br>2. 在应用中下拉刷新英雄列表<br>3. 等待网络请求超时 |
| 预期结果 | ✅ 保持显示缓存数据<br>✅ 不崩溃<br>⚠️ 可能显示错误提示 |
| 实际结果 | （待填写） |

### TC-016：弱网环境

| 项目 | 内容 |
|------|------|
| 前置条件 | 网络连接不稳定 |
| 测试步骤 | 1. 使用模拟器网络限速功能（Settings → Network → Network Type → EDGE）<br>2. 浏览各页面 |
| 预期结果 | ✅ 先展示缓存数据<br>✅ 网络请求完成后更新<br>✅ 不崩溃 |
| 实际结果 | （待填写） |

---

## 八、单元测试覆盖情况

以下离线逻辑已通过单元测试验证：

| 测试类 | 测试数量 | 覆盖内容 |
|--------|---------|---------|
| HeroRepositoryTest | 13 | URL 拼接、Entity 映射、离线缓存逻辑 |
| HeroListViewModelTest | 6+ | setOffline 同步、retry 恢复 |
| AugmentViewModelTest | 4+ | 离线状态 LiveData |
| AppDatabasePersistenceTest | 11 | Entity 持久化、Migration 版本号、默认值 |
| MainActivityTest | 4 | Activity 启动、布局完整性 |

运行全部单元测试：
```bash
.\gradlew test --no-daemon
```

运行 UI 测试（需设备连接）：
```bash
.\gradlew :app:connectedDebugAndroidTest --no-daemon
```

---

## 九、已知限制与改进建议

### 已知限制

1. **个人中心无本地缓存**：ProfileFragment 有离线检测但无 Room 缓存，离线时无法展示用户资料
2. **离线搜索依赖缓存**：搜索功能在离线时只能搜索已缓存的英雄，未缓存的英雄无法搜到
3. **社区攻略详情无缓存**：仅列表数据写入 Room，攻略详情页仍需网络

### 改进建议

| 优先级 | 改进项 | 涉及模块 |
|--------|--------|---------|
| P1 | ProfileEntity 写入 Room 缓存 | core-data + feature-profile |
| P1 | StrategyDetailEntity 写入 Room 缓存 | core-data + feature-community |
| P2 | 统一离线检测到 BaseFragment | core-ui |
| P2 | 首次离线启动显示友好空状态 | 全模块 |

---

> 文档编制：项目开发团队
> 最后更新：2026-05-22
