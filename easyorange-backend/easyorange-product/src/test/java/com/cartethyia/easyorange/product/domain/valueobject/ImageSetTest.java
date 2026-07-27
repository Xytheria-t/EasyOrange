package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageSetTest {

    @Test
    @DisplayName("传入空列表时应返回空 ImageSet")
    void create_withNullList_shouldBeEmpty() {
        var imageSet = new ImageSet(null);

        assertThat(imageSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("传入空列表时应返回空 ImageSet")
    void create_withEmptyList_shouldBeEmpty() {
        var imageSet = new ImageSet(List.of());

        assertThat(imageSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("多个主图时应抛出异常")
    void create_withMultipleMainImages_shouldThrow() {
        var image1 = new ImageSet.ProductImage(new ImageUrl("http://img/1.jpg"), 0, true);
        var image2 = new ImageSet.ProductImage(new ImageUrl("http://img/2.jpg"), 1, true);

        assertThatThrownBy(() -> new ImageSet(List.of(image1, image2)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("传入 null URL 列表时应返回空 ImageSet")
    void of_withNullUrls_shouldBeEmpty() {
        var imageSet = ImageSet.of(null);

        assertThat(imageSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("传入 URLs 时应将第一个设为主图")
    void of_withUrls_shouldSetFirstAsMain() {
        var imageSet = ImageSet.of(List.of("http://img/1.jpg", "http://img/2.jpg", "http://img/3.jpg"));

        assertThat(imageSet.images()).hasSize(3);
        assertThat(imageSet.images().get(0).isMain()).isTrue();
        assertThat(imageSet.images().get(1).isMain()).isFalse();
        assertThat(imageSet.images().get(2).isMain()).isFalse();
    }

    @Test
    @DisplayName("空 ImageSet 的 mainImage 应返回 null")
    void mainImage_whenEmpty_shouldReturnNull() {
        var imageSet = ImageSet.empty();

        assertThat(imageSet.mainImage()).isNull();
    }

    @Test
    @DisplayName("mainImage 应返回主图 URL")
    void mainImage_shouldReturnMainUrl() {
        var imageSet = ImageSet.of(List.of("http://img/1.jpg", "http://img/2.jpg"));

        assertThat(imageSet.mainImage()).isNotNull();
        assertThat(imageSet.mainImage().value()).isEqualTo("http://img/1.jpg");
    }

    @Test
    @DisplayName("size 应返回正确计数")
    void size_shouldReturnCorrectCount() {
        var imageSet = ImageSet.of(List.of("http://img/1.jpg", "http://img/2.jpg"));

        assertThat(imageSet.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("imageUrls 应返回提取的 URL 列表")
    void imageUrls_shouldReturnExtractedUrls() {
        var imageSet = ImageSet.of(List.of("http://img/1.jpg", "http://img/2.jpg"));

        var urls = imageSet.imageUrls();

        assertThat(urls).containsExactly("http://img/1.jpg", "http://img/2.jpg");
    }
}
