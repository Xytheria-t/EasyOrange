package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TagSet implements ValueObject {

    private final Set<String> tags;

    public TagSet(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.tags = Collections.emptySet();
            return;
        }
        this.tags = Collections.unmodifiableSet(
                tags.stream()
                        .filter(t -> t != null && !t.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toSet())
        );
    }

    public static TagSet empty() {
        return new TagSet(Collections.emptySet());
    }

    public static TagSet of(String... tags) {
        return new TagSet(Stream.of(tags).collect(Collectors.toSet()));
    }

    public Set<String> values() {
        return tags;
    }

    public boolean contains(String tag) {
        return tags.contains(tag);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagSet tagSet = (TagSet) o;
        return Objects.equals(tags, tagSet.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags);
    }
}
