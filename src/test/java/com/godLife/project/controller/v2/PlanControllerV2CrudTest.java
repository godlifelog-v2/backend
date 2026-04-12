package com.godLife.project.controller.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.godLife.project.dto.request.plan.v2.ActivityCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityItemV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.PlanService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PlanControllerV2 CRUD 테스트")
class PlanControllerV2CrudTest {

    @Mock private GlobalExceptionHandler handler;
    @Mock private PlanServiceV2 planServiceV2;
    @Mock private PlanService planService;

    @InjectMocks
    private PlanControllerV2 planControllerV2;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private final String authHeader = "Bearer test-token";
    private final int userIdx = 1;
    private final int planIdx = 10;
    private final int activityIdx = 5;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(planControllerV2).build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(handler.getUserIdxFromToken(authHeader)).thenReturn(userIdx);
        when(handler.getHttpStatus(200)).thenReturn(HttpStatus.OK);
        when(handler.getHttpStatus(201)).thenReturn(HttpStatus.CREATED);
        when(handler.getHttpStatus(400)).thenReturn(HttpStatus.BAD_REQUEST);
        when(handler.getHttpStatus(403)).thenReturn(HttpStatus.FORBIDDEN);
        when(handler.getHttpStatus(404)).thenReturn(HttpStatus.NOT_FOUND);
        when(handler.getHttpStatus(409)).thenReturn(HttpStatus.CONFLICT);
        when(handler.getHttpStatus(410)).thenReturn(HttpStatus.GONE);
        when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
        when(handler.getHttpStatus(500)).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        Map<String, Object> defaultResponse = new HashMap<>();
        defaultResponse.put("status", "success");
        when(handler.createResponse(anyInt(), any())).thenReturn(defaultResponse);

        Map<String, Object> validationErrors = new HashMap<>();
        validationErrors.put("errors", "validation error");
        when(handler.getValidationErrors(any())).thenReturn(validationErrors);
    }

    // ===========================================
    // 루틴 생성
    // ===========================================

    @Nested
    @DisplayName("POST /api/v2/plan/auth - 루틴 생성")
    class CreatePlanTest {

        private PlanCreateRequestV2 validDto() {
            PlanCreateRequestV2 dto = new PlanCreateRequestV2();
            dto.setPlanTitle("테스트 루틴");
            dto.setEndTo(30);
            dto.setTargetIdx(1);
            dto.setJobIdx(1);
            dto.setPlanImp(1);
            dto.setDescription("루틴 설명");
            dto.setColor("#FF5733FF");
            return dto;
        }

        @Test
        @DisplayName("생성 성공 → 201")
        void createPlan_success_returns201() throws Exception {
            when(planServiceV2.createPlan(any(), eq(userIdx))).thenReturn(201);

            mockMvc.perform(post("/api/v2/plan/auth")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("루틴 5개 초과 → 412")
        void createPlan_overLimit_returns412() throws Exception {
            when(planServiceV2.createPlan(any(), eq(userIdx))).thenReturn(412);

            mockMvc.perform(post("/api/v2/plan/auth")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isPreconditionFailed());
        }

        @Test
        @DisplayName("탈퇴 유저 → 410")
        void createPlan_deletedUser_returns410() throws Exception {
            when(planServiceV2.createPlan(any(), eq(userIdx))).thenReturn(410);

            mockMvc.perform(post("/api/v2/plan/auth")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isGone());
        }
    }

    // ===========================================
    // 루틴 수정
    // ===========================================

    @Nested
    @DisplayName("PATCH /api/v2/plan/auth/{planIdx} - 루틴 부분 수정")
    class UpdatePlanTest {

        @Test
        @DisplayName("수정 성공 → 200")
        void updatePlan_success_returns200() throws Exception {
            when(planServiceV2.updatePlan(eq(planIdx), any(), eq(userIdx))).thenReturn(200);

            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setPlanTitle("수정된 제목");

            mockMvc.perform(patch("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void updatePlan_notFound_returns404() throws Exception {
            when(planServiceV2.updatePlan(eq(planIdx), any(), eq(userIdx))).thenReturn(404);

            mockMvc.perform(patch("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("권한 없음 → 403")
        void updatePlan_forbidden_returns403() throws Exception {
            when(planServiceV2.updatePlan(eq(planIdx), any(), eq(userIdx))).thenReturn(403);

            mockMvc.perform(patch("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden());
        }
    }

    // ===========================================
    // 루틴 삭제
    // ===========================================

    @Nested
    @DisplayName("DELETE /api/v2/plan/auth/{planIdx} - 루틴 삭제")
    class DeletePlanTest {

        @Test
        @DisplayName("삭제 성공 → 200")
        void deletePlan_success_returns200() throws Exception {
            when(planServiceV2.deletePlan(planIdx, userIdx)).thenReturn(200);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void deletePlan_notFound_returns404() throws Exception {
            when(planServiceV2.deletePlan(planIdx, userIdx)).thenReturn(404);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("권한 없음 → 403")
        void deletePlan_forbidden_returns403() throws Exception {
            when(planServiceV2.deletePlan(planIdx, userIdx)).thenReturn(403);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isForbidden());
        }
    }

    // ===========================================
    // 활동 생성
    // ===========================================

    @Nested
    @DisplayName("POST /api/v2/plan/auth/{planIdx}/activities - 활동 생성")
    class CreateActivitiesTest {

        private ActivityCreateRequestV2 validDto() {
            ActivityItemV2 item = new ActivityItemV2();
            item.setActivityName("조깅 30분");
            item.setActivityImp(1);

            ActivityCreateRequestV2 dto = new ActivityCreateRequestV2();
            dto.setActivities(List.of(item));
            return dto;
        }

        @Test
        @DisplayName("활동 생성 성공 → 201")
        void createActivities_success_returns201() throws Exception {
            when(planServiceV2.createActivities(eq(planIdx), any(), eq(userIdx))).thenReturn(201);

            mockMvc.perform(post("/api/v2/plan/auth/" + planIdx + "/activities")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void createActivities_planNotFound_returns404() throws Exception {
            when(planServiceV2.createActivities(eq(planIdx), any(), eq(userIdx))).thenReturn(404);

            mockMvc.perform(post("/api/v2/plan/auth/" + planIdx + "/activities")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("권한 없음 → 403")
        void createActivities_forbidden_returns403() throws Exception {
            when(planServiceV2.createActivities(eq(planIdx), any(), eq(userIdx))).thenReturn(403);

            mockMvc.perform(post("/api/v2/plan/auth/" + planIdx + "/activities")
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validDto())))
                .andExpect(status().isForbidden());
        }
    }

    // ===========================================
    // 활동 수정
    // ===========================================

    @Nested
    @DisplayName("PATCH /api/v2/plan/auth/{planIdx}/activities/{activityIdx} - 활동 수정")
    class UpdateActivityTest {

        @Test
        @DisplayName("수정 성공 → 200")
        void updateActivity_success_returns200() throws Exception {
            when(planServiceV2.updateActivity(eq(planIdx), eq(activityIdx), any(), eq(userIdx))).thenReturn(200);

            ActivityUpdateRequestV2 dto = new ActivityUpdateRequestV2();
            dto.setActivityName("수정된 활동명");

            mockMvc.perform(patch("/api/v2/plan/auth/" + planIdx + "/activities/" + activityIdx)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("활동/루틴 없음 → 404")
        void updateActivity_notFound_returns404() throws Exception {
            when(planServiceV2.updateActivity(eq(planIdx), eq(activityIdx), any(), eq(userIdx))).thenReturn(404);

            mockMvc.perform(patch("/api/v2/plan/auth/" + planIdx + "/activities/" + activityIdx)
                    .header("Authorization", authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNotFound());
        }
    }

    // ===========================================
    // 활동 삭제
    // ===========================================

    @Nested
    @DisplayName("DELETE /api/v2/plan/auth/{planIdx}/activities/{activityIdx} - 활동 삭제")
    class DeleteActivityTest {

        @Test
        @DisplayName("삭제 성공 → 200")
        void deleteActivity_success_returns200() throws Exception {
            when(planServiceV2.deleteActivity(planIdx, activityIdx, userIdx)).thenReturn(200);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx + "/activities/" + activityIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("활동/루틴 없음 → 404")
        void deleteActivity_notFound_returns404() throws Exception {
            when(planServiceV2.deleteActivity(planIdx, activityIdx, userIdx)).thenReturn(404);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx + "/activities/" + activityIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("권한 없음 → 403")
        void deleteActivity_forbidden_returns403() throws Exception {
            when(planServiceV2.deleteActivity(planIdx, activityIdx, userIdx)).thenReturn(403);

            mockMvc.perform(delete("/api/v2/plan/auth/" + planIdx + "/activities/" + activityIdx)
                    .header("Authorization", authHeader))
                .andExpect(status().isForbidden());
        }
    }
}
