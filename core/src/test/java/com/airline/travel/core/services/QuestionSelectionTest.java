package com.airline.travel.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.jupiter.api.Test;

class QuestionSelectionTest {

    private static QuestionSelection from(Map<String, Object> properties) {
        return QuestionSelection.fromValueMap(new ValueMapDecorator(properties));
    }

    @Test
    void folderModeIsTheDefault() {
        QuestionSelection selection = from(Map.of("folderPath", " /content/dam/airline/travel-questions/infants ",
                "maxItems", 5L));
        assertEquals(QuestionSelection.Mode.FOLDER, selection.getMode());
        assertEquals("/content/dam/airline/travel-questions/infants", selection.getFolderPath());
        assertEquals(5, selection.getLimit());
        assertTrue(selection.getTagIds().isEmpty());
        assertTrue(selection.toString().startsWith("folder"));
    }

    @Test
    void tagModeReadsTagsMatchAndRoot() {
        QuestionSelection selection = from(Map.of("source", "tags",
                "tags", new String[] {"airline:travel/infants", " ", "airline:market/au", "airline:market/au"},
                "tagMatch", "all", "rootPath", "/content/dam/airline/travel-questions"));
        assertEquals(QuestionSelection.Mode.TAGS, selection.getMode());
        assertEquals(List.of("airline:travel/infants", "airline:market/au"), selection.getTagIds());
        assertTrue(selection.isMatchAll());
        assertEquals("/content/dam/airline/travel-questions", selection.getRootPath());
        assertNull(selection.getFolderPath());
        assertEquals(0, selection.getLimit());
        assertTrue(selection.toString().contains("(all)"));
        assertFalse(from(Map.of("source", "tags", "tags", new String[] {"a:b"})).isMatchAll());
    }

    @Test
    void unconfiguredSourcesReturnNull() {
        assertNull(from(Map.of()));
        assertNull(from(Map.of("source", "tags")));
        assertNull(from(Map.of("source", "folder", "folderPath", " ")));
    }

    @Test
    void equalSelectionsShareACacheKey() {
        QuestionSelection a = from(Map.of("folderPath", "/x"));
        QuestionSelection b = from(Map.of("folderPath", "/x"));
        assertEquals(a, b);
        assertEquals(a, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, from(Map.of("folderPath", "/y")));
        assertNotEquals(a, "folder /x");
    }
}
