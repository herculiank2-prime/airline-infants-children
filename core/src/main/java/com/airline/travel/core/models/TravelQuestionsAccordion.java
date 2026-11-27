package com.airline.travel.core.models;

import java.util.List;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.wcm.core.components.models.Component;

/**
 * Accordion of travel-question Content Fragments selected by folder or tag (AC-01, HD-01).
 */
@ConsumerType
public interface TravelQuestionsAccordion extends Component {

    /** Section heading, for example "Booking for infants (under 2 years)". */
    String getHeading();

    /** {@code h2}..{@code h6} from the policy; default {@code h2}. */
    String getHeadingElement();

    /** {@code h2}..{@code h6} from the policy; default {@code h3}. */
    String getItemHeadingElement();

    List<AccordionItem> getItems();

    boolean isExpandFirst();

    /** True when the scan cap was reached, so some fragments may be missing. */
    boolean isTruncated();

    /** True when no source is configured or nothing matched. */
    boolean isEmpty();
}
