package com.airline.travel.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Responsive hero image built from web renditions only; the original is never referenced.
 */
@ConsumerType
public interface HeroImage {

    String getSrc();

    String getSrcset();

    String getSizes();

    int getWidth();

    int getHeight();

    /**
     * @return the alternative text; empty for a decorative image
     */
    String getAlt();

    boolean isDecorative();
}
