package com.airline.travel.core.models.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Decorates a Core {@link Link} so that links opening a new browsing context carry
 * {@code rel="noopener noreferrer"} exactly once, merged with any {@code rel} the delegate already emits.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RelSafeLink implements Link<Object> {

    static final String ATTR_TARGET = "target";
    static final String ATTR_REL = "rel";
    static final String TARGET_BLANK = "_blank";
    static final Set<String> REQUIRED_REL = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("noopener", "noreferrer")));

    @SuppressWarnings("rawtypes")
    private final Link delegate;

    @SuppressWarnings("rawtypes")
    private RelSafeLink(Link delegate) {
        this.delegate = delegate;
    }

    /**
     * @return the decorated link, or {@code null} when {@code link} is {@code null}
     */
    @SuppressWarnings("rawtypes")
    public static RelSafeLink of(Link link) {
        if (link == null) {
            return null;
        }
        return link instanceof RelSafeLink ? (RelSafeLink) link : new RelSafeLink(link);
    }

    @Override
    @JsonProperty("valid")
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    @JsonProperty("url")
    public String getURL() {
        return delegate.getURL();
    }

    @Override
    @JsonIgnore
    public String getMappedURL() {
        return delegate.getMappedURL();
    }

    @Override
    @JsonIgnore
    public String getExternalizedURL() {
        return delegate.getExternalizedURL();
    }

    @Override
    @JsonIgnore
    public Object getReference() {
        return delegate.getReference();
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Map<String, String> getHtmlAttributes() {
        return withSafeRel(delegate.getHtmlAttributes());
    }

    static Map<String, String> withSafeRel(Map<String, String> attributes) {
        if (attributes == null || !TARGET_BLANK.equalsIgnoreCase(attributes.get(ATTR_TARGET))) {
            return attributes;
        }
        Set<String> rel = new LinkedHashSet<>();
        String existing = attributes.get(ATTR_REL);
        if (StringUtils.isNotBlank(existing)) {
            rel.addAll(Arrays.asList(existing.trim().toLowerCase().split("\\s+")));
        }
        rel.addAll(REQUIRED_REL);
        Map<String, String> safe = new LinkedHashMap<>(attributes);
        safe.put(ATTR_REL, String.join(" ", rel));
        return Collections.unmodifiableMap(safe);
    }
}
