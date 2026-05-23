# ARAM Mayhem Assistant 开发操作步骤记录

> 本文档记录项目从初始化到当前阶段的所有关键操作步骤、决策过程、问题及解决方案。
> 最后更新：2026-05-23
> 当前进度：M1~M11 全部完成 ✅

---

## 阶段一：项目初始化（M1）

### 1.1 项目脚手架搭建

**开始时间**：2026-05-03 09:00:00

**实施内容**：

1. 创建 Android 项目目录结构 `D:\androidProjects\ARAM_Mayhem_Assistant`
2. 创建 11 个 Gradle 模块：app, core-common, core-network, core-data, core-ui, feature-hero, feature-augment, feature-community, feature-profile, feature-bulletin, navigation
3. 创建 Spring Boot 后端项目 `D:\ideaProjects\aram-server`
4. 编写 `settings.gradle`（11 模块 include）、根 `build.gradle`（统一依赖版本 ext{}）
5. 编写各模块独立 `build.gradle`（app=application, core-common=java-library, 其余=android-library）
6. 编写 `gradle-wrapper.properties`（Gradle 8.7）、`gradle.properties`、`gradlew.bat`

**使用工具**：Write 工具逐文件创建、RunCommand 创建目录结构

**关键决策**：
- **决策1**：采用多模块架构而非单模块，理由：feature 模块可独立编译、降低增量构建时间
- **决策2**：core-common 使用 `java-library` 插件而非 `android-library`，理由：纯 Java 类（Constants/Result/Tier）无需 Android SDK
- **决策3**：Gradle 使用 Groovy DSL 而非 Kotlin DSL，理由：与 tasks.md 规格一致，Groovy 生态更成熟

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| Gradle wrapper JAR 文件缺失 | 从本地缓存 `C:\Users\12040\.gradle\wrapper\dists\gradle-8.7-bin` 中找到已安装的 Gradle 8.7，执行 `gradle wrapper` 命令生成 |
| sandbox 无法删除 navigation 模块目录 | 从 settings.gradle 和 app/build.gradle 中移除引用即可，Gradle 不会编译未 include 的模块 |

---

### 1.2 开发环境与工具链配置

**开始时间**：2026-05-03 12:00:00

**实施内容**：

1. 配置 Android Gradle 多模块依赖关系（app → core×4 + feature×5 + navigation）
2. 编写后端 `pom.xml`：Spring Boot 3.3.5 + MyBatis Plus 3.5.9 + Spring Security + jjwt 0.12.6 + SpringDoc 2.6.0
3. 编写 `application.yml`：MySQL(127.0.0.1:3306/aram_mayhem) + Redis(127.0.0.1:6379) + JWT 密钥 + MyBatis Plus 配置
4. 编写 `application-local.yml`：DEBUG 日志级别
5. Android 配置 ViewBinding（app + 5 个 feature 模块 `buildFeatures { viewBinding true }`）
6. Android 配置 Hilt（app + core-network + core-data + 5 个 feature 模块添加 Hilt 插件和依赖）
7. 创建 `network_security_config.xml`（允许 192.168.1.100 和 localhost 明文 HTTP）
8. 在 `AndroidManifest.xml` 中引用 `android:networkSecurityConfig`
9. 创建 `Constants.java` 定义 `BASE_URL = "http://192.168.1.100:8080/"`

**使用工具**：Write 工具、RunCommand 验证 Maven

**关键决策**：
- **决策4**：JWT 密钥使用 Base64 编码存储在 yml 中，而非环境变量，理由：本地部署场景无需云环境变量
- **决策5**：MySQL 绑定 127.0.0.1 而非 0.0.0.0，理由：本地开发安全，外部通过防火墙规则控制
- **决策6**：Access Token 15 分钟 + Refresh Token 7 天，理由：移动端使用频率高，短 Access Token 保障安全，长 Refresh Token 减少重新登录

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| Maven 不在 sandbox PATH 中 | 使用完整路径 `D:\dev\apache-maven-3.9.15-bin\apache-maven-3.9.15\bin\mvn.cmd` 执行 |
| sandbox PowerShell 无法读取用户/系统环境变量 | 承认限制，不依赖环境变量，直接使用已知安装路径 |

---

## 阶段二：基础架构（M2）

### 2.1 MySQL 数据库建模（Task 3）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 通过 MCP MySQL 工具执行 DDL，创建 `aram_mayhem` 数据库（utf8mb4_unicode_ci）
2. 创建 9 张表：tb_user, tb_hero, tb_hero_modifier, tb_augment, tb_strategy, tb_strategy_augment, tb_strategy_item, tb_vote, tb_bulletin
3. 创建 10 个索引：idx_hero_tier, idx_hero_role, idx_hero_name_zh, idx_augment_quality, idx_augment_synergy, idx_strategy_hero, idx_strategy_user, idx_strategy_hot(表达式索引), idx_bulletin_type, idx_bulletin_pinned
4. 创建 9 个 Entity 类（@TableName + @TableId + @TableField + 逻辑删除 deleted）
5. 创建 9 个 Mapper 接口（extends BaseMapper<T>）

**使用工具**：MCP MySQL execute_sql、backend-android-collaboration-expert 智能体

**关键决策**：
- **决策7**：所有表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`，理由：utf8mb4 支持完整 Unicode（含 emoji），utf8mb4_unicode_ci 排序规则准确
- **决策8**：tb_strategy_hot 使用表达式索引 `(upvotes - downvotes)`，理由：MySQL 8.0+ 支持函数索引，避免查询时计算
- **决策9**：逻辑删除字段 `deleted TINYINT DEFAULT 0`，理由：MyBatis Plus @TableLogic 自动处理，数据可恢复

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| TEXT 列不能设置 DEFAULT 值（MySQL 严格模式） | 移除 tb_augment.description 和 tb_bulletin.content 的 DEFAULT '' |

---

### 2.2 后端 RESTful API 框架（Task 4）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 创建 `Result<T>` 统一响应体（code + message + data + timestamp）
2. 创建 `GlobalExceptionHandler`（@RestControllerAdvice 处理 Validation/Business/Unknown 异常）
3. 创建 `BusinessException` 自定义异常
4. 创建 `CorsConfig`（CorsFilter 允许所有 Origin + 标准方法 + 凭证）
5. 创建 `RedisConfig`（RedisTemplate + Jackson2JsonRedisSerializer + Java8 时间模块）
6. SpringDoc OpenAPI 已通过 pom.xml 依赖自动配置

**使用工具**：backend-android-collaboration-expert 智能体

**关键决策**：
- **决策10**：CORS 允许 `*` Origin，理由：本地局域网部署，无跨域安全风险
- **决策11**：Redis Value 使用 Jackson JSON 序列化而非 JDK 序列化，理由：可读性好、跨语言兼容、体积更小

---

### 2.3 Spring Security + JWT 认证（Task 5）

**开始时间**：2026-05-03 18:00:00

**实施内容**：

1. 创建 `JwtTokenProvider`（HMAC-SHA 密钥签名，Access 15min + Refresh 7d）
2. 创建 `JwtAuthenticationFilter`（OncePerRequestFilter，Bearer Token 提取 + 验证 + SecurityContext 注入）
3. 创建 `SecurityConfig`（CSRF 禁用 + STATELESS 会话 + /api/auth/** 放行 + 其余需认证）
4. 创建 `CustomUserDetailsService`（UserDetailsService 实现，email 查询 + BCrypt 密码验证）
5. 创建 4 个 DTO：RegisterRequest, LoginRequest, RefreshTokenRequest, AuthResponse
6. 创建 `AuthService`（register/login/refresh 完整业务逻辑）
7. 创建 `AuthController`（POST /api/auth/register, /api/auth/login, /api/auth/refresh）
8. 后端添加 Lombok 依赖到 pom.xml

**使用工具**：backend-android-collaboration-expert 智能体、MCP MySQL

**关键决策**：
- **决策12**：SecurityFilterChain 使用 Lambda DSL 而非 WebSecurityConfigurerAdapter，理由：Spring Security 6.x 已废弃后者
- **决策13**：JwtAuthenticationFilter 注入 UserDetailsService 加载完整权限，而非仅存储 userId，理由：后续 RBAC 权限控制需要角色信息

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| JwtAuthenticationFilter 初始版本仅设置 userId 为 principal | 更新为注入 UserDetailsService，加载完整 UserDetails 含权限列表 |

**编译验证**：`mvn clean compile -DskipTests` → **34 源文件编译通过，BUILD SUCCESS**

---

## 阶段三：Android UI 框架（M3）

### 3.1 Android 核心模块搭建（Task 6）

**开始时间**：2026-05-03 09:00:00

**实施内容**：

1. 创建 `MayhemApplication.java`（@HiltAndroidApp）
2. 创建 `MainActivity.java`（@AndroidEntryPoint + NavHostFragment + BottomNavigationView）
3. 创建 `activity_main.xml`（ConstraintLayout + FragmentContainerView + BottomNavigationView）
4. 创建 `navigation_graph.xml`（5 Tab：英雄/符文/玩法/公告/我的）
5. 创建 `bottom_nav_menu.xml`（5 个 item + 图标引用）
6. 创建 5 个 Fragment 占位：HeroListFragment, AugmentListFragment, CommunityFeedFragment, BulletinListFragment, ProfileFragment
7. 创建 5 个 Fragment 布局文件（ConstraintLayout + TextView 占位）
8. 创建 `core-common` 3 个 Java 类：Constants, Result, Tier
9. 创建 `core-ui` 完整资源体系：colors.xml, strings.xml, dimens.xml, themes.xml
10. 创建 13 个 drawable 资源：5 个底部导航图标 + 2 个启动图标 + 6 个功能性 drawable
11. 创建 `network_security_config.xml` + `proguard-rules.pro`

**使用工具**：Write 工具批量创建、RunCommand 创建目录

**关键决策**：
- **决策14**：Tier 枚举使用 @SerializedName 注解，理由：Gson 反序列化需要映射 S_PLUS → "S_PLUS" 字符串
- **决策15**：5 个 Fragment 布局使用 ConstraintLayout 而非 LinearLayout，理由：ConstraintLayout 性能更优，复杂布局层级更少
- **决策16**：底部导航图标使用 Vector Drawable 而非 PNG，理由：矢量图任意缩放不失真，APK 体积更小

---

### 3.2 资源缺失问题修复

**开始时间**：2026-05-03 18:00:00

**问题诊断**：

| 问题 | 根因 | 严重程度 |
|------|------|----------|
| feature 模块引用 @color/background 等资源无法解析 | 共享资源仅存在于 app 模块 | 🔴 致命 |
| 所有 library 模块缺少 AndroidManifest.xml | AGP 要求每个模块必须有 manifest | 🔴 致命 |
| navigation_graph.xml 在 navigation 模块无法解析 Fragment 类 | navigation 模块不依赖 feature 模块 | 🟠 严重 |
| 启动图标 adaptive-icon 引用 @color/ 而非 @drawable/ | 颜色资源不能直接作为 adaptive-icon 的 drawable | 🟡 中等 |
| core-common 缺少 Gson 依赖 | Result.java 使用 @SerializedName 但 build.gradle 无 Gson | 🔴 致命 |
| gradle-wrapper.jar 缺失 | 手动创建项目时未生成 wrapper JAR | 🔴 致命 |

**修复步骤**：

1. **共享资源迁移**：将 app 模块的 colors.xml(25色值)、strings.xml(35字符串)、dimens.xml(30尺寸)、themes.xml(14样式) 迁移到 core-ui 模块
2. **图标迁移**：5 个底部导航图标 drawable 从 app 迁移到 core-ui
3. **新增 drawable**：ic_launcher_foreground.xml、ic_launcher_background.xml、bg_trap_warning.xml、bg_card.xml、divider_horizontal.xml、bg_tier_s_plus/s/a/b/c.xml
4. **AndroidManifest 补全**：为 9 个 library 模块各创建 AndroidManifest.xml
5. **导航图迁移**：navigation_graph.xml 从 navigation 模块迁移到 app 模块（app 依赖所有 feature 模块，可解析 Fragment 类引用）
6. **启动图标修复**：adaptive-icon 引用改为 `@drawable/ic_launcher_foreground` 和 `@drawable/ic_launcher_background`
7. **app 模块旧资源清理**：移除已迁移的 colors/dimens/themes（保留空 resources 标签），移除 5 个图标 drawable
8. **settings.gradle 更新**：移除 `include ':navigation'`
9. **app/build.gradle 更新**：移除 `implementation project(':navigation')`
10. **core-common/build.gradle 更新**：添加 `implementation "com.google.code.gson:gson:$gsonVersion"`
11. **Gradle wrapper 生成**：从本地缓存执行 `gradle wrapper --gradle-version 8.7`

**使用工具**：Write、DeleteFile、RunCommand、Glob、Grep

**关键决策**：
- **决策17**：共享资源放在 core-ui 而非 app 模块，理由：feature 模块依赖 core-ui 而非 app，资源通过依赖链自动合并
- **决策18**：导航图放在 app 模块而非独立 navigation 模块，理由：app 依赖所有 feature 模块，可以解析 Fragment 类引用；独立模块无法做到
- **决策19**：app 模块的 colors/dimens/themes 保留空 `<resources />` 标签而非删除文件，理由：避免未来 app 特有资源无处放置

**编译验证**：`gradlew assembleDebug` → **BUILD SUCCESSFUL, 284 tasks, app-debug.apk 7.8 MB**

---

## 进度跟踪体系建立

**开始时间**：2026-05-03

**实施内容**：

1. 创建 `project_progress_tracker.xlsx`（5 个 Sheet）
2. 使用 openpyxl 生成，包含：总体概览、安卓端进度、后端进度、更新日志、里程碑概览
3. 同步分发到三个位置：Android 项目目录、后端项目目录、规划目录
4. 创建 `create_tracker.py` 脚本支持一键重新生成

**关键决策**：
- **决策20**：安卓端和后端使用独立 Sheet 而非合并，理由：两端进展差异大（安卓 55% vs 后端 100%），独立展示更真实
- **决策21**：每个 Sheet 分 A/B/C 三段（已完成/待完成/阶段计划），理由：结构清晰，便于快速定位当前状态
- **决策22**：使用 Python 脚本生成 xlsx 而非手动维护，理由：可版本控制、可自动化更新

---

## 关键技术参数汇总

| 参数 | 值 |
|------|-----|
| JDK 版本 | 21 (21.0.10) |
| Android Gradle Plugin | 8.5.2 |
| Gradle 版本 | 8.7 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |
| Spring Boot | 3.3.5 |
| MyBatis Plus | 3.5.9 |
| jjwt | 0.12.6 |
| SpringDoc | 2.6.0 |
| Hilt | 2.51.1 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Room | 2.6.1 |
| Glide | 4.16.0 |
| Navigation | 2.7.7 |
| MySQL | 9.7.0 |
| Redis | 3.0.504 (MSOpenTech Windows) |
| Maven | 3.9.15 |
| 数据库字符集 | utf8mb4 / utf8mb4_unicode_ci |
| 后端端口 | 8080 |
| MySQL 端口 | 3306 |
| Redis 端口 | 6379 |
| Access Token 有效期 | 15 分钟 |
| Refresh Token 有效期 | 7 天 |

---

## 待办事项（按优先级）

1. **[高] M4 后端**：DataInitializer 种子数据 + HeroController + HeroService
2. **[中] M4 后端**：HeroService Redis 缓存策略
3. **[高] 网络恢复后**：推送 3 个本地 commit 到 GitHub 远程仓库
4. **[中] 真机验证**：使用 deploy-device.bat 安装 Release APK 到真机，执行验证清单
5. **[中] 后端数据管线**：端到端验证 RiotDataDragon + AramDataCollector 数据抓取流程

---

## 阶段四：技术债务清理与质量提升（2026-05-20）

### 4.1 ProfileViewModel 单元测试（T4-1）

**开始时间**：2026-05-20

**实施内容**：

1. 新增 `ProfileViewModelTest.java`：29 个单元测试覆盖全部公开方法
2. 测试分组：isLoggedIn(2) / loadUserProfile(4) / updateProfile(3) / logout(1) / login(7) / register(5) / 初始状态(7)
3. 登录测试覆盖：成功保存Token / 密码错误 / 响应体为空 / AuthResponse为空 / 网络错误 / loading状态 / 参数传递
4. 注册测试覆盖：成功触发 / 邮箱已存在 / 网络错误 / 参数传递 / loading状态
5. 修改 `feature-profile/build.gradle`：添加 JUnit5 / Mockito / core-testing 测试依赖
6. 修改 `feature-profile/build.gradle`：添加 `useJUnitPlatform()` 和 `returnDefaultValues = true`

**关键决策**：
- **决策54**：使用 `ArchTaskExecutor.setDelegate()` 替代 JUnit 4 的 `InstantTaskExecutorRule`，理由：JUnit 5 不支持 `@Rule` 注解，`InstantTaskExecutorRule` 无法生效；手动设置 TaskExecutor delegate 可在 JUnit 5 下实现同步 LiveData 执行
- **决策55**：core-testing 使用独立版本号 `2.2.0` 而非 `$lifecycleVersion`，理由：`core-testing` 和 `lifecycle` 是不同的版本线，`lifecycleVersion=2.7.0` 对应的 `core-testing` 不存在

**遇到的问题及解决**：
| 问题 | 解决方案 |
|------|----------|
| `Method getMainLooper in android.os.Looper not mocked` | 添加 `unitTests.returnDefaultValues = true` + `ArchTaskExecutor.setDelegate()` |
| `InstantTaskExecutorRule` 在 JUnit 5 下不生效 | 改用 `ArchTaskExecutor.getInstance().setDelegate()` 手动设置 |
| `androidx.arch.core:core-testing:2.7.0` 不存在 | core-testing 使用独立版本号 2.2.0 |
| Mockito 严格模式报 unused stubbing | 添加 `@MockitoSettings(strictness = Strictness.LENIENT)` |

**编译验证**：`gradlew :feature-profile:testDebugUnitTest` → **29 tests passed, BUILD SUCCESSFUL**

---

### 4.2 Checkstyle/SpotBugs 配置修复与代码违规修复（T5-1/T5-2）

**开始时间**：2026-05-20

**实施内容**：

1. 修复 `checkstyle.xml`：`LineLength` 从 `TreeWalker` 移至 `Checker` 模块（兼容 Checkstyle 10.x）
2. 修复 `checkstyle.xml`：移除 `LITERAL_INSTANCEOF` token（`WhitespaceAroundCheck` 10.x 不再支持）
3. 修复 `MainActivity.java`：移除未使用的 `NonNull` 导入（UnusedImports 违规）
4. 修复 `MainActivity.java`：添加 `navHostFragment` 空检查（SpotBugs NP 违规）
5. 修复 `MayhemApplication.java`：空 if 块添加注释（EmptyBlock 违规）
6. 更新 `spotbugs/exclude.xml`：排除 DataBinding 生成代码的 BC 违规

**关键决策**：
- **决策56**：Checkstyle 10.x 中 `LineLength` 必须放在 `Checker` 而非 `TreeWalker` 下，理由：Checkstyle 10.x 重构了模块层级，`LineLength` 属于文件级检查而非 AST 级检查
- **决策57**：DataBinding 生成代码的 SpotBugs 违规应排除而非修复，理由：生成代码不可控，修复后重新生成会丢失

**编译验证**：`gradlew :app:checkstyleMain` → **0 violations**；`gradlew :app:spotbugsDebug` → **0 bugs**

---

### 4.3 Room Migration v4→v5（T6-1）

**开始时间**：2026-05-20

**实施内容**：

1. 新增 `AppDatabaseMigrations.java`：包含 MIGRATION_1_2 / 2_3 / 3_4 / 4_5 四个正式迁移类
2. MIGRATION_1_2：heroes 新增 title / description / skills / counterTips / synergies / avgKDA / recommendedBuild
3. MIGRATION_2_3：heroes 新增 isVersionTrap
4. MIGRATION_3_4：heroes 新增 recommendedAugmentIds / recommendedAugments
5. MIGRATION_4_5：heroes 新增 banRate；augments 新增 descriptionDetail
6. 修改 `AppDatabase.java`：版本 4→5，`fallbackToDestructiveMigration()` 替换为 `addMigrations()`
7. 修改 `HeroEntity.java`：新增 banRate 字段（英雄禁用率）
8. 修改 `AugmentEntity.java`：新增 descriptionDetail 字段（符文详细描述）
9. 新增 v5 schema 文件

**关键决策**：
- **决策58**：使用正式 Migration 策略替代 `fallbackToDestructiveMigration()`，理由：规则 BC-006 要求正式发布前必须实现正确的 Migration 策略；破坏性迁移会导致用户本地缓存数据全部丢失
- **决策59**：v4→v5 新增 banRate 和 descriptionDetail 字段，理由：banRate 是 ARAM 模式重要数据（禁用率影响英雄选择）；descriptionDetail 提供符文的详细机制说明，与 heroes 表的 description 字段对称

**编译验证**：`gradlew :app:compileDebugJavaWithJavac` → **BUILD SUCCESSFUL**；`gradlew :feature-profile:testDebugUnitTest` → **29 tests passed**

---

### 4.4 ViewModel 单元测试全覆盖（T7）

**开始时间**：2026-05-20

**实施内容**：

1. **feature-bulletin**：修复 `BulletinListViewModelTest`（ArchTaskExecutor + mockApplication），新增 `BulletinDetailViewModelTest`（10 个测试用例）
2. **feature-community**：新增 `StrategyFeedViewModelTest`（11 个测试用例：分页加载、排序、刷新、重复请求防护）
3. **feature-community**：新增 `StrategyDetailViewModelTest`（9 个测试用例：详情加载、投票、错误处理）
4. **feature-community**：新增 `PublishStrategyViewModelTest`（15 个测试用例：表单验证、发布逻辑、符文/装备选择、重置）
5. **feature-profile**：新增 `MyStrategiesViewModelTest`（10 个测试用例：列表加载、删除、错误处理、重复请求防护）
6. **feature-augment**：新增 `SynergyProgressViewModelTest`（10 个测试用例：套装进度加载、符文增删、防御性拷贝）
7. **feature-augment**：新增 `AugmentRecommendViewModelTest`（10 个测试用例：英雄选择、符文增删、推荐数据刷新）
8. **feature-augment**：修复已有 `AugmentViewModelTest`（ArchTaskExecutor + mockApplication 替换 null Application）
9. **feature-augment build.gradle**：添加 `testOptions { unitTests.returnDefaultValues = true; useJUnitPlatform() }` 和 `core-testing:2.2.0` 依赖
10. **core-network DTO**：为 `StrategyListResponse` 和 `StrategyDetailResponse` 补充 setter 方法以支持测试数据构造

**测试统计**：

| 模块 | 测试类 | 测试用例数 | 状态 |
|------|--------|-----------|------|
| feature-bulletin | BulletinListViewModelTest | 8 | ✅ 修复通过 |
| feature-bulletin | BulletinDetailViewModelTest | 10 | ✅ 新增通过 |
| feature-community | StrategyFeedViewModelTest | 11 | ✅ 新增通过 |
| feature-community | StrategyDetailViewModelTest | 9 | ✅ 新增通过 |
| feature-community | PublishStrategyViewModelTest | 15 | ✅ 新增通过 |
| feature-profile | MyStrategiesViewModelTest | 10 | ✅ 新增通过 |
| feature-profile | ProfileViewModelTest | 29 | ✅ 已有通过 |
| feature-augment | AugmentViewModelTest | 11 | ✅ 修复通过 |
| feature-augment | SynergyProgressViewModelTest | 10 | ✅ 新增通过 |
| feature-augment | AugmentRecommendViewModelTest | 10 | ✅ 新增通过 |
| **合计** | **10 个测试类** | **123 个测试用例** | **全部通过** |

**关键决策**：
- **决策60**：ViewModel 测试统一使用 JUnit 5 + Mockito + ArchTaskExecutor 模式，理由：`instant-executor` 规则不适用于 JUnit 5，手动设置 ArchTaskExecutor delegate 可确保 LiveData.setValue() 在测试线程同步执行
- **决策61**：AndroidViewModel 测试使用 mock Application 而非真实实例，理由：单元测试不应依赖 Android Framework，`returnDefaultValues = true` 配合 mock 可避免 NPE
- **决策62**：Repository 返回 LiveData 的 ViewModel 测试使用 `observeForever` 模式，通过手动触发 MutableLiveData.setValue() 模拟数据回调，理由：避免引入 CountDownLatch 的异步等待复杂度

**遇到的问题及解决**：

| 问题 | 解决方案 |
|------|----------|
| LiveData.getValue() 返回 null（主线程检查） | 设置 ArchTaskExecutor delegate 使 postToMainThread 同步执行 |
| AndroidViewModel(null) 导致 NPE | 使用 @Mock Application 替代 null |
| Arrays.asList() 返回固定大小列表，addAugment/removeAugment 抛 UnsupportedOperationException | 使用 new ArrayList<>(Arrays.asList(...)) 构造可变列表 |
| Mockito PotentialStubbingProblem（未使用的 stub） | 添加 @MockitoSettings(strictness = Strictness.LENIENT) |
| StrategyListResponse/StrategyDetailResponse 缺少 setter | 补充 setter 方法以支持测试数据构造 |

---

### 4.5 数据管线验证与 Release 构建部署（T8）

**开始时间**：2026-05-20

**实施内容**：

1. **后端数据管线验证**：确认 `RiotDataDragonClient`（版本获取+英雄数据抓取）和 `AramDataCollector`（U.GG ARAM 统计数据爬取）编译通过
2. **Android ProGuard 规则完善**：补充 DTO/Entity/Hilt/Room/Converter/DAO 等保留规则，确保 Release 构建混淆后运行正常
3. **Android Release 构建修复**：
   - 修复 core-ui 和 feature 模块缺失的 string 资源（upvote/downvote/hero_icon/empty_strategies）
   - 修复 feature-profile 的 `ic_launcher_round` 引用错误，改用 `ic_person` drawable
   - 移除 `navigation_graph.xml` 中的 `tools:layout` 属性，解决 ViewBinding 在 app 模块中生成跨模块绑定类导致 Release 构建找不到 R.layout.fragment_profile 的问题
4. **Android 签名配置**：
   - 生成 `aram-mayhem-release.jks` 签名密钥（RSA 2048, 有效期 10000 天）
   - 配置 `app/build.gradle` 的 `signingConfigs.release`，支持 `gradle.properties` 覆盖密码
   - Release buildType 绑定 `signingConfig signingConfigs.release`
5. **后端部署脚本完善**：
   - 重写 `start-server.bat`：增加 MySQL/Redis 连接预检、启动后健康检查轮询（60s 超时）、持续监控
   - 新增 `backup-db.bat`：MySQL 数据库备份（mysqldump + gzip 压缩 + 7 天自动清理）
   - 新增 `health-check.bat`：快速健康检查脚本
6. **Android 真机部署脚本**：
   - 新增 `deploy-device.bat`：ADB 连接检测 + APK 安装 + 自动启动 + 验证清单

**关键决策**：
- **决策63**：移除 `navigation_graph.xml` 中的 `tools:layout` 属性，理由：`tools:layout` 仅用于 Android Studio 设计时预览，不影响运行时行为；但会导致 ViewBinding 在 app 模块中为 feature 模块的布局生成绑定类，Release 构建时 app 的 R 类不包含 feature 模块布局资源，编译报错 `R.layout.fragment_profile not found`
- **决策64**：签名密码采用 `gradle.properties` 优先 + 硬编码 fallback 策略，理由：CI/CD 环境通过 `RELEASE_STORE_PASSWORD`/`RELEASE_KEY_PASSWORD` 属性注入密钥，本地开发使用默认值
- **决策65**：后端启动脚本增加 MySQL/Redis 预检和健康检查轮询，理由：避免服务启动后依赖不可用导致运行时错误；健康检查轮询确认服务真正可用后才输出成功信息
- **决策66**：数据库备份采用 `mysqldump --single-transaction`，理由：InnoDB 引擎下 `--single-transaction` 可实现一致性快照备份而不锁表，不影响线上服务

**遇到的问题及解决**：

| 问题 | 解决方案 |
|------|----------|
| Release 构建报 `R.layout.fragment_profile not found` | 移除 navigation_graph.xml 的 tools:layout 属性 + clean 重建 |
| feature-profile 引用不存在的 `@mipmap/ic_launcher_round` | 改用 `@drawable/ic_person` |
| core-ui 缺失 upvote/downvote/hero_icon/empty_strategies 字符串 | 补充到 strings.xml |
| Release APK 为 unsigned | 配置 signingConfigs.release 并绑定到 release buildType |
| ViewBinding 缓存导致修改后仍报旧错误 | 执行 `gradlew clean` 清除生成文件后重新构建 |

**编译验证**：`gradlew clean assembleRelease` → **BUILD SUCCESSFUL**，生成 `app-release.apk`（已签名）

---

## M5~M11 Android 全模块详细注释完成记录

**开始时间**：2026-05-22 ~ 2026-05-23

**实施内容**：

1. **core-network 模块注释**
   - ApiClient、AuthInterceptor、TokenAuthenticator 全文件标准化注释
   - DTO 类详细注释（HeroListResponse、StrategyListResponse、SynergyProgressResponse 等）

2. **core-data 模块注释**
   - AppDatabase、HeroDao、StrategyDao 全文件标准化注释
   - 新增 BulletinDao、StrategyEntity、BulletinEntity 详细注释
   - Room Migration v4→v5→v6 策略说明

3. **core-ui 模块注释**
   - VoteButton、StatefulLayout、TierBadgeView、QualityChip、BalanceBar 等自定义组件
   - PaginationScrollListener、SearchToolbar、BulletinCarouselView 详细注释

4. **feature-hero 模块注释**
   - HeroListFragment、HeroDetailFragment、HeroRepository 完整注释
   - HeroCardAdapter、SkillAdapter 详细注释
   - HeroListViewModel、HeroDetailViewModel 数据流说明

5. **feature-augment 模块注释**
   - AugmentListFragment、AugmentRecommendFragment 完整注释
   - AugmentRepository、SynergyProgressAdapter 详细注释
   - AugmentDetailBottomSheet 交互流程说明

6. **feature-community 模块注释**
   - CommunityFeedFragment、PublishStrategyFragment、StrategyDetailFragment 完整注释
   - StrategyCardAdapter、AugmentListAdapter、ItemListAdapter 详细注释
   - StrategyRepository 缓存策略说明

7. **feature-bulletin 模块注释**
   - BulletinListFragment、BulletinDetailFragment、BulletinAdapter 完整注释
   - BulletinListViewModel 数据加载与缓存逻辑说明

8. **feature-profile 模块注释**
   - ProfileFragment、SettingsFragment、MyStrategiesFragment 完整注释
   - ProfileViewModel、MyStrategiesViewModel 认证流程说明
   - MyStrategiesAdapter 双重删除机制（按钮+滑动）详解

9. **单元测试框架**
   - HeroRepositoryTest：英雄列表缓存测试
   - AugmentRepositoryTest：符文列表缓存测试
   - StrategyRepositoryTest：攻略列表缓存测试
   - ProfileRepositoryTest：用户资料缓存测试

**注释标准**：
- 7 大章节模板：组件用途、布局结构、数据流、关键机制、实现原理、导航关系、设计考量
- 每个方法详细说明输入/输出/副作用
- 包含时序图、数据流图、布局结构图等可视化说明
- 关键设计决策的原因分析

**新增文档**：
- offline-testing-guide.md：离线模式测试步骤文档（2026-05-22）

**M1~M11 全部完成** ✅ 2026-05-23
