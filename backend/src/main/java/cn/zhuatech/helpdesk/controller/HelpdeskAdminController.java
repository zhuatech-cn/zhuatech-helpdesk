/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.controller;

import cn.zhuatech.helpdesk.common.ApiResponse;
import cn.zhuatech.helpdesk.service.HelpdeskCoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/core/helpdesk")
public class HelpdeskAdminController {
    private final HelpdeskCoreService service;
    public HelpdeskAdminController(HelpdeskCoreService service){this.service=service;}
    @PostMapping("/tickets/{id}/escalate") ApiResponse<HelpdeskCoreService.Ticket> escalate(@PathVariable Long id,@Valid @RequestBody HelpdeskCoreService.EscalationRequest request){return ApiResponse.ok(service.escalate(id,request));}
    @PostMapping("/escalations/run") ApiResponse<HelpdeskCoreService.EscalationRun> run(){return ApiResponse.ok(service.runSlaEscalation());}
}
