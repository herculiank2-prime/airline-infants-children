package com.airline.travel.core.models.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Teaser;
import com.airline.travel.core.testcontext.AppAemContext;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TeaserImplTest {

    private static final String CORE_TEASER = "core/wcm/components/teaser/v2/teaser";
    private static final String EXTERNAL = "https://club.airline.example/joey";

    private final AemContext context = AppAemContext.newAemContext();
    private Page page;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(TeaserImpl.class);
        context.create().resource("/apps/" + TeaserImpl.RESOURCE_TYPE, "sling:resourceSuperType", CORE_TEASER);
        context.create().resource("/apps/" + CORE_TEASER, "sling:resourceSuperType", "core/wcm/components/teaser/v1/teaser");
        page = context.create().page("/content/airline/au/en/flying");
        context.currentPage(page);
    }

    private Teaser teaser(String resourceType, String name, Object... properties) {
        String path = page.getContentResource().getPath() + "/root/container/content/" + name;
        Object[] all = new Object[properties.length + 2];
        all[0] = "sling:resourceType";
        all[1] = resourceType;
        System.arraycopy(properties, 0, all, 2, properties.length);
        context.create().resource(path, all);
        context.currentResource(path);
        return adapt();
    }

    private Teaser adapt() {
        return context.getService(org.apache.sling.models.factory.ModelFactory.class)
                .createModel(context.request(), Teaser.class);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void newTabLinkGetsRelOnceAndOtherMethodsDelegate() {
        Teaser teaser = teaser(TeaserImpl.RESOURCE_TYPE, "joey", "jcr:title", "Joey Club",
                "jcr:description", "<p>Activities for young flyers.</p>", "linkURL", EXTERNAL, "linkTarget", "_blank",
                "titleType", "h3", "id", "joey-club", "titleFromPage", false, "descriptionFromPage", false);

        assertTrue(teaser instanceof TeaserImpl);
        Link link = teaser.getLink();
        assertTrue(link instanceof RelSafeLink);
        assertTrue(link.isValid());
        assertEquals(EXTERNAL, link.getURL());
        Map attributes = link.getHtmlAttributes();
        assertEquals("_blank", attributes.get("target"));
        assertEquals("noopener noreferrer", attributes.get("rel"));
        assertEquals(EXTERNAL, link.getMappedURL());
        assertNotNull(link.getExternalizedURL());
        assertNull(link.getReference());

        assertEquals("Joey Club", teaser.getTitle());
        assertEquals("<p>Activities for young flyers.</p>", teaser.getDescription());
        assertEquals("joey-club", teaser.getId());
        assertEquals(TeaserImpl.RESOURCE_TYPE, teaser.getExportedType());
        assertTrue(teaser.isActionsEnabled(), "Core v2 enables actions unless the policy disables them");
        assertTrue(teaser.getActions().isEmpty());
        assertNull(teaser.getPretitle());
        assertNull(teaser.getImageResource());
        assertFalse(teaser.isImageLinkHidden());
        assertFalse(teaser.isTitleLinkHidden());
        assertNull(teaser.getTitleType(), "authored titleType is ignored unless the policy allows it");
        assertNull(teaser.getData());
        assertNull(teaser.getAppliedCssClasses());
        assertEquals(EXTERNAL, teaser.getLinkURL());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void sameTabLinkIsUnchanged() {
        Teaser teaser = teaser(TeaserImpl.RESOURCE_TYPE, "same", "jcr:title", "Joey Club", "linkURL", EXTERNAL);
        Map attributes = teaser.getLink().getHtmlAttributes();
        assertFalse(attributes.containsKey("rel"));
        assertFalse(attributes.containsKey("target"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void actionsAreWrapped() {
        Teaser teaser = teaser(TeaserImpl.RESOURCE_TYPE, "actions", "jcr:title", "Joey Club", "actionsEnabled", true);
        String actions = context.currentResource().getPath() + "/actions";
        context.create().resource(actions + "/item0", "link", EXTERNAL, "text", "Join the Joey Club", "linkTarget", "_blank");
        teaser = context.request().adaptTo(Teaser.class);

        assertTrue(teaser.isActionsEnabled());
        List<ListItem> items = teaser.getActions();
        assertEquals(1, items.size());
        ListItem action = items.get(0);
        assertTrue(action instanceof RelSafeListItem);
        assertEquals("Join the Joey Club", action.getTitle());
        Link link = action.getLink();
        assertTrue(link instanceof RelSafeLink);
        assertEquals(EXTERNAL, link.getURL());
        assertNotNull(action.getId());
        assertNull(action.getData());
        assertEquals(EXTERNAL, action.getURL());
        action.getDescription();
        action.getLastModified();
        action.getPath();
        action.getName();
        action.getTeaserResource();
        action.getAppliedCssClasses();
        action.getExportedType();
    }

    @Test
    void dataLayerReportsProjectResourceType() {
        AppAemContext.enableDataLayer(context);
        Teaser teaser = teaser(TeaserImpl.RESOURCE_TYPE, "tracked", "jcr:title", "Joey Club", "titleFromPage", false);
        assertNotNull(teaser.getData());
        assertEquals(TeaserImpl.RESOURCE_TYPE, teaser.getData().getType());
        assertEquals("Joey Club", teaser.getData().getTitle());
        assertEquals(teaser.getId(), teaser.getData().getId());
    }

    /**
     * R-05 guard: Core 2.28 Link emits no {@code rel}. If a Core upgrade starts doing so, this fails so
     * the decorator can be reviewed or removed instead of silently merging.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void coreLinkStillEmitsNoRel() {
        context.create().resource("/apps/core/wcm/components/teaser/v1/teaser");
        Teaser core = teaser(CORE_TEASER, "core", "jcr:title", "Joey Club", "linkURL", EXTERNAL, "linkTarget", "_blank");
        assertFalse(core instanceof TeaserImpl);
        Map attributes = core.getLink().getHtmlAttributes();
        assertEquals("_blank", attributes.get("target"));
        assertFalse(attributes.containsKey("rel"), "Core now emits rel; review RelSafeLink (R-05)");
    }
}
