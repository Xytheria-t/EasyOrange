package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ReportHandleHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ReportHandleHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReportHandleHistoryRepositoryImpl implements ReportHandleHistoryRepository {

    private final ReportHandleHistoryMapper reportHandleHistoryMapper;

    @Override
    public void save(ReportHandleHistory history) {
        ReportHandleHistoryDO historyDO = convertToDO(history);
        reportHandleHistoryMapper.insert(historyDO);
        history.assignId(historyDO.getId());
    }

    @Override
    public List<ReportHandleHistory> findByReportId(Long reportId) {
        LambdaQueryWrapper<ReportHandleHistoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportHandleHistoryDO::getReportId, reportId)
                .orderByDesc(ReportHandleHistoryDO::getCreateTime);

        return reportHandleHistoryMapper.selectList(wrapper).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    private ReportHandleHistory convertToDomain(ReportHandleHistoryDO do_) {
        if (do_ == null) {
            return null;
        }
        return ReportHandleHistory.reconstitute(
                do_.getId(),
                do_.getReportId(),
                do_.getOperatorId(),
                do_.getAction(),
                do_.getRemark(),
                do_.getCreateTime()
        );
    }

    private ReportHandleHistoryDO convertToDO(ReportHandleHistory history) {
        return ReportHandleHistoryDO.builder()
                .reportId(history.getReportId())
                .operatorId(history.getOperatorId())
                .action(history.getAction())
                .remark(history.getRemark())
                .build();
    }
}
