package com.airline.travel.core.models.impl;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.airline.travel.core.models.AccordionItem;
import com.airline.travel.core.richtext.RichTextSanitizer;
import com.airline.travel.core.services.TravelQuestion;

final class AccordionItemImpl implements AccordionItem {

    static final String ID_PREFIX = "travelquestions-item";
    static final String ITEM_TYPE = "airline/components/travelquestions/item";

    private final String id;
    private final String question;
    private final String answerHtml;
    private final boolean expanded;
    private final String parentId;
    private final boolean dataLayerEnabled;

    AccordionItemImpl(TravelQuestion source, boolean expanded, String parentId, boolean dataLayerEnabled) {
        this.id = ComponentUtils.generateId(ID_PREFIX, source.getPath());
        this.question = source.getQuestion();
        this.answerHtml = RichTextSanitizer.sanitize(source.getAnswerHtml());
        this.expanded = expanded;
        this.parentId = parentId;
        this.dataLayerEnabled = dataLayerEnabled;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getQuestion() {
        return question;
    }

    @Override
    public String getAnswerHtml() {
        return answerHtml;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public ComponentData getData() {
        if (!dataLayerEnabled) {
            return null;
        }
        return DataLayerBuilder.forComponent()
                .withId(() -> id)
                .withType(() -> ITEM_TYPE)
                .withTitle(() -> question)
                .withParentId(() -> parentId)
                .build();
    }
}
