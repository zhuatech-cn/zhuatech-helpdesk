/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk.service;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class HelpdeskCoreService {
    private final EntityManager em;
    public HelpdeskCoreService(EntityManager em){this.em=em;}

    @Transactional
    public Ticket create(CreateTicketRequest request){
        if(!findByNo(request.ticketNo()).isEmpty())throw conflict("工单编号已存在");
        LocalDateTime reported=request.reportedAt()==null?LocalDateTime.now():request.reportedAt();
        Ticket ticket=new Ticket(request.ticketNo(),request.requester(),request.category(),request.priority(),request.subject(),reported,
            reported.plusMinutes(request.responseSlaMinutes()),reported.plusMinutes(request.resolutionSlaMinutes()));
        em.persist(ticket);event(ticket,"CREATE","创建工单");return ticket;
    }

    public List<Ticket> list(String status){return em.createQuery("select t from HelpdeskTicket t where (:status is null or t.status=:status) order by t.reportedAt desc",Ticket.class)
        .setParameter("status",blank(status)?null:status).getResultList();}

    public Ticket detail(Long id){return get(id);}
    public List<TicketEvent> timeline(Long id){get(id);return em.createQuery("select e from HelpdeskTicketEvent e where e.ticketId=:id order by e.occurredAt",TicketEvent.class).setParameter("id",id).getResultList();}

    @Transactional public Ticket assign(Long id,AssignRequest request){Ticket t=getForUpdate(id);requireOpen(t);t.assignee=request.assignee();t.team=request.team();if("NEW".equals(t.status))t.status="ASSIGNED";event(t,"ASSIGN",request.team()+" / "+request.assignee());return t;}
    @Transactional public Ticket respond(Long id,RemarkRequest request){Ticket t=getForUpdate(id);requireOpen(t);if(t.assignee==null)throw conflict("工单必须先分派");if(t.firstRespondedAt==null)t.firstRespondedAt=LocalDateTime.now();t.status="IN_PROGRESS";event(t,"FIRST_RESPONSE",request.remark());return t;}
    @Transactional public Ticket waitCustomer(Long id,RemarkRequest request){Ticket t=getForUpdate(id);requireOpen(t);if(t.slaPausedAt!=null)return t;t.slaPausedAt=LocalDateTime.now();t.status="WAITING_CUSTOMER";event(t,"PAUSE_SLA",request.remark());return t;}
    @Transactional public Ticket resume(Long id,RemarkRequest request){Ticket t=getForUpdate(id);if(t.slaPausedAt==null)throw conflict("工单未处于等待客户状态");long paused=Duration.between(t.slaPausedAt,LocalDateTime.now()).toMinutes();t.pausedMinutes+=Math.max(0,paused);t.responseDueAt=t.responseDueAt.plusMinutes(Math.max(0,paused));t.resolutionDueAt=t.resolutionDueAt.plusMinutes(Math.max(0,paused));t.slaPausedAt=null;t.status="IN_PROGRESS";event(t,"RESUME_SLA",request.remark());return t;}
    @Transactional public Ticket resolve(Long id,ResolveRequest request){Ticket t=getForUpdate(id);requireOpen(t);if(t.assignee==null||t.firstRespondedAt==null)throw conflict("完成分派和首次响应后才能解决工单");if(t.slaPausedAt!=null)throw conflict("等待客户状态不能直接解决");t.resolution=request.resolution();t.resolvedAt=LocalDateTime.now();t.status="RESOLVED";event(t,"RESOLVE",request.resolution());return t;}
    @Transactional public Ticket close(Long id,RemarkRequest request){Ticket t=getForUpdate(id);if(!"RESOLVED".equals(t.status))throw conflict("仅已解决工单可以关闭");t.closedAt=LocalDateTime.now();t.status="CLOSED";event(t,"CLOSE",request.remark());return t;}
    @Transactional public Ticket reopen(Long id,RemarkRequest request){Ticket t=getForUpdate(id);if(!Set.of("RESOLVED","CLOSED").contains(t.status))throw conflict("仅已解决或已关闭工单可以重开");t.status="IN_PROGRESS";t.resolvedAt=null;t.closedAt=null;t.reopenCount++;t.resolutionDueAt=LocalDateTime.now().plusHours(4);event(t,"REOPEN",request.remark());return t;}

    @Transactional public Ticket escalate(Long id,EscalationRequest request){Ticket t=getForUpdate(id);requireOpen(t);if(Set.of("RESOLVED","CLOSED").contains(t.status))throw conflict("已解决工单不能升级");if(t.escalationLevel>=3)throw conflict("工单已达到最高升级级别");t.escalationLevel++;t.escalationReason=request.reason();t.escalatedBy=operator();t.escalatedAt=LocalDateTime.now();t.team=request.targetTeam();if(t.escalationLevel>=2)t.priority="P1";event(t,"ESCALATE","L"+t.escalationLevel+" / "+request.targetTeam()+" / "+request.reason());return t;}

    @Transactional public EscalationRun runSlaEscalation(){LocalDateTime now=LocalDateTime.now();List<Ticket> candidates=em.createQuery("select t from HelpdeskTicket t where t.status not in ('RESOLVED','CLOSED') and t.escalationLevel<3 and (t.responseDueAt<:now or (t.resolutionDueAt<:now and t.slaPausedAt is null))",Ticket.class).setParameter("now",now).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();for(Ticket t:candidates){t.escalationLevel++;t.escalationReason="SLA_AUTO_ESCALATION";t.escalatedBy=operator();t.escalatedAt=now;t.team="SLA升级组";if(t.escalationLevel>=2)t.priority="P1";event(t,"AUTO_ESCALATE","SLA逾期自动升级至 L"+t.escalationLevel);}return new EscalationRun(candidates.size(),now);}

    @Transactional public Ticket rate(Long id,SatisfactionRequest request){Ticket t=getForUpdate(id);if(!"CLOSED".equals(t.status))throw conflict("仅已关闭工单可以评价");if(t.satisfactionScore!=null)throw conflict("该工单已完成满意度评价");t.satisfactionScore=request.score();t.satisfactionComment=request.comment();t.ratedAt=LocalDateTime.now();event(t,"SATISFACTION",request.score()+"分 / "+(request.comment()==null?"":request.comment()));return t;}

    public SlaSummary summary(){LocalDateTime now=LocalDateTime.now();List<Ticket> all=list(null);long open=all.stream().filter(t->!"CLOSED".equals(t.status)).count();long responseBreached=all.stream().filter(t->t.firstRespondedAt==null&&!terminal(t)&&now.isAfter(t.responseDueAt)).count();long resolutionBreached=all.stream().filter(t->t.resolvedAt==null&&!terminal(t)&&now.isAfter(t.resolutionDueAt)&&t.slaPausedAt==null).count();long p1=all.stream().filter(t->"P1".equals(t.priority)&&!terminal(t)).count();long escalated=all.stream().filter(t->t.escalationLevel>0&&!terminal(t)).count();var scores=all.stream().filter(t->t.satisfactionScore!=null).mapToInt(t->t.satisfactionScore).summaryStatistics();return new SlaSummary(open,responseBreached,resolutionBreached,p1,escalated,scores.getCount(),scores.getCount()==0?0:scores.getAverage());}

    private List<Ticket> findByNo(String no){return em.createQuery("select t from HelpdeskTicket t where t.ticketNo=:no",Ticket.class).setParameter("no",no).getResultList();}
    private Ticket get(Long id){Ticket t=em.find(Ticket.class,id);if(t==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"工单不存在");return t;}
    private Ticket getForUpdate(Long id){Ticket t=em.find(Ticket.class,id,LockModeType.PESSIMISTIC_WRITE);if(t==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"工单不存在");return t;}
    private void event(Ticket t,String action,String detail){em.persist(new TicketEvent(t.id,action,operator(),detail));}
    private String operator(){var auth=SecurityContextHolder.getContext().getAuthentication();return auth==null?"system":auth.getName();}
    private void requireOpen(Ticket t){if(terminal(t))throw conflict("已关闭工单不能继续处理");}
    private boolean terminal(Ticket t){return "CLOSED".equals(t.status);}
    private boolean blank(String value){return value==null||value.isBlank();}
    private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}

    public record CreateTicketRequest(@NotBlank @Size(max=40) String ticketNo,@NotBlank @Size(max=80) String requester,
        @NotBlank @Size(max=40) String category,@Pattern(regexp="P[1-4]") String priority,@NotBlank @Size(max=160) String subject,
        @PastOrPresent LocalDateTime reportedAt,@Positive int responseSlaMinutes,@Positive int resolutionSlaMinutes){}
    public record AssignRequest(@NotBlank @Size(max=80) String team,@NotBlank @Size(max=80) String assignee){}
    public record RemarkRequest(@NotBlank @Size(max=500) String remark){}
    public record ResolveRequest(@NotBlank @Size(max=1000) String resolution){}
    public record EscalationRequest(@NotBlank @Size(max=80) String targetTeam,@NotBlank @Size(max=500) String reason){}
    public record SatisfactionRequest(@Min(1) @Max(5) int score,@Size(max=500) String comment){}
    public record EscalationRun(int escalatedTickets,LocalDateTime executedAt){}
    public record SlaSummary(long open,long responseBreached,long resolutionBreached,long openP1,long escalated,long rated,double averageSatisfaction){}

    @Entity(name="HelpdeskTicket") @Table(name="helpdesk_tickets",uniqueConstraints=@UniqueConstraint(columnNames="ticketNo"))
    public static class Ticket{
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,length=40) public String ticketNo;@Column(nullable=false,length=80) public String requester;
        @Column(nullable=false,length=40) public String category;@Column(nullable=false,length=4) public String priority;
        @Column(nullable=false,length=160) public String subject;@Column(length=80) public String team;@Column(length=80) public String assignee;
        @Column(nullable=false,length=30) public String status;@Column(length=1000) public String resolution;
        @Column(nullable=false) public LocalDateTime reportedAt;@Column(nullable=false) public LocalDateTime responseDueAt;@Column(nullable=false) public LocalDateTime resolutionDueAt;
        public LocalDateTime firstRespondedAt;public LocalDateTime resolvedAt;public LocalDateTime closedAt;public LocalDateTime slaPausedAt;
        public long pausedMinutes;public int reopenCount;public int escalationLevel;@Column(length=500)public String escalationReason;@Column(length=80)public String escalatedBy;public LocalDateTime escalatedAt;public Integer satisfactionScore;@Column(length=500)public String satisfactionComment;public LocalDateTime ratedAt;@Version public long version;
        protected Ticket(){}
        Ticket(String no,String requester,String category,String priority,String subject,LocalDateTime reported,LocalDateTime responseDue,LocalDateTime resolutionDue){this.ticketNo=no;this.requester=requester;this.category=category;this.priority=priority;this.subject=subject;this.reportedAt=reported;this.responseDueAt=responseDue;this.resolutionDueAt=resolutionDue;this.status="NEW";}
        public boolean isResponseBreached(){return firstRespondedAt==null&&LocalDateTime.now().isAfter(responseDueAt);}
        public boolean isResolutionBreached(){return resolvedAt==null&&slaPausedAt==null&&LocalDateTime.now().isAfter(resolutionDueAt);}
    }
    @Entity(name="HelpdeskTicketEvent") @Table(name="helpdesk_ticket_events")
    public static class TicketEvent{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;public Long ticketId;@Column(nullable=false,length=30)public String action;@Column(nullable=false,length=80)public String operatorName;@Column(length=500)public String detail;public LocalDateTime occurredAt;protected TicketEvent(){}TicketEvent(Long ticketId,String action,String operator,String detail){this.ticketId=ticketId;this.action=action;this.operatorName=operator;this.detail=detail;this.occurredAt=LocalDateTime.now();}}
}
