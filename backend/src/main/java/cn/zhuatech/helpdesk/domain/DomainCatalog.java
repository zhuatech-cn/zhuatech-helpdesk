/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.domain;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class DomainCatalog {
    private final Map<String, WorkflowAction> actions = new LinkedHashMap<>();
    public DomainCatalog() {
        actions.put("ACCEPT", new WorkflowAction("ACCEPT", "受理并分派", List.of("草稿"), "处理中", "OPERATOR"));
        actions.put("RESOLVE", new WorkflowAction("RESOLVE", "提交解决方案", List.of("处理中"), "待确认", "OPERATOR"));
        actions.put("CLOSE", new WorkflowAction("CLOSE", "确认工单关闭", List.of("待确认"), "已关闭", "ADMIN"));
    }
    public String systemName() { return "知华科技企业服务台与工单管理系统"; }
    public String scene() { return "服务门户、服务目录、工单、队列、分派、SLA、升级、知识库、满意度与服务分析"; }
    public String initialStatus() { return "草稿"; }
    public String partyLabel() { return "请求人/服务团队"; }
    public String amountLabel() { return "服务成本"; }
    public String quantityLabel() { return "工单数量"; }
    public String dueLabel() { return "SLA解决期限"; }
    public List<ModuleDefinition> modules() { return List.of(
            new ModuleDefinition("PORTAL", "自助服务门户", "提供提单、进度查询、补充材料和服务评价"),
            new ModuleDefinition("CATALOG", "服务目录", "定义服务项、表单、承诺时限、责任组和审批"),
            new ModuleDefinition("TICKET", "工单中心", "统一登记事件、请求、投诉和咨询工单"),
            new ModuleDefinition("ROUTING", "队列与分派", "按技能、组织、值班、负载和优先级自动路由"),
            new ModuleDefinition("SLA", "SLA管理", "配置响应与解决目标、暂停规则、预警和违约升级"),
            new ModuleDefinition("ESCALATION", "重大工单升级", "触发分级响应、管理层通知、会商和复盘"),
            new ModuleDefinition("KNOWLEDGE", "服务知识库", "维护标准答案、解决方案、版本、审核和使用反馈"),
            new ModuleDefinition("SATISFACTION", "满意度与质检", "执行评价、回访、抽检、申诉和改进计划"),
            new ModuleDefinition("REPORTING", "服务运营分析", "分析首响、解决率、积压、违约和人员负载")
        ); }
    public Map<String, WorkflowAction> actions() { return Collections.unmodifiableMap(actions); }
    public record ModuleDefinition(String code,String name,String description) {}
    public record WorkflowAction(String code,String label,List<String> from,String to,String requiredRole) {}
}
