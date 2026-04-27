package com.cartethyia.easyorange.framework.operlog.service.impl;

import com.cartethyia.easyorange.framework.operlog.entity.SysOperLog;
import com.cartethyia.easyorange.framework.operlog.mapper.SysOperLogMapper;
import com.cartethyia.easyorange.framework.operlog.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl implements SysOperLogService {

    private final SysOperLogMapper operLogMapper;

    @Override
    public void insertOperLog(SysOperLog operLog) {
        operLogMapper.insert(operLog);
    }
}
