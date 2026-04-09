package com.godLife.project.service.interfaces;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.query.user.GetNameNEmail;
import com.godLife.project.dto.request.myPage.GetUserPwRequestDTO;
import com.godLife.project.dto.response.user.UserProfileResponseDTO;

public interface UserService {
    // 회원가입
    String insertUser(UserDTO joinUserDTO);
    // 아이디 중복 체크
    Boolean checkUserIdExist(String userId);
    // 유저 정보 조회
    UserDTO findByUserId(String userId);

    // 유저 인덱스로 아이디 찾기
    String getUserIdByUserIdx(int userIdx);

    // 아이디 찾기
    String FindUserIdByNameNEmail(GetNameNEmail getNameNEmail, boolean isMasked);
    // 비번 찾기
    int FindUserPw(GetUserPwRequestDTO userPwRequestDTO, String userEmail);

    // 프로필 조회
    UserProfileResponseDTO getUserProfile(String userId);
}
