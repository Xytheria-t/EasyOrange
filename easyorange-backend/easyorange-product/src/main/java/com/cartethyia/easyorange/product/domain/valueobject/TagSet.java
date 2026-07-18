package com.cartethyia.easyorange.product.domain.valueobject;

import java.util.Arrays;
import java.util.Set;

public record TagSet(Set<String> tags) {

    public TagSet {
        tags = tags == null || tags.isEmpty()
                ? Set.of()
                : Set.copyOf(tags.stream()
                        .filter(t -> t != null && !t.isBlank())
                        .map(String::trim)
                        .toList());
    }

    public static TagSet empty() {
        return new TagSet(Set.of());
    }

    public static TagSet of(String... tags) {
        if (tags == null || tags.length == 0) {
            return empty();
        }
        var filtered = Arrays.stream(tags)
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .toList();
        return filtered.isEmpty() ? empty() : new TagSet(Set.copyOf(filtered));
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }
}
