package com.airline.travel.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered questions for a selection, plus whether the scan cap stopped the walk early.
 */
public final class SelectionResult {

    public static final SelectionResult EMPTY = new SelectionResult(Collections.emptyList(), false);

    private final List<TravelQuestion> questions;
    private final boolean truncated;

    public SelectionResult(List<TravelQuestion> questions, boolean truncated) {
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
        this.truncated = truncated;
    }

    public List<TravelQuestion> getQuestions() {
        return questions;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
