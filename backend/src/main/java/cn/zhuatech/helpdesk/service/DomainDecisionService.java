/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.service;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class DomainDecisionService {
 public DecisionResult assess(DecisionRequest request) { double consumed=request.elapsedMinutes()*100d/request.resolutionSlaMinutes();int score=100;List<String> actions=new ArrayList<>();if(!request.ownerAssigned()){score-=40;actions.add("立即分派责任人和处理组");}if(request.firstResponseMinutes()>request.responseSlaMinutes()){score-=30;actions.add("登记首次响应违约并升级");}if(consumed>=100&&!request.resolved()){score-=55;actions.add("启动解决SLA违约升级");}else if(consumed>=80&&!request.resolved()){score-=25;actions.add("在解决时限前升级处理");}if("P1".equalsIgnoreCase(request.priority())&&!request.resolved()){score-=20;actions.add("启动重大工单响应机制");}if(request.customerWaiting())actions.add("暂停SLA前确认等待客户依据");return result(score,actions,"ON_TRACK","AT_RISK","BREACHED",Map.of("slaConsumedPercent",Math.round(consumed),"remainingMinutes",Math.max(0,request.resolutionSlaMinutes()-request.elapsedMinutes()),"firstResponseMet",request.firstResponseMinutes()<=request.responseSlaMinutes(),"resolved",request.resolved())); }
 private DecisionResult result(int raw,List<String> actions,String good,String warn,String bad,Map<String,Object> metrics) { int score=Math.max(0,Math.min(100,raw));String decision=score>=80?good:score>=50?warn:bad;return new DecisionResult(decision,score,metrics,List.copyOf(actions)); }
 private DecisionResult riskResult(int raw,List<String> actions,String good,String warn,String bad,Map<String,Object> metrics) { int score=Math.max(0,Math.min(100,raw));String decision=score>=70?bad:score>=40?warn:good;return new DecisionResult(decision,score,metrics,List.copyOf(actions)); }
 public record DecisionRequest(
        @NotBlank String ticketNo,
        @Pattern(regexp="P[1-4]") String priority,
        @PositiveOrZero int elapsedMinutes,
        @Positive int responseSlaMinutes,
        @Positive int resolutionSlaMinutes,
        @PositiveOrZero int firstResponseMinutes,
        boolean ownerAssigned,
        boolean customerWaiting,
        boolean resolved) {}
 public record DecisionResult(String decision,int score,Map<String,Object> metrics,List<String> actions) {}
}
