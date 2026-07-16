# 羽毛球 MES 微信小程序 Stitch 重设计与实施计划

> 更新规则：只有存在可验证的设计、代码、测试输出或用户确认时，才能将 `[ ]` 改为 `[x]`。部分完成或被阻塞的项目保持未勾选，并在条目后注明原因。

## 0. 基线与边界

- [x] Stitch MCP 已通过 `list_projects` 验证认证可用。
- [x] 已确认现有小程序包含登录、绑定、实时看板、生产分析、产品追溯、个人中心 6 个页面。
- [x] 已确认登录、绑定、解绑、退出、看板、分析、追溯 REST 接口已经存在。
- [x] 已确认小程序基线通过 `npm run typecheck`。
- [x] 已确认旧 Figma 本地资料没有真实 PNG，仅有两份未提交 Markdown。
- [x] 不修改 Flyway 历史、不执行 `repair`、不重建数据库、不改动 A/B/C 正在整合的迁移脚本。
- [x] AppID 可以保留在 `project.config.json`；AppSecret、Stitch API Key、Token、OpenID 和真实密码不得进入前端、设计稿或 Git。

## 1. 丢弃旧 Figma 方案

- [x] 删除 `design/ui-review/README.md` 中的旧 Figma 设计说明。
- [x] 删除 `design/ui-review/前后端交互逻辑.md`，其有效交互逻辑已纳入本计划和代码验收项。
- [x] 使用全文搜索确认仓库不再引用旧 Figma URL、文件 Key 或额度说明。
- [ ] 用户在 Figma 网页端将旧文件“羽毛球 MES 微信小程序 UI”移入回收站。
- [x] 后续以 Stitch 为唯一有效 UI 设计来源。

## 2. Stitch 项目与设计系统

- [x] 创建移动端 Stitch 项目“羽毛球 MES 微信小程序（Stitch）”（`projects/12814139172772667125`）。
- [x] 在 `design/stitch/README.md` 记录 Stitch 项目 ID、页面 ID、生成模型和同步时间。
- [x] 创建项目级 Design System（`assets/5173856779641194057`），后续页面显式复用。
- [x] 主色采用工业青绿 `#0F766E`，深主色 `#115E59`，安全橙 `#F97316`。
- [x] 页面背景 `#F8FAFC`，正文 `#172033`，次要文字 `#64748B`，危险色 `#B42318`。
- [x] 设计说明采用 Noto Sans SC 中文视觉规范、8px 间距节奏、12–16px 卡片圆角；Stitch 内置预览回退为 Source Sans 3。
- [x] 所有触控目标不小于 44×44px，相邻触控目标至少间隔 8px。
- [x] 使用统一线性图标，不用 Emoji 代替功能图标。

## 3. Stitch 页面与状态图

- [ ] 登录页：默认、登录中、微信登录失败、网络失败、Session 失效。
- [ ] MES 绑定页：默认、字段校验、绑定中、账号错误、重复绑定、票据过期。
- [ ] 实时看板：默认、骨架加载、无任务、请求失败、数据陈旧、异常预警。
- [ ] 生产分析：今日、近 7 天、近 30 天、加载、空数据和错误状态。
- [ ] 产品追溯：手动查询、扫码、结果、无结果、数据不完整和错误状态。
- [ ] 个人中心：默认、退出确认、解绑危险确认、解绑中和解绑失败。
- [x] 生成全局加载、空数据、请求错误、重试和 Session 失效状态总览。
- [ ] 用户完成默认页面第一轮审查。
- [ ] 用户完成状态页面第二轮审查。

设计资源目录：

```text
miniapp-wechat/design/stitch/
├── README.md
├── screens/
└── source/
```

- [x] 8 份业务页面 Stitch 原始 HTML 下载到 `design/stitch/source/`，并保留 DESIGN.md 原始内容与静态渲染文件。
- [x] 10 张可审查 PNG 保存到 `design/stitch/screens/`，均已逐张打开验证且不是占位图。
- [x] 生成 `00-design-system.png` 和 `15-ui-overview.png`；前者由已有 DESIGN.md 内容静态渲染，后者由六张默认页机械拼接，均未调用生成模型。
- [x] 生成六个默认页、全局状态和解绑确认 PNG，文件名与页面/状态一一对应。
- [x] Stitch HTML/CSS 仅作为参考，不直接当作微信小程序运行代码。

## 4. 小程序组件化与 Mock

- [x] 集中定义颜色、间距、字体、圆角、阴影、按钮、输入框和卡片样式。
- [x] 实现统一状态视图：`loading`、`empty`、`error`、`sessionExpired`。
- [x] 实现指标卡、状态标签、任务卡、确认弹窗和追溯时间轴等复用组件。
- [x] 为 tabBar 准备统一线性图标体系的 4 组普通态和选中态本地 PNG 资源。
- [x] 增加 `DataSourceMode = 'mock' | 'api'`。
- [x] 增加 `MockScenario = 'normal' | 'empty' | 'error' | 'unbound' | 'sessionExpired'`。
- [x] Mock 与 API 服务保持相同的 Promise 和 TypeScript 数据类型。
- [x] Mock 模式覆盖登录、绑定、看板、分析、追溯、退出和解绑。
- [x] Mock 数据只使用明显虚构的用户、任务和批次信息。
- [x] Mock 模式在个人中心显示开发环境标记，真实联调时通过运行配置切换为 `api`。

## 5. 六个页面实现

- [x] 登录页接入 `wx.login()`，处理重复点击、失败和网络错误。
- [x] 未绑定用户携带 `bindTicket` 进入绑定页；已绑定用户进入看板。
- [x] 绑定页完成字段校验、密码保护、提交禁用，并直接展示后端票据过期提示。
- [x] 看板完成首屏加载、下拉刷新、指标卡、任务列表、异常和最后更新时间。
- [x] 生产分析支持今日、近 7 天、近 30 天，并正确生成起止时间参数。
- [x] 产品追溯同时支持手动查询和 `wx.scanCode()` 查询。
- [x] 追溯结果展示任务、工序、报工、维修、警告和数据完整性。
- [x] 个人中心分别实现退出登录和解除绑定，并使用不同确认文案。
- [x] 解绑成功后清理会话并回登录页；退出登录不删除绑定关系。
- [x] HTTP 401 统一清理 Token，并用重入保护避免并发请求重复跳转。
- [x] 错误优先显示 `userTip`，其次显示 `message`，网络错误提供重试。
- [x] 只维护 TypeScript 源码，通过构建统一生成 JavaScript。

## 6. REST 与实时看板

保持现有 REST 路径，不新增数据库迁移：

```text
POST   /api/system/mini_app/auth/login
POST   /api/system/mini_app/auth/bind
DELETE /api/system/mini_app/auth/unbind
POST   /api/system/auth/logout
GET    /api/report/mini_app/realtime_dashboard
GET    /api/report/mini_app/production_analysis
GET    /api/report/mini_app/product_trace
```

- [x] 前端统一添加 `Authorization: Bearer {mes_token}`。
- [x] 增加 `unbindAccount(): Promise<void>` 前端服务。
- [x] TypeScript DTO 与后端 VO 字段一致，不展示后端没有返回的虚构业务字段。
- [x] 后端将 `/topic/report/mini_app` 加入 STOMP simple broker。
- [x] 前端使用 `wx.connectSocket()` 实现轻量 STOMP 1.2 客户端。
- [x] STOMP CONNECT 帧携带 Bearer Token。
- [x] 优先订阅 `/topic/report/mini_app/realtime/line/{lineId}`，否则订阅 workshop 主题。
- [x] 无订阅范围时只使用 REST 轮询。
- [x] WebSocket 断线后按 1/3/5/10/30 秒有限重连并启用 60 秒 REST 轮询兜底。
- [x] WebSocket 恢复后停止兜底轮询，避免重复刷新。
- [x] 鉴权失效后停止重连、清理 Token 并回到登录页。

## 7. 验证与交付

- [x] `miniapp-wechat`: `npm run typecheck` 通过。
- [x] `miniapp-wechat`: `npm run build` 通过。
- [x] `backend-java`: `.\gradlew.bat compileJava` 通过。
- [x] `backend-java`: `KanbanWebSocketConfigTest` 的 5 个聚焦测试通过。
- [x] `backend-java`: `.\gradlew.bat test` 全量测试通过。
- [x] `git diff --check` 通过；仅存在 Windows 环境的 LF/CRLF 提示。
- [x] Mock 的 normal、empty、error、unbound、sessionExpired 场景通过 10 项自动化脚本验证。
- [x] STOMP URL、子协议、CONNECT、Bearer Header、SUBSCRIBE 和 MESSAGE 解析通过 7 项自动化脚本验证。
- [ ] 微信开发者工具可使用 AppID `wx472e037adad346cf` 导入并编译。
- [ ] 常见手机模拟器下无横向滚动、遮挡、软键盘覆盖和按钮溢出。
- [ ] REST 登录、绑定、看板、分析、追溯、退出和解绑完成真实联调。
- [ ] STOMP 推送、断线轮询接管、恢复连接和 Session 失效行为通过验证。
- [x] 全文扫描确认不存在 AppSecret、Stitch API Key、Bearer Token、OpenID 或真实密码。
- [x] 检查变更未覆盖 A/B/C 迁移脚本和无关用户改动，迁移文件变更数为 0。
- [x] 在本文末尾记录最终完成日期、Stitch 项目 ID、联调模式和遗留问题。

## 8. 当前阻塞与遗留记录

- [ ] 远端旧 Figma 文件需要用户在 Figma 网页端手动移入回收站。
- [ ] Flyway `2026071202` checksum mismatch 仍由 A/B/C 集成阶段处理，本任务不擅自修复。
- [ ] Stitch 设计需要在默认页面和状态页面两个审查点等待用户确认；审查前可以完成技术落地，但不得声称视觉已最终验收。

## 9. 2026-07-15 实施记录

- Stitch 项目：`projects/12814139172772667125`。
- Stitch Design System：`assets/5173856779641194057`。
- 本地审查资源：10 张 PNG、业务页面 8 份 HTML、Design System 原始/静态渲染文件。
- 小程序默认联调模式：`mock`；通过 `miniapp_data_source=api` 切换真实接口。
- 小程序：6 个页面、6 个复用组件、8 个 tabBar 图标、Mock/API 数据源、统一 Session 处理、STOMP 1.2 与轮询兜底已实现。
- 后端：小程序 broker 前缀和看板主题作用域索引已修正，5 个聚焦测试及全量 Gradle 测试通过。
- 自动验证：TypeScript typecheck/build、Mock 10 项脚本、STOMP 7 项脚本、JSON/运行文件检查、敏感凭据扫描、`git diff --check` 均通过。
- 尚需用户完成：在微信开发者工具中进行多机型视觉审查；确认默认页和状态页设计；将旧 Figma 远端文件移入回收站。
- 尚需集成阶段完成：解决既有 Flyway checksum mismatch 后执行真实微信登录、REST 与 STOMP 端到端联调。
