package com.godLife.project.service.impl;

import com.godLife.project.dto.request.verify.VerifyRequestDTO;
import com.godLife.project.dto.internal.verify.CheckAllFireActivateDTO;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.UserMapper;
import com.godLife.project.mapper.VerifyMapper;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.VerifyService;
import com.godLife.project.service.interfaces.emailInterface.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyServiceImpl implements VerifyService {

  private static final String AUTH_CODE_PREFIX = "AuthCode ";
  private static final String AUTH_CODE_FAIL_PREFIX = "AuthCode:fail:";
  private static final String AUTH_CODE_COOLDOWN_PREFIX = "AuthCode:cooldown:";
  private static final int MAX_FAIL_COUNT = 5;

  private final VerifyMapper verifyMapper;

  private final PlanMapper planMapper;

  private final UserMapper userMapper;

  private final EmailService emailService;

  private final RedisService redisService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int verifyActivity(VerifyRequestDTO verifyRequestDTO) {
    try {
      int planIdx = verifyRequestDTO.getPlanIdx();
      int activityIdx = verifyRequestDTO.getActivityIdx();
      int userIdx = verifyRequestDTO.getUserIdx();

      if (!planMapper.checkPlanByPlanIdx(planIdx, 0)) { return  404; } // not found 루틴 없음
      if (!planMapper.checkActByActivityIdx(planIdx, activityIdx)) { return 404; } // not found 활동 없음
      if (planMapper.getUserIdxByPlanIdx(planIdx) != verifyRequestDTO.getUserIdx()) { return 403; } // another 작성자 아님
      if (!planMapper.checkActiveByPlanIdx(planIdx)) { return 412; } // preCondition 루틴 활성화 상태 아님
      if (verifyMapper.checkTodayVerified(verifyRequestDTO)) { return 409; } // conflict 이미 인증 함 (activityIdx 사용)
      if (!planMapper.getUserIsDeleted(userIdx).contains("N")) { return 410; } // 탈퇴한 유저 인증 불가

      // 활동 인증 처리 ( 데이터 저장 )
      verifyMapper.verifyActivity(verifyRequestDTO); // activityIdx , userIdx 사용
      // 루틴 불꽃 경험치 증가 로직
      verifyMapper.increaseCertEXP(verifyRequestDTO); // planIdx, userIdx 사용
      // 루틴 마지막 인증 경험치 백업
      verifyMapper.setLastEXP(verifyRequestDTO); // planIdx, userIdx 사용
      // 루틴 인증율 확인
      double verifyRate = verifyMapper.getVerifyRate(planIdx);
      //System.out.println(verifyRate);
      if (verifyRate >= 80.0 && !verifyMapper.checkFireState(planIdx)) {
        verifyMapper.setFireState(verifyRequestDTO); // planIdx, userIdx 사용
        //System.out.println("불꽃 활성화");

        // 불꽃 활성화 여부 조회
        List<CheckAllFireActivateDTO> checkAllFires = verifyMapper.checkAllFireIsActivateByUserIdx(userIdx);
        boolean isAllFireActivated = checkAllFires.stream().findFirst().map(CheckAllFireActivateDTO::isFireMatch).orElse(false);

        if (isAllFireActivated) { // 불꽃 모두 활성화 시 combo 증가
          //System.out.println("콤보 증가");
          verifyMapper.increaseCombo(userIdx);
        }
        // 유저 경험치 증가
        int combo = verifyMapper.getComboByUserIdx(userIdx);
        //System.out.println(getUserExpByCombo(combo));
        verifyMapper.increaseUserExp(getUserExpByCombo(combo), userIdx);
      }
      return 200; // ok
    } catch (Exception e) {
      log.error("Error verifying plan: ", e);
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 롤백
      return 500; // 서버 오류
    }
  }

  // 인증 코드 생성 및 이메일 전송 (가입/수정용 — 이미 등록된 이메일이 아닌 경우에 사용)
  @Override
  public void sendCodeToEmail(String toEmail) {
    // 재발송 쿨다운 체크 (1분 내 재발송 차단)
    String cooldownKey = AUTH_CODE_COOLDOWN_PREFIX + toEmail;
    if (redisService.checkExistsValue(cooldownKey)) {
      throw new IllegalStateException("잠시 후 다시 시도해주세요.");
    }

    String title = "[갓생 로그] 이메일 인증 코드 입니다.";
    String authCode = this.createCode();
    emailService.sendEmail(toEmail, title, authCode);

    // 이메일 인증 요청 시 인증 번호 Redis에 저장
    // (key = "AuthCode " + Email / value = AuthCode) 소멸시간 5분
    redisService.saveStringData(AUTH_CODE_PREFIX + toEmail, authCode, 'm', 5);
    redisService.saveStringData(cooldownKey, "1", 'm', 1);
  }

  // 인증 코드 생성 및 이메일 전송 (단순인증용 — 아이디 찾기 / 비밀번호 초기화)
  // 이메일 열거 공격 방지: 미등록 이메일이어도 항상 동일한 응답 반환
  @Override
  public void sendCodeToEmailForFindAccount(String toEmail) {
    // 재발송 쿨다운 체크 (1분 내 재발송 차단) — 미등록 이메일에도 적용하여 타이밍 공격 방지
    String cooldownKey = AUTH_CODE_COOLDOWN_PREFIX + toEmail;
    if (redisService.checkExistsValue(cooldownKey)) {
      throw new IllegalStateException("잠시 후 다시 시도해주세요.");
    }

    // 등록된 이메일인 경우에만 실제 전송 (미등록 이메일은 전송 없이 쿨다운만 설정)
    if (userMapper.checkUserEmailExist(toEmail)) {
      String title = "[갓생 로그] 이메일 인증 코드 입니다.";
      String authCode = this.createCode();
      emailService.sendEmail(toEmail, title, authCode);
      redisService.saveStringData(AUTH_CODE_PREFIX + toEmail, authCode, 'm', 5);
    }

    // 이메일 등록 여부와 무관하게 항상 쿨다운 적용
    redisService.saveStringData(cooldownKey, "1", 'm', 1);
  }

  // 인증 코드 검증 (Brute Force 방어 — 5회 실패 시 코드 무효화)
  @Override
  public boolean verifiedAuthCode(String email, String authCode) {
    String key = AUTH_CODE_PREFIX + email;
    String failKey = AUTH_CODE_FAIL_PREFIX + email;

    // 실패 횟수 조회
    String failCountStr = redisService.getStringData(failKey);
    int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;

    String redisAuthCode = redisService.getStringData(key);
    boolean codeExists = redisAuthCode != null;
    boolean result = codeExists && redisAuthCode.equals(authCode);

    if (result) {
      // 검증 성공: 인증 코드 및 실패 카운트 삭제 후 인증 완료 플래그 저장
      redisService.deleteData(key);
      redisService.deleteData(failKey);
      redisService.saveStringData("EMAIL_VERIFIED: " + email, "true", 'm', 10);
    } else if (codeExists) {
      // 코드가 존재하지만 불일치: 실패 횟수 증가
      int newFailCount = failCount + 1;
      if (newFailCount >= MAX_FAIL_COUNT) {
        // 최대 실패 횟수 초과 시 코드 즉시 무효화 → 재발송 강제
        redisService.deleteData(key);
        redisService.deleteData(failKey);
        log.warn("이메일 인증 코드 최대 실패 횟수 초과 — 코드 무효화: {}", email);
      } else {
        redisService.saveStringData(failKey, String.valueOf(newFailCount), 'm', 5);
      }
    }

    return result;
  }


  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */
  // 유저 경험치 계산 함수
  private double getUserExpByCombo(int combo) {
    double defaultExp = 50; // 기본 경험치
    int percentage = 10; // 퍼센티지
    int quotient = combo / 10; // 몫

    if (combo == 0 || quotient == 0) { return defaultExp; } // 콤보가 0이거나 10 보다 작을 경우 기본 경험치

    double bonusExp = defaultExp * ((quotient * percentage) * 0.01);

    return defaultExp + bonusExp;
  }

  private String createCode() {
    int length = 6;
    try {
      Random random = SecureRandom.getInstanceStrong();
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < length; i++) {
        builder.append(random.nextInt(10));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException e) {
      log.debug("MemberService.createCode() exception occur");
      Random random = new Random();
      int code = 100000 + random.nextInt(900000); // 6자리 랜덤 숫자
      return String.valueOf(code);
    }
  }
  /* --------------------------------------------------------------------------------------------------------------- */

}
