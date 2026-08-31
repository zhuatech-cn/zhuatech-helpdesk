/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.controller;
import cn.zhuatech.helpdesk.common.ApiResponse;
import cn.zhuatech.helpdesk.service.MajorIncidentReadinessService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/enterprise/incidents")
public class MajorIncidentReadinessController {
    private final MajorIncidentReadinessService service;
    public MajorIncidentReadinessController(MajorIncidentReadinessService service) { this.service = service; }
    @PostMapping("/readiness")
    public ApiResponse<MajorIncidentReadinessService.Result> evaluate(
            @Valid @RequestBody MajorIncidentReadinessService.Request request) {
        return ApiResponse.ok(service.evaluate(request));
    }
}
