/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.controller;

import cn.zhuatech.helpdesk.common.ApiResponse;
import cn.zhuatech.helpdesk.service.ProblemClosureGovernanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enterprise/helpdesk")
public class ProblemClosureGovernanceController {
    private final ProblemClosureGovernanceService service;
    public ProblemClosureGovernanceController(ProblemClosureGovernanceService service) { this.service = service; }

    @PostMapping("/problem-closure")
    public ApiResponse<ProblemClosureGovernanceService.Assessment> assess(
            @Valid @RequestBody ProblemClosureGovernanceService.Request request) {
        return ApiResponse.ok(service.assess(request));
    }
}
