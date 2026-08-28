/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.helpdesk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc class DomainDecisionApiTests {
 @Autowired MockMvc mvc;
 @Test void domainDecisionReturnsAuditableScoreMetricsAndActions() throws Exception {
  mvc.perform(post("/api/domain/decision").with(httpBasic("operator","operator123")).contentType(MediaType.APPLICATION_JSON).content("{\"ticketNo\":\"HD-2026-1001\",\"priority\":\"P2\",\"elapsedMinutes\":90,\"responseSlaMinutes\":30,\"resolutionSlaMinutes\":240,\"firstResponseMinutes\":12,\"ownerAssigned\":true,\"customerWaiting\":false,\"resolved\":false}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.decision").isString()).andExpect(jsonPath("$.data.score").isNumber()).andExpect(jsonPath("$.data.metrics").isMap()).andExpect(jsonPath("$.data.actions").isArray());
 }
 @Test void domainRiskScenarioReturnsExpectedBlockingDecision() throws Exception {
  mvc.perform(post("/api/domain/decision").with(httpBasic("operator","operator123")).contentType(MediaType.APPLICATION_JSON).content("{\"ticketNo\":\"HD-2026-1001\",\"priority\":\"P1\",\"elapsedMinutes\":600,\"responseSlaMinutes\":15,\"resolutionSlaMinutes\":120,\"firstResponseMinutes\":100,\"ownerAssigned\":false,\"customerWaiting\":false,\"resolved\":false}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.decision").value("BREACHED")).andExpect(jsonPath("$.data.actions").isNotEmpty());
 }
 @Test void domainDecisionRequiresAuthentication() throws Exception {mvc.perform(post("/api/domain/decision").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isUnauthorized());}
}
