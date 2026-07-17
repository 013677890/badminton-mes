# 羽毛球 MES 微信小程序 Stitch 设计资源

## 项目信息

- Stitch 项目名称：`羽毛球 MES 微信小程序（Stitch）`
- Stitch 项目 ID：`12814139172772667125`
- 项目资源名：`projects/12814139172772667125`
- 可见性：`PRIVATE`
- 项目类型：`PROJECT_DESIGN`
- 目标设备：`MOBILE`
- Design System：`Badminton MES Industrial Mobile`
- Design System 资产：`assets/5173856779641194057`
- 生成模型：`GEMINI_3_FLASH`
- 最近同步日期：`2026-07-15`

Stitch MCP 没有返回可验证的项目网页 URL。请登录 [Google Stitch](https://stitch.withgoogle.com/)，在项目列表中按上述项目名称或项目 ID 打开，不在文档中猜测未验证的深层链接。

## 设计基线

- 工业青绿：`#0F766E`；深主色：`#115E59`。
- 安全橙：`#F97316`，仅用于预警和待处理状态。
- 浅色背景：`#F8FAFC`；卡片：`#FFFFFF`。
- 危险色：`#B42318`；成功色：`#15803D`。
- 中文字体规范优先 `Noto Sans SC`。Stitch 字体枚举不直接提供该字体，因此生成主题使用 `Source Sans 3`，并在 Design System 和页面提示中明确中文字体规范。
- 移动端竖屏、8px 间距体系、页面左右 16px、触控目标至少 44px。
- 不使用桌面侧边栏、Emoji、玻璃拟态、依赖 hover 的交互或大面积装饰渐变。

## 已生成页面

| 序号 | 页面 | Stitch Screen ID | 本地 PNG | 本地 HTML |
| --- | --- | --- | --- | --- |
| 01 | 登录默认页 | `5dc8486d222d4694808275ee41115ce9` | `screens/01-login-default.png` | `source/01-login-default.html` |
| 02 | MES 账号绑定默认页（归档版） | `63221c4c6e6a42818a484e7341a089af` | `screens/02-bind-account-default.png` | `source/02-bind-account-default.html` |
| 03 | 实时生产看板默认页 | `d011c9f750044f41afcb8ac9d5abeb60` | `screens/03-dashboard-default.png` | `source/03-dashboard-default.html` |
| 04 | 生产分析默认页 | `75ab04339ca14729ada4d65ad5df9478` | `screens/04-production-analysis-default.png` | `source/04-production-analysis-default.html` |
| 05 | 产品追溯默认页 | `d5c5fd2532304f1fb5cc65be6b5ef72e` | `screens/05-product-trace-default.png` | `source/05-product-trace-default.html` |
| 06 | 个人中心默认页 | `7fd77d0f289d4e0a85ce27928f8e33e4` | `screens/06-profile-default.png` | `source/06-profile-default.html` |
| 07 | 加载、空数据、错误、会话失效状态总览 | `5affb0e43e284c75b0cd5f4f7ce960f9` | `screens/07-global-states.png` | `source/07-global-states.html` |
| 08 | 解绑确认与解绑中状态 | `b3283cadc4054882a4ff31aed40a74ce` | `screens/08-unbind-confirmation.png` | `source/08-unbind-confirmation.html` |

补充审查图：

| 文件 | 来源 | 生成方式 |
| --- | --- | --- |
| `screens/00-design-system.png` | DESIGN.md Screen `16844689000197391937` | `get_screen` 只返回原始 HTML、没有 screenshot URL；下载其 HTML 后通过本地 Playwright 静态渲染截图，没有重新生成设计 |
| `screens/15-ui-overview.png` | 六张默认业务页 PNG | 使用 `source/create_overview.py` 和 Pillow 按 3 列 × 2 行机械拼接，没有 AI 重绘 |

项目包含通过 `DESIGN.md` 上传产生的规范画面：`16844689000197391937`。其原始内容保存在 `source/00-design-system.html`，用于截图的 UTF-8 静态包装保存在 `source/00-design-system-render.html`。

## 本地产物说明

- 六个默认页和两张状态页 PNG 均直接下载自对应 Stitch Screen 的 screenshot 资源，没有使用占位图或人工伪造图片。
- `00-design-system.png` 是已有 DESIGN.md 内容的浏览器静态截图；`15-ui-overview.png` 是六张已有默认页的机械拼接，不包含 AI 生成或重绘。
- 八个业务页面 HTML 及 DESIGN.md 原始内容均直接下载自对应 Stitch Screen 的 htmlCode 资源。
- HTML/CSS 是 UI 设计参考，不可直接作为微信小程序代码使用；实现阶段应转换为 WXML、WXSS 和 TypeScript，并复用现有业务接口类型。
- 页面内示例工号、批次和生产数据均为虚构审查数据，不包含真实用户、OpenID、Token、密码或生产记录。

## 已知限制与未完成项

- Stitch 的 `get_project` / `list_screens` 当前只在项目画布列表中返回上传的 `DESIGN.md` 实例；文本生成调用返回的业务 Screen 仍可通过完整资源名和 `get_screen` 正常读取，并已成功下载截图及 HTML。本目录以实际可读取的 Screen ID 和已验证文件为准。
- 第一批生成的绑定页因聚合工具输出过长而没有保留可用 Screen ID；随后生成了标题带“归档版”的等价页面，并将其作为本地唯一权威绑定页产物。
- 当前没有生成单独的产品追溯结果页或各业务页逐一错误变体。关键状态已集中在 `07-global-states`，解绑危险操作集中在 `08-unbind-confirmation`。
- 尚未进行用户视觉审查和反馈后的第二轮精修。
- 尚未把设计转换为原生微信小程序代码；该工作不属于本目录的 Stitch 资源生成任务。

## 安全约束

- 不在设计、HTML、文档或截图中保存 AppSecret、Stitch API Key、Bearer Token、OpenID、真实密码或数据库密码。
- 微信 AppID 可以存在于小程序项目配置中，但不应写入本设计资源说明作为认证凭据。
