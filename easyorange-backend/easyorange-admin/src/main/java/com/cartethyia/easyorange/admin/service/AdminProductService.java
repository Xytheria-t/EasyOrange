package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductSummary;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminProductQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CommonConstant.DATETIME_FORMAT);

    private final AdminProductQueryPort adminProductQueryPort;
    private final ProductRepository productRepository;
    private final ProductCacheEvictionPort productCachePort;

    public PageResult<AdminProductResponse> listProducts(AdminProductQueryRequest request) {
        LocalDateTime startTime = parseStartTime(request.startTime());
        LocalDateTime endTime = parseEndTime(request.endTime());

        ProductQueryCondition condition = new ProductQueryCondition(
            request.keyword(),
            request.categoryId(),
            request.status(),
            request.sellerId(),
            startTime,
            endTime,
            request.pageNum(),
            request.pageSize()
        );

        ProductQueryResult result = adminProductQueryPort.queryProducts(condition);

        List<String> productIds = result.records().stream()
            .map(ProductSummary::id)
            .collect(Collectors.toList());

        Map<String, List<String>> imagesMap = adminProductQueryPort.getProductImages(productIds);

        List<AdminProductResponse> records = result.records().stream()
            .map(p -> toAdminProductResponse(p, imagesMap))
            .collect(Collectors.toList());

        return PageResult.of(records, result.total(), result.pageNum(), result.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getProductDetail(String id) {
        ProductDetail productDetail = adminProductQueryPort.getProductDetail(id);
        if (productDetail == null) {
            throw BusinessException.of("商品不存在");
        }

        Map<String, List<String>> imagesMap = adminProductQueryPort.getProductImages(List.of(id));
        List<String> images = imagesMap.getOrDefault(id, List.of());
        String mainImage = images.isEmpty() ? null : images.get(0);

        return AdminProductResponse.builder()
            .productId(productDetail.id())
            .name(productDetail.name())
            .description(productDetail.description())
            .price(productDetail.price())
            .originalPrice(productDetail.originalPrice())
            .stock(productDetail.stock())
            .status(productDetail.status())
            .statusDesc(productDetail.statusDesc())
            .conditionLevel(productDetail.conditionLevel())
            .location(productDetail.location())
            .contactMethod(productDetail.contactMethod())
            .images(images)
            .mainImage(mainImage)
            .categoryId(productDetail.categoryId())
            .sellerId(productDetail.sellerId())
            .viewCount(productDetail.viewCount())
            .createTime(productDetail.createTime())
            .updateTime(productDetail.updateTime())
            .build();
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

        Product updated = applyStatusTransition(product, newStatus);
        productRepository.update(updated);
        productCachePort.evictProductCache(id);
    }

    private Product applyStatusTransition(Product product, ProductStatus newStatus) {
        return switch (newStatus) {
            case ONLINE -> product.putOnline().product();
            case OFFLINE -> product.takeOffline().product();
            case SOLD -> product.markAsSold().product();
            default -> product.toBuilder()
                    .status(newStatus)
                    .updateTime(LocalDateTime.now())
                    .build();
        };
    }

    private LocalDateTime parseStartTime(String startTimeStr) {
        if (!StringUtils.hasText(startTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(startTimeStr + " 00:00:00", DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("无法解析开始时间: {}, 格式应为 yyyy-MM-dd", startTimeStr);
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endTimeStr) {
        if (!StringUtils.hasText(endTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(endTimeStr + " 23:59:59", DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("无法解析结束时间: {}, 格式应为 yyyy-MM-dd", endTimeStr);
            return null;
        }
    }

    private AdminProductResponse toAdminProductResponse(ProductSummary product, Map<String, List<String>> imagesMap) {
        List<String> images = imagesMap.getOrDefault(product.id(), List.of());
        String mainImage = images.isEmpty() ? null : images.get(0);

        return AdminProductResponse.builder()
            .productId(product.id())
            .name(product.name())
            .price(product.price())
            .originalPrice(product.originalPrice())
            .stock(product.stock())
            .status(product.status())
            .statusDesc(product.statusDesc())
            .conditionLevel(product.conditionLevel())
            .location(product.location())
            .contactMethod(product.contactMethod())
            .images(images)
            .mainImage(mainImage)
            .categoryId(product.categoryId())
            .sellerId(product.sellerId())
            .viewCount(product.viewCount())
            .createTime(product.createTime())
            .updateTime(product.updateTime())
            .build();
    }
}
