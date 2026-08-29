<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ -->
<script setup>
import {onMounted,reactive,ref} from 'vue'
import {request} from '../api'
const tickets=ref([]),summary=ref({}),error=ref('')
const form=reactive({ticketNo:'HD-'+Date.now(),requester:'华东客户',category:'应用故障',priority:'P2',subject:'业务系统访问异常',responseSlaMinutes:30,resolutionSlaMinutes:240})
async function load(){[tickets.value,summary.value]=await Promise.all([request('/api/core/helpdesk/tickets'),request('/api/core/helpdesk/sla-summary')])}
async function run(fn){try{error.value='';await fn();await load()}catch(e){error.value=e.message}}
async function create(){await run(async()=>{await request('/api/core/helpdesk/tickets',{method:'POST',body:JSON.stringify(form)});form.ticketNo='HD-'+Date.now()})}
async function act(t,a){await run(async()=>{let body={remark:'工作台处理'};let path=`/api/core/helpdesk/tickets/${t.id}/${a}`;if(a==='assign')body={team:'应用支持组',assignee:'值班工程师'};if(a==='resolve')body={resolution:'已恢复服务并完成验证'};if(a==='escalate'){path=`/api/admin/core/helpdesk/tickets/${t.id}/escalate`;body={targetTeam:'专家支持组',reason:'业务影响扩大，人工升级'}}if(a==='satisfaction')body={score:5,comment:'服务响应及时，问题已解决'};await request(path,{method:'POST',body:JSON.stringify(body)})})}
async function runEscalation(){await run(()=>request('/api/admin/core/helpdesk/escalations/run',{method:'POST'}))}
onMounted(load)
</script>
<template>
  <section class="head"><div><span>SERVICE ASSURANCE</span><h3>SLA 工单指挥台</h3><p>覆盖受理、协同、SLA 自动升级、解决关闭与客户满意度回收，形成服务质量闭环。</p></div><button @click="runEscalation">执行 SLA 扫描</button></section>
  <p class="err" v-if="error">{{error}}</p>
  <section class="metrics"><article><b>{{summary.open||0}}</b><small>处理中</small></article><article><b>{{summary.resolutionBreached||0}}</b><small>解决违约</small></article><article><b>{{summary.escalated||0}}</b><small>已升级</small></article><article><b>{{Number(summary.averageSatisfaction||0).toFixed(1)}}</b><small>满意度</small></article></section>
  <form class="create" @submit.prevent="create"><h4>快速受理</h4><input v-model="form.requester" placeholder="请求人"><input v-model="form.subject" placeholder="问题主题"><select v-model="form.priority"><option>P1</option><option>P2</option><option>P3</option><option>P4</option></select><button>创建工单</button></form>
  <section class="list"><article v-for="t in tickets" :key="t.id"><div><code>{{t.ticketNo}}</code><h4>{{t.subject}}</h4><p>{{t.requester}} · {{t.team||'待分派'}} / {{t.assignee||'未指定'}} · 升级 L{{t.escalationLevel}}</p></div><span :class="['priority',t.priority]">{{t.priority}}</span><b>{{t.status}}</b><div class="actions"><button v-if="!['RESOLVED','CLOSED'].includes(t.status)" class="outline" @click="act(t,'escalate')">升级</button><button v-if="t.status==='NEW'" @click="act(t,'assign')">分派</button><button v-if="t.status==='ASSIGNED'" @click="act(t,'respond')">响应</button><button v-if="t.status==='IN_PROGRESS'" @click="act(t,'resolve')">解决</button><button v-if="t.status==='RESOLVED'" @click="act(t,'close')">关闭</button><button v-if="t.status==='CLOSED'&&!t.satisfactionScore" @click="act(t,'satisfaction')">五星评价</button><button v-if="t.status==='CLOSED'" @click="act(t,'reopen')">重开</button></div></article></section>
</template>
<style scoped>
.head,.create,.list,.metrics{background:#fff;border:1px solid #dce2df;margin-top:20px;padding:24px}.head{display:flex;justify-content:space-between;align-items:center}.head span{color:#a56c24;font-size:11px;letter-spacing:.15em}.head h3{margin:6px 0}.head p{margin:0;color:#68777c}.head button,.create button,.actions button{border:0;background:#235a74;color:#fff;padding:9px 12px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.metrics article{display:grid;gap:3px}.metrics b{font-size:26px}.metrics small{color:#68777c}.create{display:grid;grid-template-columns:1fr 2fr 100px 120px;gap:10px}.create h4{grid-column:1/-1;margin:0}.create input,.create select{padding:10px;border:1px solid #ccd6d2}.list article{display:grid;grid-template-columns:1fr 50px 120px minmax(260px,auto);align-items:center;gap:12px;padding:14px 0;border-top:1px solid #edf0ef}.list h4{margin:4px 0}.list p{margin:0;color:#68777c}.priority{font-weight:800}.P1{color:#a03f38}.actions{display:flex;gap:5px;justify-content:flex-end;flex-wrap:wrap}.actions .outline{background:#7d5a52}.err{color:#a03f38}@media(max-width:900px){.create,.list article,.metrics{grid-template-columns:1fr}.create h4{grid-column:auto}}
</style>
