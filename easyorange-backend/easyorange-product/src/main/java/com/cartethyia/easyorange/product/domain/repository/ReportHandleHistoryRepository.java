package com.cartethyia.easyorange.product.domain.repository;

import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import java.util.List;

public interface ReportHandleHistoryRepository {

    void save(ReportHandleHistory history);

    List<ReportHandleHistory> findByReportId(String reportId);
}
