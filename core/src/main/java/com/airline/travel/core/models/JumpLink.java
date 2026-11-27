package com.airline.travel.core.models;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * An in-page link from the hero to a section ID on the same page.
 */
@ConsumerType
public interface JumpLink {

    String getLabel();

    /**
     * @return the normalised fragment identifier, without the leading {@code #}
     */
    String getAnchor();
}
