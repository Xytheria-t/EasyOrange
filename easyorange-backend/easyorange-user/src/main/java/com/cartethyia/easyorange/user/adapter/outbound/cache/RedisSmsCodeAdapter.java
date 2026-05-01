package com.cartethyia.easyorange.user.adapter.outbound.cache;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisSmsCodeAdapter implements SmsCodePort {

  private static final String SMS_CODE_PREFIX = CommonConstant.APP_PREFIX + "sms:code:";
  private static final String SMS_LIMIT_PREFIX = CommonConstant.APP_PREFIX + "sms:limit:";
  private static final String DAILY_COUNT_PREFIX = CommonConstant.APP_PREFIX + "sms:daily:";
  private static final String VERIFY_COUNT_PREFIX = CommonConstant.APP_PREFIX + "sms:verify:";

  private final RedisCache redisCache;

  @Override
  public void saveCode(String phone, String code, int expireMinutes) {
    redisCache.set(SMS_CODE_PREFIX + phone, code, expireMinutes, TimeUnit.MINUTES);
  }

  @Override
  public String getCode(String phone) {
    return redisCache.get(SMS_CODE_PREFIX + phone, String.class);
  }

  @Override
  public void deleteCode(String phone) {
    redisCache.delete(SMS_CODE_PREFIX + phone);
    redisCache.delete(VERIFY_COUNT_PREFIX + phone);
  }

  @Override
  public boolean isSendLimited(String phone) {
    return Boolean.TRUE.equals(redisCache.hasKey(SMS_LIMIT_PREFIX + phone));
  }

  @Override
  public void setSendLimit(String phone, int intervalSeconds) {
    redisCache.set(SMS_LIMIT_PREFIX + phone, "1", intervalSeconds, TimeUnit.SECONDS);
  }

  @Override
  public long incrementDailyCount(String phone) {
    Long count = redisCache.increment(DAILY_COUNT_PREFIX + phone);
    return count != null ? count : 0;
  }

  @Override
  public void expireDailyCount(String phone, int days) {
    redisCache.expire(DAILY_COUNT_PREFIX + phone, days, TimeUnit.DAYS);
  }

  @Override
  public long incrementVerifyCount(String phone) {
    Long count = redisCache.increment(VERIFY_COUNT_PREFIX + phone);
    return count != null ? count : 0;
  }

  @Override
  public void expireVerifyCount(String phone, int expireMinutes) {
    redisCache.expire(VERIFY_COUNT_PREFIX + phone, expireMinutes, TimeUnit.MINUTES);
  }
}
