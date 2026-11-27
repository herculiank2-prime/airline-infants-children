package com.airline.travel.core.models;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;

/**
 * One question and answer in a {@link TravelQuestionsAccordion}.
 */
@ConsumerType
public interface AccordionItem {

    /** Stable ID derived from the fragment path, so analytics survive reordering. */
    String getId();

    String getQuestion();

    /** Allow-listed answer HTML. */
    String getAnswerHtml();

    /** Rendered expanded on the server. */
    boolean isExpanded();

    /** Data layer entry, or {@code null} when the data layer is disabled. */
    ComponentData getData();
}
