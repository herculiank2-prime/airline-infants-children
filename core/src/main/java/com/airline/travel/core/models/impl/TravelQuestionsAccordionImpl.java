package com.airline.travel.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.airline.travel.core.models.AccordionItem;
import com.airline.travel.core.models.TravelQuestionsAccordion;
import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.SelectionResult;
import com.airline.travel.core.services.TravelQuestion;
import com.airline.travel.core.services.TravelQuestionService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Style;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = {TravelQuestionsAccordion.class, ComponentExporter.class},
        resourceType = TravelQuestionsAccordionImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TravelQuestionsAccordionImpl implements TravelQuestionsAccordion {

    static final String RESOURCE_TYPE = "airline/components/travelquestions";
    static final String PN_HEADING_ELEMENT = "headingElement";
    static final String PN_ITEM_HEADING_ELEMENT = "itemHeadingElement";
    static final String DEFAULT_HEADING_ELEMENT = "h2";
    static final String DEFAULT_ITEM_HEADING_ELEMENT = "h3";
    private static final Set<String> HEADINGS = Set.of("h2", "h3", "h4", "h5", "h6");

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @OSGiService(injectionStrategy = InjectionStrategy.OPTIONAL)
    private TravelQuestionService service;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ComponentContext componentContext;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Style currentStyle;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private boolean expandFirst;

    private List<AccordionItem> items = Collections.emptyList();
    private boolean truncated;
    private String id;

    @PostConstruct
    protected void init() {
        heading = StringUtils.trimToNull(heading);
        SelectionResult result = SelectionCache.find(request, service,
                QuestionSelection.fromValueMap(resource.getValueMap()));
        truncated = result.isTruncated();
        boolean dataLayer = ComponentUtils.isDataLayerEnabled(resource);
        List<AccordionItem> built = new ArrayList<>();
        for (TravelQuestion question : result.getQuestions()) {
            built.add(new AccordionItemImpl(question, expandFirst && built.isEmpty(), getId(), dataLayer));
        }
        items = Collections.unmodifiableList(built);
    }

    @Override
    public String getHeading() {
        return heading;
    }

    @Override
    public String getHeadingElement() {
        return policyHeading(PN_HEADING_ELEMENT, DEFAULT_HEADING_ELEMENT);
    }

    @Override
    public String getItemHeadingElement() {
        return policyHeading(PN_ITEM_HEADING_ELEMENT, DEFAULT_ITEM_HEADING_ELEMENT);
    }

    private String policyHeading(String property, String fallback) {
        String value = currentStyle == null ? null : currentStyle.get(property, String.class);
        return value != null && HEADINGS.contains(value) ? value : fallback;
    }

    @Override
    public List<AccordionItem> getItems() {
        return items;
    }

    @Override
    public boolean isExpandFirst() {
        return expandFirst;
    }

    @Override
    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String getId() {
        if (id == null) {
            id = ComponentUtils.getId(resource, currentPage, componentContext);
        }
        return id;
    }

    @Override
    @JsonProperty("dataLayer")
    public ComponentData getData() {
        if (!ComponentUtils.isDataLayerEnabled(resource)) {
            return null;
        }
        return DataLayerBuilder.forComponent()
                .withId(this::getId)
                .withType(resource::getResourceType)
                .withTitle(this::getHeading)
                .build();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
