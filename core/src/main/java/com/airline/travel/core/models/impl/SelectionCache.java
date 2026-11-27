package com.airline.travel.core.models.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;

import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.SelectionResult;
import com.airline.travel.core.services.TravelQuestionService;

/**
 * Request-scoped cache of selection results. The page head (JSON-LD) runs first and the accordion reuses
 * its result, so each selection is scanned once per request (ADR-014).
 */
final class SelectionCache {

    static final String ATTRIBUTE = SelectionCache.class.getName();

    private SelectionCache() {
    }

    @SuppressWarnings("unchecked")
    static SelectionResult find(SlingHttpServletRequest request, TravelQuestionService service,
            QuestionSelection selection) {
        if (selection == null || service == null) {
            return SelectionResult.EMPTY;
        }
        Object existing = request.getAttribute(ATTRIBUTE);
        Map<QuestionSelection, SelectionResult> cache;
        if (existing instanceof Map) {
            cache = (Map<QuestionSelection, SelectionResult>) existing;
        } else {
            cache = new HashMap<>();
            request.setAttribute(ATTRIBUTE, cache);
        }
        return cache.computeIfAbsent(selection, key -> service.find(request.getResourceResolver(), key));
    }
}
