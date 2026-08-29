# 企业服务台与工单管理系统 API

所有业务接口默认位于 `/api`，除 `/public/**` 和健康检查外均需要 HTTP Basic 身份认证。生产环境应接入企业 IAM 或统一身份平台。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/public/about` | 产品、公司、官网和许可元数据 |
| GET | `/catalog` | 业务模块、字段标签和状态动作 |
| GET | `/dashboard` | 业务规模、金额、状态和模块统计 |
| GET/POST | `/records` | 业务台账查询与创建 |
| GET/PUT/DELETE | `/records/{id}` | 详情、草稿修改与删除 |
| POST | `/records/{id}/actions` | 执行服务端状态迁移 |
| POST | `/records/{id}/comments` | 增加协作记录 |
| GET | `/records/{id}/timeline` | 查询完整操作时间线 |
| GET | `/records/search` | 组合检索、分页和逾期筛选 |
| GET | `/records/export.csv` | 导出 UTF-8 CSV |
| GET | `/sla-summary` | SLA、逾期、风险和人员工作量 |
| POST | `/domain/decision` | 执行企业服务台与工单管理系统专属领域规则 |
| GET/POST | `/enterprise/controls` | 企业控制项查询与幂等创建 |
| POST | `/enterprise/controls/{id}/submit` | 提交复核 |
| POST | `/admin/enterprise/controls/{id}/review` | 管理员审批或驳回 |
| POST | `/enterprise/controls/{id}/documents` | 登记附件哈希及存储元数据 |
| POST | `/enterprise/controls/{id}/complete` | 凭证完整后办结 |
| POST | `/admin/enterprise/controls/{id}/sync` | 登记外部系统回执 |

## 领域决策字段

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `ticketNo` | String | 工单编号 |
| `priority` | String | 优先级(P1-P4) |
| `elapsedMinutes` | int | 已耗时(分钟) |
| `responseSlaMinutes` | int | 响应SLA(分钟) |
| `resolutionSlaMinutes` | int | 解决SLA(分钟) |
| `firstResponseMinutes` | int | 首次响应(分钟) |
| `ownerAssigned` | boolean | 已分派责任人 |
| `customerWaiting` | boolean | 正在等待客户 |
| `resolved` | boolean | 问题已解决 |

接口统一返回 `ApiResponse`；业务冲突使用 HTTP 409，参数错误使用 400，未认证使用 401，无权限使用 403。

## 工单核心 API

`POST /core/helpdesk/tickets` 创建工单；`GET /core/helpdesk/tickets` 查询队列；工单操作分别为 `/assign`、`/respond`、`/wait-customer`、`/resume`、`/resolve`、`/close` 和 `/reopen`。`GET /core/helpdesk/tickets/{id}/timeline` 返回完整事件链，`GET /core/helpdesk/sla-summary` 返回首响与解决违约统计。

管理员可使用 `POST /admin/core/helpdesk/escalations/run` 扫描 SLA 违约并自动升级，或通过 `/admin/core/helpdesk/tickets/{id}/escalate` 人工升级；关闭后的工单使用 `POST /core/helpdesk/tickets/{id}/satisfaction` 提交一次性满意度评价。
