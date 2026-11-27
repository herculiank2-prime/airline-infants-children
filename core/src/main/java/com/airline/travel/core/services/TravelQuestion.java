package com.airline.travel.core.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One travel-question Content Fragment, as stored. The answer is the raw rich text; callers sanitise it
 * before output.
 */
public final class TravelQuestion {

    private final String path;
    private final String question;
    private final String answerHtml;
    private final List<String> tags;
    private final int sortOrder;

    public TravelQuestion(String path, String question, String answerHtml, List<String> tags, int sortOrder) {
        this.path = path;
        this.question = question;
        this.answerHtml = answerHtml;
        this.tags = tags == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tags));
        this.sortOrder = sortOrder;
    }

    public String getPath() {
        return path;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerHtml() {
        return answerHtml;
    }

    public List<String> getTags() {
        return tags;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
