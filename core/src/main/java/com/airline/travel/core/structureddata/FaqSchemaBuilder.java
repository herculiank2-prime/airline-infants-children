package com.airline.travel.core.structureddata;

import java.util.List;

import com.airline.travel.core.richtext.RichTextSanitizer;
import com.airline.travel.core.services.TravelQuestion;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Builds Schema.org FAQPage JSON-LD (SE-01). Gson's default HTML-safe mode writes {@code < > & = '} as
 * {@code \\uXXXX}, which is what makes it safe to print the result inside a {@code <script>} element.
 */
public final class FaqSchemaBuilder {

    private static final Gson GSON = new Gson();

    private FaqSchemaBuilder() {
    }

    /**
     * @return the JSON-LD document, or {@code null} when there are no questions
     */
    public static String build(List<TravelQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        JsonArray entities = new JsonArray();
        for (TravelQuestion question : questions) {
            JsonObject answer = new JsonObject();
            answer.addProperty("@type", "Answer");
            answer.addProperty("text", RichTextSanitizer.sanitize(question.getAnswerHtml()));
            JsonObject entity = new JsonObject();
            entity.addProperty("@type", "Question");
            entity.addProperty("name", question.getQuestion());
            entity.add("acceptedAnswer", answer);
            entities.add(entity);
        }
        JsonObject page = new JsonObject();
        page.addProperty("@context", "https://schema.org");
        page.addProperty("@type", "FAQPage");
        page.add("mainEntity", entities);
        return GSON.toJson(page);
    }
}
