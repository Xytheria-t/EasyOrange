package com.cartethyia.easyorange.product.adapter.outbound.persistence.repository;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.ReportHandleHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ReportHandleHistoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReportHandleHistoryRepositoryImpl extends BaseRepository<ReportHandleHistoryMapper, ReportHandleHistoryDO> implements ReportHandleHistoryRepository {

    public ReportHandleHistoryRepositoryImpl(ReportHandleHistoryMapper reportHandleHistoryMapper) {
        super(reportHandleHistoryMapper);
    }

    @Override
    public void save(ReportHandleHistory history) {
        ReportHandleHistoryDO historyDO = convertToDO(history);
        mapper.insert(historyDO);
    }

    @Override
    public List<ReportHandleHistory> findByReportId(String reportId) {
        return lambdaQuery()
                .eq(ReportHandleHistoryDO::getReportId, reportId)
                .orderByDesc(ReportHandleHistoryDO::getCreateTime)
                .list()
                .stream()
                .map(this::convertToDomain)
                .toList();
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
