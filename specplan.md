# 智能健康助手（Health Assistant）项目开发计划书

| 项目 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 编制日期 | 2026-09-12 |
| 项目代号 | health-assistant |
| 工程目录 | D:\code2\AI\health-assistant |
| 技术基线 | Java 17 + Spring Boot 3.5.x + Vue 3 + Supabase(PostgreSQL/Storage) + DeepSeek API |
| 文档状态 | 已定稿（基于 3 轮需求澄清会结论） |

---

## 目录

1. [项目概述](#1-项目概述)
2. [需求分析](#2-需求分析)
3. [技术选型](#3-技术选型)
4. [系统架构设计](#4-系统架构设计)
5. [数据库设计](#5-数据库设计)
6. [后端详细设计](#6-后端详细设计)
7. [API 接口设计](#7-api-接口设计)
8. [前端设计](#8-前端设计)
9. [安全设计](#9-安全设计)
10. [日志、监控与可观测性](#10-日志监控与可观测性)
11. [测试计划](#11-测试计划)
12. [部署方案](#12-部署方案)
13. [开发规范与协作流程](#13-开发规范与协作流程)
14. [里程碑计划与验收标准](#14-里程碑计划与验收标准)
15. [风险与对策](#15-风险与对策)
16. [二期规划](#16-二期规划)
17. [附录](#17-附录)

---

## 1. 项目概述

### 1.1 项目背景

个人与家庭健康管理存在三个普遍痛点：

1. **体检报告看不懂**：报告上全是专业术语和箭头（↑↓），普通人无法判断严重性，去医院复诊又耗费时间。
2. **数据分散**：体检报告是 PDF、运动数据在手机 App、病历在医院系统，无法形成一个人的完整健康画像。
3. **建议不个性化**：网上的"每天走一万步""多喝水"是通用建议，无法基于个人病史、指标、运动能力给出针对性方案。

本项目旨在构建一个 **Java 技术栈的智能健康助手**：一个账号管理全家人档案，用大模型把专业数据翻译成"大白话"，并基于每个人的历史健康数据提供个性化锻炼/康复建议与对话式健康咨询。

### 1.2 项目目标

| 目标 | 说明 | 可量化验收点 |
| --- | --- | --- |
| G1 多人档案 | 单账号管理多名家庭成员完整健康档案 | 支持 ≥ 6 个成员，档案含 6 大维度子信息 |
| G2 数据录入 | 手动表单 + PDF 体检报告上传 | PDF 指标自动提取准确率 ≥ 85%（可人工校对） |
| G3 大白话解读 | 异常指标逐条大白话解释 + 风险总览 | 解读结构化入库，卡片式展示 |
| G4 个性建议 | 基于档案+指标+运动的周训练计划与康复建议 | 输出结构化周计划表，可历史追溯 |
| G5 健康对话 | 基于成员历史数据的多轮对话，流式输出 | 首 token 延迟 ≤ 3s，上下文含成员画像 |
| G6 可视化 | 健康指标趋势图、运动统计图、Dashboard | 折线/柱状/饼图覆盖核心指标 |
| G7 落地部署 | Docker Compose 一键部署，前后端分离 | 单机 2C4G 可运行，含部署文档 |

### 1.3 核心价值

- **对用户**：5 分钟看懂体检报告；拿到一份可执行的每周锻炼/康复计划；随时像问医生一样问自己的健康数据。
- **对技术**：一个完整的 AI Native 应用样板：LLM 结构化输出、SSE 流式、异步任务、Prompt 工程、RAG 预留。

### 1.4 术语表

| 术语 | 含义 |
| --- | --- |
| 成员（Member） | 一个账号下的家庭成员实体，所有健康数据挂在成员维度 |
| 指标（Metric） | 体检报告中的检查项目，如谷丙转氨酶、低密度脂蛋白 |
| 指标字典 | 系统内置的指标定义库（名称、单位、参考范围、解释） |
| 参考范围 | 指标正常值区间，按性别/年龄可能不同，如 ALT 男 9-50 U/L |
| 判定状态 | 指标相对参考范围的结果：正常/偏高/偏低/异常/阳性/阴性/未知 |
| 解读（Interpretation） | AI 对一份报告所有指标的大白话解释 |
| 建议（Suggestion） | AI 生成的锻炼计划与康复建议 |
| 上下文注入 | 对话时将成员档案、指标、运动数据拼接进 System Prompt |
| Token 预算 | 单次 LLM 请求允许的最大 token 数，用于成本控制 |

---

## 2. 需求分析

### 2.1 用户角色

| 角色 | 说明 | 主要权限 |
| --- | --- | --- |
| 游客 | 未登录用户 | 注册、登录、查看产品介绍 |
| 普通用户（USER） | 注册后的主账号，管理全家档案 | 成员 CRUD、数据录入、AI 解读/建议、对话、图表、导出 |
| 管理员（ADMIN） | 系统运维者 | 用户管理、指标字典维护、Prompt 模板管理、AI 日志、任务队列 |

权限模型说明：一期采用**单账号多成员**模型。成员（Member）不是登录账号，而是挂在 `user_id` 下的数据实体。所有成员维度接口必须校验 `member.user_id == 当前登录用户 id`，防止越权（水平越权是健康类应用最大的安全风险点，需重点测试）。

### 2.2 功能需求清单

#### FR-1 账号与认证

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-1.1 | 开放注册：用户名 + 密码 + 昵称，密码 BCrypt 加密 | P0 |
| FR-1.2 | 登录颁发 JWT（access token 2h + refresh token 7d） | P0 |
| FR-1.3 | 登出：吊销 refresh token | P0 |
| FR-1.4 | 修改密码、修改个人资料 | P1 |
| FR-1.5 | 管理员可禁用/启用用户、重置密码 | P1 |

#### FR-2 多人健康档案

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-2.1 | 成员 CRUD：姓名、与账号主人关系、性别、出生日期、身高、体重、血型、头像 | P0 |
| FR-2.2 | 既往病史：疾病名称、确诊日期、当前状态（已治愈/治疗中/长期管理）、治疗情况、备注 | P0 |
| FR-2.3 | 过敏史：过敏原、类型（药物/食物/接触/其他）、反应、严重程度 | P0 |
| FR-2.4 | 长期用药：药名、剂量、频次、用法、起止日期、目的、状态 | P0 |
| FR-2.5 | 家族史：亲属关系、疾病名称、发病年龄、备注 | P0 |
| FR-2.6 | 生活习惯：吸烟、饮酒、睡眠时长/质量、运动频率、久坐时长、饮食习惯、压力水平 | P0 |
| FR-2.7 | 成员档案聚合视图（一个接口返回全部维度） | P0 |
| FR-2.8 | 自动计算并展示 BMI、年龄 | P0 |
| FR-2.9 | 成员删除（逻辑删除，同时隐藏关联数据） | P1 |

#### FR-3 体检报告与指标

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-3.1 | 上传电子版 PDF 报告（≤ 20MB，type=application/pdf） | P0 |
| FR-3.2 | PDF 文本自动提取（PDFBox），识别扫描版并给出提示 | P0 |
| FR-3.3 | 大模型将提取文本结构化为指标列表（名称/数值/单位/参考范围） | P0 |
| FR-3.4 | 指标与内置字典自动匹配（精确 + 别名 + 模糊） | P0 |
| FR-3.5 | 按性别/年龄匹配参考范围，自动判定正常/偏高/偏低/异常 | P0 |
| FR-3.6 | 人工校对界面：解析结果可增删改后再确认入库 | P0 |
| FR-3.7 | 手动录入报告（不传 PDF，直接填指标表格或粘贴文本解析） | P0 |
| FR-3.8 | 报告列表按成员/日期筛选，异常指标数红色角标 | P1 |
| FR-3.9 | 删除报告（逻辑删除，含指标明细） | P1 |
| FR-3.10 | 扫描版 PDF/图片：一期明确提示"暂不支持，请手动录入"（二期 OCR） | P1 |

#### FR-4 AI 大白话解读

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-4.1 | 手动点击"生成解读"，异步调用 DeepSeek，前端轮询任务状态 | P0 |
| FR-4.2 | 逐指标输出：大白话解释、是否需要注意、一句话建议 | P0 |
| FR-4.3 | 全报告风险总览：风险等级（低/中/高）、重点关注的 N 个指标 | P0 |
| FR-4.4 | 解读结果结构化 JSON 入库 + Markdown 冗余存储 | P0 |
| FR-4.5 | 解读历史版本：同一报告可重新生成，保留最新版本 | P1 |
| FR-4.6 | 固定输出免责声明：不构成医疗诊断，异常请就医 | P0 |

#### FR-5 个性化锻炼/康复建议

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-5.1 | 输入来源：可选"基于某报告解读"或"基于近期运动数据"或"综合" | P0 |
| FR-5.2 | 输出周训练计划：星期一~日，每天训练类型/时长/强度/具体内容/注意事项 | P0 |
| FR-5.3 | 输出康复建议：针对异常指标或病史的康复动作、频次、禁忌 | P0 |
| FR-5.4 | 结合既往病史/用药/过敏给出禁忌与预警（如膝关节病史不给跑跳计划） | P0 |
| FR-5.5 | 结构化周计划 JSON 入库，前端渲染为周计划表格 | P0 |
| FR-5.6 | 建议历史列表，可对比不同时期建议 | P1 |

#### FR-6 健康对话

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-6.1 | 多会话管理：新建、重命名、删除、列表按最近活跃排序 | P0 |
| FR-6.2 | 会话绑定成员：每次对话在成员维度下进行 | P0 |
| FR-6.3 | System Prompt 自动注入：成员基础画像 + 最近异常指标 + 最近运动统计 + 既往病史/过敏/用药 | P0 |
| FR-6.4 | SSE 流式输出（打字机效果），前端 fetch 流读取 | P0 |
| FR-6.5 | 历史上下文：最近 12 轮消息注入，超出截断并在提示词中说明 | P0 |
| FR-6.6 | 消息落库：用户与助手消息均持久化，含 token 统计 | P0 |
| FR-6.7 | 首条消息后自动生成会话标题（LLM 摘要或截断） | P1 |
| FR-6.8 | 安全护栏：拒绝诊断/处方/急救场景引导就医；回答标注数据来源 | P0 |
| FR-6.9 | Markdown 渲染、代码块、表格、复制、重新生成 | P1 |

#### FR-7 数据可视化

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-7.1 | Dashboard：成员切换器 + 健康概览（BMI、最新异常指标数、本周运动量、最近报告） | P0 |
| FR-7.2 | 指标趋势折线图：任意指标跨报告时间序列（如血糖 6 次测量） | P0 |
| FR-7.3 | 运动统计：近 7/30 天步数、运动时长、卡路里柱状图 | P0 |
| FR-7.4 | 运动类型分布饼图 | P1 |
| FR-7.5 | 体重/血压/血糖趋势（来自日常记录） | P0 |

#### FR-8 运动数据管理

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-8.1 | 日常活动记录：日期、步数、活动时长、卡路里、距离、睡眠、静息心率、体重、血压、血糖 | P0 |
| FR-8.2 | 同一成员同一天日常记录唯一，重复提交为更新 | P0 |
| FR-8.3 | 训练记录：日期、类型（跑步/步行/骑行/游泳/力量/瑜伽/球类/登山/其他）、时长、强度、距离、平均/最大心率、配速、卡路里、感受、备注 | P0 |
| FR-8.4 | 日常记录日历视图 + 快捷补录 | P1 |
| FR-8.5 | 训练记录列表筛选（类型/时间范围） | P1 |

#### FR-9 PDF 报告导出

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-9.1 | 导出报告解读为 PDF（含成员信息、指标表格、大白话解读、风险总览） | P0 |
| FR-9.2 | 导出锻炼/康复建议为 PDF（含周计划表） | P0 |
| FR-9.3 | 导出任务异步执行，完成后生成下载链接（签名 URL，60 分钟有效） | P0 |
| FR-9.4 | 中文字体正确嵌入，表格分页美观 | P0 |

#### FR-10 管理后台

| 编号 | 需求 | 优先级 |
| --- | --- | --- |
| FR-10.1 | 用户管理：列表、搜索、禁用/启用、重置密码 | P1 |
| FR-10.2 | 指标字典：分类维护、指标 CRUD、参考范围 CRUD、启用/停用、批量导入 | P1 |
| FR-10.3 | Prompt 模板：按场景维护、版本管理、启用/停用、变量说明 | P1 |
| FR-10.4 | AI 调用日志：按场景/用户/时间筛选，token 消耗与费用估算统计 | P1 |
| FR-10.5 | 异步任务列表：状态查看、失败重试 | P1 |

### 2.3 非功能需求

| 编号 | 类别 | 要求 |
| --- | --- | --- |
| NFR-1 | 性能 | 普通接口 P95 ≤ 500ms；列表分页默认 10 条；AI 流式首 token ≤ 3s |
| NFR-2 | 并发 | 单机支持 50 并发在线；AI 异步任务线程池核心 4 / 最大 8 / 队列 100 |
| NFR-3 | 成本 | 单次解读 token 消耗 ≤ 6000；单次对话上下文 ≤ 8000 token；全量 AI 调用可审计 |
| NFR-4 | 可用性 | Supabase 不可用时文件/数据功能降级提示；AI 失败可重试且不阻塞主流程 |
| NFR-5 | 安全 | 密码 BCrypt；JWT 签名 HS256；全部成员接口水平越权校验；上传文件类型白名单 |
| NFR-6 | 兼容 | Chrome/Edge/Safari 最近两年版本；响应式适配 ≥ 375px 宽度 |
| NFR-7 | 可维护 | 后端分层清晰；Flyway 管理 schema；统一异常/响应；核心逻辑单测覆盖 |
| NFR-8 | 合规 | 所有 AI 输出附免责声明；不采集身份证等非必要敏感信息（预留加密字段但不强制填） |
| NFR-9 | 国际化 | 一期仅简体中文 |
| NFR-10 | 可观测 | 全链路 traceId，AI 调用全量日志（模型/耗时/token/费用） |

### 2.4 核心用户故事与验收标准

**US-1 录入报告并解读**
> 作为用户，我想上传爸爸的体检 PDF，5 分钟内拿到他能看懂的解读。

验收标准（AC）：
1. 上传 PDF 后 3 秒内返回，任务进入 PARSING；
2. 解析完成后进入校对页，指标以表格展示，异常项标红；
3. 点击"生成解读"，30 秒内（典型 10-20 秒）展示逐指标卡片与风险总览；
4. 解读中所有大白话不含未解释的专业缩写；底部有免责声明。

**US-2 生成个性化锻炼计划**
> 作为用户，我想让助手根据妈妈的血糖指标和她的散步习惯，给一份下周的运动计划。

验收标准（AC）：
1. 选择来源"综合（报告 + 运动数据）"生成建议；
2. 输出的周计划含 7 天安排，每天有类型/时长/强度/内容；
3. 计划中考虑她的膝关节病史（既往病史中的"长期管理"项在计划中有对应禁忌说明）；
4. 建议可导出 PDF。

**US-3 健康对话**
> 作为用户，我想问"我最近三个月的血压趋势怎么样，要注意什么？"

验收标准（AC）：
1. 在"我"的会话中提问，回答中引用了我最近 3 次体检的血压值与日常记录；
2. 回答流式输出，无卡顿；
3. 回答末尾说明数据来源（哪几次报告/记录）；
4. 涉及用药建议时提示"请遵医嘱"。

### 2.5 需求边界（一期明确不做）

| 不做项 | 原因 | 二期计划 |
| --- | --- | --- |
| 图片/扫描件 OCR 解析 | 控制一期复杂度 | 接入多模态模型或云 OCR |
| RAG 知识库检索 | 先用上下文注入验证效果 | 内置慢病指南 + pgvector |
| 可穿戴设备数据导入 | 文件格式适配工作量大 | Apple Health/华为健康导出解析 |
| 成员独立账号与授权共享 | 单账号模型已满足家庭场景 | 家庭组 + 邀请码 |
| 消息推送（邮件/微信） | 非核心 | 报告完成通知 |
| 慢病专病知识库 | 同上 | 高血压/糖尿病/高血脂专题 |
| 移动端 App | 响应式 Web 已覆盖 | 可选小程序/App |
| 多语言 | 仅中文用户 | i18n |

---

## 3. 技术选型

### 3.1 技术栈总表

| 层次 | 技术 | 版本 | 说明 |
| --- | --- | --- | --- |
| 语言 | Java | 17 | 现有 pom 已指定，长期支持 |
| 后端框架 | Spring Boot | 3.5.x | 稳定版；若生态允许可评估 4.x，一期不冒险 |
| Web | Spring MVC | 内置 | 不引入 WebFlux；SSE 用 SseEmitter |
| 安全 | Spring Security + jjwt | 6.x + 0.12.x | JWT 认证 |
| ORM | MyBatis-Plus | 3.5.x（boot3 starter） | 国内主流，CRUD 高效 |
| 数据库 | Supabase PostgreSQL | 15+ | 托管 PG，免费额度 500MB |
| 连接池 | HikariCP | 内置 | 注意 pooler 模式参数 |
| 数据库迁移 | Flyway | 10.x | schema 版本化 + 种子数据 |
| 对象存储 | Supabase Storage | - | 通过官方 REST API（service_role key）服务端访问 |
| HTTP 客户端（存储） | OkHttp | 4.12.0 | 调用 Supabase Storage REST API（上传/下载/签名/删除） |
| PDF 文本提取 | Apache PDFBox | 3.x | 提取电子版 PDF 文本 |
| PDF 生成 | Flying Saucer + OpenPDF + Freemarker | 9.x / 1.3.x / 2.3.x | HTML→PDF，嵌入中文字体 |
| LLM | DeepSeek API | deepseek-chat | OpenAI 兼容，文本模型 |
| HTTP 客户端 | OkHttp + okhttp-sse | 4.x | 流式与非流式调用 |
| JSON | Jackson | 内置 | 统一序列化，Long 转 String |
| 参数校验 | Spring Validation (jakarta) | 内置 | DTO 注解校验 |
| 工具 | Lombok、MapStruct、Hutool | 1.18.x / 1.6.x / 5.8.x | 按需引入 |
| 前端框架 | Vue | 3.5.x | Composition API + `<script setup>` |
| 构建 | Vite | 6.x | 前端构建 |
| UI | Element Plus | 2.9.x | 组件库 |
| 状态 | Pinia | 2.x | 状态管理 |
| 路由 | Vue Router | 4.x | 路由 + 守卫 |
| 图表 | ECharts | 5.x | 全部图表 |
| Markdown | markdown-it + DOMPurify + highlight.js | - | 消息渲染 + XSS 过滤 |
| 流读取 | fetch + ReadableStream | - | POST SSE（EventSource 不支持 POST） |
| 测试(后端) | JUnit 5 + Mockito + Testcontainers + MockMvc | - | 单测 + 集成测试 |
| 测试(前端) | Vitest（可选） | - | 组件测试 |
| 部署 | Docker + Docker Compose + Nginx | - | 前后端两容器 |

### 3.2 关键选型理由

- **Supabase 为主存储**：用户已有免费额度、无需运维数据库；Postgres 功能强（jsonb、部分索引、窗口函数）适合健康数据分析 SQL。风险（免费项目 7 天不活跃暂停、网络波动）在 15 节给出对策。架构上 `StorageService` 保留接口抽象，本地磁盘实现作为降级/测试替身；数据源通过 `application.yml` 可切换本地 Postgres（Docker 起）。
- **MyBatis-Plus**：健康报表类查询多，XML 手写 SQL 聚合（趋势图）更直观；CRUD 由 BaseMapper 兜底，开发效率高。
- **SSE 而非 WebSocket**：对话是单向流式推送，SSE 更简单、易穿透 Nginx；SseEmitter + OkHttp SSE 客户端组合成熟。
- **PDFBox 而非 OCR**：一期只支持电子版 PDF；PDFBox 纯 Java、无外部依赖。
- **Flying Saucer + OpenPDF**：HTML/CSS 模板排版能力远强于纯代码绘制，中文字体嵌入成熟；OpenPDF 为 LGPL/MPL 许可，避免 iText AGPL 商用风险。

### 3.3 Supabase 接入专项说明（重点）

#### 3.3.1 数据库连接

Supabase 提供两种连接方式，**本项目默认使用 Supavisor Session Pooler**：

| 方式 | 主机端口 | 特点 | 项目使用 |
| --- | --- | --- | --- |
| Direct | `db.<ref>.supabase.co:5432` | 仅 IPv6，本地网络可能不通 | 备用 |
| Session Pooler | `aws-0-<region>.pooler.supabase.com:5432` | IPv4，支持 prepared statement | **默认（开发/部署）** |
| Transaction Pooler | `aws-0-<region>.pooler.supabase.com:6543` | 并发连接数友好，不支持 prepared statement | 高并发时切换，JDBC URL 需加 `?prepareThreshold=0` |

JDBC URL 示例：

```properties
# Session Pooler（默认）
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
spring.datasource.username=<db-user>.<project-ref>
spring.datasource.password=${SUPABASE_DB_PASSWORD}
```

注意事项：
1. 必须 `sslmode=require`（Supabase 强制 TLS）。
2. Session Pooler 用户名格式为 `<db-user>.<project-ref>`。默认 postgres 账号的密码不可通过 SQL 修改（非超级用户），项目使用管理 SQL 创建专用应用角色 `health_app`（LOGIN + public schema 权限），`.env` 中配置为 `health_app.<project-ref>`。
3. Flyway 配置 `baseline-on-migrate=true` + `baseline-version=0`：即使 schema 非空（Supabase 预置对象），V1 仍会正常执行。
4. 本地/CI 测试不使用云库，使用 Testcontainers PostgreSQL 16 或本地 Docker Postgres，schema 由 Flyway 在测试启动时执行，保证环境一致性。

#### 3.3.2 对象存储（Storage）——实现修订版（弃用 S3 SDK，改用官方 REST API）

> 修订说明：原计划使用 S3 兼容端点 + AWS SDK v2。实操后改为官方 Storage REST API + `service_role` key，理由：只需一个凭证（Dashboard 直接可见）、无 S3 beta 兼容性风险、OkHttp 已在依赖内，代码更轻。

1. 在 Supabase Dashboard → Storage 创建 **私有 bucket**：`health-reports`（报告）与 `health-exports`（导出文件），禁止公开访问。
2. 服务端使用 `service_role` secret（Project Settings → API，仅环境变量注入，绝不进前端）：

```properties
storage.supabase.url=https://<ref>.supabase.co
storage.supabase.service-role-key=${SUPABASE_SERVICE_ROLE_KEY}
storage.supabase.bucket-reports=health-reports
storage.supabase.bucket-exports=health-exports
```

3. REST 端点约定（OkHttp 调用，Header 同时带 `apikey` 与 `Authorization: Bearer <service_role>`）：
   - 上传：`POST /storage/v1/object/{bucket}/{objectKey}`（`x-upsert: false`）
   - 下载：`GET /storage/v1/object/{bucket}/{objectKey}`
   - 签名 URL：`POST /storage/v1/object/sign/{bucket}/{objectKey}`，body `{"expiresIn": 3600}`，返回 `signedURL`（拼接 `storage/v1` 前缀）
   - 删除：`DELETE /storage/v1/object/{bucket}/{objectKey}`
4. 上传对象 key 规范：`reports/{userId}/{memberId}/{yyyyMM}/{uuid}.pdf`，避免中文文件名与冲突问题，原始文件名存 `file_object.original_name`。
5. `StorageService` 接口设计为双实现：

```java
public interface StorageService {
    String upload(InputStream in, String objectKey, String contentType, long size);
    InputStream download(String objectKey);
    String presignedUrl(String objectKey, Duration ttl);
    void delete(String objectKey);
}
// SupabaseRestStorageServiceImpl（默认，OkHttp + service_role）
// LocalDiskStorageServiceImpl（本地开发/降级，存 ./data/storage）
```

Switch 由 `storage.type=supabase|local` 控制。

#### 3.3.3 免费版风险提示（写入部署文档）

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| 免费项目 7 天无访问自动暂停 | 演示环境首次访问慢（需唤醒） | UptimeRobot 每 3 天 ping 一次 REST 接口保活；或升级 Pro |
| 500MB 数据库上限 | 长期使用可能满 | AI 大文本字段可归档；监控用量 |
| 1GB 存储上限 | PDF 堆积可能满 | 单文件 ≤ 20MB；删除报告时同步删对象 |
| 国内网络波动 | 偶发连接失败 | 后端统一重试 + 前端友好提示；保留切本地 PG 的配置 |

---

## 4. 系统架构设计

### 4.1 总体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          浏览器（响应式 Web）                          │
│  Vue 3 + Element Plus + Pinia + ECharts + SSE 流式消息                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTPS /api/**
┌──────────────────────────────▼──────────────────────────────────────┐
│                     Nginx（前端静态资源 + 反向代理）                     │
│        gzip、SSE 专用配置（proxy_buffering off）、SPA fallback          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                   Spring Boot 后端（单体分层，端口 8080）                │
│                                                                     │
│  Controller 层 ── REST API / SSE 输出                                 │
│  Service 层 ── 业务编排                                              │
│    ├─ AuthService / MemberService / ReportService                   │
│    ├─ MetricMatchService / AbnormalJudgeService                     │
│    ├─ AiInterpretService / AiSuggestionService / ChatService        │
│    ├─ DashboardService / ExportService                              │
│    └─ AsyncTaskService（线程池调度）                                  │
│  Client 层 ── DeepSeekClient(OkHttp+SSE) / StorageClient(S3)        │
│  Mapper 层 ── MyBatis-Plus + XML 聚合 SQL                            │
└───────┬──────────────┬────────────────────┬─────────────────────────┘
        │ JDBC(TLS)    │ S3 API             │ HTTPS
┌───────▼───────┐ ┌────▼─────────────┐ ┌────▼──────────────────────┐
│  Supabase     │ │  Supabase        │ │  DeepSeek API             │
│  PostgreSQL   │ │  Storage         │ │  (chat/completions)       │
│  业务数据      │ │  PDF/导出文件     │ │  解读/建议/对话/结构化      │
└───────────────┘ └──────────────────┘ └───────────────────────────┘
```

### 4.2 后端分层架构

| 层 | 包 | 职责 | 规则 |
| --- | --- | --- | --- |
| 接入层 | `controller` | 参数校验、鉴权注解、响应包装 | 不写业务逻辑 |
| 业务层 | `service` + `service.impl` | 业务编排、事务边界、权限校验 | 接口与实现分离 |
| 领域辅助 | `support` | 指标匹配、异常判定、上下文组装、Prompt 渲染 | 无状态、可单测 |
| 客户端 | `client` | DeepSeek、Supabase S3 封装 | 只做协议转换与重试 |
| 任务层 | `task` | 异步任务执行器、线程池配置 | 幂等、可重试 |
| 数据层 | `mapper` + `entity` | 持久化 | 不含业务判断 |
| 通用层 | `common` | 统一响应、异常、常量、工具 | 无业务依赖 |

### 4.3 模块划分（Maven 单模块 + 包隔离）

```
com.health.assistant
├── common          # Result、ErrorCode、BizException、TraceId、JsonUtil、DateUtil
├── config          # Security、MyBatisPlus、Jackson、Async、Web、OpenApi、Storage 配置
├── security        # JwtTokenProvider、JwtAuthFilter、UserPrincipal、CurrentUser 注解
├── controller
├── service / service.impl
├── support         # MetricMatcher、RangeEvaluator、PromptContextBuilder、PromptTemplateRenderer、SseChatSession
├── client          # DeepSeekClient、DeepSeekStreamCallback、SupabaseStorageClient
├── task            # AsyncTaskExecutorRegistry、ReportParseTask、AiInterpretTask、AiSuggestionTask、ExportPdfTask
├── entity
├── mapper          # 接口 + resources/mapper/*.xml
├── dto             # Request DTO
├── vo              # Response VO
├── convert         # MapStruct 转换器
└── HealthAssistantApplication
```

### 4.4 关键流程设计

#### 4.4.1 注册 / 登录流程

```
注册: POST /api/auth/register
  → 校验用户名唯一 → BCrypt 加密 → 插入 sys_user → 自动登录（返回双 token）

登录: POST /api/auth/login
  → 查用户 → BCrypt.matches → 生成 access(2h)+refresh(7d)
  → refresh token SHA-256 后存 sys_user_token → 返回

请求鉴权:
  → JwtAuthFilter 解析 Authorization: Bearer <access>
  → 校验签名/过期 → 构建 SecurityContext(UserPrincipal)
  → 业务层通过 CurrentUserHolder 取 userId

刷新: POST /api/auth/refresh
  → 校验 refresh 有效性（未吊销未过期）→ 旋转 refresh token（旧的吊销）
```

#### 4.4.2 报告上传与解析流程

```
前端上传 PDF
  → FileController 校验类型/大小
  → StorageService.upload 到 Supabase Storage
  → 插入 file_object + exam_report(parse_status=UPLOADED)
  → 提交异步任务 REPORT_PARSE → 立即返回 {reportId, taskId}
  → 前端轮询 GET /api/tasks/{taskId}

后台任务 ReportParseTask:
  1. 下载 PDF → PDFBox 提取文本
  2. 判断扫描件（可提取字符数 < 50 或 字符/页数 比例过低）
     → NEED_MANUAL，任务结束，前端提示引导手动录入
  3. 文本清洗（去页眉页脚重复行、合并断行、归一全半角）
  4. DeepSeek 结构化（scene=REPORT_PARSE, json_object）
  5. 逐指标匹配 metric_dict（精确→别名→模糊）
  6. 逐指标按性别/年龄取参考范围 → RangeEvaluator 判定状态
  7. 批量插入 exam_indicator → report.parse_status=PARSED, indicator_count/abnormal_count
  8. 前端进入校对页 → 用户修改 → PUT /api/reports/{id}/indicators → CONFIRMED
```

#### 4.4.3 AI 解读流程

```
用户点击"生成解读"
  → 校验报告已 CONFIRMED
  → 创建 ai_async_task(AI_INTERPRET) + ai_interpretation(PROCESSING)
  → 返回 taskId，前端轮询

AiInterpretTask:
  1. 组装 PromptContext：成员画像 + 指标全量（名称/值/单位/参考范围/状态）+ 既往病史/过敏/用药/家族史/生活习惯
  2. 渲染 sys_prompt_template(code=AI_INTERPRET, 当前启用版本)
  3. DeepSeek 调用（json_object, temperature=0.3）
  4. 解析校验 JSON（不符合 Schema → 带错误信息修复重试 1 次）
  5. 更新 ai_interpretation(DONE, content_json/content_md/风险等级/token)
  6. 写入 ai_call_log
```

#### 4.4.4 对话流式流程（SSE）

```
POST /api/chat/sessions/{id}/messages/stream  (Accept: text/event-stream)
  → 保存用户消息 → 组装上下文（画像卡片 + 最近12轮）
  → 返回 SseEmitter，异步线程执行：
       调用 DeepSeek stream=true
       逐 chunk -> emitter.send(event=delta, data={content})
       完成 -> 保存助手消息 -> emitter.send(event=done, data={messageId,tokens}) -> complete
       异常 -> emitter.send(event=error, data={code,message}) -> complete
  → 超时 180s；客户端断开 → onCompletion 取消上游 OkHttp Call
  → 首条消息完成后异步生成会话标题
```

---

## 5. 数据库设计

### 5.1 设计约定

| 项 | 约定 |
| --- | --- |
| 数据库 | PostgreSQL 15（Supabase） |
| 字符集 | UTF8 |
| 主键 | `bigint`，MyBatis-Plus 雪花 `ASSIGN_ID`；Jackson 全局 Long→String 防 JS 精度丢失 |
| 时间 | `timestamp`（不带时区，实现修订：PG 驱动不支持 timestamptz→LocalDateTime 读取；JVM/连接会话统一 Asia/Shanghai），序列化为 `yyyy-MM-dd HH:mm:ss` |
| 逻辑删除 | `deleted smallint default 0`（0 正常 1 删除） |
| 审计字段 | `created_at`、`updated_at` 由 MyBatis-Plus 自动填充 |
| 命名 | 表/字段 snake_case；布尔语义用小整数，枚举语义用 varchar 存英文码 |
| JSON 字段 | `jsonb` + 自定义 `JsonbTypeHandler`（PGobject 包装） |
| 枚举存储 | 存英文码（如 `HIGH`），中文展示在前端字典映射 |
| 迁移 | Flyway：`V1__init_schema.sql`、`V2__seed_metric_dict.sql`、`V3__seed_prompt_template.sql` |

### 5.2 表清单（18 张）

| # | 表名 | 说明 | 所属模块 |
| --- | --- | --- | --- |
| 1 | sys_user | 用户账号 | 认证 |
| 2 | sys_user_token | refresh token | 认证 |
| 3 | family_member | 家庭成员主档 | 档案 |
| 4 | member_medical_history | 既往病史 | 档案 |
| 5 | member_allergy | 过敏史 | 档案 |
| 6 | member_medication | 长期用药 | 档案 |
| 7 | member_family_history | 家族史 | 档案 |
| 8 | member_lifestyle | 生活习惯（1:1） | 档案 |
| 9 | metric_category | 指标分类 | 字典 |
| 10 | metric_dict | 指标字典 | 字典 |
| 11 | metric_reference_range | 参考范围（多规则） | 字典 |
| 12 | exam_report | 体检报告 | 报告 |
| 13 | exam_indicator | 报告指标明细 | 报告 |
| 14 | ai_interpretation | AI 解读记录 | AI |
| 15 | ai_suggestion | AI 建议记录 | AI |
| 16 | exercise_daily | 日常活动记录 | 运动 |
| 17 | exercise_workout | 训练记录 | 运动 |
| 18 | chat_session | 对话会话 | 对话 |
| 19 | chat_message | 对话消息 | 对话 |
| 20 | ai_async_task | 异步任务 | 任务 |
| 21 | file_object | 文件对象 | 文件 |
| 22 | ai_call_log | AI 调用日志 | AI |
| 23 | sys_prompt_template | Prompt 模板 | 管理 |
| 24 | sys_audit_log | 审计日志 | 管理 |
| 25 | export_record | 导出记录 | 导出 |

### 5.3 表结构明细

#### 5.3.1 sys_user 用户表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | 雪花 ID |
| username | varchar(50) | UK, NOT NULL | 登录名，4-20 位字母数字下划线 |
| password | varchar(100) | NOT NULL | BCrypt 哈希 |
| nickname | varchar(50) | NOT NULL | 昵称 |
| phone | varchar(30) | | 手机号（选填） |
| email | varchar(100) | | 邮箱（选填） |
| avatar_url | varchar(500) | | 头像 |
| role | varchar(20) | NOT NULL DEFAULT 'USER' | USER / ADMIN |
| status | smallint | NOT NULL DEFAULT 1 | 1 启用 / 0 禁用 |
| last_login_at | timestamptz | | 最近登录时间 |
| last_login_ip | varchar(45) | | 最近登录 IP |
| created_at | timestamptz | NOT NULL | |
| updated_at | timestamptz | NOT NULL | |
| deleted | smallint | NOT NULL DEFAULT 0 | |

索引：`uk_user_username(username)`、`idx_user_phone(phone)`、`idx_user_email(email)`

#### 5.3.2 sys_user_token 刷新令牌表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | |
| user_id | bigint | NOT NULL | 用户 |
| token_hash | varchar(128) | UK, NOT NULL | refresh token 的 SHA-256 |
| expires_at | timestamptz | NOT NULL | 过期时间 |
| revoked | smallint | NOT NULL DEFAULT 0 | 是否吊销 |
| client_info | varchar(200) | | UA 摘要 |
| created_at | timestamptz | NOT NULL | |

索引：`uk_token_hash(token_hash)`、`idx_token_user(user_id)`

#### 5.3.3 family_member 家庭成员表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | |
| user_id | bigint | NOT NULL | 归属账号 |
| name | varchar(50) | NOT NULL | 姓名 |
| relation | varchar(20) | NOT NULL | SELF/FATHER/MOTHER/SPOUSE/CHILD/SIBLING/GRANDPARENT/OTHER |
| gender | smallint | NOT NULL | 1 男 / 2 女 |
| birth_date | date | | 出生日期 |
| height_cm | numeric(5,1) | | 身高 cm |
| weight_kg | numeric(5,1) | | 体重 kg（同步日常记录最新体重） |
| blood_type | varchar(10) | | A/B/AB/O/UNKNOWN |
| phone | varchar(30) | | |
| id_card_cipher | varchar(255) | | 身份证（预留，AES 加密，选填） |
| avatar_url | varchar(500) | | |
| remark | varchar(500) | | 备注 |
| sort | int | NOT NULL DEFAULT 0 | 排序 |
| created_at / updated_at | timestamptz | NOT NULL | |
| deleted | smallint | NOT NULL DEFAULT 0 | |

索引：`idx_member_user(user_id, deleted)`

#### 5.3.4 member_medical_history 既往病史

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint | PK | |
| member_id | bigint | NOT NULL | |
| disease_name | varchar(100) | NOT NULL | 疾病名称 |
| diagnosed_date | date | | 确诊日期（可只填年份） |
| disease_status | varchar(20) | NOT NULL | CURED/TREATING/MANAGING（已治愈/治疗中/长期管理） |
| treatment_desc | varchar(500) | | 治疗/用药情况 |
| hospital | varchar(100) | | 就诊医院 |
| remark | varchar(500) | | |
| created_at / updated_at / deleted | | | |

索引：`idx_mh_member(member_id, deleted)`

#### 5.3.5 member_allergy 过敏史

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id | bigint | NOT NULL |
| allergen | varchar(100) | 过敏原，如"青霉素" |
| allergy_type | varchar(20) | DRUG/FOOD/CONTACT/OTHER |
| reaction | varchar(200) | 过敏反应描述 |
| severity | varchar(10) | MILD/MODERATE/SEVERE |
| remark | varchar(500) | |
| created_at / updated_at / deleted | | |

#### 5.3.6 member_medication 长期用药

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id | bigint | NOT NULL |
| drug_name | varchar(100) | 药品名 |
| dose | varchar(50) | 剂量，如"5mg" |
| frequency | varchar(50) | 频次，如"每日 1 次" |
| route | varchar(20) | ORAL/TOPICAL/INJECTION/OTHER |
| start_date | date | 开始日期 |
| end_date | date | 结束日期（长期为空） |
| purpose | varchar(100) | 用途，如"降压" |
| status | varchar(20) | TAKING/STOPPED |
| doctor_advice | varchar(500) | 医嘱 |
| remark | varchar(500) | |
| created_at / updated_at / deleted | | |

#### 5.3.7 member_family_history 家族史

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id | bigint | NOT NULL |
| relation | varchar(20) | FATHER/MOTHER/SIBLING/GRANDPARENT/OTHER |
| disease_name | varchar(100) | 疾病名称 |
| onset_age | int | 发病年龄（可空） |
| remark | varchar(500) | |
| created_at / updated_at / deleted | | |

#### 5.3.8 member_lifestyle 生活习惯（1:1）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id | bigint | UK, NOT NULL |
| smoking | varchar(20) | NEVER/QUIT/SMOKING |
| smoking_amount | varchar(50) | 如"每日 10 支" |
| drinking | varchar(20) | NEVER/OCCASIONAL/FREQUENT/QUIT |
| drinking_amount | varchar(50) | 如"每周 3 次啤酒" |
| sleep_hours | numeric(3,1) | 平均睡眠时长 |
| sleep_quality | varchar(10) | GOOD/FAIR/POOR |
| exercise_frequency | varchar(50) | 如"每周 3 次" |
| sedentary_hours | numeric(3,1) | 日均久坐时长 |
| diet_habit | varchar(255) | 饮食习惯，如"偏咸、爱吃肉" |
| stress_level | varchar(10) | LOW/MEDIUM/HIGH |
| updated_at | timestamptz | |

#### 5.3.9 metric_category 指标分类

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| code | varchar(50) | UK，如 CBC/LFT/RFT/LIPID/GLU/THYROID/URINE/ELECTROLYTE/CARDIAC/TUMOR/INFLAMMATION/OTHER |
| name | varchar(50) | 血常规/肝功能/肾功能/血脂/血糖/甲状腺功能/尿常规/电解质/心肌酶/肿瘤标志物/炎症/其他 |
| sort | int | 排序 |
| status | smallint | 1 启用 |

#### 5.3.10 metric_dict 指标字典

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| category_id | bigint | 分类 |
| code | varchar(50) | UK，系统码，如 ALT、LDL_C、GLU |
| name | varchar(100) | 中文名，如"谷丙转氨酶" |
| name_en | varchar(100) | 英文/缩写，如"ALT" |
| alias | varchar(500) | 别名列表（英文逗号分隔），用于匹配，如"丙氨酸氨基转移酶,ALT,GPT" |
| value_type | varchar(10) | NUMERIC / TEXT（定性如尿蛋白"阴性"） |
| unit | varchar(30) | 单位，如 U/L |
| description | text | 这个指标是干嘛的（大白话） |
| high_desc | text | 偏高意味着什么 |
| low_desc | text | 偏低意味着什么 |
| sort | int | |
| status | smallint | 1 启用 |
| created_at / updated_at | | |

索引：`uk_metric_code(code)`、`idx_metric_category(category_id)`、`idx_metric_name(name)`

#### 5.3.11 metric_reference_range 参考范围

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| metric_id | bigint | 指标 |
| gender | varchar(5) | ALL / M / F |
| age_min | int | 适用年龄下限（含） |
| age_max | int | 适用年龄上限（含） |
| ref_min | numeric(12,3) | 下限（可空表示无界） |
| ref_max | numeric(12,3) | 上限 |
| ref_text | varchar(100) | 展示文本，如"9-50" |
| normal_text | varchar(100) | 定性指标正常值文本，如"阴性" |
| status | smallint | |

索引：`idx_range_metric(metric_id)`
匹配算法：按 `gender in (M/F, ALL)` 优先精确性别 → `age between age_min and age_max` → 找不到回退 `ALL + 全年龄`。

#### 5.3.12 exam_report 体检报告

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id | bigint | NOT NULL |
| user_id | bigint | NOT NULL（冗余，便于权限过滤） |
| report_name | varchar(100) | 如"2026 年度体检" |
| report_date | date | NOT NULL 体检日期 |
| institution | varchar(100) | 体检机构 |
| report_type | varchar(20) | ANNUAL/EMPLOYMENT/SPECIAL/FOLLOW_UP/OTHER |
| source_type | varchar(10) | MANUAL / PDF |
| file_id | bigint | 对应 file_object（手动录入为空） |
| parse_status | varchar(20) | UPLOADED/PARSING/PARSED/CONFIRMED/PARSE_FAILED/NEED_MANUAL |
| indicator_count | int | 指标总数 |
| abnormal_count | int | 异常数 |
| ai_status | varchar(20) | NONE/PROCESSING/DONE/FAILED（最新解读状态） |
| remark | varchar(500) | |
| created_at / updated_at / deleted | | |

索引：`idx_report_member_date(member_id, report_date DESC)`、`idx_report_user(user_id)`

#### 5.3.13 exam_indicator 报告指标明细

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| report_id | bigint | NOT NULL |
| member_id | bigint | NOT NULL |
| metric_id | bigint | 匹配到的字典 id（可空） |
| metric_code | varchar(50) | 快照 |
| metric_name | varchar(100) | 快照（未匹配时存原始名称） |
| category_name | varchar(50) | 快照分类名 |
| value_text | varchar(100) | 原始值文本，如"5.2"、"阴性" |
| value_numeric | numeric(12,3) | 数值化值（可空） |
| unit | varchar(30) | |
| ref_range_text | varchar(100) | 报告上的参考范围原文 |
| ref_min / ref_max | numeric(12,3) | 系统判定用 |
| judge_status | varchar(20) | NORMAL/HIGH/LOW/ABNORMAL/POSITIVE/NEGATIVE/UNKNOWN |
| judge_note | varchar(200) | 判定说明，如"高于参考上限 1.2 倍" |
| match_type | varchar(10) | EXACT/ALIAS/FUZZY/CUSTOM |
| source | varchar(10) | AI/MANUAL |
| sort | int | 展示顺序 |
| created_at | timestamptz | |

索引：`idx_ind_report(report_id)`、`idx_ind_member_metric(member_id, metric_id)`

#### 5.3.14 ai_interpretation AI 解读记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| report_id | bigint | NOT NULL |
| member_id / user_id | bigint | NOT NULL |
| status | varchar(20) | PROCESSING/DONE/FAILED |
| risk_level | varchar(10) | LOW/MEDIUM/HIGH |
| summary | text | 风险总览文字 |
| content_json | jsonb | 结构化卡片数据（见 6.9.2 Schema） |
| content_md | text | 冗余 Markdown（导出用） |
| abnormal_count | int | 异常数 |
| focus_metrics | jsonb | 重点关注指标编码数组 |
| model | varchar(50) | deepseek-chat |
| prompt_version | int | 模板版本 |
| token_in / token_out | int | |
| duration_ms | int | |
| error_msg | text | |
| created_at / updated_at | | |

索引：`idx_interp_report(report_id)`、`idx_interp_member(member_id)`

#### 5.3.15 ai_suggestion AI 建议记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id / user_id | bigint | NOT NULL |
| suggestion_type | varchar(20) | EXERCISE/REHAB/COMPREHENSIVE |
| source_type | varchar(20) | REPORT/EXERCISE/INTERPRETATION/MANUAL |
| source_id | bigint | 来源业务 id |
| goal | varchar(30) | FAT_LOSS/MUSCLE/GLUCOSE/BLOOD_PRESSURE/REHAB/MAINTAIN |
| status | varchar(20) | PROCESSING/DONE/FAILED |
| assessment_md | text | 现状评估 |
| weekly_plan_json | jsonb | 周计划（见 6.9.3 Schema） |
| rehab_json | jsonb | 康复建议数组 |
| precautions_md | text | 禁忌与注意事项 |
| progress_metrics_json | jsonb | 建议跟踪的指标 |
| model / prompt_version | | |
| token_in / token_out / duration_ms | | |
| error_msg | text | |
| created_at / updated_at / deleted | | |

#### 5.3.16 exercise_daily 日常活动记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id / user_id | bigint | NOT NULL |
| record_date | date | NOT NULL |
| steps | int | 步数 |
| active_minutes | int | 活动时长（分钟） |
| calories_kcal | numeric(7,1) | 活动消耗 |
| distance_km | numeric(6,2) | |
| sleep_hours | numeric(3,1) | |
| sleep_quality | varchar(10) | GOOD/FAIR/POOR |
| resting_heart_rate | int | 静息心率 |
| weight_kg | numeric(5,1) | |
| systolic_bp | int | 收缩压 |
| diastolic_bp | int | 舒张压 |
| blood_glucose | numeric(4,1) | 血糖 mmol/L |
| glucose_type | varchar(10) | FASTING/POSTPRANDIAL/RANDOM |
| mood | varchar(10) | GOOD/FAIR/BAD |
| note | varchar(500) | |
| created_at / updated_at | | |

索引：`uk_daily_member_date(member_id, record_date) WHERE deleted = 0`（部分唯一索引）、`idx_daily_date(record_date)`

#### 5.3.17 exercise_workout 训练记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id / user_id | bigint | NOT NULL |
| workout_date | date | NOT NULL |
| start_time | time | 开始时间 |
| workout_type | varchar(20) | RUNNING/WALKING/CYCLING/SWIMMING/STRENGTH/YOGA/BALL_GAME/HIKING/OTHER |
| title | varchar(100) | 如"晨跑 5 公里" |
| duration_min | int | 时长 |
| intensity | varchar(10) | LOW/MEDIUM/HIGH |
| distance_km | numeric(6,2) | |
| avg_heart_rate / max_heart_rate | int | |
| pace_sec_per_km | int | 配速（秒/公里） |
| calories_kcal | numeric(7,1) | |
| feeling | varchar(10) | EASY/MODERATE/HARD |
| note | varchar(500) | |
| created_at / updated_at / deleted | | |

索引：`idx_workout_member_date(member_id, workout_date DESC)`、`idx_workout_type(workout_type)`

#### 5.3.18 chat_session 会话表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| member_id / user_id | bigint | NOT NULL |
| title | varchar(100) | NOT NULL DEFAULT '新对话' |
| last_message_at | timestamptz | 排序用 |
| message_count | int | 消息数 |
| created_at / updated_at / deleted | | |

索引：`idx_session_member(member_id, last_message_at DESC)`、`idx_session_user(user_id)`

#### 5.3.19 chat_message 消息表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| session_id / member_id / user_id | bigint | NOT NULL |
| role | varchar(20) | USER / ASSISTANT / SYSTEM |
| content | text | 消息内容（Markdown） |
| status | varchar(20) | SENDING/DONE/FAILED |
| model | varchar(50) | |
| token_in / token_out | int | |
| duration_ms | int | |
| error_msg | text | |
| created_at | timestamptz | |

索引：`idx_msg_session(session_id, created_at)`

#### 5.3.20 ai_async_task 异步任务表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| user_id / member_id | bigint | |
| task_type | varchar(30) | REPORT_PARSE/AI_INTERPRET/AI_SUGGESTION/PDF_EXPORT/SESSION_TITLE |
| biz_type / biz_id | varchar(30) / bigint | 关联业务 |
| status | varchar(20) | PENDING/RUNNING/SUCCESS/FAILED/CANCELED |
| progress | int | 0-100 |
| payload_json | jsonb | 任务参数 |
| result_json | jsonb | 任务结果（含产物 id） |
| retry_count / max_retry | int | 默认 0/2 |
| error_msg | text | |
| started_at / finished_at | timestamptz | |
| created_at / updated_at | | |

索引：`idx_task_status_type(status, task_type)`、`idx_task_biz(biz_type, biz_id)`

#### 5.3.21 file_object 文件表

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| user_id / member_id | bigint | |
| biz_type | varchar(20) | REPORT/AVATAR/EXPORT/OTHER |
| original_name | varchar(255) | 原始文件名 |
| storage_type | varchar(10) | SUPABASE/LOCAL |
| bucket | varchar(50) | |
| object_key | varchar(500) | |
| file_size | bigint | 字节 |
| mime_type | varchar(100) | |
| sha256 | varchar(64) | 去重/完整性 |
| status | varchar(20) | ACTIVE/DELETED |
| created_at / deleted | | |

索引：`idx_file_user(user_id)`、`idx_file_member_biz(member_id, biz_type)`

#### 5.3.22 ai_call_log AI 调用日志

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| user_id | bigint | |
| task_id | bigint | 关联任务 |
| scene | varchar(20) | REPORT_PARSE/INTERPRET/SUGGESTION/CHAT/TITLE |
| model | varchar(50) | |
| request_id | varchar(64) | DeepSeek 响应 id |
| prompt_tokens / completion_tokens / total_tokens | int | |
| cost_estimate | numeric(10,6) | 费用估算（元） |
| duration_ms | int | |
| is_stream | smallint | |
| status | varchar(20) | SUCCESS/FAILED |
| error_code / error_msg | | |
| created_at | timestamptz | |

索引：`idx_log_scene_time(scene, created_at)`、`idx_log_user(user_id)`、`idx_log_task(task_id)`

#### 5.3.23 sys_prompt_template Prompt 模板

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| code | varchar(50) | 场景码，如 AI_INTERPRET |
| name | varchar(100) | 模板名 |
| scene | varchar(30) | 场景 |
| version | int | 版本号，递增 |
| content | text | 模板正文（占位符 `{{memberProfile}}`） |
| variables_json | jsonb | 变量说明数组 |
| temperature | numeric(3,2) | 建议温度 |
| max_tokens | int | |
| status | varchar(20) | ENABLED/DISABLED |
| is_default | smallint | |
| created_at / updated_at | | |

唯一：`uk_template_code_version(code, version)`

#### 5.3.24 sys_audit_log 审计日志

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| user_id / username | | |
| action | varchar(50) | LOGIN/REGISTER/EXPORT/DELETE_MEMBER/ADMIN_* |
| resource_type / resource_id | | |
| detail_json | jsonb | |
| ip varchar(45) / user_agent varchar(300) | | |
| created_at | timestamptz | |

索引：`idx_audit_user_time(user_id, created_at)`、`idx_audit_action(action)`

#### 5.3.25 export_record 导出记录

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | PK |
| user_id / member_id | bigint | |
| export_type | varchar(30) | INTERPRETATION/SUGGESTION/HEALTH_PROFILE |
| ref_id | bigint | 来源业务 id |
| file_id | bigint | 生成的 file_object |
| status | varchar(20) | PROCESSING/SUCCESS/FAILED |
| error_msg | text | |
| created_at / finished_at | | |

### 5.4 指标字典种子数据（V2）

首次迁移写入约 **70+ 项**常用指标（完整字段含描述与高低值含义），分类与示例如下：

| 分类 | 种子指标（部分示例） |
| --- | --- |
| 血常规 CBC | 白细胞 WBC、红细胞 RBC、血红蛋白 HGB、红细胞压积 HCT、平均红细胞体积 MCV、平均血红蛋白量 MCH、平均血红蛋白浓度 MCHC、血小板 PLT、中性粒细胞比率 NEUT%、淋巴细胞比率 LYM%、单核细胞比率 MONO%、嗜酸性粒细胞比率 EO% |
| 肝功能 LFT | 谷丙转氨酶 ALT、谷草转氨酶 AST、总胆红素 TBIL、直接胆红素 DBIL、间接胆红素 IBIL、总蛋白 TP、白蛋白 ALB、球蛋白 GLB、白球比 A/G、碱性磷酸酶 ALP、γ-谷氨酰转肽酶 GGT |
| 肾功能 RFT | 尿素 BUN、肌酐 CREA、尿酸 UA、估算肾小球滤过率 eGFR、胱抑素 C CysC |
| 血脂 LIPID | 总胆固醇 TC、甘油三酯 TG、高密度脂蛋白 HDL-C、低密度脂蛋白 LDL-C、载脂蛋白 A1、载脂蛋白 B |
| 血糖 GLU | 空腹血糖 GLU、糖化血红蛋白 HbA1c、空腹胰岛素 INS |
| 甲状腺 THYROID | 促甲状腺激素 TSH、游离 T3 FT3、游离 T4 FT4、总 T3、总 T4、甲状腺过氧化物酶抗体 TPOAb |
| 尿常规 URINE | 尿蛋白 PRO、尿糖 GLU-U、尿潜血 BLD、尿白细胞 LEU、尿比重 SG、尿酸碱度 pH、尿酮体 KET |
| 电解质 ELECTROLYTE | 钾 K、钠 Na、氯 Cl、钙 Ca、磷 P、镁 Mg |
| 心肌酶/心血管 CARDIAC | 肌酸激酶 CK、肌酸激酶同工酶 CK-MB、乳酸脱氢酶 LDH、同型半胱氨酸 HCY |
| 肿瘤标志物 TUMOR | 甲胎蛋白 AFP、癌胚抗原 CEA、糖类抗原 CA19-9、前列腺特异性抗原 PSA |
| 炎症 INFLAMMATION | C 反应蛋白 CRP、超敏 C 反应蛋白 hs-CRP、降钙素原 PCT |
| 其他 OTHER | 维生素 D、铁蛋白、叶酸、维生素 B12、体重指数 BMI（参考） |

每条种子示例（SQL 形态说明）：

```sql
-- metric_dict
('ALT','谷丙转氨酶','ALT','丙氨酸氨基转移酶,GPT,ALT', 'NUMERIC','U/L',
 '主要存在于肝细胞里的一种酶。肝细胞受损时它会漏到血液里，所以是反映肝脏损伤最敏感的指标之一。',
 '常见于熬夜、饮酒、脂肪肝、肝炎、药物影响；轻度升高可先复查。',
 '一般临床意义较小，可能与维生素 B6 缺乏等有关。')
-- metric_reference_range
(ALT, 'M', 18, 120, 9, 50, '9-50', null)
(ALT, 'F', 18, 120, 7, 40, '7-40', null)
(ALT, 'ALL', 0, 17, 0, 40, '0-40', null)   -- 儿童
```

Prompt 模板种子（V3）：预置 4 个场景的默认模板（完整 Prompt 见 6.9 与附录 C）。

---

## 6. 后端详细设计

### 6.1 统一响应与异常

```java
// 统一响应
public class Result<T> {
    private int code;        // 0 成功，非 0 失败
    private String message;  // 提示信息
    private T data;
    private String traceId;
}
```

分页响应：`PageResult<T> { long pageNum, pageSize, total, pages; List<T> list; }`

异常体系：

```
BizException(code, message)                    业务异常
AuthException(code, message)                   认证/授权异常
ThirdPartyException(code, message, cause)      DeepSeek/Supabase 异常
GlobalExceptionHandler(@RestControllerAdvice)
  ├─ BizException → 200 + code!=0
  ├─ MethodArgumentNotValidException → 400 + 字段错误
  ├─ AccessDeniedException → 403
  ├─ MaxUploadSizeExceededException → 413
  └─ Exception → 500 + traceId（不泄漏堆栈）
```

错误码规范（1 开头业务，4 开头客户端，5 开头服务端，9 第三方）：

| 码 | 含义 |
| --- | --- |
| 10001 | 用户名已存在 |
| 10002 | 用户名或密码错误 |
| 10003 | 账号已被禁用 |
| 10004 | refresh token 无效或已过期 |
| 10101 | 成员不存在或无权访问 |
| 10201 | 报告不存在或状态不允许操作 |
| 10202 | PDF 解析失败 |
| 10203 | 检测到扫描版 PDF，请手动录入 |
| 10301 | AI 生成失败，请稍后重试 |
| 10302 | AI 返回格式异常（已重试） |
| 10401 | 会话不存在或无权访问 |
| 10501 | 导出失败 |
| 10901 | 上传文件类型不允许 |
| 10902 | 上传文件超过大小限制 |
| 90101 | DeepSeek 调用超时 |
| 90102 | DeepSeek 限流（429） |
| 90201 | 对象存储服务异常 |

### 6.2 认证与权限设计

- `JwtTokenProvider`：HS256，access 2h / refresh 7d；claims 含 `uid`、`role`、`typ`。
- `JwtAuthFilter`（OncePerRequestFilter）：解析 Bearer token，构造 `UserPrincipal` 放 SecurityContext；白名单 `/api/auth/**`、`/api/public/**`、静态资源。
- `CurrentUserHolder`（ThreadLocal）与 `@CurrentUserId` 参数解析器。
- 方法级权限：`@PreAuthorize("hasRole('ADMIN')")` 用于管理后台。
- 水平越权防护统一封装：`MemberAccessChecker.check(userId, memberId)`，所有成员接口入口调用；MyBatis 查询一律带 `user_id = #{currentUserId}` 条件兜底。
- 刷新令牌旋转：refresh 一次有效，刷新后旧 token 立即吊销。

### 6.3 文件存储抽象

```java
public interface StorageService { // 见 3.3.2
}

@Component
@ConditionalOnProperty(name="storage.type", havingValue="supabase", matchIfMissing=true)
public class SupabaseS3StorageServiceImpl implements StorageService { ... }

@Component
@ConditionalOnProperty(name="storage.type", havingValue="local")
public class LocalDiskStorageServiceImpl implements StorageService { ... }
```

- 上传：`PutObjectRequest`，设置 `contentType`，服务端计算 sha256。
- 下载：`GetObjectRequest` → `InputStream`（仅后端内部使用，如再解析）。
- 签名链接：`S3Presigner.presignGetObject`，TTL 60 分钟。
- 删除：`DeleteObjectRequest`；报告删除时级联删除对象。
- 本地实现：文件写入 `./data/storage/{objectKey}`，`presignedUrl` 返回 `/api/files/local/{base64key}` 由后端流式下载（仅开发用）。

### 6.4 PDF 文本提取流水线（ReportParseTask）

```
[1] 下载 PDF（StorageService.download → InputStream → byte[]）
[2] PDFBox 3.x: PDDocument.load(bytes)
    - 加密文档：报告 PARSE_FAILED("PDF 已加密")
    - 逐页 PDFTextStripper.getText
[3] 扫描件判定（任一命中）：
    - 总可打印字符 < 50
    - 平均每页字符 < 30
    → parse_status=NEED_MANUAL，任务 SUCCESS（附提示），前端引导手动录入
[4] 文本清洗：去空行、去页码行（纯数字/第x页正则）、去重复行（同一行出现≥3次视为页眉页脚）、
    中文全角转半角（数字/字母/括号）、合并被换行截断的指标行
[5] 截断保护：文本 > 15000 字符时分段调用（按行切块，重叠 5 行），结果合并去重
[6] DeepSeek 结构化（json_object, temperature=0.1），输出 Schema 见 6.9.1
[7] 指标匹配与参考范围判定（见 6.5/6.6）
[8] 批量入库（每批 200 条），更新报告统计
```

### 6.5 指标匹配算法（MetricMatcher）

对 AI 解析出的指标名 `rawName`，按以下优先级匹配字典：

```
1. 标准化：去空格/换行/全角转半角/去括号内容中的英文重复/统一大小写
   例："谷丙转氨酶(ALT)" → "谷丙转氨酶"
2. 精确匹配 metric_dict.name 或 code（忽略大小写）
3. 别名匹配：按逗号切分 alias，逐项 equalsIgnoreCase
4. 模糊匹配：候选 = 名称/别名与 rawName 的
   - 包含关系（一方包含另一方）
   - 编辑距离 ≤ 2 且长度 ≥ 4
   - Jaccard 字符相似度 ≥ 0.75
   取最高分且与第二名分差 > 0.1 才采纳（防误配）
5. 均失败 → source=CUSTOM, match_type 为空，仅存原始名称与报告参考范围
```

匹配结果写 `exam_indicator.match_type`，模糊匹配的项在校对页用黄色提示"请确认指标对应关系"。

### 6.6 异常判定（RangeEvaluator）

```
输入：indicator.value_numeric/value_text + 匹配到的 metric_reference_range
规则：
1. 数值型且拿到 ref_min/ref_max：
   - value < ref_min → LOW（judge_note="低于参考下限"）
   - value > ref_max → HIGH（judge_note="高于参考上限 X 倍/超上限 X"）
   - 否则 NORMAL
2. 定性型（value_type=TEXT）：
   - 值 ∈ {阴性, 正常, 未见异常, -} → NEGATIVE/NORMAL
   - 值 ∈ {阳性, +, ++, +++} → POSITIVE
   - 其他 → ABNORMAL 或 UNKNOWN
3. 无参考范围（CUSTOM 且报告未给）→ UNKNOWN，前端灰色
4. 边界值等于上下限视为 NORMAL（含边界）
```

注意：判定以系统参考范围优先；若报告自带范围且与系统冲突，`judge_note` 记录报告范围文本供人工参考。

### 6.7 异步任务框架

```java
ThreadPoolTaskExecutor aiExecutor:
  corePoolSize=4, maxPoolSize=8, queueCapacity=100,
  threadNamePrefix="ai-task-", rejectedHandler=CallerRunsPolicy（防丢任务）

AsyncTaskService:
  submit(taskType, userId, memberId, bizType, bizId, payload): 创建 ai_async_task(PENDING) + 提交线程池
  execute: CAS 更新为 RUNNING（乐观锁：UPDATE ... SET status='RUNNING' WHERE id=? AND status='PENDING'）→ 执行 → SUCCESS/FAILED
  失败自动重试：retry_count < max_retry 时延迟 5s 重投（一期用 Thread.sleep 或 ScheduledExecutor）
  幂等：任务执行前检查业务是否已完成（如解读已 DONE 则跳过）
启动补偿：应用启动时扫描 RUNNING 且 started_at 超 10 分钟的任务标记 FAILED（僵尸任务清理），PENDING 任务重新提交
```

任务与业务表的关系：`task.result_json` 记录业务产物 id，业务表有独立 status，前端优先轮询任务，任务成功后拉业务详情。

### 6.8 DeepSeek 客户端设计

```java
public interface DeepSeekClient {
    ChatResult chat(ChatRequest req);                 // 非流式（支持 json_object）
    void chatStream(ChatRequest req, StreamCallback cb); // 流式
    record ChatResult(String content, String requestId, int promptTokens, int completionTokens, long durationMs) {}
}

public interface StreamCallback {
    void onDelta(String delta);
    void onComplete(ChatResult result);
    void onError(Throwable t);
}
```

实现要点（OkHttp + okhttp-sse）：

| 项 | 配置 |
| --- | --- |
| Base URL | `https://api.deepseek.com`（可配置，兼容代理/中转） |
| 路径 | `POST /chat/completions` |
| 模型 | `deepseek-chat`（可配置） |
| 非流式超时 | connect 10s / read 90s / write 30s |
| 流式超时 | connect 10s / read 180s |
| 温度 | 解读 0.3 / 建议 0.4 / 对话 0.7 / 解析 0.1（可被模板覆盖） |
| JSON 模式 | `response_format={"type":"json_object"}`（解读/建议/解析场景） |
| 流式 usage | `stream_options={"include_usage":true}`，末帧统计 token |
| 重试 | 429/5xx/超时：指数退避 1s、2s，最多 2 次；4xx 不重试 |
| 日志 | 每次调用写 ai_call_log（含 requestId、token、耗时、费用估算） |
| 费用估算 | 按配置单价（元/千 token）计算，仅为估算展示 |

费用估算默认参数（写配置，用户可改）：输入 2 元/百万 token，输出 8 元/百万 token（以官网现价为准，可在管理后台说明）。

### 6.9 Prompt 工程设计

#### 6.9.1 场景一：报告结构化解析（REPORT_PARSE）

System Prompt（模板 code=REPORT_PARSE）核心要素：

```
你是体检报告结构化助手。从给定的体检报告文本中提取所有检查指标。
规则：
1. 只提取文本中真实存在的指标，严禁编造；
2. value 保留原文数值或定性词（如"阴性"），不要换算单位；
3. refRange 为报告中标注的参考范围原文，没有则为 null；
4. 分类按常见医学分类归入 category；
5. 忽略姓名、年龄、日期等非指标信息（reportDate/institution 除外）。
仅输出 JSON：
{
  "reportDate": "yyyy-MM-dd | null",
  "institution": "string | null",
  "indicators": [
    {"name":"谷丙转氨酶","value":"52","unit":"U/L","refRange":"9-50","category":"肝功能"}
  ]
}
```

#### 6.9.2 场景二：大白话解读（AI_INTERPRET）

输入变量：`memberProfile`、`indicators`、`medicalHistory`、`allergy`、`medication`、`familyHistory`、`lifestyle`。

输出 JSON Schema：

```json
{
  "riskLevel": "LOW | MEDIUM | HIGH",
  "summary": "整体风险总览 2-4 句话，先结论后原因",
  "focusMetrics": ["ALT", "LDL_C"],
  "indicators": [
    {
      "name": "谷丙转氨酶",
      "value": "52",
      "unit": "U/L",
      "refRange": "9-50",
      "status": "HIGH",
      "plainExplain": "大白话解释：这个指标是看肝细胞有没有受损的……你的值略高于正常上限",
      "riskLevel": "LOW | MEDIUM | HIGH",
      "advice": "一句话建议：近期别喝酒、少熬夜，1 个月后复查"
    }
  ],
  "disclaimer": "固定文字"
}
```

System Prompt 规则要点：
1. 用初中生能听懂的话，禁止未解释的医学缩写；
2. 只基于给定数据，不编造；
3. 不做诊断、不开药；异常项建议就医或复查；
4. 结合年龄/性别/病史做个体化解读（如同样偏高的 LDL，糖尿病患者风险更高）；
5. 每条解释 50-120 字，建议 20-50 字；
6. 输出必须为合法 JSON。

#### 6.9.3 场景三：锻炼/康复建议（AI_SUGGESTION）

输出 JSON Schema：

```json
{
  "assessment": "现状评估 3-5 句：结合指标异常、运动习惯、病史",
  "goal": "FAT_LOSS | GLUCOSE | BLOOD_PRESSURE | REHAB | MAINTAIN | MUSCLE",
  "weeklyPlan": [
    {
      "day": "MONDAY",
      "workoutType": "WALKING",
      "title": "快走",
      "durationMin": 30,
      "intensity": "MEDIUM",
      "content": "具体做法：热身5分钟，快走（能说话但不能唱歌的速度）20分钟，放松5分钟",
      "notes": "膝盖不适立即停止"
    }
  ],
  "rehabAdvice": [
    {
      "target": "血糖控制",
      "action": "餐后 30 分钟散步 15 分钟",
      "frequency": "每日 3 次（三餐后）",
      "caution": "出现头晕、心慌立即停止并测血糖"
    }
  ],
  "precautions": "结合病史/用药/过敏的禁忌汇总（Markdown 列表）",
  "progressMetrics": ["空腹血糖", "体重", "每周运动时长"],
  "disclaimer": "固定文字"
}
```

System Prompt 规则要点：
1. 计划必须可执行：具体到每天做什么、多久、什么强度；
2. 有氧/力量/柔韧/休息合理搭配，每周至少 1-2 天休息或低强度；
3. 疾病约束：高血压避免憋气与爆发性运动；糖尿病随身带糖；关节病史避免高冲击；心脏病史需注明"运动前请咨询医生"；
4. 循序渐进：基线差则从每次 10-15 分钟起步；
5. 康复建议需注明禁忌与停止信号；
6. 严禁推荐药物与补剂。

#### 6.9.4 场景四：健康对话（CHAT）

System Prompt 结构（由 `PromptContextBuilder` 动态拼装）：

```
你是"健康小助手"，一位耐心、专业的健康顾问，正在与用户讨论【{{memberName}}】的健康情况。

【成员画像】
姓名/关系/性别/年龄/身高/体重/BMI/血型

【既往病史】...
【过敏史】...
【长期用药】...
【家族史】...
【生活习惯】...

【最近一次体检】日期/机构；异常指标：
- 谷丙转氨酶 52 U/L（参考 9-50，偏高）
- 低密度脂蛋白 3.8 mmol/L（参考 <3.4，偏高）
（正常指标从略）

【最近 30 天运动】共 12 次训练；周均运动 150 分钟；日均步数 6800；最近体重 72.3kg
血压记录：最近 3 次 138/86、142/88、136/84
血糖记录：最近 3 次空腹 6.4、6.1、6.3

【对话规则】
1. 只讨论健康、运动、饮食、体检相关话题，其他话题礼貌拉回；
2. 基于上方数据回答，引用时说明数据来源（如"根据 8 月 12 日的体检报告"）；
3. 数据中没有的信息，明确说"档案中没有这项记录"，不要编造；
4. 不做诊断、不开处方；紧急症状（胸痛、呼吸困难、大出血等）立即建议拨打 120/就医；
5. 用药相关问题一律引导"请咨询开药医生或药师"；
6. 回答结构清晰，适当使用列表；每次回答末尾附一句免责声明（简短版）。
```

上下文策略：
- 最近 12 轮消息全量注入（user+assistant 交替）；
- 超出部分丢弃，但在系统提示中说明"更早的对话已省略，如需可请用户重述"；
- 成员画像与数据摘要固定注入（约 800-1500 token）。

### 6.10 上下文组装与 Token 预算

`PromptContextBuilder` 按场景构建，统一预算控制：

| 场景 | 预算(输入) | 超限策略 |
| --- | --- | --- |
| REPORT_PARSE | 单块 4000 字符（约 3000 token） | 分段调用合并 |
| AI_INTERPRET | 指标最多 150 条 | 超出按异常优先后截断，summary 中说明 |
| AI_SUGGESTION | 画像 + 解读摘要 + 运动最近 30 天 | 运动数据压缩为统计摘要 |
| CHAT | 画像 1500 + 历史 12 轮 + 本轮 | 历史从最早开始丢，保留最近 |

### 6.11 图表统计接口设计（SQL 要点）

- 指标趋势：`SELECT r.report_date, i.value_numeric FROM exam_indicator i JOIN exam_report r ON ... WHERE i.member_id=? AND i.metric_code=? AND i.value_numeric IS NOT NULL ORDER BY r.report_date`（可加时间范围）。
- 运动汇总：按天/周聚合 `exercise_daily` 与 `exercise_workout`，周粒度用 `date_trunc('week', record_date)`。
- 运动类型分布：`GROUP BY workout_type` 统计次数与时长。
- Dashboard 概览：数次单查询组合（最新体重取日常记录与成员主档较新者、异常指标数取最近报告、本周运动量聚合）。

### 6.12 PDF 导出设计

```
ExportService:
  1. 创建 ai_async_task(PDF_EXPORT) + export_record(PROCESSING)
  2. 查询数据（解读 JSON 或建议 JSON + 成员信息）
  3. Freemarker 渲染 templates/pdf/interpretation.ftl | suggestion.ftl → XHTML
  4. Flying Saucer ITextRenderer：
     - addFont(Noto Sans SC Regular/Bold 字体文件, family="Noto Sans SC")
     - CSS @page 设置 A4 边距与页脚页码
     - setDocumentFromString(xhtml, baseUrl) → createPDF(out)
  5. 上传 StorageService（health-exports bucket）
  6. 生成签名 URL → export_record(SUCCESS, file_id) → task.result_json 附 URL
模板要点：中文换行（word-break）、表格斑马纹、异常值红色、风险等级色块、页脚"免责声明 + 生成时间"。
```

---

## 7. API 接口设计

### 7.1 通用规范

- 前缀：`/api`；认证：`Authorization: Bearer <accessToken>`。
- 分页参数：`pageNum`（默认 1）、`pageSize`（默认 10，最大 100）；响应 `PageResult`。
- 时间参数：`yyyy-MM-dd`（日期）/ `yyyy-MM-dd HH:mm:ss`（时间）。
- 所有响应包裹 `Result<T>`，HTTP 状态码语义化（200 业务、400 参数、401 未认证、403 无权限、404、413、500）。
- 成员维度接口统一在路径含 `memberId`：`/api/members/{memberId}/...`。

### 7.2 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 请求 | 响应 |
| --- | --- | --- | --- | --- |
| POST | /register | 注册并自动登录 | `{username, password, nickname}` | `{accessToken, refreshToken, user:{id,username,nickname,role}}` |
| POST | /login | 登录 | `{username, password}` | 同上 |
| POST | /refresh | 刷新令牌 | `{refreshToken}` | `{accessToken, refreshToken}` |
| POST | /logout | 登出（吊销 refresh） | `{refreshToken}` | `null` |
| GET | /me | 当前用户信息 | - | `UserVO` |
| PUT | /password | 修改密码 | `{oldPassword, newPassword}` | `null` |
| PUT | /profile | 修改昵称/头像 | `{nickname, avatarUrl}` | `UserVO` |

### 7.3 成员档案模块 `/api/members`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/members | 成员列表（含年龄/BMI 计算字段） |
| POST | /api/members | 新增成员 |
| GET | /api/members/{id} | 成员主档 |
| PUT | /api/members/{id} | 修改主档 |
| DELETE | /api/members/{id} | 逻辑删除（含子数据级联标记） |
| GET | /api/members/{id}/profile | **聚合档案**：主档+病史+过敏+用药+家族史+生活习惯 |
| GET/POST | /api/members/{id}/medical-histories | 病史列表/新增 |
| PUT/DELETE | /api/members/{id}/medical-histories/{hid} | 修改/删除 |
| GET/POST | /api/members/{id}/allergies | 过敏列表/新增 |
| PUT/DELETE | /api/members/{id}/allergies/{aid} | 修改/删除 |
| GET/POST | /api/members/{id}/medications | 用药列表/新增 |
| PUT/DELETE | /api/members/{id}/medications/{mid} | 修改/删除 |
| GET/POST | /api/members/{id}/family-histories | 家族史列表/新增 |
| PUT/DELETE | /api/members/{id}/family-histories/{fid} | 修改/删除 |
| GET/PUT | /api/members/{id}/lifestyle | 生活习惯查询/保存（upsert） |

新增成员请求示例：

```json
{
  "name": "张建国",
  "relation": "FATHER",
  "gender": 1,
  "birthDate": "1967-03-15",
  "heightCm": 172.0,
  "weightKg": 78.5,
  "bloodType": "A",
  "remark": ""
}
```

聚合档案响应示例（节选）：

```json
{
  "member": {"id":"1835...","name":"张建国","age":59,"bmi":26.5,"relation":"FATHER"},
  "medicalHistories": [{"diseaseName":"高血压","diseaseStatus":"MANAGING","diagnosedDate":"2018-05-01"}],
  "allergies": [{"allergen":"青霉素","allergyType":"DRUG","severity":"SEVERE"}],
  "medications": [{"drugName":"氨氯地平","dose":"5mg","frequency":"每日1次","status":"TAKING"}],
  "familyHistories": [],
  "lifestyle": {"smoking":"QUIT","drinking":"OCCASIONAL","sleepHours":6.5,"exerciseFrequency":"每周2次"}
}
```

### 7.4 指标字典模块 `/api/metrics`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/metrics/categories | 分类列表 |
| GET | /api/metrics | 分页搜索（keyword/categoryId） |
| GET | /api/metrics/{id} | 指标详情（含参考范围与解释） |

### 7.5 报告模块 `/api/reports`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/reports/upload | multipart：file + memberId + reportName + reportDate + institution + reportType；返回 `{reportId, taskId, parseStatus}` |
| POST | /api/reports/manual | 手动创建：`{memberId, reportName, reportDate, ..., indicators:[...]}`；直接 CONFIRMED |
| POST | /api/reports/{id}/parse-text | 粘贴文本解析：`{text}`，进入异步解析 |
| GET | /api/reports | 分页列表：memberId/reportType/dateFrom/dateTo；返回含 abnormalCount |
| GET | /api/reports/{id} | 报告详情（含指标明细列表） |
| PUT | /api/reports/{id}/indicators | 校对保存：全量提交指标数组（增删改），重新判定状态后 CONFIRMED |
| PUT | /api/reports/{id} | 修改报告元信息 |
| DELETE | /api/reports/{id} | 删除（含明细与文件） |
| GET | /api/reports/{id}/file-url | 获取原 PDF 签名 URL |

校对保存请求示例：

```json
{
  "indicators": [
    {"id":"1835...","metricId":"1002","metricName":"谷丙转氨酶","valueText":"52","unit":"U/L","refRangeText":"9-50","sort":1},
    {"id":null,"metricId":null,"metricName":"某新指标","valueText":"1.2","unit":"x","refRangeText":null,"sort":2}
  ]
}
```

### 7.6 AI 解读 / 建议模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/reports/{id}/interpretations | 触发解读，返回 `{interpretationId, taskId}`；报告需 CONFIRMED |
| GET | /api/reports/{id}/interpretations/latest | 最新解读详情（status=DONE 时含 contentJson） |
| POST | /api/members/{memberId}/suggestions | 生成建议：`{suggestionType, sourceType, sourceId, goal}` → `{suggestionId, taskId}` |
| GET | /api/members/{memberId}/suggestions | 建议分页历史 |
| GET | /api/suggestions/{id} | 建议详情 |
| DELETE | /api/suggestions/{id} | 删除建议 |

### 7.7 运动模块 `/api/members/{memberId}/exercise`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/PUT | /daily?date=yyyy-MM-dd | 查询/保存某天日常记录（upsert） |
| GET | /daily/range?from=&to= | 范围查询（日历视图） |
| DELETE | /daily/{id} | 删除 |
| GET/POST | /workouts | 训练记录分页/新增 |
| PUT/DELETE | /workouts/{id} | 修改/删除 |
| GET | /workouts/summary?from=&to= | 统计：总次数/总时长/总卡路里/按类型分布 |

### 7.8 图表模块 `/api/charts/{memberId}`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /overview | Dashboard 概览卡片数据 |
| GET | /indicator-trend?metricCode=&from=&to= | 指标趋势 [{date, value, status}] |
| GET | /exercise-trend?granularity=day|week&from=&to= | 运动量趋势 |
| GET | /workout-distribution?from=&to= | 类型分布 |
| GET | /vital-trend?type=weight|bp|glucose&from=&to= | 日常体征趋势 |

### 7.9 对话模块 `/api/chat`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /sessions?memberId= | 会话列表（最近活跃排序） |
| POST | /sessions | 新建会话 `{memberId, title?}` |
| PUT | /sessions/{id} | 重命名 |
| DELETE | /sessions/{id} | 删除（含消息） |
| GET | /sessions/{id}/messages?pageNum=&pageSize= | 历史消息分页（倒序拉取，正序展示） |
| POST | /sessions/{id}/messages/stream | **SSE 流式**：`{content}`，`Accept: text/event-stream` |
| POST | /messages/{id}/regenerate | 重新生成最后一条助手回复（SSE） |

SSE 事件协议：

```
event: delta
data: {"content":"根据你"}

event: delta
data: {"content":"8月12日的体检报告..."}

event: done
data: {"messageId":"1835...","tokenIn":1820,"tokenOut":356,"durationMs":8200}

event: error
data: {"code":10301,"message":"AI 服务暂时不可用，请稍后重试"}
```

前端实现：`fetch(url, {method:'POST', headers:{Authorization, 'Content-Type':'application/json'}, body})` → `response.body.getReader()` → `TextDecoder` 按 `\n\n` 解析事件块。

### 7.10 文件与导出模块

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/files/{id}/url | 获取文件签名 URL（校验归属） |
| POST | /api/exports | 创建导出任务：`{exportType, refId}` → `{exportId, taskId}` |
| GET | /api/exports/{id} | 导出状态与下载 URL |
| GET | /api/exports?memberId= | 导出历史 |

### 7.11 任务模块 `/api/tasks`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/tasks/{id} | 任务状态：`{id, taskType, status, progress, errorMsg, resultJson}` |
| POST | /api/tasks/{id}/retry | 手动重试失败任务（校验归属） |

前端轮询建议：间隔 2s，连续 30 次未完成提示"仍在处理"；任务 FAILED 立即停止并展示错误。

### 7.12 管理后台 `/api/admin`（role=ADMIN）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /users | 用户分页（keyword/status） |
| PUT | /users/{id}/status | 禁用/启用 |
| PUT | /users/{id}/reset-password | 重置密码（返回临时密码） |
| GET/POST | /metrics | 指标字典分页/新增 |
| PUT/DELETE | /metrics/{id} | 修改/停用 |
| POST | /metrics/{id}/ranges | 维护参考范围 |
| GET/POST | /prompts | 模板列表/新增版本 |
| PUT | /prompts/{id}/status | 启用/停用（同 code 启用新版本自动停旧版本） |
| GET | /ai-logs | AI 日志分页 + 汇总统计（按天 token/费用） |
| GET | /tasks | 任务分页（status/taskType） |

---

## 8. 前端设计

### 8.1 工程结构

```
frontend/
├── index.html
├── vite.config.ts          # dev proxy: /api -> http://localhost:8080
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/                # axios 封装 + 各模块 api 文件
│   │   ├── request.ts      # 拦截器：token、401 刷新、错误提示、traceId
│   │   ├── auth.ts / member.ts / report.ts / exercise.ts
│   │   ├── chat.ts / chart.ts / export.ts / admin.ts
│   ├── stores/             # Pinia: user / member(当前成员) / theme
│   ├── router/index.ts     # 路由 + 登录守卫 + admin 守卫
│   ├── layouts/            # MainLayout(侧边栏+顶栏+成员切换) / AdminLayout
│   ├── views/
│   │   ├── auth/           # Login.vue / Register.vue
│   │   ├── dashboard/      # Dashboard.vue
│   │   ├── member/         # MemberList.vue / MemberDetail.vue(档案聚合 6 个 Tab) / MemberForm.vue
│   │   ├── report/         # ReportList.vue / ReportUpload.vue / ReportDetail.vue / IndicatorReview.vue
│   │   ├── interpretation/ # InterpretationView.vue（卡片式解读）
│   │   ├── suggestion/     # SuggestionList.vue / SuggestionDetail.vue（周计划表）
│   │   ├── exercise/       # DailyRecord.vue(日历) / WorkoutList.vue / WorkoutForm.vue
│   │   ├── charts/         # ChartsView.vue
│   │   ├── chat/           # ChatView.vue（会话侧栏+消息流）
│   │   ├── export/         # ExportCenter.vue
│   │   └── admin/          # Users.vue / Metrics.vue / Prompts.vue / AiLogs.vue / Tasks.vue
│   ├── components/         # 通用组件（见 8.4）
│   ├── composables/        # useSseChat.ts / useTaskPolling.ts / useChartTheme.ts
│   ├── styles/             # 主题变量、深色模式、全局样式
│   └── utils/              # 日期、格式化、字典映射、markdown 渲染
└── package.json
```

### 8.2 路由表

| 路径 | 页面 | 权限 | 说明 |
| --- | --- | --- | --- |
| /login | Login | 公开 | |
| /register | Register | 公开 | |
| / | Dashboard | USER | 首页概览 |
| /members | MemberList | USER | |
| /members/new, /members/:id/edit | MemberForm | USER | 分步表单 |
| /members/:id | MemberDetail | USER | 档案聚合 Tab |
| /reports | ReportList | USER | |
| /reports/upload | ReportUpload | USER | |
| /reports/:id | ReportDetail | USER | 指标表格 + 解读入口 |
| /reports/:id/review | IndicatorReview | USER | 解析校对 |
| /interpretations/:id | InterpretationView | USER | |
| /suggestions | SuggestionList | USER | |
| /suggestions/:id | SuggestionDetail | USER | |
| /exercise | ExerciseHome | USER | 日历 + 训练列表 |
| /charts | ChartsView | USER | |
| /chat | ChatView | USER | |
| /exports | ExportCenter | USER | |
| /admin/* | admin 页面 | ADMIN | 独立布局 |

### 8.3 页面交互细节（关键页面）

**Dashboard**
- 顶部成员切换器（下拉 + 头像 + 快速新增成员），切换后全局 Pinia `currentMember` 更新，所有页面跟随。
- 概览卡片 4 个：BMI（含区间色块）、最近异常指标数（红/绿）、本周运动时长、最近体检日期。
- 快捷操作：上传报告 / 记录运动 / 问健康助手（跳转对话并自动带当前成员）。
- 下方两图：近 90 天体重/血压趋势、近 30 天运动量柱状图。

**IndicatorReview（校对页）**
- 顶部提示条：解析出 N 项，其中异常 M 项，请校对后确认。
- 表格可编辑：指标名（带字典匹配下拉）、值、单位、参考范围；行操作：删除；底部：添加一行。
- 模糊匹配行黄色高亮，悬浮提示"自动匹配为 XXX，请确认"。
- 底部按钮：重新解析 / 保存并确认。

**InterpretationView**
- 风险总览横幅（低=绿/中=橙/高=红），重点指标 chips。
- 指标卡片列表：卡片头（指标名 + 值 + 状态标签），卡体（大白话解释），卡片脚（一句话建议）。
- 顶部操作：导出 PDF / 基于此生成锻炼建议 / 重新生成。
- 底部免责声明。

**SuggestionDetail**
- 现状评估段落。
- 周计划表：表头 周一~周日，每格显示类型图标/时长/强度/内容摘要，点击展开详情；移动端改为垂直列表。
- 康复建议卡片列表。
- 禁忌与注意事项（Markdown）。
- 操作：导出 PDF / 重新生成。

**ChatView**
- 左侧：会话列表（新建按钮、重命名/删除悬浮菜单、按成员过滤）。
- 中间：消息流（气泡、Markdown 渲染、流式打字机光标、消息操作：复制/重新生成）。
- 输入区：多行输入（Enter 发送，Shift+Enter 换行）、发送/停止按钮、当前成员提示条（"正在咨询：爸爸"）。
- 首次进入空会话展示引导问题 chips（"最近体检有什么要注意的？""帮我制定本周运动计划"）。

**MemberForm（分步表单）**
- 步骤：基础信息 → 既往病史 → 过敏史 → 用药 → 家族史 → 生活习惯。
- 每一步可跳过；子项列表支持行内新增/编辑抽屉。
- 保存时一次性提交聚合 payload。

### 8.4 核心组件清单

| 组件 | 说明 |
| --- | --- |
| MemberSwitcher | 全局成员切换器 |
| MetricStatusTag | 指标状态标签（正常/偏高/偏低/异常配色） |
| MetricCard | 解读卡片 |
| WeeklyPlanTable | 周计划表格 |
| TrendLineChart / SportBarChart / TypePieChart | ECharts 封装（自适应尺寸、主题联动） |
| TaskProgress | 异步任务进度组件（轮询 + 状态展示，可复用于解析/解读/导出） |
| MarkdownRenderer | markdown-it + highlight.js + DOMPurify |
| EmptyState / PageHeader / ConfirmDelete | 通用 UI |
| FileUploader | 支持拖拽、类型大小校验、进度条 |
| SseChatStream | 封装 fetch 流式读取（onDelta/onDone/onError/abort） |

### 8.5 状态与请求封装

- `request.ts`：axios 实例；请求拦截注入 accessToken；响应拦截统一处理 `code!=0`（ElMessage.error）；401 时用 refresh token 静默刷新并重放原请求（并发刷新加锁，刷新失败跳登录）。
- `userStore`：token 持久化 localStorage（refresh）、access 内存存储；用户信息。
- `memberStore`：当前选中成员 id 持久化 localStorage；切换后广播刷新。
- `useTaskPolling(taskId, onDone)`：通用轮询 composable。
- `useSseChat(sessionId)`：封装 SSE 流解析与中止。

### 8.6 主题与响应式

- 医疗蓝主色：`#2F7DE1`，辅助青绿 `#2BB3A3`，异常红 `#E5484D`，警告橙 `#F5A524`，成功绿 `#30A46C`。
- Element Plus 变量覆盖 + `html.dark` 深色模式（CSS vars 全量适配，图表主题同步切换，偏好存 localStorage，跟随系统可开关）。
- 响应式断点：≥1200 桌面（侧边栏展开）、768-1199 平板（侧边栏收起图标）、<768 移动（底部 TabBar：首页/报告/记录/对话/我的；表格改卡片列表）。
- 字体：系统字体栈 + 数字使用等宽优化（指标数值对齐）。

---

## 9. 安全设计

一期按"基础安全"执行，具体清单：

| 项 | 措施 |
| --- | --- |
| 密码 | BCrypt（strength 10），注册校验复杂度（8-32 位，含字母数字） |
| JWT | HS256，密钥 ≥ 32 字节从环境变量读取；access 2h / refresh 7d；refresh 存哈希且可吊销 |
| 越权 | 所有 memberId/bizId 接口调用 `MemberAccessChecker`；SQL 层附加 user_id 条件 |
| 上传 | 类型白名单（application/pdf）、大小 ≤ 20MB、文件名不使用（用 UUID 作 object key） |
| XSS | 前端 Markdown 渲染经 DOMPurify 过滤；后端不直接输出 HTML |
| SQL 注入 | MyBatis `#{}` 参数化；动态排序字段白名单校验 |
| 敏感信息 | 错误响应不含堆栈；日志不打印密码/token/API Key；身份证字段预留 AES 加密但一期不强制采集 |
| 限流（轻量） | 登录/注册接口 IP 维度简单计数限流（内存 Caffeine，如 10 次/分钟）；AI 接口用户维度每日上限（如 50 次/天，可配置） |
| CORS | 开发环境放开 Vite 源；生产同域部署无需 CORS |
| 审计 | 登录、导出、删除成员、管理操作写入 sys_audit_log |
| 依赖安全 | 定期 `mvn versions:display-dependency-updates` + OWASP Dependency-Check（可选 CI 步骤） |

---

## 10. 日志、监控与可观测性

| 项 | 方案 |
| --- | --- |
| 日志框架 | SLF4J + Logback；控制台 + 滚动文件（`logs/app.log`，按天 + 100MB 滚动，保留 30 天） |
| traceId | `TraceIdFilter`：每请求生成 UUID，MDC 注入，响应头 `X-Trace-Id`，日志格式含 traceId |
| 业务日志 | 关键节点结构化记录：报告解析耗时/指标数、AI 场景/耗时/token、任务状态流转 |
| AI 可观测 | ai_call_log 全量落库；管理后台按天聚合 token 与费用 |
| 健康检查 | Spring Boot Actuator：`/actuator/health`、`/actuator/metrics` 仅内网/管理端口暴露 |
| 异常告警（可选） | 日志中 ERROR 计数；二期接入邮件/Webhook |
| 前端监控（可选） | 全局 errorHandler + 未捕获 Promise 上报后端（一期仅控制台） |

---

## 11. 测试计划

### 11.1 单测清单（JUnit5 + Mockito）

| 被测对象 | 测试点 |
| --- | --- |
| JwtTokenProvider | 签发/解析/过期/篡改签名 |
| MetricMatcher | 精确/别名/全半角/括号/模糊匹配/误配防护 |
| RangeEvaluator | 数值高低/边界/定性/无范围 UNKNOWN/性别年龄选择 |
| PromptContextBuilder | 各场景上下文组装、超限截断、敏感信息不泄漏 |
| PromptTemplateRenderer | 占位符替换、缺失变量、HTML 转义 |
| DeepSeekResponseParser | JSON 解析、markdown 代码块剥离、非法 JSON 修复重试路径 |
| StorageService(Local) | 上传/下载/删除/签名 URL |
| ExitRuleScanner/汇总 SQL | 图表聚合（Testcontainers PG 上验证） |

### 11.2 集成测试（MockMvc + Testcontainers PostgreSQL）

| 测试 | 内容 |
| --- | --- |
| AuthFlowIT | 注册 → 登录 → 访问受保护接口 → 刷新 → 登出后 refresh 失效 |
| MemberIT | 成员 CRUD + 聚合档案 + 越权访问（用户 B 访问用户 A 的成员应 403/404） |
| ReportParseIT | 上传 PDF（测试资源）→ Mock DeepSeek → 解析 → 校对 → 确认 → 异常判定正确 |
| InterpretIT | 触发解读 → Mock DeepSeek 返回固定 JSON → 落库 → 查询结果 |
| ChatStreamIT | 流式接口返回 SSE、消息落库、Mock 上游流 |
| ExportIT | 生成 PDF（断言文件非空、包含成员姓名） |
| AdminIT | 非管理员访问 /api/admin/** 返回 403 |

Mock DeepSeek：`@MockBean DeepSeekClient`（或本地 WireMock 模拟 OpenAI 兼容响应，含流式分块）。

### 11.3 测试数据与策略

- 测试资源：`src/test/resources/pdf/` 放置 3 份自制电子版 PDF（不同排版）。
- 数据库：Testcontainers `postgres:16`，Flyway 自动迁移 + 种子数据。
- 覆盖率目标：核心模块行覆盖 ≥ 60%（JaCoCo 报告，不作为 CI 强制门禁，一期人工审查）。
- 前端：Vitest 覆盖 `useSseChat` 解析逻辑与工具函数（可选）。

### 11.4 手工验收测试清单（里程碑收尾执行）

- 注册新账号 → 建 3 个成员 → 每人录 1 份报告 + 5 天运动 → 生成解读/建议 → 对话 5 轮 → 导出 PDF → 手机浏览器全流程走一遍。
- 异常场景：上传非 PDF、超大文件、扫描件 PDF、断开网络重连、AI Key 失效。

---

## 12. 部署方案

### 12.1 环境准备（M0）

| 项 | 版本/说明 |
| --- | --- |
| JDK | 17（Temurin） |
| Maven | 3.9+ |
| Node.js | 20 LTS + pnpm/npm |
| Docker Desktop | 24+（本地构建/运行） |
| Supabase 账号 | 创建项目，记录 Project Ref / DB 密码 / S3 Keys |
| DeepSeek 账号 | 创建 API Key，充值少量额度 |
| IDE 插件 | Lombok、MyBatisX、Vue - Official |

### 12.2 Supabase 初始化步骤

1. 创建项目（区域按需选择，本项目实际为 `ap-south-1`），保存数据库密码（仅 Dashboard 重置用，应用不依赖它）。
2. 用管理 SQL（或 Dashboard SQL Editor）创建专用应用角色：
   `CREATE ROLE health_app LOGIN PASSWORD '...'; GRANT CONNECT ON DATABASE postgres TO health_app; GRANT USAGE, CREATE ON SCHEMA public TO health_app; ALTER ROLE health_app SET search_path = public;`
3. Storage 创建 `health-reports`、`health-exports` 两个 **私有** bucket。
4. Project Settings → API 复制 `service_role` secret（服务端专用）。
5. Connect 页面获取 Session Pooler 连接串（主机 + 用户名格式 `<db-user>.<project-ref>`）。
6. 应用启动时 Flyway 自动执行迁移建表。
7. 配置 UptimeRobot（或 cron-job.org）每 3 天请求一次 `https://<ref>.supabase.co/rest/v1/`（带 apikey）保活。

### 12.3 环境变量清单（.env.example）

```dotenv
# ===== 数据库（Supabase Session Pooler） =====
SUPABASE_DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=health_app.<project-ref>
SUPABASE_DB_PASSWORD=change-me

# ===== 对象存储（Supabase Storage REST API） =====
STORAGE_TYPE=supabase
SUPABASE_URL=https://<ref>.supabase.co
SUPABASE_SERVICE_ROLE_KEY=change-me
SUPABASE_BUCKET_REPORTS=health-reports
SUPABASE_BUCKET_EXPORTS=health-exports

# ===== DeepSeek =====
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=sk-xxxx
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_PRICE_INPUT_PER_M=2.0
DEEPSEEK_PRICE_OUTPUT_PER_M=8.0

# ===== 应用 =====
JWT_SECRET=至少32字节的随机字符串
JWT_ACCESS_EXPIRE_MINUTES=120
JWT_REFRESH_EXPIRE_DAYS=7
AI_DAILY_LIMIT_PER_USER=50
```

### 12.4 后端 Dockerfile（多阶段）

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xms256m","-Xmx768m","-jar","app.jar"]
```

### 12.5 前端 Dockerfile + Nginx

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

`nginx.conf` 关键配置：

```nginx
server {
    listen 80;
    client_max_body_size 25m;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        # SSE 关键配置
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
}
```

### 12.6 docker-compose.yml

```yaml
services:
  backend:
    build: .
    container_name: health-backend
    env_file: .env
    restart: unless-stopped
    networks: [health-net]
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  frontend:
    build: ./frontend
    container_name: health-frontend
    ports: ["80:80"]
    depends_on: [backend]
    restart: unless-stopped
    networks: [health-net]

networks:
  health-net:
```

说明：数据库不需要容器（Supabase 托管）；若切换本地模式，增加可选 profile：

```yaml
  postgres:
    image: postgres:16
    profiles: ["local-db"]
    environment:
      POSTGRES_DB: health
      POSTGRES_USER: health
      POSTGRES_PASSWORD: health123
    volumes: ["pgdata:/var/lib/postgresql/data"]
```

### 12.7 本地开发运行

```
后端:  mvn spring-boot:run -Dspring-boot.run.profiles=dev   (读取 .env 或 IDE 环境变量)
前端:  cd frontend && npm run dev                            (Vite 代理 /api → localhost:8080)
测试:  mvn verify                                            (Testcontainers 需 Docker 运行)
构建:  mvn clean package && cd frontend && npm run build
部署:  docker compose --env-file .env up -d --build
```

### 12.8 部署验收步骤

1. `docker compose up -d` → 两容器 Up，healthcheck 通过；
2. 浏览器访问 `http://服务器IP/` → 注册 → 建成员 → 全流程手工验收（11.4）；
3. 检查 Supabase Dashboard：表有数据、Storage 有文件；
4. 管理后台查看 AI 日志有记录；导出 PDF 可下载打开。

---

## 13. 开发规范与协作流程

### 13.1 Git 分支与提交

| 项 | 约定 |
| --- | --- |
| 分支 | `main`（可发布）、`develop`（集成分支）、`feature/<模块>-<简述>`、`fix/<简述>` |
| 提交信息 | Conventional Commits：`feat: 新增报告上传接口`、`fix: 修复指标匹配空指针`、`docs:`、`test:`、`chore:` |
| 粒度 | 一个功能点一次提交，禁止巨型提交 |
| 合并 | feature → develop 自测通过后合并；里程碑完成 develop → main 打 tag（v0.1.0 ~ v1.0.0） |

### 13.2 编码规范

- 遵循阿里巴巴 Java 开发手册：命名、常量、集合、并发、异常处理；
- 后端：Controller 不写业务；Service 事务边界明确（`@Transactional(rollbackFor = Exception.class)`）；Controller/DTO/VO 不暴露 Entity；
- 前端：`<script setup lang="ts">`、组件 PascalCase、组合式函数 `useXxx`、样式 scoped；
- 不允许魔法值：状态/枚举用常量类与前端字典文件统一维护；
- 接口文档：集成 springdoc-openapi，访问 `/swagger-ui.html`（仅 dev 环境开启）。

### 13.3 目录与配置管理

- 分支保护：main 禁止直接推送（个人项目可放宽，但建议）；
- `.env` 不入库（.gitignore），提供 `.env.example`；
- `application.yml` + `application-dev.yml` + `application-prod.yml`，敏感项全部环境变量注入；
- 日志、上传测试文件、node_modules、target 全部 gitignore。

---

## 14. 里程碑计划与验收标准

总工期估算：单人开发约 **34 人日（约 7 周）**。每阶段结束进行演示与验收后再进入下一阶段。

### M1 基础底座（8 人日）

| # | 任务 | 人日 |
| --- | --- | --- |
| 1 | 后端工程搭建：pom 依赖、包结构、统一响应/异常、Jackson（Long→String）、Actuator、Swagger | 1 |
| 2 | Supabase 项目初始化 + Flyway V1 全量表结构 + 本地 Testcontainers 配置 | 1.5 |
| 3 | 认证模块：注册/登录/刷新/登出/JWT 过滤器/限流 + 单测 | 2 |
| 4 | StorageService 抽象 + Supabase S3 实现 + Local 实现 + 文件上传下载接口 | 1 |
| 5 | 成员模块：主档 CRUD + 5 个子资源 + 聚合档案 + MemberAccessChecker | 2 |
| 6 | 前端工程搭建：Vite + Element Plus + 主题 + axios 封装 + 登录注册页 + MainLayout + 成员切换器 + 成员管理页面 | 待计（见下） |

> 说明：前端与后端并行推进。M1 前端工作量 4 人日，含在下一行总账中。

**M1 验收标准**
1. 注册登录全流程可用，token 过期自动刷新；
2. 新建成员并在 6 个档案 Tab 中增删改查数据成功；
3. 用户 B 无法访问用户 A 的成员数据（越权测试用例通过）；
4. Flyway 在全新 Supabase 库上一键建表成功。

### M2 数据资产（10 人日）

| # | 任务 | 人日 |
| --- | --- | --- |
| 1 | 指标字典：V2 种子数据（70+ 指标与参考范围）+ 字典查询接口 + 管理端字典页 | 2 |
| 2 | 报告模块：上传/手动创建/列表/详情/删除 + 文件关联 | 1.5 |
| 3 | ReportParseTask：PDFBox 提取 + 扫描件判定 + 文本清洗 + Mock 联调 | 2 |
| 4 | DeepSeekClient 非流式实现 + 结构化解析 + MetricMatcher + RangeEvaluator + 校对接 | 2.5 |
| 5 | 运动模块：日常记录 upsert + 训练记录 CRUD + 统计接口 | 1 |
| 6 | 前端：报告列表/上传/详情/校对页 + 运动页面（日历/训练列表/表单） + 图表页（趋势/柱状/饼图） | 1（前端部分另计） |

**M2 验收标准**
1. 上传一份电子版 PDF，指标自动提取并匹配字典，异常判定正确（与人工核对）；
2. 校对页可编辑保存；扫描件 PDF 得到明确提示；
3. 运动数据录入与图表展示正确（含 7/30 天口径）；
4. MetricMatcher/RangeEvaluator 单测通过。

### M3 智能核心（8 人日）

| # | 任务 | 人日 |
| --- | --- | --- |
| 1 | 异步任务框架：任务表 + 线程池 + 状态机 + 轮询接口 + 启动补偿 | 1.5 |
| 2 | Prompt 模板体系：模板表 + V3 种子 + 渲染器 + 管理接口 | 1.5 |
| 3 | AI 解读：上下文组装 + JSON Schema 校验修复 + 落库 + 前端解读页 | 2 |
| 4 | AI 建议：综合输入 + 周计划 Schema + 前端周计划表 | 1.5 |
| 5 | AI 日志与费用统计 + 管理后台日志页 | 1 |
| 6 | PDF 导出：Freemarker + Flying Saucer 中文字体 + 导出中心页 | 1.5 |

**M3 验收标准**
1. 解读结果结构完整（每条指标有解释与建议），JSON 校验通过率 100%（含修复重试）；
2. 建议输出 7 天计划且含病史禁忌（用高血压+膝关节炎测试样例验证）；
3. 导出 PDF 中文不乱码、表格不溢出，可下载；
4. AI 日志记录 token 与耗时，与 DeepSeek 后台账单量级一致。

### M4 对话与上线（8 人日）

| # | 任务 | 人日 |
| --- | --- | --- |
| 1 | 对话后端：会话 CRUD + 上下文组装 + SSE 流式 + 消息落库 + 标题生成 | 2.5 |
| 2 | 前端聊天页：会话侧栏 + 流式渲染 + Markdown + 中止/重生成 | 2 |
| 3 | 管理后台：用户管理 + Prompt 管理 + 任务队列页 | 1.5 |
| 4 | 安全加固与全量测试：越权用例、限流、集成测试补齐 | 1 |
| 5 | Docker Compose 部署 + 部署文档 + 保活配置 + v1.0.0 发布 | 1 |

**M4 验收标准**
1. 对话流式首 token ≤ 3s，回答正确引用成员历史数据；
2. 断开页面后后端取消上游调用（无孤儿请求）；
3. 集成测试全绿；`docker compose up -d` 一键部署后完成 11.4 全流程手工验收；
4. 输出 README + 部署文档 + 用户使用手册（短文）。

---

## 15. 风险与对策

| # | 风险 | 概率 | 影响 | 对策 |
| --- | --- | --- | --- | --- |
| R1 | Supabase 免费项目暂停/网络不稳 | 中 | 高 | 保活 ping；StorageService/数据源可切换；本地 Postgres profile 兜底 |
| R2 | DeepSeek 返回非法 JSON | 中 | 中 | json_object 模式 + Schema 校验 + 修复重试 + 最终降级为纯文本展示 |
| R3 | PDF 解析准确率不达标 | 中 | 高 | 校对接是必经流程（AI 结果必须人工确认）；持续用真实报告迭代 Prompt；二期 OCR |
| R4 | Token 成本失控 | 低 | 中 | 每日每用户调用上限；输入截断策略；日志监控费用；解读手动触发 |
| R5 | SSE 经 Nginx 被缓冲（无打字机效果） | 中 | 中 | `proxy_buffering off` + 响应头 `X-Accel-Buffering: no` + 集成测试验证 |
| R6 | 中文字体缺失导致 PDF 乱码 | 中 | 中 | 内置 Noto Sans SC 字体文件随 jar 打包，导出测试断言中文内容 |
| R7 | 大报告超上下文 | 低 | 中 | 分段解析合并；指标按异常优先截断 |
| R8 | 健康合规/免责风险 | 中 | 中 | 全场景免责声明；不做诊断；紧急症状引导就医；隐私数据最小化采集 |
| R9 | 水平越权漏洞 | 中 | 高 | 统一 AccessChecker + 集成测试专项用例 |
| R10 | 进度拖延 | 中 | 中 | 里程碑制 + MVP 优先；非核心（PDF 美化、管理后台细节）可降级 |

---

## 16. 二期规划（排期预留，不在本期范围）

| # | 功能 | 说明 |
| --- | --- | --- |
| P1 | 图片/扫描件 OCR 解析 | 接入多模态模型（Qwen-VL/GLM-4V）或百度 OCR，扫描件自动识别 |
| P2 | RAG 健康知识库 | pgvector + 内置慢病指南 + 个人病历文档，检索增强问答，注明引用 |
| P3 | 可穿戴设备导入 | Apple Health/华为运动健康/小米运动导出文件解析 |
| P4 | 成员独立账号与家庭组 | 邀请码、数据授权、成员自行查看 |
| P5 | 提醒与推送 | 复查提醒、用药提醒、运动打卡；邮件/微信推送 |
| P6 | 专病管理 | 高血压/糖尿病/高血脂专题档案与随访模板 |
| P7 | 健康报告周报/月报 | 定时生成家庭成员健康周报 |
| P8 | 移动端 | 小程序或跨端 App（uni-app/Flutter） |
| P9 | 数据导出与迁移 | 全量健康数据导出（JSON/Excel）、账号注销与数据删除 |

---

## 17. 附录

### 附录 A：后端依赖清单（pom.xml 规划）

```
org.springframework.boot:spring-boot-starter-web
org.springframework.boot:spring-boot-starter-security
org.springframework.boot:spring-boot-starter-validation
org.springframework.boot:spring-boot-starter-actuator
org.springframework.boot:spring-boot-starter-freemarker
com.baomidou:mybatis-plus-spring-boot3-starter:3.5.x
org.postgresql:postgresql
org.flywaydb:flyway-core / flyway-database-postgresql
io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson:0.12.x
com.squareup.okhttp3:okhttp / okhttp-sse:4.x
software.amazon.awssdk:s3:2.2x
org.apache.pdfbox:pdfbox:3.x
org.xhtmlrenderer:flying-saucer-pdf-openpdf:9.x
cn.hutool:hutool-core / hutool-crypto
org.projectlombok:lombok
org.mapstruct:mapstruct + mapstruct-processor
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x
(测试) org.springframework.boot:spring-boot-starter-test
(测试) org.testcontainers:postgresql / junit-jupiter
(测试) com.squareup.okhttp3:mockwebserver
```

### 附录 B：前端依赖清单（package.json 规划）

```
vue@3.5, vue-router@4, pinia, element-plus, @element-plus/icons-vue
axios, echarts, markdown-it, dompurify, highlight.js
dayjs, nprogress
(dev) vite, @vitejs/plugin-vue, typescript, vue-tsc, unplugin-auto-import,
      unplugin-vue-components, sass, vitest(可选)
```

### 附录 C：核心 Prompt 模板全文示例（AI_INTERPRET）

```
你是"健康小助手"团队的体检解读专家。请根据用户的健康档案和本次体检指标，生成通俗易懂的解读。

【成员档案】
{{memberProfile}}

【本次体检指标】（status 为系统判定：NORMAL/HIGH/LOW/ABNORMAL/POSITIVE/UNKNOWN）
{{indicators}}

【既往病史】{{medicalHistory}}
【过敏史】{{allergy}}
【长期用药】{{medication}}
【家族史】{{familyHistory}}
【生活习惯】{{lifestyle}}

【解读要求】
1. 语言必须通俗：用初中生能听懂的话解释，任何医学名词必须当场用括号或短句解释；
2. 逐指标解读：每条包含"这个指标是查什么的"+"你的结果意味着什么"+"一句可执行的建议"；
3. 结合个人情况：同样的数值，对不同年龄、性别、病史的人意义不同，请个性化解释；
4. 风险分级：结合异常指标的临床意义给 single indicator 与整体风险分级；
5. 不做诊断、不推荐具体药物，异常明显时建议"尽快到相应科室复诊"；
6. 只依据提供的数据，不编造未见到的检查结果；
7. 输出严格为如下 JSON（不要输出任何其他文字、不要用 markdown 代码块包裹）：
{
  "riskLevel": "LOW|MEDIUM|HIGH",
  "summary": "整体总览，2-4 句，先结论后原因",
  "focusMetrics": ["指标名1","指标名2"],
  "indicators": [
    {"name":"...","value":"...","unit":"...","refRange":"...","status":"...",
     "plainExplain":"50-120字大白话","riskLevel":"LOW|MEDIUM|HIGH","advice":"20-50字建议"}
  ],
  "disclaimer": "以上内容基于体检数据由 AI 生成，仅供参考，不构成医疗诊断。如有异常请遵医嘱复查就医。"
}
```

### 附录 D：命名与状态码速查

| 枚举 | 取值 |
| --- | --- |
| 成员关系 | SELF/FATHER/MOTHER/SPOUSE/CHILD/SIBLING/GRANDPARENT/OTHER |
| 报告状态 | UPLOADED/PARSING/PARSED/CONFIRMED/PARSE_FAILED/NEED_MANUAL |
| 指标判定 | NORMAL/HIGH/LOW/ABNORMAL/POSITIVE/NEGATIVE/UNKNOWN |
| 任务状态 | PENDING/RUNNING/SUCCESS/FAILED/CANCELED |
| 任务类型 | REPORT_PARSE/AI_INTERPRET/AI_SUGGESTION/PDF_EXPORT/SESSION_TITLE |
| AI 场景 | REPORT_PARSE/INTERPRET/SUGGESTION/CHAT/TITLE |
| 运动类型 | RUNNING/WALKING/CYCLING/SWIMMING/STRENGTH/YOGA/BALL_GAME/HIKING/OTHER |
| 强度 | LOW/MEDIUM/HIGH |
| 建议目标 | FAT_LOSS/MUSCLE/GLUCOSE/BLOOD_PRESSURE/REHAB/MAINTAIN |

---

**文档结束。下一步：确认本计划后从 M1 任务 1 开始实施。**
