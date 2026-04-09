package com.godLife.project.service.interfaces;


import com.godLife.project.dto.internal.test.GetPlanIdxDTO;
import com.godLife.project.dto.internal.test.GetUserListDTO;

import java.util.List;

public interface TestService {
    List<GetPlanIdxDTO> findPlanIdx();
    List<GetUserListDTO> getUserList();
    void deleteReview(int planIdx);
    void changePlanStatus(int isActive, int isCompleted, int isDeleted, int isShared, int planIdx);
}
