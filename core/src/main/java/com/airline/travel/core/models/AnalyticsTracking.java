package com.airline.travel.core.models;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Author-configured click tracking for a component (DL-01, ADR-031). When configured, the component's
 * markup carries the event name and label as data attributes, and the site script pushes that event to
 * the Adobe Client Data Layer instead of the default one.
 */
@ConsumerType
public interface AnalyticsTracking {

    /** The event name, always in the {@code airline:} namespace, or null when not configured. */
    String getEvent();

    /** The label sent with the event, or null when not set. */
    String getLabel();

    boolean isConfigured();

    /** Data attributes for the HTL wrapper; empty when not configured. */
    Map<String, String> getAttributes();
}
