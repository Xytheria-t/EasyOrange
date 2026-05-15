package com.cartethyia.easyorange.framework.operlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.framework.operlog.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    int deleteExpiredLogs(LocalDateTime expireDate);

    List<SysOperLog> selectLogsBefore(LocalDateTime targetDate);

    int batchInsertArchive(List<SysOperLog> logs);
}
