package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminProductAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryResult;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final AdminProductQueryPort adminProductQueryPort;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCachePort;
    private final DomainEventPublisher domainEventPublisher;
    private final AdminProductAssembler adminProductAssembler;

    @Transactional(readOnly = true)
    public PageResult<AdminProductResponse> listProducts(AdminProductQueryRequest request) {
        ProductQueryCondition condition = new ProductQueryCondition(
            request.keyword(),
            request.categoryId(),
            request.status(),
            request.sellerId(),
            parseDate(request.startTime(), false),
            parseDate(request.endTime(), true),
            request.pageNum(),
            request.pageSize()
        );

        ProductQueryResult result = adminProductQueryPort.queryProducts(condition);

        List<String> productIds = result.records().stream()
            .map(AdminProductQueryPort.ProductSummary::id)
            .toList();

        Map<String, List<String>> imagesMap = adminProductQueryPort.getProductImages(productIds);

        List<AdminProductResponse> records = result.records().stream()
            .map(p -> adminProductAssembler.toSummaryResponse(p, imagesMap.getOrDefault(p.id(), List.of())))
            .toList();

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getProductDetail(String id) {
        ProductDetail productDetail = adminProductQueryPort.getProductDetail(id);
        if (productDetail == null) {
            throw BusinessException.of("商品不存在");
        }

        List<String> images = adminProductQueryPort.getProductImages(List.of(id))
            .getOrDefault(id, List.of());

        return adminProductAssembler.toDetailResponse(productDetail, images);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatus(String id, UpdateStatusRequest request) {
        ProductStatus newStatus;
        try {
            newStatus = ProductStatus.fromCode(request.getStatus());
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的商品状态");
        }

        Product product = productRepository.findById(ProductId.of(id))
            .orElseThrow(() -> BusinessException.of("商品不存在"));

        applyStatusTransition(product, newStatus);
        productCachePort.evictProductCache(id);
    }

    private void applyStatusTransition(Product product, ProductStatus newStatus) {
        var transition = switch (newStatus) {
            case ONLINE -> product.putOnline();
            case OFFLINE -> product.takeOffline();
            case SOLD -> product.markAsSold().orElse(null);
            default -> throw BusinessException.of("不支持将该商品状态改为: " + newStatus.getDesc());
        };
        if (transition != null) {
            productRepository.save(transition.aggregate());
            domainEventPublisher.publish(transition.event());
        }
    }

    private LocalDateTime parseDate(String dateStr, boolean endOfDay) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("无法解析时间: {}, 格式应为 yyyy-MM-dd", dateStr);
            return null;
        }
    }
}
