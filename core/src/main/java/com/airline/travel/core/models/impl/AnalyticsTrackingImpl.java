package com.airline.travel.core.models.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.airline.travel.core.models.AnalyticsTracking;

/** DL-01, ADR-031: reads and normalises the Analytics tab fields. */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        adapters = AnalyticsTracking.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class AnalyticsTrackingImpl implements AnalyticsTracking {

    static final String PREFIX = "airline:";
    static final String ATTR_EVENT = "data-airline-track-event";
    static final String ATTR_LABEL = "data-airline-track-label";
    static final int MAX_EVENT_LENGTH = 64;
    static final int MAX_LABEL_LENGTH = 100;

    @ValueMapValue
    private String trackingEvent;

    @ValueMapValue
    private String trackingLabel;

    @Override
    public String getEvent() {
        return normaliseEvent(trackingEvent);
    }

    @Override
    public String getLabel() {
        String label = StringUtils.trimToNull(trackingLabel);
        return label == null ? null : StringUtils.left(label, MAX_LABEL_LENGTH);
    }

    @Override
    public boolean isConfigured() {
        return getEvent() != null;
    }

    @Override
    public Map<String, String> getAttributes() {
        String event = getEvent();
        if (event == null) {
            return Collections.emptyMap();
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put(ATTR_EVENT, event);
        String label = getLabel();
        if (label != null) {
            attributes.put(ATTR_LABEL, label);
        }
        return attributes;
    }

    /**
     * Lower-cases, replaces anything outside {@code a-z 0-9 : _ -} with a hyphen and forces the
     * {@code airline:} prefix, so authored names can't collide with Core's {@code cmp:*} events (ADR-017).
     */
    static String normaliseEvent(String raw) {
        String value = StringUtils.trimToNull(raw);
        if (value == null) {
            return null;
        }
        value = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9:_-]+", "-");
        if (value.startsWith(PREFIX)) {
            value = value.substring(PREFIX.length());
        }
        value = value.replaceAll("^[-:]+|[-:]+$", "");
        if (value.isEmpty()) {
            return null;
        }
        return StringUtils.left(PREFIX + value, MAX_EVENT_LENGTH);
    }
}
