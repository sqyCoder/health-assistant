# 前端全面改版计划书（specface.md）
## "极光临床 Aurora Clinical" 视觉与交互重设计

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 编制日期 | 2026-09-12 |
| 适用工程 | D:\code2\AI\health-assistant\frontend |
| 当前技术栈 | Vue 3.5 + TypeScript + Vite 6 + Element Plus 2.9 + Pinia + ECharts 5 |
| 改版目标 | 全站视觉、布局、动效、反馈系统彻底重构，达到"高级产品级"观感与手感 |
| 设计语言代号 | Aurora Clinical（极光临床）：深空暗色为底、极光渐变、玻璃拟态、精密动效 |
| 实施方式 | 保留业务逻辑与接口层，重写外壳、页面模板与样式层；Element Plus 深度换肤 + 自研高级组件库并行 |
| 文档状态 | 待评审（评审通过后按 Phase 0→4 实施） |

---

## 目录

1. [现状诊断](#1-现状诊断)
2. [改版目标与设计原则](#2-改版目标与设计原则)
3. [设计语言系统 Design Tokens](#3-设计语言系统-design-tokens)
4. [技术选型与依赖清单](#4-技术选型与依赖清单)
5. [应用外壳 App Shell 改造详案](#5-应用外壳-app-shell-改造详案)
6. [页面级改造详案（逐页）](#6-页面级改造详案逐页)
7. [自研组件库清单（40+ 组件）](#7-自研组件库清单40-组件)
8. [全局交互与反馈系统](#8-全局交互与反馈系统)
9. [Element Plus 深度换肤映射表](#9-element-plus-深度换肤映射表)
10. [ECharts 主题化与图表升级](#10-echarts-主题化与图表升级)
11. [性能预算与优化清单](#11-性能预算与优化清单)
12. [无障碍、兼容与降级策略](#12-无障碍兼容与降级策略)
13. [实施阶段规划（Phase 0-4）](#13-实施阶段规划phase-0-4)
14. [风险与回退方案](#14-风险与回退方案)
15. [验收清单](#15-验收清单)
16. [附录](#16-附录)

---

## 1. 现状诊断

### 1.1 整体观感问题（为什么"普通"）

| # | 问题 | 具体表现 |
| --- | --- | --- |
| P1 | 视觉无记忆点 | 全站是 Element Plus 默认风格 + 主色微调的"模板感"，没有任何品牌化视觉符号 |
| P2 | 布局平淡 | 清一色"白卡片 + 表格"，信息层级只靠字号区分，无版面节奏 |
| P3 | 零动效编排 | 页面切换无转场、数据加载无骨架、卡片无入场、操作无微反馈 |
| P4 | 反馈贫瘠 | 只有 ElMessage 小提示条，提交/成功/失败缺乏仪式感 |
| P5 | 暗色模式粗糙 | 只是把 Element Plus 的 dark css-vars 打开，自研组件配色不统一 |
| P6 | 数字表达弱 | 关键指标（BMI、异常数、运动量）只是文本，没有可视化强化 |
| P7 | 空状态无聊 | `el-empty` 默认图，无插画、无引导、无情感 |
| P8 | 加载体验差 | 路由切换白屏、表格转圈、AI 生成时只有一句提示 |
| P9 | 无键盘流 | 没有快捷键、没有命令面板，"高级应用"标配缺失 |
| P10 | 移动端将就 | 侧边栏在手机上体验割裂，无底部导航、无手势 |

### 1.2 逐页问题清单

| 页面（现有文件） | 主要问题 |
| --- | --- |
| `views/auth/LoginView.vue` | 居中白卡片 + 纯色渐变背景，和任何后台模板撞脸 |
| `views/auth/RegisterView.vue` | 同上，且表单无浮动标签、无密码强度 |
| `layouts/MainLayout.vue` | 经典"侧栏 + 顶栏"，菜单无动画、无折叠、无命令栏 |
| `views/dashboard/DashboardView.vue` | 一张成员卡 + 四张"开发中"占位卡，信息密度与观感都垫底 |
| `views/member/MemberListView.vue` | 纯表格，成员这种"人"的数据用表格展示最无温度 |
| `views/member/MemberDetailView.vue` | 描述列表 + 5 个 tab 表格，长而平 |
| `views/member/MemberFormView.vue` | 普通表单，无分步、无动效校验 |
| `views/report/ReportListView.vue` | 表格列表，解析状态只是一个小 tag |
| `views/report/ReportDetailView.vue` | 指标表格无"区间位置"可视化，异常仅靠红字 |
| `components/InterpretationPanel.vue` | 卡片列表直出，AI 生成时缺"思考感" |
| `views/exercise/ExerciseView.vue` | 大表单 + 表格，运动数据毫无动感 |
| `views/charts/ChartsView.vue` | 默认 ECharts 白底风格，未随主题演进，图表无渐变无编排 |
| `views/suggestion/*.vue` | 周计划表格平淡，无"计划感"（打卡感/进度感） |
| `views/chat/ChatView.vue` | 气泡基础，无 typing 动画、无会话切换动效、无流式光标 |
| `views/admin/AdminView.vue` | 五个 tab 全是大表格，工具感过重 |
| 全局 | 无 Toast 系统设计、无确认弹窗设计、无 404/500 页、无引导 |

### 1.3 保留与重写边界（重要）

**保留不动**：
- 全部 `api/*.ts`（接口层零改动，只可追加）
- `types/`、`utils/dict.ts` 业务字典
- `stores/*` 数据逻辑（只追加 UI 相关状态，如命令面板开关）
- `router/index.ts` 的路由表与守卫逻辑（页面组件替换即可）

**重写/大改**：
- `layouts/MainLayout.vue` → 全新 App Shell（拆为 6 个子组件）
- `views/**` 全部页面模板与样式（业务逻辑复用）
- `styles/index.scss` → 拆分为 tokens / themes / overrides / utilities 四层
- `components/**` 全部重构为"基础 UI 组件 + 业务组件"两层
- `App.vue`：挂载全局背景、Toast 宿主、命令面板、转场容器

---

## 2. 改版目标与设计原则

### 2.1 量化目标

| 维度 | 目标 |
| --- | --- |
| 观感 | 第一眼"这不是模板站"：极光背景 + 玻璃拟态 + 品牌渐变 + 精密排版 |
| 动效 | 每个页面至少 3 处编排动效（入场/交互/状态切换），全程 60fps |
| 反馈 | 任何用户操作 100ms 内必有视觉响应；长任务（>3s）有阶段化反馈 |
| 一致性 | 颜色、圆角、阴影、动效时长全部走 Token，无魔法值 |
| 性能 | 首屏 JS gzip ≤ 550KB，LCP ≤ 2.2s（本地），CLS ≤ 0.05 |
| 可用性 | 全站可键盘操作；`prefers-reduced-motion` 全量降级；亮/暗双主题等价 |
| 移动端 | 375px 宽度下完整可用，底部 Tab + 抽屉式交互 |

### 2.2 六条设计原则

1. **深色优先（Dark-first）**：默认跟随系统，暗色是精心设计的"主场"，宣纸亮色是"客串"；两套 Token 同构，切换无闪烁。
2. **玻璃与极光（Glass & Aurora）**：背景是"有生命的"极光渐变 + 噪点质感；内容面板是磨砂玻璃（blur + 半透明 + 细描边）。
3. **数据有表情（Data with Texture）**：每个关键数字必须有可视化伴随物（环、条、火花线、热力图），杜绝"裸数字"。
4. **动效有理由（Motion with Meaning）**：入场表达层级、悬停表达可交互、完成表达收束；禁止无意义乱飞。
5. **反馈即时分层（Feedback Layering）**：<100ms 微反馈（按钮按压）→ 1s 内轻反馈（涟漪/勾选）→ 长任务阶段反馈（进度环 + 文案轮播）→ 成功仪式（彩带/勾动画）。
6. **高级但不难用（Luxury but Usable）**：所有花哨效果必须可被 `reduced-motion` 一键关闭；信息密度不因"炫"而下降。

### 2.3 视觉关键词对照（改造前后）

| 维度 | 改造前 | 改造后 |
| --- | --- | --- |
| 背景 | 纯色 `#f5f7fa` | 极光网格渐变 + 噪点 + 微光斑（暗）/ 云白弥散（亮） |
| 卡片 | 白底 + 1px 灰边 + 2px 阴影 | 玻璃面板 + 渐变描边 + 悬浮光斑 + 多层阴影 |
| 主色 | 单一蓝 `#2f7de1` | 蓝紫渐变主色 + 薄荷健康色 + 珊瑚警示色三系 |
| 标题 | 系统字体加粗 | 大号紧排 + 渐变文字 + 数字专用字体 |
| 图标 | Element Plus 图标 | Lucide 线性图标（1.5px 描边）+ 渐变容器 |
| 动效 | 几乎为零 | 12 类标准动效 + 页面转场 + 列表编排 |
| 图表 | 默认样式 | 渐变填充 + 光晕 + 主题联动 + 入场动画 |
| 弹层 | 白色 dialog | 玻璃 dialog + 背景模糊 + 弹性入场 |

---

## 3. 设计语言系统 Design Tokens

> 实施载体：`src/design/tokens.css`（CSS 变量，`:root` 与 `html.dark` 双份）+ `src/design/motion.ts`（TS 常量）+ `src/design/echarts-theme.ts`（图表主题）。
> 所有页面禁止写死颜色/圆角/时长，必须引用 Token。

### 3.1 色彩体系

#### 3.1.1 品牌色

| Token | 值（亮） | 值（暗） | 用途 |
| --- | --- | --- | --- |
| `--ac-primary` | `#4F7CFF` | `#6C8CFF` | 主操作、链接、选中 |
| `--ac-primary-deep` | `#365DF0` | `#4F7CFF` | 渐变深端、按压态 |
| `--ac-violet` | `#7C5CFF` | `#9B7CFF` | 渐变浅端、AI 场景强调 |
| `--ac-cyan` | `#22D3EE` | `#3EE0F5` | 极光辅助、数据高亮 |
| `--ac-mint` | `#14C9A3` | `#2DD4A7` | 健康/成功/正常指标 |
| `--ac-amber` | `#F5A524` | `#FFB84D` | 提醒/偏低/待处理 |
| `--ac-coral` | `#FF5C7A` | `#FF6E8A` | 异常/危险/删除 |

#### 3.1.2 语义色

| Token | 亮色值 | 暗色值 | 用途 |
| --- | --- | --- | --- |
| `--ac-bg-base` | `#F6F8FC` | `#070B14` | 页面底色 |
| `--ac-bg-mesh-1` | `rgba(79,124,255,.10)` | `rgba(108,140,255,.16)` | 极光斑 1（蓝） |
| `--ac-bg-mesh-2` | `rgba(124,92,255,.08)` | `rgba(155,124,255,.14)` | 极光斑 2（紫） |
| `--ac-bg-mesh-3` | `rgba(34,211,238,.08)` | `rgba(62,224,245,.10)` | 极光斑 3（青） |
| `--ac-surface` | `rgba(255,255,255,.86)` | `rgba(17,24,39,.72)` | 玻璃面板底 |
| `--ac-surface-solid` | `#FFFFFF` | `#111827` | 不透明面板（表格行等） |
| `--ac-surface-sunken` | `#EDF1F8` | `#0B1120` | 内嵌区域、代码块 |
| `--ac-border` | `rgba(15,23,42,.10)` | `rgba(148,163,184,.16)` | 常规描边 |
| `--ac-border-strong` | `rgba(15,23,42,.18)` | `rgba(148,163,184,.28)` | 强调描边 |
| `--ac-text-1` | `#0F172A` | `#E8EEF9` | 主文字 |
| `--ac-text-2` | `#475569` | `#9AA7BD` | 次要文字 |
| `--ac-text-3` | `#94A3B8` | `#5B6B85` | 弱化文字/占位 |
| `--ac-success-bg` | `rgba(20,201,163,.12)` | `rgba(45,212,167,.14)` | 正常指标底 |
| `--ac-warning-bg` | `rgba(245,165,36,.14)` | `rgba(255,184,77,.16)` | 提醒底 |
| `--ac-danger-bg` | `rgba(255,92,122,.12)` | `rgba(255,110,138,.15)` | 异常底 |

#### 3.1.3 渐变资产

```css
--ac-gradient-primary: linear-gradient(135deg, #6C8CFF 0%, #7C5CFF 52%, #22D3EE 130%);
--ac-gradient-aurora:  radial-gradient(60% 80% at 15% 10%, rgba(108,140,255,.34), transparent 60%),
                       radial-gradient(50% 70% at 85% 15%, rgba(124,92,255,.28), transparent 62%),
                       radial-gradient(70% 90% at 50% 100%, rgba(34,211,238,.22), transparent 60%);
--ac-gradient-health:  linear-gradient(135deg, #2DD4A7, #22D3EE);
--ac-gradient-danger:  linear-gradient(135deg, #FF6E8A, #F5A524);
--ac-gradient-text:    linear-gradient(100deg, #E8EEF9 0%, #9BB8FF 45%, #7CE7F7 100%);  /* 暗色标题 */
--ac-gradient-shimmer: linear-gradient(100deg, transparent 20%, rgba(255,255,255,.35) 50%, transparent 80%);
```

### 3.2 字体与排版

| 用途 | 字体栈 | 说明 |
| --- | --- | --- |
| 中文正文 | `"PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", system-ui` | 系统字体，零加载成本 |
| 拉丁/数字 | `"Inter Variable", "SF Pro Display", system-ui` | **自托管 Inter 子集（仅拉丁+数字，woff2 约 40KB，`font-display: swap` + preload）**，数字启用 `font-variant-numeric: tabular-nums` |
| 代码/日志 | `"JetBrains Mono", ui-monospace, Consolas` | 管理后台日志、Token 数 |

字号阶梯（`--ac-font-*`）：`xs 12 / sm 13 / base 14 / md 16 / lg 18 / xl 22 / 2xl 28 / 3xl 34 / 4xl 44`；行高 `1.45~1.65`；标题启用 `letter-spacing: -.01em`。

数字展示规范：所有统计数字使用 `font-feature-settings:"tnum"`；大数字（健康分、步数）使用 `2xl~4xl` + 渐变文字；单位使用 `text-3` 小号上标。

### 3.3 空间、圆角、阴影、层级

| 类别 | 阶梯 |
| --- | --- |
| 间距 | `4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 56`（`--ac-space-1..9`） |
| 圆角 | `6 / 10 / 14 / 20 / 28 / 999`（卡片默认 20，按钮 12，胶囊 999） |
| 阴影 | `--ac-shadow-1: 0 1px 2px rgba(7,11,20,.06)`；`-2: 0 8px 24px -8px rgba(7,11,20,.18)`；`-3: 0 24px 48px -24px rgba(7,11,20,.32)`；`--ac-shadow-glow: 0 0 0 1px rgba(108,140,255,.35), 0 12px 40px -12px rgba(108,140,255,.45)` |
| 层级 | base 0 / sticky 50 / dock 100 / header 200 / drawer 300 / modal 400 / toast 500 / palette 600 / confetti 700 |

### 3.4 玻璃拟态规范

```css
.ac-glass {
  background: var(--ac-surface);
  backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid var(--ac-border);
  box-shadow: var(--ac-shadow-2);
}
.ac-glass-hover:hover { border-color: var(--ac-border-strong); box-shadow: var(--ac-shadow-3); }
```
边界：低端设备（`navigator.hardwareConcurrency <= 4` 或 `prefers-reduced-motion`）自动降级为不透明表面 + 简化阴影（`body.ac-lite` 类）。

### 3.5 动效 Token（`src/design/motion.ts`）

| Token | 值 | 用途 |
| --- | --- | --- |
| `DUR.instant` | 100ms | 按压、色彩变化 |
| `DUR.fast` | 160ms | 悬停、工具提示 |
| `DUR.base` | 240ms | 大多数过渡、下拉 |
| `DUR.slow` | 400ms | 抽屉、对话框、卡片入场 |
| `DUR.xslow` | 640ms | 页面转场、数字滚动 |
| `EASE.standard` | `cubic-bezier(.2,0,0,1)` | 默认 |
| `EASE.emphasized` | `cubic-bezier(.05,.7,.1,1)` | 入场、转场 |
| `EASE.spring` | `cubic-bezier(.34,1.56,.64,1)` | 弹层、勾选、徽标 |
| `STAGGER` | 每条 45ms，上限 8 条 | 列表/卡片编排 |

动效清单（标准 12 类，全站复用）：
1. **页面转场**：旧页 `opacity .8→0 / translateY 0→-8px`，新页 `0→1 / 12px→0 + blur 6→0`，总时长 360ms。
2. **卡片入场**：`translateY(16px)+scale(.98)+blur(8px)` → 原位，stagger 45ms。
3. **数字滚动**：`useCountUp` 800ms，`EASE.emphasized`。
4. **环绘制**：SVG `stroke-dashoffset` 1.2s。
5. **骨架微光**：`--ac-gradient-shimmer` 1.4s 循环。
6. **悬停光斑**：卡片内 `radial-gradient` 跟随鼠标（CSS 变量 `--mx/--my`）。
7. **3D 倾斜**：最大 6°，spring 回弹，仅桌面端。
8. **涟漪**：按钮点击扩散 300ms。
9. **勾选描边**：成功 SVG path 描边 400ms。
10. **打字机**：AI 流式光标 `▌` 闪烁 + 逐字淡入。
11. **打字点**：typing dots 三点弹性。
12. **彩带**：canvas-confetti，成功/里程碑事件触发。

---

## 4. 技术选型与依赖清单

### 4.1 新增依赖（含体积预算）

| 依赖 | 版本 | 用途 | gzip 预算 | 加载策略 |
| --- | --- | --- | --- | --- |
| `tailwindcss` + `@tailwindcss/vite` | v4.x | 主力样式层（新组件与布局） | 运行时极小，产出按需 | 构建期 |
| `lucide-vue-next` | latest | 高品质线性图标系统 | 按需 tree-shake ≈ 8KB/100 图标 | 按需导入 |
| `gsap` | 3.x | 时间线编排（首页、登录、转场） | ~28KB（仅 core+Tween） | 动态 `import()` |
| `@vueuse/core` | 11.x | useMouse、useTransition、useScroll 等 | ~15KB | 直接引入 |
| `@vueuse/motion` | 3.x | 声明式 spring 微交互 | ~10KB | 直接引入 |
| `@formkit/auto-animate` | 0.8.x | 列表增删自动动画 | ~3KB | 直接引入 |
| `lenis` | 1.x | 惯性平滑滚动（内容区） | ~7KB | 直接引入 |
| `canvas-confetti` | 1.9.x | 成功庆祝彩带 | ~7KB | 动态 `import()` |
| `driver.js` | 1.x | 新手引导 Tour | ~12KB + CSS | 首次登录后动态 `import()` |
| `@tsparticles/vue3` + `@tsparticles/slim` | 3.x | 登录页粒子极光 | ~45KB | **仅登录页懒加载** |
| `nprogress`（或自研） | 0.2.x | 顶部路由进度条 | ~2KB | 直接用自研版（见组件清单） |
| `@number-flow/vue`（备选） | — | 高级数字翻转 | ~9KB | 若不满意 `useCountUp` 效果再引入 |
| 字体 | Inter 子集 woff2 | 数字与拉丁 | ~40KB | `preload` + `swap` |

### 4.2 明确不引入（避免过度工程）

| 候选 | 不引入理由 |
| --- | --- |
| Three.js / WebGL 3D | 收益集中在登录页，成本高、低端机风险大；用 CSS 极光 + tsParticles 替代 |
| shadcn-vue 全量迁移 | 表格/表单/弹窗全部重写成本 > 收益；采用"换肤 + 自研关键组件"混合策略 |
| Lottie 全站插画 | 需设计源文件；空状态先用 CSS/SVG 手绘动画，二期再评估 |
| 微前端/SSR/Nuxt | 与改版目标无关，保持 SPA |
| 国际化 i18n | 当前单语言，不引入 |

### 4.3 与 Element Plus 的关系（混合策略）

- **保留 EP**：`el-table`、`el-form`、`el-date-picker`、`el-select`、`el-upload`、`el-dialog` 等复杂交互组件（保证功能与可访问性）。
- **深度换肤**：通过 CSS 变量映射（见第 9 节）+ 少量 `!important` 修正，把 EP 变"自己的"。
- **自研替代**：所有"展示型"组件（卡片、统计、图表容器、状态、气泡、时间线、命令面板、Toast、确认框、分段控件等）一律自研，不受 EP 默认观感拖累。
- **新增包装**：`AcTable`、`AcForm`（EP 的薄包装，统一圆角/斑马纹/悬停高亮/空状态插槽）。

### 4.4 依赖体积控制红线

- 首屏（含 EP + ECharts 主包）gzip ≤ 550KB；当前 index 主包约 371KB，新增预算 ≈ 120KB。
- GSAP / tsParticles / confetti / driver.js 全部动态导入，不进入首屏。
- ECharts 按需引入（`echarts/core` + `LineChart/BarChart/PieChart` + `CanvasRenderer`），剥离地图/3D。

---

## 5. 应用外壳 App Shell 改造详案

> 现有 `layouts/MainLayout.vue` 一次性重写，拆分为下列子组件（`src/components/shell/`）。

### 5.1 全局背景层 `AuroraBackground.vue`

- 固定定位 `inset: 0`，三层内容：
  1. `--ac-gradient-aurora` 弥散色斑（两个伪元素缓慢漂移，`transform: translate3d` 60s 循环，`will-change: transform`）。
  2. SVG 噪点纹理（`feTurbulence`，透明度 3-5%，暗色下 6-8%），消除渐变"塑料感"。
  3. 暗色下额外叠加"星尘"点阵（CSS radial 多层，静态）。
- 登录页在背景层上再挂 tsParticles（懒加载）。
- `prefers-reduced-motion` 或 `ac-lite` 下：动画停止，仅保留静态渐变。

### 5.2 玻璃悬浮侧边栏 `AppSidebar.vue`

- 形态：左侧悬浮玻璃条（距边 16px，圆角 20px，同页面滚动独立），折叠态 72px / 展开态 240px。
- 折叠交互：悬停或点击展开，`width` spring 240ms；折叠态仅图标 + 悬浮 Tooltip（玻璃款）。
- 菜单项：图标（Lucide）+ 文案 + 可选徽标（如"待校对 3"红点）。激活项 = 渐变胶囊（`--ac-gradient-primary`，40% 透明）+ 左侧 3px 高亮条，**切换时胶囊做 FLIP 位移动画**（`gsap.to` 或 CSS `view-transition-name` 技巧）。
- 底部：深色/浅色切换 + 折叠按钮 + 版本号。
- 分组分隔："健康数据"（首页/成员/报告/运动/图表）与"智能服务"（建议/问答）两组，组标题小号大写字母间距。
- 管理后台入口仅管理员可见（带盾牌图标 + 微光描边）。

### 5.3 顶栏 `AppTopbar.vue`

- 左：面包屑（页面标题逐级，切换时右侧往左滑出 + 淡入）。
- 中：全局搜索触发器 —— 长得像输入框的按钮（`⌘/Ctrl + K` 键帽样式），点击/快捷键打开命令面板。
- 右（从左到右）：
  1. `MemberDock`：当前成员的渐变头像环 + 悬停弹出成员横向 Dock（其余成员头像上浮 + 名字淡入，点击切换，切换时全站数据淡出-淡入）。
  2. 主题切换按钮：太阳/月亮 SVG morph + **View Transition 圆形揭示动画**（从按钮圆心扩散）。
  3. 通知铃铛：暂接异步任务（解析/解读/导出）状态，有新完成时红点脉冲；下拉为玻璃 Popover 任务列表。
  4. 头像菜单：玻璃下拉（个人资料/修改密码/键盘快捷键/退出），项 hover 有左侧渐变条。
- 顶栏本体：`position: sticky; top:0`，滚动后背景由透明渐变为玻璃（`IntersectionObserver` 切换类）。

### 5.4 内容区 `AppContent.vue`

- 最大宽度 1440px 居中，页面 padding `24px`（移动 16px）。
- 内容使用 Lenis 平滑滚动（仅桌面、`reduced-motion` 关闭）。
- 页面切换容器：`<RouterView v-slot="{ Component }">` 包裹 `<Transition name="ac-page" mode="out-in">`，配合 Section 级 stagger。

### 5.5 移动端 `MobileTabbar.vue` + 手势

- `<768px`：侧边栏隐藏，底部悬浮玻璃 Tab：首页 / 报告 / 运动 / 问答 / 我的（5 项），激活项图标上浮 + 渐变底光，切换滑动指示条。
- 安全区：`padding-bottom: env(safe-area-inset-bottom)`。
- 手势：内容区下拉刷新（首页）、会话列表左右滑删除（后续可选）。

### 5.6 全局进度与瞬时反馈

- `TopProgressBar.vue`：路由切换 + 长请求（可配置）时顶部 3px 渐变进度条（自研，`nprogress` 风格但渐变+光晕）。
- `RouteAnnouncer`：转场时对屏幕阅读器播报页面名（无障碍）。
- 快捷键提示浮层（`?` 键呼出）。

---

## 6. 页面级改造详案（逐页）

> 通用规范：每页顶部为 `PageHeader`（标题 + 描述 + 操作区），卡片统一 `AcGlassCard`，空状态统一 `AcEmptyState`，加载统一 `AcSkeleton`。以下只写各页特有设计。

### 6.1 登录页 `LoginView.vue`（重写）

- 布局：左右分屏（≥1024px）。左 55% 为品牌区，右 45% 为表单区；移动端仅显示表单 + 顶部渐变 Logo。
- 品牌区：
  - 极光背景 + 缓慢漂移的光斑 + tsParticles 粒子（30-50 个，低速，颜色为主题色）。
  - 大标题"家庭健康，由 AI 读懂"（渐变文字 + 分字入场）。
  - 打字机副标语轮播："上传体检报告，10 秒读懂 / 一人一策的锻炼计划 / 和家人的健康数据对话"。
  - 三枚功能胶囊（AI 解读 / 个性建议 / 健康问答）带微光扫过。
- 表单区：
  - 玻璃卡（20px 圆角 + 渐变描边），鼠标进入有 3D 倾斜（最大 4°，离开回弹）。
  - 浮动标签输入框（自研 `AcFloatingInput`），聚焦时标签上浮 + 底线渐变展开。
  - 登录按钮：主渐变 + 悬停光泽扫过 + 点击涟漪；提交时按钮内文字切换为"加载点"形态，成功后按钮短暂变为"对勾"再跳转。
  - 失败反馈：卡片水平抖动 3 次 + 输入框红边微光 + 顶部 Toast。
- 注册页同设计语言；增加密码强度条（四段渐变色 + 文案）。

### 6.2 首页 Dashboard（重写，核心门面）

- 布局：Bento 网格（CSS Grid 12 列），桌面端：
  ```
  ┌──────────────────────────┬──────────┬──────────┐
  │ 问候 Hero（跨 6 列）        │ 健康分环  │ 快捷操作  │
  ├──────────┬──────────┬────┴────┬─────┴──────────┤
  │ BMI 卡    │ 异常指标  │ 周运动   │ 最近体检        │
  ├──────────┴──────────┴─────────┴────────────────┤
  │ 体征趋势迷你图（跨 8）      │ 近期事件时间线（跨 4） │
  └────────────────────────────────────────────────┘
  ```
- Hero：按时间问候（早上好/下午好/晚上好）+ 当前成员姓名渐变字 + 一句"今日健康提示"（来自最新异常指标的轻量提示，静态拼装不上 LLM）；右侧渐变大圆装饰缓慢旋转。
- 健康分环 `ScoreRing`：0-100 综合分（规则：异常指标数、BMI、周运动量加权，前端计算），SVG 双环（底环 + 渐变进度环 + 端点光点），中央数字滚动，下方评级文字（优秀/良好/需关注）；进入视口时环绘制 + 数字滚动。
- 统计卡 `StatCard`：图标渐变容器 + 大数字（CountUp）+ 迷你 Sparkline + 环比 TrendChip（升/降箭头 + 颜色语义：运动↑绿，异常↑红）。
- 体征迷你图：Tab 切换体重/血压/血糖，渐变面积图（60px 高），数据点 hover 显示值。
- 时间线：最近事件流（报告上传/解读完成/建议生成/运动记录），图标 + 文案 + 相对时间（"3 分钟前"），最新一条带脉冲点。
- 快捷操作：4 个磁吸按钮（上传报告/记录运动/问 AI/生成建议），hover 图标上浮 + 光晕，点击直达并携带当前成员。
- 无成员时：整页替换为"欢迎引导"（插画式 CSS 动画 + 3 步添加成员引导 + 按钮呼吸光）。

### 6.3 成员列表 `MemberListView.vue`（重写）

- 从表格改为**卡片画廊**：响应式 `repeat(auto-fill, minmax(260px, 1fr))`。
- 成员卡 `MemberCard`：
  - 顶部渐变色带（按关系不同色相：本人蓝紫/父亲靛蓝/母亲玫紫/子女薄荷/其他青灰）。
  - 大号头像（首字）带渐变环 + 呼吸光晕（当前选中成员）。
  - 姓名 + 关系胶囊 + 年龄性别。
  - BMI 迷你进度弧 + 数值；血压/体重小字。
  - 悬停：卡片上浮 4px + 光斑跟随 + 底部操作条从下方滑入（档案/编辑/删除）。
- 新增卡：虚线玻璃框 + 中央"+"图标脉动 + hover 边框流转渐变（conic-gradient 旋转）。
- 删除：卡片先"抖动 + 变红"再进入确认弹窗（玻璃款），确认后卡片缩小消失，其余卡片自动补位（AutoAnimate）。

### 6.4 成员详情 `MemberDetailView.vue`（重写）

- 头部 `ProfileHero`：玻璃面板，含头像、姓名、关系、年龄/身高/体重/BMI/血型 六宫格微卡；滚动时高度收缩为紧凑条（sticky，`IntersectionObserver` 触发）：头像缩小、描述行折叠、Tab 吸附到顶。
- Tab 改为 `SegmentedControl`（自研胶囊滑动指示器），替代 `el-tabs`。
- 既往病史：**垂直时间线**（左侧渐变轴线 + 节点按状态着色：已治愈薄荷、治疗中琥珀、长期管理珊瑚；节点悬浮脉冲）。
- 过敏史：标签墙（`SeverityChip`：轻度灰、中度琥珀、重度红 + 呼吸光），药物过敏加药丸图标。
- 长期用药：卡片列表（药名大字 + 剂量/频次胶囊 + "服用中"绿点脉冲 / "已停用"灰）；过期药卡整体降饱和。
- 家族史：关系图（CSS 简易家系节点连线，非 SVG 复杂图）或卡片列表 + 关系图标。
- 生活习惯：五组"仪表条"（吸烟/饮酒/睡眠/久坐/压力），每项为横向渐变条 + 状态标签。
- 所有新增/编辑仍用玻璃 Dialog + 浮动标签表单；保存成功弹 `SuccessCheck` 动画。

### 6.5 报告中心 `ReportListView.vue`（重写）

- 顶部：视图切换 `SegmentedControl`（列表 / 时间线），筛选区玻璃化（类型、日期范围、状态）。
- 时间线视图：左侧年份月份轨道，报告节点卡沿轨道排布；状态用 `StatusPipeline` 展示四段流程（上传→解析→待校对→已确认），当前阶段呼吸 + 后续阶段灰色。
- 列表视图：`AcTable` 精修（行悬停渐变高亮、异常数用 `CountBadge` 红点脉冲、报告名 hover 下划线渐变）。
- 上传流程升级：`AcUploader` 拖拽区（虚线渐变边框 + 拖入时边框流光 + 文件图标弹跳）；上传后立即插入列表中一条"解析中"卡片（乐观占位），带 conic 进度环 + 状态文案轮播（"正在提取文字… / AI 正在识别指标…"），完成后原地翻转成正式卡片（flip 动画）+ 若完成触发彩带（首次）。
- `ProgressRing`（conic-gradient 进度环）为解析状态组件；失败时环变红 + 抖动 + "重试"按钮。

### 6.6 报告详情 `ReportDetailView.vue`（重写）

- 顶部信息条：玻璃紧凑卡 + 状态胶囊 + 操作按钮组（校对/重解析/导出/删除）。
- 指标表格升级为 `IndicatorTable`：
  - 每行：指标名 + 分类小签 + **区间定位条**（`IndicatorBar`：灰色参考区间上标出数值位置点，偏高越界点变珊瑚色并轻微呼吸；正常点为薄荷色）。
  - 判定列：`StatusPill`（正常=薄荷点、偏高=珊瑚箭头↑、偏低=琥珀箭头↓、阳性=红+、阴性=灰-）。
  - 工具栏：搜索、筛选（全部/仅异常）、按分类分组折叠。
  - 异常行：左侧 3px 红条 + 行底微红；首次加载时异常行有一次"划过高光"动效。
- 校对模式：点击"校对指标"进入编辑态 —— 表格行变为可编辑输入（`AcFloatingInput` 紧凑款）；被修改单元格显示"已改动"琥珀描边；底部滑入吸附操作条（保存并确认 / 取消 / 恢复原值），保存成功后 diff 高光逐行闪绿一次。
- AI 解读区（`InterpretationPanel` 重写）：
  - 未生成：中央"AI 罗盘"静态插画（CSS 绘制）+ 按钮"生成解读"。
  - 生成中：`AiThinking` 组件 —— 旋转的渐变光球 + 状态文案轮播（"正在读懂 9 项指标… / 正在翻译成大白话… / 正在评估风险…"）+ 骨架卡片微光。
  - 完成后：风险横幅（`RiskBanner` 三色渐变 + 左侧仪表图标）、重点指标胶囊组、逐项卡片错峰入场（每张卡：指标名 + 状态环 + 大白话 + 建议高亮条）；卡片 hover 上浮。
  - 顶部操作：导出 PDF 按钮带下载进度（导出任务进度环嵌在按钮内）。

### 6.7 锻炼与康复建议 `SuggestionListView.vue` / `SuggestionDetailView.vue`（重写）

- 列表：建议卡（目标渐变图标 + 生成时间 + 状态胶囊 + "查看"箭头 hover 位移），空状态引导直接生成。
- 详情：
  - 现状评估：引言式排版（左侧渐变竖线引用块）。
  - 周计划：从表格升级为**周计划看板** —— 7 列（移动端纵向堆叠）卡片，每列：星期头 + 运动类型图标（渐变圆片）+ 时长胶囊 + 强度条（LOW/MEDIUM/HIGH 三段点亮）+ 内容摘要；hover 卡片抬起；今天所在列高亮描边 + "今天"角标。
  - 康复建议：卡片列表 + 频次胶囊 + 禁忌红色提示条。
  - 跟踪指标：胶囊 + 可点击"去记录"跳转运动页。
  - 导出 PDF 按钮同解读（进度内嵌）。

### 6.8 运动记录 `ExerciseView.vue`（重写）

- 顶部统计区：三枚 `StatRing`（活动时长/消耗/训练次数，圆环渐变色不同）横向排列，数字滚动。
- 日常记录：
  - **年度热力图**（`HeatmapCalendar`，GitHub 风格）：15 列 × 7 行，颜色按步数强度 5 档（从透明到薄荷-青渐变）；hover 卡片 Tooltip（日期/步数/时长/体重）；点击某日弹出右侧 Drawer 快速录入；今日格子呼吸描边。
  - 录入抽屉：玻璃 Drawer，从右滑入 400ms；字段分组（活动/体征/睡眠），保存后热力图对应格子"点亮"动画。
- 训练记录：
  - 卡片流（非表格）：左侧类型渐变图标（跑步/骑行/游泳…不同图标与色相）、标题、时长/距离/心率胶囊、强度条、消耗；hover 显示编辑/删除。
  - 顶部类型筛选胶囊组（滑动指示器）。
- 删除/新增列表使用 AutoAnimate。

### 6.9 数据分析 `ChartsView.vue`（重写）

- 布局：玻璃面板网格（2×2 + 通栏指标趋势）。
- 所有图表接入 `echarts-theme.ts` 双主题：透明背景、渐变面积、细网格线、主题文字色、Tooltip 玻璃化（`extraCssText` 圆角模糊）。
- 指标趋势：顶部指标选择器升级为 `MetricPicker`（带图标的下拉 + 最近使用 + 远程搜索高亮），数值点按判定着色（正常薄荷、异常珊瑚）+ 异常点脉冲；支持区域缩放（dataZoom，细条玻璃化）。
- 运动趋势：柱状渐变（薄荷→青）+ 消耗折线（琥珀）+ 双 Y 轴。
- 类型分布：环形图中心显示总次数（CountUp），扇区 hover 外扩 6px。
- 体征趋势：体重/血压/血糖切换，切换时图表数据 `transition` 补间（ECharts `setOption(notMerge:false)` + 动画）。
- 空数据：手绘感 SVG 插画 + "去记录一条"按钮。

### 6.10 健康问答 `ChatView.vue`（重写）

- 布局：三栏（桌面）：会话列表（260px）+ 消息流（自适应）+ 右侧"成员健康上下文卡"（可折叠，展示当前注入的档案摘要与最近异常，让用户"看见 AI 知道什么"）。
- 会话列表：悬停显示重命名/删除图标；激活项左侧渐变条 + 背景光；时间分组（今天/更早）。
- 消息气泡：
  - 用户：主渐变底 + 白色文字，右对齐，右上角小圆角差异。
  - 助手：玻璃底 + 左对齐；**Markdown 渲染**（新增 `markdown-it` + `dompurify`，代码块高亮、表格、列表），流式时末尾光标 `▌` 闪烁 + 文字逐段淡入。
  - 悬停消息出现操作条（复制/重新生成/查 token 统计）。
- "思考中"：三点弹性跳动 + 文案"正在结合体检与运动数据分析…"。
- 输入区：自增高 textarea（至 6 行）+ 发送按钮渐变圆形（内容非空时从灰变亮 + 悬停放大 1.06）；生成中变"停止生成"（方形图标 + 珊瑚色）。
- 引导：空会话显示 4 个问题胶囊（微光扫过 + hover 上浮），点击自动填充。
- 流式完成：消息底部出现 token/耗时小胶囊（淡入）。

### 6.11 管理后台 `AdminView.vue`（重写外观）

- Tab 保留但换 `SegmentedControl`；KPI 汇总卡（AI 日志页顶部）：三张渐变卡 + CountUp。
- 表格保持功能，`AcTable` 精修：粘性表头玻璃、行悬停、状态胶囊（RUNNING 带旋转小环 + 脉冲）。
- 任务队列：表格左侧新增状态点列；"重试"按钮危险态描边。
- Prompt 模板：列表项卡片化，内容预览用代码字体 + 折叠展开动画；版本号用渐变徽标；启用状态用呼吸绿点。
- 视觉与前台统一（玻璃、渐变、Token），但密度更高（紧凑表格）。

### 6.12 全局状态页

- `NotFoundView.vue`（404）：居中大号渐变"404" + 漂浮粒子 + "回到首页"按钮；路由捕获兜底。
- `ErrorView.vue`（500/异常兜底）：`app.config.errorHandler` 与路由错误。
- 骨架屏 `AcSkeleton`：卡片/表格/图表三种形态，微光扫过，替换现有 `v-loading` 主要场景（保留 EP loading 于表格局部）。
- 空状态 `AcEmptyState`：CSS 手绘插画（如空报告 = 飘动纸张、无数据 = 休眠仪表）+ 主文案 + 副文案 + 行动按钮。

---

## 7. 自研组件库清单（40+ 组件）

> 目录：`src/components/ui/`（基础）、`src/components/biz/`（业务）、`src/components/charts/`（图表）、`src/components/shell/`（外壳）。
> 所有组件：`<script setup lang="ts">` + Props/Emits 显式声明 + `ac-` 前缀 class + Token 化样式 + 支持暗色 + reduced-motion。

### 7.1 基础 UI（ui/）

| 组件 | 关键 API | 说明 |
| --- | --- | --- |
| `AcGlassCard` | `hoverable, spotlight, tilt, padding` | 玻璃卡片，支持光标光斑与 3D 倾斜 |
| `AcButton` | `variant(primary/ghost/danger/text), loading, icon, size` | 渐变/幽灵按钮 + 涟漪 + loading 形变 |
| `AcTag` / `AcStatusPill` | `tone, pulse, dot` | 胶囊标签与状态点 |
| `AcSegmented` | `v-model, options` | 胶囊滑动指示器分段控件 |
| `AcFloatingInput` | `v-model, label, error, hint` | 浮动标签输入框 |
| `AcSelect`（包 EP） | 同 EP + `variant` | 统一玻璃下拉观感 |
| `AcDialog`（包 EP） | `v-model, size, glass` | 玻璃对话框 + 弹性入场 |
| `AcDrawer`（包 EP） | `placement` | 玻璃抽屉 |
| `AcTooltip`（包 EP） | `content, placement` | 玻璃 Tooltip |
| `AcToast` + `useToast` | `toast.success/error/info/promise` | 堆叠式通知（右上/移动顶部），进度条 + 图标动画 |
| `AcConfirm` + `useConfirm` | `confirm({title, content, danger})` | 返回 Promise 的玻璃确认框 |
| `AcSkeleton` | `variant(card/table/chart), rows` | 微光骨架 |
| `AcEmptyState` | `art, title, description` + slot | 插画空状态 |
| `AcErrorState` | `code, message, retry` | 错误态 |
| `AcAvatar` | `name, src, ring, size, breathing` | 渐变环头像 |
| `AcCountUp` | `value, decimals, duration` | 数字滚动 |
| `AcSparkline` | `data, color, height` | 迷你火花线（SVG） |
| `AcScoreRing` | `value, size, stroke, label` | 健康分环 |
| `AcProgressRing` | `progress, tone, indeterminate` | 进度环（conic 或 SVG） |
| `AcHeatmapCalendar` | `data, colorScale, onSelect` | 热力图日历 |
| `AcTimeline` | `items, tone` | 垂直时间线 |
| `AcCopyButton` | `text` | 复制 + 对勾反馈 |
| `AcRelativeTime` | `datetime` | 相对时间（3 分钟前），自动刷新 |
| `AcKey` | `keys` | 键帽样式（⌘K） |
| `AcDivider` | `label` | 渐变小标题分隔 |
| `AcShimmerText` | `loading` | 微光文字（AI 生成中） |
| `AcTypewriter` | `phrases, speed` | 打字机轮播 |
| `AcSuccessCheck` | `visible` | 成功勾选描边动画 |

### 7.2 业务组件（biz/）

| 组件 | 说明 |
| --- | --- |
| `MemberCard` / `MemberAvatarDock` | 成员卡与成员 Dock |
| `HealthScoreRing` | 首页健康分（规则计算 + 环） |
| `StatCard` | 统计卡（图标/数字/火花线/趋势） |
| `IndicatorTable` / `IndicatorBar` | 指标表与区间定位条 |
| `StatusPipeline` | 报告四段状态管线 |
| `AiThinking` | AI 生成中光球 + 文案轮播 |
| `RiskBanner` | 解读风险横幅 |
| `InterpretationCard` | 解读条目卡 |
| `WeekPlanBoard` | 周计划看板 |
| `WorkoutCard` | 训练记录卡 |
| `SessionList` / `MessageBubble` / `TypingDots` / `StreamingCaret` / `SuggestedChips` | 对话套件 |
| `UploadDropzone` | 上传拖拽区（流光边框） |
| `ReviewActionBar` | 校对的吸附保存条 |
| `MetricPicker` | 指标选择器（最近使用 + 搜索高亮） |
| `TrendChip` | 环比趋势胶囊 |

### 7.3 组合式函数（composables/）

| Hook | 用途 |
| --- | --- |
| `useCountUp` | 数字滚动 |
| `useSpotlight` | 卡片光标光斑（写 `--mx/--my`） |
| `useTilt` | 3D 倾斜 |
| `useReveal` | 进入视口触发入场（IntersectionObserver + stagger） |
| `useReducedMotion` | 读取系统动效偏好 + 全局 lite 降级 |
| `useTheme` | 主题切换 + View Transition 圆形揭示 |
| `useCommandPalette` | 命令面板开关与快捷键注册 |
| `useToast` / `useConfirm` | 全局反馈 |
| `useTopProgress` | 顶部进度条（路由/请求挂钩） |
| `useNow` | 相对时间刷新 |

---

## 8. 全局交互与反馈系统

### 8.1 路由转场

- 采用 **View Transitions API 优先 + Vue Transition 兜底**：
  - 支持浏览器：`router.afterEach` 配合 `document.startViewTransition`，旧页淡出上移、新页淡入下移，列表页保留方向感（前进左滑入/后退右滑入）。
  - 不支持：`<Transition name="ac-page" mode="out-in">` 等效动画（360ms）。
- 同一页内的 Tab/内容切换不做整页转场，使用局部淡入 + stagger。

### 8.2 列表与增删动画

- `useAutoAnimate`（`@formkit/auto-animate`）接入所有 `v-for` 列表（会话、指标行、成员卡、训练卡、解读卡、任务日志），统一 200ms 位移补间。
- 删除采用"先退场后移除"模式（FLIP），避免闪现。

### 8.3 Toast / 确认 / 庆祝

- **Toast**：右上角玻璃堆叠卡（移动端顶部下拉），左侧图标带状态色渐变，底部 2s 进度条，悬停暂停；API：`toast.promise(promise, {loading, success, error})` 用于提交场景。
- **确认框**：玻璃对话框 + 危险操作红色渐变按钮 + 图标呼吸；危险删除的确认按钮需按住 0.4s 或双击？——采用常规点击但配"抖动 + 变红"预反馈。
- **庆祝**：以下事件触发 `canvas-confetti`（3 秒、主题色、`prefers-reduced-motion` 关闭）：首次上传解析成功、首次生成解读、导出 PDF 完成、连续 7 天运动打卡。
- **触觉隐喻**（视觉）：按钮按压 `scale(.97)`，开关滑动弹簧，签名式微交互。

### 8.4 加载策略

| 场景 | 方案 |
| --- | --- |
| 路由切换 | 顶部进度条 + 目标页骨架屏（不用转圈） |
| 列表首屏 | 骨架 3-6 行（形状贴合真实内容） |
| 图表 | 容器骨架（波动线动画）→ 数据到达后图表入场动画 |
| AI 生成 | `AiThinking` 阶段文案 + 骨架卡；流式对话用打字机 |
| 导出任务 | 按钮内嵌进度环 + 完成弹 Toast（带"打开文件"操作） |
| 图片/头像 | 无重图，首字头像保证零闪烁 |

### 8.5 命令面板 `CommandPalette.vue`（Ctrl/⌘ + K）

- 玻璃居中面板（600px）+ 背景全屏模糊；弹性入场。
- 分组结果：**页面**（首页/报告/运动/图表/建议/问答/管理）、**操作**（新增成员/上传报告/记录运动/生成解读/切换主题/退出登录）、**成员**（切换至 ×××）、**指标**（搜索结果直达图表页并预选指标）。
- 键盘：↑↓ 选择、Enter 执行、Esc 关闭、输入即模糊过滤（高亮命中字符）。
- 数据来源：静态注册表 + 动态（成员列表、指标搜索接口防抖）。

### 8.6 键盘快捷键（`?` 呼出帮助浮层）

| 快捷键 | 功能 |
| --- | --- |
| `Ctrl/⌘ K` | 命令面板 |
| `G` 然后 `H/M/R/E/C` | 跳转 首页/成员/报告/运动/图表（两段式，Vim 风） |
| `N` | 新建（当前语境：成员/报告/会话/训练） |
| `Ctrl/⌘ Enter` | 聊天发送 |
| `Esc` | 关闭浮层/中止流式生成 |
| `Shift + ?` | 快捷键帮助 |

### 8.7 新手引导 `OnboardingTour`（driver.js）

- 首次登录（localStorage 标记）自动启动：欢迎 → 成员切换 → 上传报告 → 生成解读 → 健康问答，共 5 步；高亮区玻璃描边 + 渐变聚光，气泡卡片可"跳过"。
- 设置页/头像菜单中可"重看引导"。

### 8.8 表单体验

- 浮动标签 + 实时校验（失焦校验，错误时字段抖动 + 错误文案滑入）。
- 提交按钮上下文 loading（"保存中…"）；成功勾形态 300ms 后弹 Toast。
- 长表单（成员表单）支持草稿自动保存 localStorage + 离开时提示恢复。
- 危险操作（删除成员）输入姓名确认（可选，Phase 4 评估）。

---

## 9. Element Plus 深度换肤映射表

> 载体：`src/styles/element-overrides.scss`。原则：只覆盖变量与必要的结构性类，不魔改组件逻辑。

| EP 变量 | 映射到 | 说明 |
| --- | --- | --- |
| `--el-color-primary` 系列（含 light-3/5/7/8/9、dark-2） | `--ac-primary` 系列 | 主色全阶梯重算（蓝紫渐变基色） |
| `--el-color-success/warning/danger` 系列 | `--ac-mint/amber/coral` 系列 | 语义色统一 |
| `--el-bg-color / page / overlay` | `--ac-surface-solid / bg-base / overlay` | 暗色玻璃近似（overlay 用半透明） |
| `--el-text-color-primary/regular/secondary/placeholder` | `--ac-text-1/2/3` | 文字统一 |
| `--el-border-color` 系列 | `--ac-border` 系列 | 描边统一为半透明 |
| `--el-fill-color / light / lighter / blank` | 玻璃填充色系 | 表格悬停/斑马纹 |
| `--el-border-radius-base / small / round` | `12 / 10 / 999` | 全局圆角 |
| `--el-box-shadow / light / lighter` | `--ac-shadow-1/2/3` | 阴影统一 |
| `--el-font-family` | `--ac-font-family` | 字体栈统一 |

结构性精修（scoped 覆盖）：

| 组件 | 改动 |
| --- | --- |
| `el-table` | 去边框、表头玻璃粘性、行悬停渐变高亮、斑马纹极淡、圆角容器 |
| `el-dialog` / `el-drawer` | 去默认阴影用 `--ac-shadow-3`、圆角 20、背景 `--ac-surface` + 模糊、入场弹性（`transition` 覆盖） |
| `el-select` / `el-dropdown` 弹层 | 玻璃底 + 圆角 14 + 阴影 -3 |
| `el-input` / `el-textarea` | 圆角 12、聚焦渐变描边（box-shadow 双层）、hover 边框强化 |
| `el-button` | 保留功能，视觉尽量改用 `AcButton`；必须用 EP 场景（表格内）做描边/悬停统一 |
| `el-tabs` | 尽量替换为 `AcSegmented`；保留场景重画指示器为渐变胶囊 |
| `el-message` | 全站不再直接使用，统一 `useToast` |
| `el-pagination` | 按钮胶囊化、激活项渐变底 |
| `el-date-picker` | 弹层玻璃化 + 今日高亮渐变 |
| `el-upload` | 仅保留请求逻辑，视觉全部由 `UploadDropzone` 承担（`el-upload` 隐藏/无样式） |

暗色适配：所有覆盖同时给 `html.dark` 分支；专门检查 EP 内部硬编码色（如 `--el-mask-color`、`--el-disabled-bg-color`）。

---

## 10. ECharts 主题化与图表升级

### 10.1 主题注册（`src/design/echarts-theme.ts`）

两套主题 `ac-light` / `ac-dark`，随主题切换 `echarts.registerTheme` + 重建实例（或 `setOption` 后 `dispose/init`），包含：

- `backgroundColor: 'transparent'`
- 文本色映射 `--ac-text-2/3`；轴线 `--ac-border`；分割线更淡
- 色板（8 色）：`#6C8CFF #7C5CFF #22D3EE #2DD4A7 #F5A524 #FF6E8A #8B5CF6 #38BDF8`
- Tooltip：玻璃底 `rgba(17,24,39,.86)` + `backdrop-filter`（`extraCssText`）+ 圆角 12 + 无边框
- 动画默认：`animationDuration: 800, animationEasing: 'cubicOut'`
- 折线：`smooth: true, symbol: 'circle', symbolSize: 7, lineStyle: { width: 2.5, shadowBlur: 12, shadowColor }`
- 面积：`LinearGradient(0,0,0,1)` 主色 35% → 0%
- 柱状：渐变 + `borderRadius: [6,6,0,0]`

### 10.2 图表专项升级

| 图表 | 升级 |
| --- | --- |
| 指标趋势 | 异常点 itemStyle 珊瑚 + `markPoint` 脉冲；dataZoom 玻璃细条；y 轴参考带 |
| 运动趋势 | 柱渐变 + 消耗折线双轴 + 图例胶囊化 |
| 类型分布 | 环形 + 中心 CountUp + 扇区 hover offset 6px + 引导线隐藏 |
| 体征趋势 | 切换时数据补间；血压双线（收缩珊瑚/舒张蓝） |
| 新增：健康分仪表 | 半圆仪表（Phase 2 首页用 AcScoreRing，可不动用 ECharts） |
| 新增：热力图 | 年度运动热力（自研 SVG/Canvas，不用 ECharts calendar，保证性能与样式） |

### 10.3 图表交互

- 图例点击切换带过渡；Tooltip 跟随延迟 20ms；空数据展示 `AcEmptyState`（不用 ECharts `title` 兜底）。
- 容器 `ResizeObserver` 自适应；暗色切换时图表平滑换肤（`setOption` 主题色）。
- 性能：图表组件路由级懒加载；同屏最多 4 图；数据量 > 365 点时启用 `sampling: 'lttb'`。

---

## 11. 性能预算与优化清单

### 11.1 预算红线

| 指标 | 红线 | 目标 |
| --- | --- | --- |
| 首屏 JS（gzip） | ≤ 650KB | ≤ 550KB |
| 首屏 CSS（gzip） | ≤ 80KB | ≤ 60KB |
| LCP（本地 prod preview） | ≤ 2.5s | ≤ 2.0s |
| CLS | ≤ 0.1 | ≤ 0.05 |
| 动画帧率 | ≥ 55fps | 60fps |
| 路由切换可交互 | ≤ 300ms | ≤ 200ms |

### 11.2 优化清单

1. **代码分割**：GSAP/tsParticles/confetti/driver.js/导出相关全部 `import()` 动态加载；ECharts 按需注册；管理后台、图表页、对话页独立 chunk（现状已具备）。
2. **字体**：Inter 子集预加载，`font-display: swap`；禁止引入完整中文字体。
3. **CSS**：Tailwind 按需产出；`content-visibility: auto` 用于长列表页；动画只使用 `transform/opacity/filter`。
4. **图片**：不引入位图资源；插画全部 CSS/SVG 内联。
5. **渲染**：列表 `key` 稳定；`v-memo` 用于消息气泡；热力图使用 Canvas 渲染（53×7=371 格不宜 371 个 DOM 节点）。
6. **网络**：axios 请求去重（同 URL 并发合并）；搜索防抖 250ms；AI 流式按帧批处理 `requestAnimationFrame` 追加快照。
7. **监控**：开发期 `rollup-plugin-visualizer` 输出包体报告，Phase 4 复核预算。
8. **降级**：`body.ac-lite`（低端机/省电模式/减动效）关闭毛玻璃、光斑、粒子、倾斜，仅保留淡入。

---

## 12. 无障碍、兼容与降级策略

| 项 | 策略 |
| --- | --- |
| 键盘可达 | 全部交互元素可 Tab 聚焦；`:focus-visible` 双环（主色 + 白）；命令面板/对话框焦点陷阱 |
| ARIA | 对话框 `role="dialog" aria-modal`；状态胶囊 `aria-label`；Toast `role="status"` |
| 对比度 | 正文 ≥ 4.5:1，大字 ≥ 3:1；暗色专项校验（text-3 不用于正文） |
| reduced-motion | 全局 `useReducedMotion`：转场降为淡入、禁用粒子/光斑/彩带/热图点亮动画 |
| 浏览器 | Chrome/Edge 111+、Safari 16.4+、Firefox 111+（View Transitions 渐进增强，不支持则自动回退） |
| 屏幕阅读器 | 路由切换播报页面标题；表单错误 `aria-describedby` |
| 触屏 | 44px 最小点击区；底部 Tab 手势；抽屉可下滑关闭 |

---

## 13. 实施阶段规划（Phase 0-4）

> 人日估算基于单人开发；每阶段结束可独立验收，主干随时保持可运行。

### Phase 0：设计基建（3-4 人日）

| # | 任务 | 产出 |
| --- | --- | --- |
| 0.1 | 安装依赖：tailwind v4、lucide、vueuse、motion、auto-animate、lenis、gsap、confetti、driver.js、tsparticles；配置 `@tailwindcss/vite` 与 preflight 隔离（避免与 EP 冲突） | `package.json`、`vite.config.ts` |
| 0.2 | 建立 `src/design/`：tokens.css（双主题全量变量）、motion.ts、typography.css | 设计系统落地 |
| 0.3 | `styles/` 重构：`tailwind.css`、`element-overrides.scss`、`utilities.css`（.ac-glass/.ac-scroll 等） | 样式四层结构 |
| 0.4 | 主题系统升级：`useTheme` + View Transition 圆形揭示 + 系统跟随 + 无闪烁首帧（`index.html` 内联脚本） | 主题切换高级化 |
| 0.5 | `AuroraBackground` + `TopProgressBar` + `AcToast/useToast` + `AcConfirm/useConfirm` + `AcSkeleton` + `AcEmptyState` | 全局氛围与反馈基座 |
| 0.6 | `CommandPalette` 骨架 + 快捷键注册表 + `OnboardingTour` 接入点 | 高级交互基座 |
| 0.7 | Inter 子集字体与预加载；`rollup-plugin-visualizer` 基线报告 | 性能基线 |

**验收**：任意旧页面挂上新背景 + 新 Toast + 路由进度条 + Ctrl+K 命令面板可开关；主题切换有圆形揭示动画；`prefers-reduced-motion` 生效。

### Phase 1：外壳 + 登录 + 首页（5-6 人日）

| # | 任务 | 产出 |
| --- | --- | --- |
| 1.1 | 重写 `MainLayout` 为 App Shell（`AppSidebar`/`AppTopbar`/`MemberDock`/`MobileTabbar`/`AppContent`） | 新骨架 |
| 1.2 | 侧边栏 FLIP 激活胶囊 + 折叠弹簧 + 分组 + 移动端 Tab | 导航高级化 |
| 1.3 | 顶栏：面包屑动画、搜索触发器、主题按钮、任务通知、头像菜单 | 顶栏完成 |
| 1.4 | 登录/注册页重写（极光 + 粒子 + 打字机 + 玻璃卡 + 3D 倾斜 + 按钮形变） | 门面页 |
| 1.5 | Dashboard Bento 重写（健康分环、StatCard、火花线、时间线、快捷操作、引导态） | 首页门面 |
| 1.6 | 页面转场（View Transition + 回退）与首屏 stagger | 转场系统 |

**验收**：登录→首页全链路"高级感"达标（录屏对比旧版）；移动端 375px 可用；无控制台错误。

### Phase 2：成员 + 报告 + 解读（6-8 人日）

| # | 任务 |
| --- | --- |
| 2.1 | `MemberCard` 画廊 + 新增卡 + AutoAnimate 增删 |
| 2.2 | 成员详情 ProfileHero 收缩头部 + `AcSegmented` + 时间线/标签墙/仪表条 |
| 2.3 | 成员表单浮动标签 + 草稿保存 |
| 2.4 | 报告列表时间线/列表双视图 + `StatusPipeline` + 乐观解析卡（`ProgressRing`） |
| 2.5 | 报告详情 `IndicatorTable` + `IndicatorBar` 区间定位 + 校对模式（diff 高亮 + 吸附操作条） |
| 2.6 | `InterpretationPanel` 重写（AiThinking 文案轮播 + RiskBanner + 卡片错峰 + 导出进度） |
| 2.7 | `ReviewActionBar`、`SuccessCheck`、首次成功彩带 |

**验收**：上传→解析→校对→解读→导出全流程动画与反馈完整；暗色模式逐屏走查无穿帮。

### Phase 3：运动 + 图表 + 建议 + 对话 + 管理（5-6 人日）

| # | 任务 |
| --- | --- |
| 3.1 | 运动页：`StatRing` 三环 + `HeatmapCalendar`（Canvas）+ 录入 Drawer + `WorkoutCard` 卡片流 |
| 3.2 | 图表页：ECharts 双主题 + 渐变图表 + 图表骨架 + MetricPicker |
| 3.3 | 建议页：`WeekPlanBoard` 七列看板 + 康复卡 + 导出进度 |
| 3.4 | 对话页：会话列表动效 + 气泡系统 + Markdown 渲染 + TypingDots/StreamingCaret + 上下文侧卡 |
| 3.5 | 管理后台换肤（KPI 渐变卡、表格精修、任务状态脉冲） |
| 3.6 | 404/500/全局错误兜底页 |

**验收**：全部 12 个路由页面完成新皮肤；功能回归通过（对照 M1-M4 验收清单）。

### Phase 4：动效润色 + 性能 + 收尾（4-5 人日）

| # | 任务 |
| --- | --- |
| 4.1 | 全站动效巡检（时长/缓动/编排统一，去掉任何"廉价感"动效） |
| 4.2 | 性能专项：包体报告核对预算、Lighthouse 跑分、长列表/热力图性能、流式渲染节流 |
| 4.3 | 无障碍：键盘全流程走查、焦点环、ARIA、对比度审计 |
| 4.4 | reduced-motion / ac-lite 全量降级测试 |
| 4.5 | 移动端逐页走查（375/390/768 三档）+ 底部 Tab 手势 |
| 4.6 | 浏览器矩阵测试（Chrome/Edge/Safari/Firefox 最近两版） |
| 4.7 | 交付：设计说明（README 章节）、验收录屏脚本、视觉走查表归档 |

**验收**：性能预算达标；15 节验收清单全绿；用户演示认可"高级感"。

### 13.1 建议实施顺序（影响面 × 观感收益）

```
Phase 0 → 1（登录+首页先亮相，立刻能看到效果）
→ 2（核心业务页：报告/解读是产品灵魂）
→ 3 → 4
```

---

## 14. 风险与回退方案

| # | 风险 | 概率 | 影响 | 对策 |
| --- | --- | --- | --- | --- |
| R1 | Tailwind preflight 与 Element Plus 样式冲突 | 中 | 中 | 关闭 preflight 或用 `@layer` 隔离，仅对 `ac-` 组件启用重置；CSS Modules/Scoped 兜底 |
| R2 | 包体超出预算 | 中 | 中 | 高体积依赖全部动态导入；visualizer 每阶段复核；超限即砍粒子/倾斜效果 |
| R3 | 动效导致低端机卡顿 | 中 | 中 | `ac-lite` 降级 + `will-change` 精准使用 + 动画只用合成属性；性能监听 FPS 自动降级（可选） |
| R4 | 暗色模式遗漏（EP 内部硬编码） | 高 | 中 | 逐组件截图对比走查表；统一 `html.dark` 分支变量 |
| R5 | View Transitions 兼容差异 | 低 | 低 | 特性检测 + Vue Transition 回退，视觉等效 |
| R6 | 改版期间功能回归 | 中 | 高 | 业务逻辑零改动红线；每页改完对照 M1-M4 验收清单跑一遍；分阶段而不是"一刀切" |
| R7 | 3D 倾斜/光斑干扰输入 | 低 | 中 | 表单卡禁用 tilt；`prefers-reduced-motion` 关闭 |
| R8 | 工期膨胀 | 中 | 中 | P0/P1 必做，P2 观感项可裁剪（如彩带、粒子、热力图）；每 Phase 末验收再继续 |

**回退机制**：每页重写前保留旧文件于 `src/views-legacy/`（git 分支亦可），如单页验收不过可 10 分钟内切回；App Shell 通过环境变量 `VITE_UI=next|classic` 双轨切换（Phase 1 期间使用，Phase 3 后移除）。

---

## 15. 验收清单

### 15.1 全局

- [ ] 亮/暗主题切换有圆形揭示动画，刷新无闪烁，跟随系统
- [ ] 全站无 Element Plus 默认蓝残留；主色渐变统一
- [ ] 路由切换有转场；顶部进度条不闪烁、不重复
- [ ] Ctrl/⌘+K 命令面板可搜页面/成员/操作并执行
- [ ] Toast/确认框/空状态/骨架屏全站统一，无 `ElMessage` 直调残留
- [ ] 页面入场 stagger 一致（45ms，最多 8 项）
- [ ] `prefers-reduced-motion: reduce` 下全部动效降级、毛玻璃关闭、无粒子
- [ ] 375px 宽度全页面可用，底部 Tab 正常
- [ ] 键盘可完成：登录 → 切成员 → 上传报告 → 生成解读 → 发消息
- [ ] Lighthouse（本地 prod）：Performance ≥ 85、Accessibility ≥ 90

### 15.2 逐页清单

| 页面 | 必达 |
| --- | --- |
| 登录/注册 | 粒子极光、打字机、浮动标签、失败抖动、按钮形变 |
| 首页 | Bento 网格、健康分环绘制、数字滚动、火花线、时间线、快捷操作磁吸 |
| 成员列表 | 卡片画廊、关系色相、悬停操作条、删除 FLIP、新增卡流光 |
| 成员详情 | 收缩头部、胶囊 Tab 滑动、病史时间线、严重度光效 |
| 报告列表 | 双视图、状态管线、解析乐观卡 + 进度环、完成彩带（首次） |
| 报告详情 | 区间定位条、异常行高光、校对 diff、吸附操作条 |
| 解读面板 | AiThinking 文案轮播、风险横幅、卡片错峰、导出进度环 |
| 运动 | 三环统计、年度热力图（Canvas）、Drawer 录入、训练卡流 |
| 图表 | 双主题、渐变面积、异常点脉冲、切换补间、图表骨架 |
| 建议 | 周计划看板、今天高亮、康复卡、导出进度 |
| 对话 | typing 点、流式光标、Markdown、消息操作条、上下文侧卡 |
| 管理后台 | KPI 渐变卡、状态脉冲、Prompt 预览折叠、暗色表格可读 |
| 404/错误页 | 插画 + 返回按钮 + 粒子/光效 |

### 15.3 演示脚本（验收录屏 3 分钟）

1. 打开登录页（极光 + 粒子）→ 登录按钮形变 → 进入首页（Bento 入场 + 健康分环绘制）
2. 按 `Ctrl+K` 切换成员 → 打开报告列表（时间线视图）→ 上传 PDF（拖拽流光 → 解析进度环 → 完成彩带）
3. 报告详情（区间条 + 异常高光）→ 生成解读（AiThinking → 卡片错峰）→ 导出 PDF（按钮进度环 → 新窗口打开）
4. 运动页（热力图点亮）→ 图表页（主题切换，图表换肤）
5. 对话页（流式打字机 + 停止）→ 切暗色主题（圆形揭示）→ 手机尺寸（底部 Tab）

---

## 16. 附录

### 16.1 文件改动总览

**重写（内容全新）**：
```
src/layouts/MainLayout.vue
src/views/auth/LoginView.vue, RegisterView.vue
src/views/dashboard/DashboardView.vue
src/views/member/MemberListView.vue, MemberDetailView.vue, MemberFormView.vue
src/views/report/ReportListView.vue, ReportDetailView.vue
src/views/exercise/ExerciseView.vue
src/views/charts/ChartsView.vue
src/views/suggestion/SuggestionListView.vue, SuggestionDetailView.vue
src/views/chat/ChatView.vue
src/views/admin/AdminView.vue
src/components/MemberSwitcher.vue, ProfileDialogs.vue, InterpretationPanel.vue, IndicatorEditorDialog.vue
src/styles/index.scss（拆分）
src/App.vue
```

**新增（约 60 文件）**：
```
src/design/tokens.css, motion.ts, echarts-theme.ts, typography.css
src/styles/tailwind.css, element-overrides.scss, utilities.css
src/components/shell/AuroraBackground.vue, AppSidebar.vue, AppTopbar.vue, AppContent.vue,
                     MemberDock.vue, MobileTabbar.vue, TopProgressBar.vue, CommandPalette.vue,
                     OnboardingTour.vue, RouteAnnouncer.vue
src/components/ui/**（约 28 个，见第 7 节）
src/components/biz/**（约 18 个）
src/components/charts/**（约 6 个）
src/composables/**（约 10 个）
src/views/system/NotFoundView.vue, ErrorView.vue
public/fonts/inter-subset.woff2
```

**修改（小改）**：
```
src/router/index.ts（页面组件替换 + 404 + 页面 meta.icon 供菜单/命令面板）
src/stores/theme.ts（View Transition + 系统跟随）
src/stores/user.ts（追加偏好设置）
src/main.ts（挂载 Toast/Confirm 宿主、图标注册、主题首帧）
index.html（内联主题脚本 + 字体 preload）
package.json / vite.config.ts（依赖与插件）
```

**不动**：`src/api/**`、`src/types/**`、`src/utils/dict.ts`、全部后端代码。

### 16.2 依赖安装命令

```bash
cd frontend
npm i tailwindcss @tailwindcss/vite lucide-vue-next gsap @vueuse/core @vueuse/motion \
      @formkit/auto-animate lenis canvas-confetti driver.js @tsparticles/vue3 @tsparticles/slim \
      markdown-it dompurify
npm i -D @types/markdown-it @types/dompurify rollup-plugin-visualizer
```

### 16.3 Token 速查卡（暗色）

```
底：#070B14  面：rgba(17,24,39,.72)  线：rgba(148,163,184,.16)
字：#E8EEF9 / #9AA7BD / #5B6B85
主渐变：#6C8CFF → #7C5CFF → #22D3EE
健康：#2DD4A7   提醒：#FFB84D   危险：#FF6E8A
圆角：卡 20 / 按钮 12 / 胶囊 999     模糊：blur(20px) saturate(160%)
动效：160/240/400ms，standard(.2,0,0,1) / emphasized(.05,.7,.1,1) / spring(.34,1.56,.64,1)
```

### 16.4 一句话验收标准

> 打开任意页面 3 秒内，必须至少触发一次"这做得挺精致"的视觉反馈（入场、光效、数字滚动或图表动画），且全程无卡顿、无穿帮、无模板感。

---

**文档结束。评审通过后从 Phase 0 开始实施。**
