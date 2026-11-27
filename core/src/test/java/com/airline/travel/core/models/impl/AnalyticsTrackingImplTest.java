package com.airline.travel.core.models.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.models.AnalyticsTracking;
import com.airline.travel.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

/** DL-01, ADR-031. */
@ExtendWith(AemContextExtension.class)
class AnalyticsTrackingImplTest {

    private static final String PATH = "/content/airline/au/en/flying/jcr:content/root/button";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(AnalyticsTrackingImpl.class);
    }

    private AnalyticsTracking tracking(Object... properties) {
        context.create().resource(PATH, properties);
        context.currentResource(PATH);
        return context.request().adaptTo(AnalyticsTracking.class);
    }

    @Test
    void notConfiguredWithoutEvent() {
        AnalyticsTracking tracking = tracking("trackingLabel", "Ignored without an event");
        assertFalse(tracking.isConfigured());
        assertNull(tracking.getEvent());
        assertTrue(tracking.getAttributes().isEmpty());
    }

    @Test
    void eventAndLabelBecomeDataAttributes() {
        AnalyticsTracking tracking = tracking("trackingEvent", "airline:header:login", "trackingLabel", "  Header - Log in ");
        assertTrue(tracking.isConfigured());
        assertEquals("Header - Log in", tracking.getLabel());
        assertEquals(Map.of(AnalyticsTrackingImpl.ATTR_EVENT, "airline:header:login",
                AnalyticsTrackingImpl.ATTR_LABEL, "Header - Log in"), tracking.getAttributes());
    }

    @Test
    void labelIsOptional() {
        AnalyticsTracking tracking = tracking("trackingEvent", "cta");
        assertNull(tracking.getLabel());
        assertEquals(Map.of(AnalyticsTrackingImpl.ATTR_EVENT, "airline:cta"), tracking.getAttributes());
    }

    @Test
    void resourceAdaptationWorks() {
        context.create().resource(PATH, "trackingEvent", "x");
        AnalyticsTracking tracking = context.resourceResolver().getResource(PATH).adaptTo(AnalyticsTracking.class);
        assertEquals("airline:x", tracking.getEvent());
    }

    @Test
    void eventNamesAreNormalisedIntoTheAirlineNamespace() {
        assertEquals("airline:header-log-in", AnalyticsTrackingImpl.normaliseEvent(" Header Log in! "));
        assertEquals("airline:cmp:click", AnalyticsTrackingImpl.normaliseEvent("cmp:click"));
        assertEquals("airline:footer:back-to-top", AnalyticsTrackingImpl.normaliseEvent("AIRLINE:Footer:Back to top"));
        assertNull(AnalyticsTrackingImpl.normaliseEvent("airline:"));
        assertNull(AnalyticsTrackingImpl.normaliseEvent("  !!  "));
        assertNull(AnalyticsTrackingImpl.normaliseEvent(null));
        assertEquals(AnalyticsTrackingImpl.MAX_EVENT_LENGTH,
                AnalyticsTrackingImpl.normaliseEvent(StringUtils.repeat("a", 200)).length());
    }

    @Test
    void longLabelsAreTruncated() {
        AnalyticsTracking tracking = tracking("trackingEvent", "x", "trackingLabel", StringUtils.repeat("b", 300));
        assertEquals(AnalyticsTrackingImpl.MAX_LABEL_LENGTH, tracking.getLabel().length());
    }
}
