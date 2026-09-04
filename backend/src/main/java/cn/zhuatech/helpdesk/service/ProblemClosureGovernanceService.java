/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProblemClosureGovernanceService {
    public Assessment assess(Request request) {
        List<String> blockers = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        if (!request.rootCauseVerified()) blockers.add("根因分析尚未完成独立验证");
        if (!request.affectedServicesMapped()) blockers.add("受影响服务和配置项未完整关联");
        if (!request.permanentFixReleased()) blockers.add("永久修复尚未受控发布");
        if (!request.linkedIncidentsResolved()) blockers.add("仍有关联事件未解决");
        if (!request.changeEvidenceReady()) blockers.add("修复变更及验证证据不完整");
        if (!request.monitoringStable()) blockers.add("修复后稳定性观察未达标");
        if (!request.businessOwnerAccepted()) blockers.add("业务服务负责人尚未验收");
        if (!request.reviewerSeparated()) blockers.add("问题经办人与关闭复核人未职责分离");
        if (!request.auditReady()) blockers.add("问题记录审计证据链不完整");
        if (!request.workaroundPublished()) actions.add("发布已知错误与临时规避方案");
        if (!request.knowledgeArticlePublished()) actions.add("沉淀并审核服务知识文章");
        if (!request.residualRiskAccepted()) actions.add("登记残余风险及责任人接受意见");
        Decision decision = !blockers.isEmpty() ? Decision.BLOCKED : !actions.isEmpty() ? Decision.REVIEW : Decision.CLOSE;
        return new Assessment(request.problemId(), request.linkedIncidentCount(), decision,
                List.copyOf(blockers), List.copyOf(actions));
    }

    public record Request(@NotBlank String problemId, @Min(0) int linkedIncidentCount,
                          boolean rootCauseVerified, boolean affectedServicesMapped,
                          boolean workaroundPublished, boolean permanentFixReleased,
                          boolean linkedIncidentsResolved, boolean changeEvidenceReady,
                          boolean monitoringStable, boolean knowledgeArticlePublished,
                          boolean businessOwnerAccepted, boolean residualRiskAccepted,
                          boolean reviewerSeparated, boolean auditReady) {}
    public record Assessment(String problemId, int linkedIncidentCount, Decision decision,
                             List<String> blockers, List<String> actions) {}
    public enum Decision { CLOSE, REVIEW, BLOCKED }
}
