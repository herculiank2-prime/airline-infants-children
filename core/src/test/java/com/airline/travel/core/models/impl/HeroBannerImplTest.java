package com.airline.travel.core.models.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.models.HeroBanner;
import com.airline.travel.core.models.HeroImage;
import com.airline.travel.core.testcontext.AppAemContext;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class HeroBannerImplTest {

    private static final String ASSET = "/content/dam/airline/travel-information/infants-children/family-hero.jpg";
    private static final String DESCRIPTION = "A parent holding a baby beside an aircraft window";

    private final AemContext context = AppAemContext.newAemContext();
    private Page page;
    private int counter;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(HeroBannerImpl.class);
        page = context.create().page("/content/airline/au/en/flying");
        context.currentPage(page);
    }

    private Asset asset(String description, String... renditionNames) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tiff:ImageWidth", "1600");
        metadata.put("tiff:ImageLength", "900");
        if (description != null) {
            metadata.put("dc:description", description);
        }
        Asset asset = context.create().asset(ASSET, 1600, 900, "image/jpeg", metadata);
        for (String name : renditionNames) {
            context.create().assetRendition(asset, name, 10, 10, "image/jpeg");
        }
        return asset;
    }

    private HeroBanner hero(Object... properties) {
        Object[] all = new Object[properties.length + 2];
        all[0] = "sling:resourceType";
        all[1] = HeroBannerImpl.RESOURCE_TYPE;
        System.arraycopy(properties, 0, all, 2, properties.length);
        String path = page.getContentResource().getPath() + "/root/container/hero" + (counter++);
        context.create().resource(path, all);
        context.currentResource(path);
        return context.request().adaptTo(HeroBanner.class);
    }

    @Test
    void emptyWhenTitleIsBlank() {
        HeroBanner hero = hero("jcr:title", "   ");
        assertTrue(hero.isEmpty());
        assertNull(hero.getTitle());
        assertNull(hero.getImage());
        assertFalse(hero.isImageMissingAlt());
    }

    @Test
    void textOnlyHero() {
        HeroBanner hero = hero("jcr:title", " Flying with infants and children ", "introduction", "Plan ahead.");
        assertFalse(hero.isEmpty());
        assertEquals("Flying with infants and children", hero.getTitle());
        assertEquals("Plan ahead.", hero.getIntroduction());
        assertNull(hero.getImage());
        assertEquals(HeroBannerImpl.RESOURCE_TYPE, hero.getExportedType());
    }

    @Test
    void imageUsesWebRenditionsOnlyAndDamDescription() {
        asset(DESCRIPTION, "cq5dam.web.1280.720.jpeg", "cq5dam.web.640.360.jpeg", "cq5dam.web.960.540.jpeg",
                "cq5dam.thumbnail.319.319.png", "cq5dam.web.480.270.jpeg");
        HeroImage image = hero("jcr:title", "Title", "fileReference", ASSET).getImage();

        assertNotNull(image);
        assertEquals(DESCRIPTION, image.getAlt());
        assertFalse(image.isDecorative());
        String renditions = ASSET + "/_jcr_content/renditions/";
        assertEquals(renditions + "cq5dam.web.960.540.jpeg", image.getSrc());
        assertEquals(renditions + "cq5dam.web.640.360.jpeg 640w, "
                + renditions + "cq5dam.web.960.540.jpeg 960w, "
                + renditions + "cq5dam.web.1280.720.jpeg 1280w", image.getSrcset());
        assertFalse(image.getSrcset().contains("original"));
        assertEquals(960, image.getWidth());
        assertEquals(540, image.getHeight());
        assertEquals(HeroBannerImpl.SIZES, image.getSizes());
    }

    @Test
    void authoredAltWinsOverDam() {
        asset(DESCRIPTION, "cq5dam.web.960.540.jpeg");
        HeroImage image = hero("jcr:title", "Title", "fileReference", ASSET, "alt", " Authored alt ").getImage();
        assertEquals("Authored alt", image.getAlt());
    }

    @Test
    void missingAltHidesImageAndFlagsAuthor() {
        asset(null, "cq5dam.web.960.540.jpeg");
        HeroBanner hero = hero("jcr:title", "Title", "fileReference", ASSET);
        assertNull(hero.getImage());
        assertTrue(hero.isImageMissingAlt());
    }

    @Test
    void decorativeImageHasEmptyAlt() {
        asset(null, "cq5dam.web.960.540.jpeg");
        HeroBanner hero = hero("jcr:title", "Title", "fileReference", ASSET, "isDecorative", true, "alt", "ignored");
        assertFalse(hero.isImageMissingAlt());
        assertEquals("", hero.getImage().getAlt());
        assertTrue(hero.getImage().isDecorative());
    }

    @Test
    void noImageWithoutWebRenditionsOrAsset() {
        asset(DESCRIPTION, "cq5dam.thumbnail.140.100.png");
        assertNull(hero("jcr:title", "Title", "fileReference", ASSET).getImage());
        assertNull(hero("jcr:title", "Title", "fileReference", "/content/dam/airline/missing.jpg").getImage());
    }

    @Test
    void srcFallsBackToClosestWidthAndNameHeight() throws PersistenceException {
        Asset asset = asset(DESCRIPTION, "cq5dam.web.1280.720.jpeg");
        ModifiableValueMap metadata = context.resourceResolver()
                .getResource(asset.getPath() + "/jcr:content/metadata").adaptTo(ModifiableValueMap.class);
        metadata.remove("tiff:ImageWidth");
        metadata.remove("tiff:ImageLength");
        context.resourceResolver().commit();

        HeroImage image = hero("jcr:title", "Title", "fileReference", ASSET).getImage();
        assertEquals(1280, image.getWidth());
        assertEquals(720, image.getHeight());
    }

    @Test
    void idUsesAuthoredValueOrGeneratedPrefix() {
        assertEquals("hero-custom", hero("jcr:title", "Title", "id", "hero-custom").getId());
        assertTrue(hero("jcr:title", "Title").getId().startsWith("hero-"));
    }

    @Test
    void dataLayerOnlyWhenEnabled() {
        assertNull(hero("jcr:title", "Title").getData());

        AppAemContext.enableDataLayer(context);

        HeroBanner hero = hero("jcr:title", "Title");
        assertNotNull(hero.getData());
        assertEquals(HeroBannerImpl.RESOURCE_TYPE, hero.getData().getType());
        assertEquals("Title", hero.getData().getTitle());
        assertEquals(hero.getId(), hero.getData().getId());
    }
}
