package com.cartethyia.easyorange.product.domain.valueobject;

import java.util.Set;

public record TagSet(Set<String> tags) {

    public static final TagSet EMPTY = new TagSet(Set.of());

    public TagSet {
        tags = tags == null || tags.isEmpty()
                ? Set.of()
                : Set.copyOf(tags.stream()
                        .filter(t -> t != null && !t.isBlank())
                        .map(String::trim)
                        .toList());
    }

    public static TagSet empty() {
        return EMPTY;
    }

    public static TagSet of(String... tags) {
        return new TagSet(Set.of(tags));
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }
}
