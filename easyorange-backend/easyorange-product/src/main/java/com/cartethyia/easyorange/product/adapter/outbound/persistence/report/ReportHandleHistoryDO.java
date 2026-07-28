package com.cartethyia.easyorange.product.adapter.outbound.persistence.report;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_report_handle_history")
public class ReportHandleHistoryDO extends BaseDO {

    private String reportId;

    private String operatorId;

    private String action;

    private String remark;
}
