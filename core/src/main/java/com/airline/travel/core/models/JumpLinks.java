package com.airline.travel.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * "On this page" jump links ({@code airline/components/jumplinks}), authored inside the hero's
 * links container (HB-01, ADR-030).
 */
@ConsumerType
public interface JumpLinks extends Component {

    String getTitle();

    /**
     * @return links with a label and a normalised in-page anchor; incomplete items are skipped
     */
    List<JumpLink> getLinks();

    /**
     * @return {@code true} when there is no complete link; the component then renders only the edit placeholder
     */
    boolean isEmpty();
}
