package com.airline.travel.core.models.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.adobe.cq.wcm.core.components.commons.link.Link;

class RelSafeLinkTest {

    @Test
    void mergesExistingRelWithoutDuplicates() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("href", "https://example.org");
        attributes.put("target", "_BLANK");
        attributes.put("rel", " NoOpener  external ");
        assertEquals("noopener external noreferrer", RelSafeLink.withSafeRel(attributes).get("rel"));
    }

    @Test
    void applyingTwiceIsStable() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("target", "_blank");
        Map<String, String> once = RelSafeLink.withSafeRel(attributes);
        assertEquals("noopener noreferrer", RelSafeLink.withSafeRel(once).get("rel"));
    }

    @Test
    void leavesOtherTargetsAndNullAlone() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("target", "_self");
        assertSame(attributes, RelSafeLink.withSafeRel(attributes));
        assertNull(RelSafeLink.withSafeRel(null));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void ofHandlesNullAndAlreadyWrapped() {
        assertNull(RelSafeLink.of(null));
        Link link = mock(Link.class);
        when(link.getHtmlAttributes()).thenReturn(null);
        RelSafeLink wrapped = RelSafeLink.of(link);
        assertSame(wrapped, RelSafeLink.of(wrapped));
        assertNull(wrapped.getHtmlAttributes());
    }
}
