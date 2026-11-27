package com.airline.travel.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * One FAQPage JSON-LD document for the current page, from every travel questions component on it (SE-01).
 */
@ConsumerType
public interface FaqStructuredData {

    /** HTML-safe JSON, or {@code null} when {@link #isEmpty()}. */
    String getJson();

    boolean isEmpty();
}
