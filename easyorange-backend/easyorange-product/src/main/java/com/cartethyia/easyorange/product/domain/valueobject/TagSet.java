package com.cartethyia.easyorange.product.domain.valueobject;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TagSet(Set<String> tags) {
    public TagSet {
        if (tags == null || tags.isEmpty()) {
            tags = Collections.emptySet();
        } else {
            tags = Collections.unmodifiableSet(
                    tags.stream()
                            .filter(t -> t != null && !t.isBlank())
                            .map(String::trim)
                            .collect(Collectors.toSet())
            );
        }
    }

    public static TagSet empty() {
        return new TagSet(Collections.emptySet());
    }

    public static TagSet of(String... tags) {
        return new TagSet(Stream.of(tags).collect(Collectors.toSet()));
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }
}