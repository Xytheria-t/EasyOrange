package com.cartethyia.easyorange.product.domain.valueobject;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TagSet(Set<String> tags) {

    public static final TagSet EMPTY = new TagSet(Collections.emptySet());

    public TagSet {
        if (tags == null || tags.isEmpty()) {
            tags = Collections.emptySet();
        } else {
            tags = tags.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    public static TagSet empty() {
        return EMPTY;
    }

    public static TagSet of(String... tags) {
        return new TagSet(Stream.of(tags).collect(Collectors.toUnmodifiableSet()));
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }
}
