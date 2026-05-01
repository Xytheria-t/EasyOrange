package com.cartethyia.easyorange.user.domain.port;

public interface SmsCodePort {

  void saveCode(String phone, String code, int expireMinutes);

  String getCode(String phone);

  void deleteCode(String phone);

  boolean isSendLimited(String phone);

  void setSendLimit(String phone, int intervalSeconds);

  long incrementDailyCount(String phone);

  void expireDailyCount(String phone, int days);

  long incrementVerifyCount(String phone);

  void expireVerifyCount(String phone, int expireMinutes);
}
