package com.airline.travel.core.services;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * Finds travel-question fragments with the same semantic as the persisted queries (SOLUTION_DESIGN 8.1):
 * folder descendants or exact tag IDs, ordered by {@code sortOrder} then {@code question}.
 */
public interface TravelQuestionService {

    /**
     * @param resolver the request's resolver, so publish reads as the anonymous visitor would
     * @param selection what to find; {@code null} returns {@link SelectionResult#EMPTY}
     * @return ordered, limited questions
     */
    SelectionResult find(ResourceResolver resolver, QuestionSelection selection);
}
