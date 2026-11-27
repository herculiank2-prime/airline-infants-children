package com.airline.travel.core.models.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.models.JumpLink;
import com.airline.travel.core.models.JumpLinks;
import com.airline.travel.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class JumpLinksImplTest {

    private final AemContext context = AppAemContext.newAemContext();
    private String path;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(JumpLinksImpl.class);
        Page page = context.create().page("/content/airline/au/en/flying");
        context.currentPage(page);
        path = page.getContentResource().getPath() + "/root/container/hero/links/jumplinks";
    }

    private JumpLinks jumpLinks(Object... properties) {
        Object[] all = new Object[properties.length + 2];
        all[0] = "sling:resourceType";
        all[1] = JumpLinksImpl.RESOURCE_TYPE;
        System.arraycopy(properties, 0, all, 2, properties.length);
        context.create().resource(path, all);
        context.currentResource(path);
        return context.request().adaptTo(JumpLinks.class);
    }

    @Test
    void emptyWithoutLinksAndDefaultTitle() {
        JumpLinks links = jumpLinks("title", "  ");
        assertTrue(links.isEmpty());
        assertTrue(links.getLinks().isEmpty());
        assertEquals(JumpLinksImpl.DEFAULT_TITLE, links.getTitle());
        assertEquals(JumpLinksImpl.RESOURCE_TYPE, links.getExportedType());
    }

    @Test
    void linksAreNormalisedAndIncompleteOnesSkipped() {
        context.create().resource(path, "sling:resourceType", JumpLinksImpl.RESOURCE_TYPE, "title", " Jump to ");
        context.create().resource(path + "/links/item0", "label", "Booking", "anchor", "#Booking Infants!");
        context.create().resource(path + "/links/item1", "label", "No anchor");
        context.create().resource(path + "/links/item2", "label", "Bad", "anchor", "javascript:alert(1)");
        context.create().resource(path + "/links/item3", "anchor", "#no-label");
        context.create().resource(path + "/links/item4", "label", "Symbols only", "anchor", "#!!!");
        context.currentResource(path);
        JumpLinks links = context.request().adaptTo(JumpLinks.class);

        assertFalse(links.isEmpty());
        assertEquals("Jump to", links.getTitle());
        List<JumpLink> items = links.getLinks();
        assertEquals(2, items.size());
        assertEquals("Booking", items.get(0).getLabel());
        assertEquals("booking-infants", items.get(0).getAnchor());
        assertEquals("javascript-alert-1", items.get(1).getAnchor());
    }

    @Test
    void normaliseAnchor() {
        assertNull(JumpLinksImpl.normaliseAnchor(null));
        assertNull(JumpLinksImpl.normaliseAnchor(" # "));
        assertEquals("seat_1-a", JumpLinksImpl.normaliseAnchor("##Seat_1 A"));
    }

    @Test
    void idUsesAuthoredValueAndNoDataLayerByDefault() {
        JumpLinks links = jumpLinks("id", "on-this-page");
        assertEquals("on-this-page", links.getId());
        assertNull(links.getData());
    }

    @Test
    void dataLayerWhenEnabled() {
        AppAemContext.enableDataLayer(context);
        JumpLinks links = jumpLinks();
        assertNotNull(links.getData());
        assertEquals("On this page", links.getData().getTitle());
        assertEquals(links.getId(), links.getData().getId());
    }
}
