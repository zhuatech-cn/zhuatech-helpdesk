/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.service;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class MajorIncidentReadinessService {
    public Result evaluate(Request request) {
        List<String> gaps = new ArrayList<>();
        boolean major = request.severity() <= 2;
        if (major && !request.incidentCommanderAssigned()) gaps.add("重大事件未指定事件指挥官");
        if (major && !request.stakeholderPlanReady()) gaps.add("重大事件缺少干系人沟通计划");
        if (major && !request.rollbackReady()) gaps.add("重大事件缺少回退或降级方案");
        if (!request.timelineEvidenceReady()) gaps.add("处置时间线证据不完整");
        if (request.affectedServices() < 1) gaps.add("未登记受影响服务");
        int readiness = Math.max(0, 100 - gaps.size() * 22 - (major ? 0 : 5));
        String decision = gaps.isEmpty() ? "READY" : major ? "BLOCKED" : "REVIEW";
        return new Result(request.incidentId(), decision, readiness, major,
                List.copyOf(gaps), major ? 15 : 60);
    }
    public record Request(@NotBlank String incidentId, @Min(1) @Max(4) int severity,
                          @Min(0) int affectedServices, boolean incidentCommanderAssigned,
                          boolean stakeholderPlanReady, boolean rollbackReady,
                          boolean timelineEvidenceReady) {
        public Request {
            if (incidentId == null || incidentId.isBlank()) throw new IllegalArgumentException("incidentId is required");
            if (severity < 1 || severity > 4) throw new IllegalArgumentException("severity must be 1..4");
            if (affectedServices < 0) throw new IllegalArgumentException("affectedServices must be non-negative");
        }
    }
    public record Result(String incidentId, String decision, int readinessScore,
                         boolean majorIncident, List<String> gaps, int nextReviewMinutes) {}
}
