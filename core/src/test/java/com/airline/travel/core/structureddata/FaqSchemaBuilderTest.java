package com.airline.travel.core.structureddata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.airline.travel.core.services.TravelQuestion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FaqSchemaBuilderTest {

    private static TravelQuestion question(String name, String answer) {
        return new TravelQuestion("/content/dam/airline/travel-questions/x", name, answer, Collections.emptyList(), 1);
    }

    @Test
    void buildsFaqPageWithQuestionsInOrder() {
        String json = FaqSchemaBuilder.build(List.of(question("First?", "<p>One</p>"), question("Second?", "<p>Two</p>")));
        JsonObject page = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("https://schema.org", page.get("@context").getAsString());
        assertEquals("FAQPage", page.get("@type").getAsString());
        assertEquals(2, page.getAsJsonArray("mainEntity").size());
        JsonObject first = page.getAsJsonArray("mainEntity").get(0).getAsJsonObject();
        assertEquals("Question", first.get("@type").getAsString());
        assertEquals("First?", first.get("name").getAsString());
        assertEquals("Answer", first.getAsJsonObject("acceptedAnswer").get("@type").getAsString());
        assertEquals("<p>One</p>", first.getAsJsonObject("acceptedAnswer").get("text").getAsString());
    }

    @Test
    void outputCannotCloseTheScriptElement() {
        String json = FaqSchemaBuilder.build(List.of(question("</script><script>alert(1)</script>",
                "<p>Fine</p><script>alert(2)</script>")));
        assertFalse(json.contains("<"), json);
        assertFalse(json.contains(">"), json);
        assertTrue(json.contains("\\u003c/script\\u003e"));
        JsonObject entity = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("mainEntity").get(0)
                .getAsJsonObject();
        assertEquals("<p>Fine</p>", entity.getAsJsonObject("acceptedAnswer").get("text").getAsString());
    }

    @Test
    void noQuestionsGivesNull() {
        assertNull(FaqSchemaBuilder.build(null));
        assertNull(FaqSchemaBuilder.build(Collections.emptyList()));
    }
}
