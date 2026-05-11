package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.admin.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.dto.response.BatchAuditResultVO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductAuditService {

    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(Long id, ProductAuditRequest request) {
        ProductDO product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        int targetStatus = Integer.parseInt(request.getStatus());

        if (targetStatus == 1) {
            if (product.getStatus() != ProductStatus.DRAFT.getCode()) {
                throw BusinessException.of("只有草稿状态的商品可以审核通过");
            }
            product.setStatus(ProductStatus.ONLINE.getCode());
        } else if (targetStatus == -1) {
            if (product.getStatus() != ProductStatus.DRAFT.getCode()) {
                throw BusinessException.of("只有草稿状态的商品可以拒绝");
            }
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw BusinessException.of("拒绝时必须填写原因");
            }
            product.setStatus(ProductStatus.OFFLINE.getCode());
        } else {
            throw BusinessException.of("无效的审核状态");
        }

        productMapper.updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultVO batchAudit(BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditRequest.AuditItem item : request.getItems()) {
            try {
                ProductDO product = productMapper.selectById(item.productId());
                if (product == null || product.getDelFlag() != 0) {
                    errors.add("商品ID " + item.productId() + ": 不存在");
                    continue;
                }

                if (item.status() == 1) {
                    if (product.getStatus() != ProductStatus.DRAFT.getCode()) {
                        errors.add("商品ID " + item.productId() + ": 非草稿状态，无法通过");
                        continue;
                    }
                    product.setStatus(ProductStatus.ONLINE.getCode());
                } else if (item.status() == -1) {
                    if (product.getStatus() != ProductStatus.DRAFT.getCode()) {
                        errors.add("商品ID " + item.productId() + ": 非草稿状态，无法拒绝");
                        continue;
                    }
                    product.setStatus(ProductStatus.OFFLINE.getCode());
                } else {
                    errors.add("商品ID " + item.productId() + ": 无效的状态值 " + item.status());
                    continue;
                }

                productMapper.updateById(product);
                successCount++;
            } catch (Exception e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            }
        }

        return new BatchAuditResultVO(
            request.getItems().size(),
            successCount,
            errors.size(),
            errors
        );
    }
}
