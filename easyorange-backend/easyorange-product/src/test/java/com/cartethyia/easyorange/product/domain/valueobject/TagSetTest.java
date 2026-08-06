package com.cartethyia.easyorange.product.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagSetTest {

    @Test
    @DisplayName("传入空 tags 时应为空集合")
    void create_withNullTags_shouldBeEmpty() {
        var tagSet = new TagSet(null);

        assertThat(tagSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("传入只含空白 tags 时应为空集合")
    void create_withOnlyBlankTags_shouldBeEmpty() {
        var tagSet = new TagSet(Set.of("", "  "));

        assertThat(tagSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("创建时应过滤空白并 trim 标签")
    void create_shouldTrimAndFilterBlanks() {
        var tagSet = new TagSet(Set.of("  tag1  ", "", "tag2"));

        assertThat(tagSet.tags()).containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(tagSet.tags()).hasSize(2);
    }

    @Test
    @DisplayName("empty 应返回空 TagSet")
    void empty_shouldReturnEmptyTagSet() {
        var tagSet = TagSet.empty();

        assertThat(tagSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("传入 null 可变参数应返回空 TagSet")
    void of_withNullVarargs_shouldBeEmpty() {
        var tagSet = TagSet.of(null, null);

        assertThat(tagSet.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("of 应创建非空 TagSet")
    void of_shouldCreateNonEmptyTagSet() {
        var tagSet = TagSet.of("a", "b", "");

        assertThat(tagSet.tags()).containsExactlyInAnyOrder("a", "b");
        assertThat(tagSet.tags()).hasSize(2);
    }

    @Test
    @DisplayName("contains 应返回正确结果")
    void contains_shouldReturnCorrectResult() {
        var tagSet = TagSet.of("apple", "banana");

        assertThat(tagSet.contains("apple")).isTrue();
        assertThat(tagSet.contains("cherry")).isFalse();
    }
}
