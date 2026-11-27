package com.airline.travel.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Hero banner ({@code airline/components/hero}): the page's single {@code h1}, an introduction,
 * an optional image and "on this page" jump links (HB-01..HB-03, ADR-020).
 */
@ConsumerType
public interface HeroBanner extends Component {

    String getTitle();

    String getIntroduction();

    /**
     * @return the image to render, or {@code null} when there is no image or when a non-decorative
     *         image has no alternative text (see {@link #isImageMissingAlt()})
     */
    HeroImage getImage();

    String getJumpLinksTitle();

    List<JumpLink> getJumpLinks();

    /**
     * @return {@code true} when an image is selected, is not decorative, and neither the author nor
     *         the asset's {@code dc:description} supplies alternative text
     */
    boolean isImageMissingAlt();

    /**
     * @return {@code true} when the title is blank; the component then renders only the edit placeholder
     */
    boolean isEmpty();
}
