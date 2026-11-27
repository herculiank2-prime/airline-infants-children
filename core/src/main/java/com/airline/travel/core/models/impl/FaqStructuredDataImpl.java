package com.airline.travel.core.models.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.airline.travel.core.models.FaqStructuredData;
import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.TravelQuestion;
import com.airline.travel.core.services.TravelQuestionService;
import com.airline.travel.core.structureddata.FaqSchemaBuilder;
import com.day.cq.wcm.api.Page;

/**
 * Collects the questions of every travel questions component on the page, in document order and without
 * duplicates, into a single FAQPage (ADR-014).
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = FaqStructuredData.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FaqStructuredDataImpl implements FaqStructuredData {

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    @OSGiService(injectionStrategy = InjectionStrategy.OPTIONAL)
    private TravelQuestionService service;

    private String json;

    @PostConstruct
    protected void init() {
        Resource content = currentPage == null ? null : currentPage.getContentResource();
        if (content == null) {
            return;
        }
        List<Resource> accordions = new ArrayList<>();
        collect(content, accordions);
        Map<String, TravelQuestion> questions = new LinkedHashMap<>();
        for (Resource accordion : accordions) {
            QuestionSelection selection = QuestionSelection.fromValueMap(accordion.getValueMap());
            for (TravelQuestion question : SelectionCache.find(request, service, selection).getQuestions()) {
                questions.putIfAbsent(question.getPath(), question);
            }
        }
        json = FaqSchemaBuilder.build(new ArrayList<>(questions.values()));
    }

    private static void collect(Resource parent, List<Resource> found) {
        for (Resource child : parent.getChildren()) {
            if (TravelQuestionsAccordionImpl.RESOURCE_TYPE.equals(child.getResourceType())) {
                found.add(child);
            } else {
                collect(child, found);
            }
        }
    }

    @Override
    public String getJson() {
        return json;
    }

    @Override
    public boolean isEmpty() {
        return json == null;
    }
}
