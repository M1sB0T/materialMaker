# 素材工坊 materialMaker

一个可独立运行的素材制作网站（全栈笔试题实现）：浏览模板 → 上传素材 → 提交制作任务 → 后台异步模拟生成 → 查看历史与结果。制作过程不调用真实 AI，使用可控制耗时与成功/失败的模拟生成器完成业务闭环。

- 后端：Java 17 + Spring Boot 3.3.5 + MyBatis 3.0.3 + MySQL 8.0
- 前端：Next.js 14.2.18 (App Router) + React 18.3.1 + TypeScript 5.x
- 交付：GitHub Actions CI + Docker Compose 一键启动

## 一键启动（推荐）

```bash
docker compose up --build
```

启动后访问：

| 服务 | 地址 |
|---|---|
| 前端网站 | http://localhost:3000 |
| Java 后端 | http://localhost:8080/api/templates |
| MySQL | 容器内 3306（如需宿主机访问，放开 compose 中 3307 映射） |

数据库初始化：compose 将 `src/main/resources/schema.sql`、`data.sql` 挂载到 MySQL 的 `/docker-entrypoint-initdb.d`，首次启动自动建表并写入 8 个模板种子数据（脚本幂等，后端每次启动也会重复执行，不会产生脏数据）。

> 凭据说明：compose 内的数据库密码默认值仅供本地/演示开箱即用，生产部署请复制 `.env.example` 为 `.env` 覆盖。应用不使用 root 连接，而是使用最小权限专用账号 `material_app`（由 MySQL 镜像自动创建，仅授权 `material_maker` 库）。
> 升级提示：若此前用旧版 compose 启动过，MySQL 数据卷中仍是旧凭据/旧账号，需 `docker compose down -v` 清空数据卷重建，或通过 `.env` 提供与旧卷一致的密码。

## 本地开发启动

1. 准备 MySQL 8：确保有可写账号（后端 JDBC 参数含 `createDatabaseIfNotExist=true`，无需手动建库）。
2. 复制 `src/main/resources/application-local.yml.example` 为 `application-local.yml`，填入本地凭据（该文件已 gitignore）。
3. 启动后端：

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. 启动前端（依赖 Node 18+）：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. 浏览器打开 http://localhost:3000。

> 若本机 8080 被占用，可追加 `-Dspring-boot.run.arguments="--server.port=8081"`，并同步设置前端环境变量 `API_ORIGIN=http://localhost:8081`。

## 项目结构

```
materialMaker/
├── pom.xml                        # Spring Boot 3.3.5 / Java 17
├── src/main/java/com/bot/materialmaker/
│   ├── controller/                # 模板/素材/任务/登录/模拟配置接口
│   ├── service/
│   │   ├── TaskService.java       # 任务受理校验 + 落库（WAITING）
│   │   ├── TaskPipeline.java      # 后台三步流水线（核心）
│   │   ├── MaterialService.java   # 上传校验与本地存储
│   │   └── ...
│   ├── mapper/                    # MyBatis 注解 Mapper
│   ├── model/ dto/ config/ auth/ common/
├── src/main/resources/
│   ├── schema.sql / data.sql      # 建表与模板种子数据（幂等）
│   ├── static/demo/covers/        # 模板封面 PNG
│   └── demo/results/              # 各模板的“生成结果”演示图
├── src/test/java/                 # 后端核心测试
├── frontend/
│   ├── src/app/                   # 列表 / 详情制作 / 任务三页
│   ├── src/lib/tasksStore.ts      # 任务跟踪 Store（轮询启停核心）
│   └── src/lib/__tests__/         # Store 单元测试
├── scripts/
│   ├── generate-demo-images.mjs   # 演示图片生成脚本（已执行，产物已入库）
│   └── smoke-test.mjs             # 端到端冒烟：登录→上传→提交→轮询→失败注入
├── docker-compose.yml
├── Dockerfile                     # 后端镜像（多阶段）
├── frontend/Dockerfile            # 前端镜像（standalone）
└── .github/workflows/ci.yml
```

## 构建与测试命令

| 目标 | 命令 |
|---|---|
| 后端测试 | `mvn test` |
| 后端打包 | `mvn package -DskipTests` |
| 前端 lint | `cd frontend && npm run lint` |
| 前端类型检查 | `npm run typecheck` |
| 前端测试 | `npm test` |
| 前端构建 | `npm run build` |
| 端到端冒烟（需后端已启动，且以 `DEV_TOOLS_ENABLED=true` 启动后端） | `node scripts/smoke-test.mjs http://localhost:8080` |

## 配置项

| 配置 | 位置 | 默认值 | 说明 |
|---|---|---|---|
| `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` | 环境变量 | localhost/3306/material_maker/root/空 | 数据库连接（Docker 部署使用专用账号 `material_app`，仅授权 material_maker 库） |
| `MYSQL_ROOT_PASSWORD` / `DB_PASSWORD` | compose `.env` 文件 | 演示默认值 | 生产部署请复制 `.env.example` 为 `.env` 覆盖，不要使用仓库内默认密码 |
| `STORAGE_DIR` | 环境变量 | `./data` | 上传素材与生成结果目录（`uploads/`、`results/` 子目录） |
| `DEV_TOOLS_ENABLED` | 环境变量 | `false` | 演示调试接口 `/api/dev/simulation` 开关；生产环境保持关闭 |
| `simulation.step-delay-millis` | application.yml | 三阶段 1500/3500/1500ms | 各阶段模拟耗时（运行时可通过 dev 接口调整，单阶段上限 600 秒） |
| `simulation.fail-phase` | application.yml | 空 | 注入失败的阶段 |
| `API_ORIGIN` | 前端环境变量 | http://localhost:8080 | Next 服务端代理目标 |

所有配置均有无凭据示例；数据库密码等敏感信息通过环境变量或本地 `application-local.yml`（gitignore）注入，不进入仓库。

## 用户身份与个人记录

演示用轻量方案：任意用户名即可登录（`POST /api/auth/login`），首次登录自动建用户并签发随机令牌；前端存入 localStorage，刷新后保持登录。后续请求携带 `X-Auth-Token`，后端拦截器解析用户，任务/素材数据全部按用户隔离。

安全约束：

- 令牌有效期 7 天（过期自动失效，登录时顺带清理过期令牌）；
- `DELETE /api/auth/logout` 登出即吊销令牌；
- 同一用户名登录限流（每分钟 10 次），缓解刷号/刷令牌滥用；
- `/files/uploads/**` 与 `/files/results/**` 为私有内容，静态访问同样要求登录（`<img>` 场景通过 `?token=` 查询参数携带，由前端 `fileUrl()` 统一附加）；
- 上传文件服务端校验 MIME 白名单 + 文件头魔数，存储扩展名由服务端映射生成，杜绝伪装成图片的可执行内容（存储型 XSS）。

演示方法：浏览器打开网站 → 右上角“登录” → 输入用户名（如 `alice`）→ 提交任务后另开无痕窗口用 `bob` 登录，可见两人任务与素材互不可见。

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/templates?category=` | 模板列表（可按分类筛选） |
| GET | `/api/templates/{id}` | 模板详情 |
| POST | `/api/auth/login` | 登录，返回 `{token, username}` |
| DELETE | `/api/auth/logout` | 登出，吊销当前令牌（幂等） |
| POST | `/api/materials` | 上传素材（multipart，含 templateId），返回素材记录与预览地址 |
| POST | `/api/tasks` | 提交任务 `{templateId, materialIds[]}`，校验通过后立即返回 `{taskId, status:WAITING}` |
| GET | `/api/tasks` | 当前用户历史任务（含步骤明细） |
| GET | `/api/tasks/{id}` | 单个任务状态 |
| GET/POST | `/api/dev/simulation` | 查看/修改失败注入与各阶段耗时（演示用，需 `DEV_TOOLS_ENABLED=true`，默认关闭） |
| GET | `/files/uploads/**`、`/files/results/**` | 上传素材与生成结果的静态访问（需登录，支持 `?token=` 参数） |

错误统一返回 `{timestamp, status, message}`。

## 任务状态与后台执行

- 整体状态：`WAITING（等待处理）→ PROCESSING（处理中）→ SUCCESS / FAILED`，状态仅由后台流水线写库推进，查询接口只读，不会驱动任务前进，已完成的任务不会回退。
- 处理阶段：`MATERIAL_CHECK（素材检查）→ GENERATING（模拟生成）→ ASSEMBLING（结果整理）`，当前阶段记录在 `tasks.current_phase`，每步的 PENDING/RUNNING/SUCCESS/FAILED 与起止时间记录在 `task_steps` 表，构成可独立观察的执行边界。
- 执行方式：提交接口完成校验、创建任务与步骤记录（事务提交）后，将任务投递到独立线程池（4~8 线程），多任务排队或并行执行，关闭页面不影响；`ASSEMBLING` 成功后将演示结果图复制到结果目录并写入 `result_url`。

## 演示指南

1. **成功流程**：登录 → 挑模板 → 按上传位传图（类型/大小前端即时校验，服务端二次校验）→ 提交 → 自动跳转“我的任务”，观察 等待处理 → 各阶段 → 成功，显示结果预览图。
2. **指定阶段失败**：
   ```bash
   curl -X POST localhost:8080/api/dev/simulation -H "Content-Type: application/json" -d '{"failPhase":"GENERATING"}'
   ```
   再提交任务：该阶段标记失败、后续步骤不再执行，任务页展示失败阶段与原因。还原传 `{"failPhase":""}`。
3. **调整模拟耗时**（便于观察多任务）：
   ```bash
   curl -X POST localhost:8080/api/dev/simulation -H "Content-Type: application/json" \
     -d '{"stepDelayMillis":{"GENERATING":10000}}'
   ```
4. **多任务**：把耗时调大后连续提交多个任务，任务页各卡片状态独立刷新、互不串扰。
5. **刷新恢复**：任务处理中刷新页面（或关闭浏览器后重开）→ “我的任务”从后端恢复记录并继续跟踪；全部任务结束后轮询自动停止。
6. **查询异常 ≠ 任务失败**：处理中停掉后端再启动，任务页顶部会出现“状态查询暂时不可用”提示并保留最后已知状态，任务不会被误标失败；恢复后自动继续。

## CI

`.github/workflows/ci.yml` 在 push / pull request 时执行三个 job：

1. **backend**：`mvn test` + 打包；
2. **frontend**：`npm ci` → lint → typecheck → jest → build；
3. **docker**：`docker compose build` → 启动整套环境 → 就绪探测 → `node scripts/smoke-test.mjs` 端到端冒烟（登录→上传→提交→轮询→失败注入）→ 清理。

## 测试覆盖说明

- 后端（JUnit 5 + Mockito）：
  - `TaskPipelineTest`：三步执行顺序、注入失败后中断后续步骤、已完成任务不重复执行、结果整理异常处理；
  - `TaskServiceTest`：模板不存在 / 素材数量不符 / 素材不属于用户 / 类型不符等受理校验，成功受理落库 WAITING + 三个 PENDING 步骤。
- 前端（Jest）：
  - `tasksStore.test.ts`：多任务状态互不串扰、查询异常保留任务信息且不改写状态（异常 ≠ 失败）、有未完成任务才轮询且全部结束后自动停止、reset 清空与过期结果丢弃。

## 已完成 / 未完成与已知限制

已完成：R-01 ~ R-14 全部条目，包括模板列表/分类、详情与上传校验、异步三步流水线与状态持久化、失败注入与耗时调整、前端异步状态管理与轮询启停、刷新恢复、多任务隔离、用户身份、CI（含 Docker 冒烟）、Compose 一键启动。

未实现 / 边界说明（按题目要求如实声明）：

- **服务重启恢复**：应用重启后，处于 WAITING/PROCESSING 的任务会停留在数据库中不再继续执行（题目已声明不需要自动恢复）。
- **任务重试/断点续跑**：未实现，取舍讨论：受理幂等可通过提交时带客户端去重键实现，重试需要引入阶段级 checkpoint，复杂度收益比不高。
- 登录无密码校验，令牌不过期——面向演示的轻量方案，生产环境应替换为正式认证。
- 演示图片为脚本生成的抽象图案（纯 Node PNG 编码，无第三方素材版权问题）。

## 部署与回滚

- 部署：任意有 Docker 的机器 `docker compose up -d --build` 即可；数据（MySQL 卷 + 素材/结果目录卷）持久化在 named volumes。
- 回滚：`docker compose down` 后检出上一验收 commit（本仓库各阶段均有独立提交）重新 `up -d --build`；数据库结构变更时回滚需同时清理 `mysql-data` 卷（演示环境可接受）。

## AI 使用说明

本项目在开发中使用了 AI 编程工具，工作流：先与 AI 一起把题目拆解为后端骨架 → 流水线 → 前端状态 → CI 交付四个任务块，并为每个任务提供题目对应条目（R-xx）作为上下文；由 AI 生成初始代码，随后人工/自动化验证：`mvn test`、`jest`、`next build`、端到端冒烟脚本逐层把关。例如 `tasksStore` 的轮询启停逻辑第一版存在“首刷未返回就判断是否轮询”的缺陷，正是单元测试暴露后修正的——生成、验证、修正闭环共迭代了三轮。

未使用现成开源业务脚手架；演示封面/结果图由自带脚本生成，无外部素材。
