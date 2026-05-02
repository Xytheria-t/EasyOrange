package com.cartethyia.easyorange.user.domain.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.shared.enums.UserResultCode;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsCodeDomainService {

  private static final int CODE_LENGTH = 6;
  private static final int CODE_EXPIRE_MINUTES = 5;
  private static final int SEND_INTERVAL_SECONDS = 60;
  private static final int MAX_DAILY_SEND_COUNT = 10;
  private static final int MAX_VERIFY_ATTEMPTS = 5;

  private final SmsCodePort smsCodePort;

  public void sendCode(String phone) {
    if (smsCodePort.isSendLimited(phone)) {
      throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
    }

    long dailyCount = smsCodePort.incrementDailyCount(phone);
    if (dailyCount == 1) {
      smsCodePort.expireDailyCount(phone, 1);
    }
    if (dailyCount > MAX_DAILY_SEND_COUNT) {
      throw BusinessException.of(UserResultCode.SMS_CODE_SEND_TOO_FREQUENT);
    }

    String code = generateCode();
    smsCodePort.saveCode(phone, code, CODE_EXPIRE_MINUTES);
    smsCodePort.setSendLimit(phone, SEND_INTERVAL_SECONDS);
  }

  public void verifyCode(String phone, String code) {
    if (code == null || code.isBlank()) {
      throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
    }

    long verifyCount = smsCodePort.incrementVerifyCount(phone);
    if (verifyCount == 1) {
      smsCodePort.expireVerifyCount(phone, CODE_EXPIRE_MINUTES);
    }
    if (verifyCount > MAX_VERIFY_ATTEMPTS) {
      smsCodePort.deleteCode(phone);
      throw BusinessException.of(UserResultCode.SMS_CODE_VERIFY_TOO_FREQUENT);
    }

    String storedCode = smsCodePort.getCode(phone);

    if (storedCode == null || !storedCode.equals(code)) {
      throw BusinessException.of(UserResultCode.SMS_CODE_INVALID);
    }

    smsCodePort.deleteCode(phone);
  }

  private String generateCode() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    StringBuilder sb = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      sb.append(random.nextInt(10));
    }
    return sb.toString();
  }
}
