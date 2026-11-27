package com.airline.travel.core.models.impl;

import static com.airline.travel.core.testcontext.Fragments.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.models.FaqStructuredData;
import com.airline.travel.core.services.impl.TravelQuestionServiceImpl;
import com.airline.travel.core.testcontext.AppAemContext;
import com.airline.travel.core.testcontext.Fragments;
import com.day.cq.wcm.api.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class FaqStructuredDataImplTest {

    private final AemContext context = AppAemContext.newAemContext();
    private Page page;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(FaqStructuredDataImpl.class, TravelQuestionsAccordionImpl.class);
        Fragments.sixInfantQuestions(context);
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        page = context.create().page("/content/airline/au/en/flying");
        context.currentPage(page);
        context.currentResource(page.getContentResource());
    }

    private void accordion(String name, Object... properties) {
        Object[] all = new Object[properties.length + 2];
        all[0] = "sling:resourceType";
        all[1] = TravelQuestionsAccordionImpl.RESOURCE_TYPE;
        System.arraycopy(properties, 0, all, 2, properties.length);
        context.create().resource(page.getContentResource().getPath() + "/root/content/" + name, all);
    }

    @Test
    void oneFaqPageForAllAccordionsWithoutDuplicates() {
        context.create().resource(page.getContentResource().getPath() + "/root/content/text",
                "sling:resourceType", "airline/components/text");
        accordion("first", "folderPath", ROOT + "/infants", "maxItems", 2L);
        accordion("second", "source", "tags", "tags", new String[] {"airline:travel/infants"});
        FaqStructuredData faq = context.request().adaptTo(FaqStructuredData.class);
        assertFalse(faq.isEmpty());
        JsonArray entities = JsonParser.parseString(faq.getJson()).getAsJsonObject().getAsJsonArray("mainEntity");
        assertEquals(6, entities.size());
        assertEquals("Infants on domestic flights", entities.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    void jsonLdMatchesTheAccordionForTheSameSelection() {
        accordion("only", "source", "tags", "tagMatch", "all",
                "tags", new String[] {"airline:travel/infants", "airline:market/au"});
        FaqStructuredData faq = context.request().adaptTo(FaqStructuredData.class);
        context.currentResource(page.getContentResource().getPath() + "/root/content/only");
        com.airline.travel.core.models.TravelQuestionsAccordion accordion = context.request()
                .adaptTo(com.airline.travel.core.models.TravelQuestionsAccordion.class);
        JsonArray entities = JsonParser.parseString(faq.getJson()).getAsJsonObject().getAsJsonArray("mainEntity");
        for (int i = 0; i < entities.size(); i++) {
            assertEquals(accordion.getItems().get(i).getQuestion(),
                    entities.get(i).getAsJsonObject().get("name").getAsString());
            assertEquals(accordion.getItems().get(i).getAnswerHtml(), entities.get(i).getAsJsonObject()
                    .getAsJsonObject("acceptedAnswer").get("text").getAsString());
        }
    }

    @Test
    void pageWithoutAccordionsHasNoStructuredData() {
        FaqStructuredData faq = context.request().adaptTo(FaqStructuredData.class);
        assertTrue(faq.isEmpty());
        assertNull(faq.getJson());
    }
}
