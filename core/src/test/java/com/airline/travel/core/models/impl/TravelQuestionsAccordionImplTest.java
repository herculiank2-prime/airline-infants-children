package com.airline.travel.core.models.impl;

import static com.airline.travel.core.testcontext.Fragments.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.airline.travel.core.models.AccordionItem;
import com.airline.travel.core.models.TravelQuestionsAccordion;
import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.SelectionResult;
import com.airline.travel.core.services.TravelQuestionService;
import com.airline.travel.core.services.impl.TravelQuestionServiceImpl;
import com.airline.travel.core.testcontext.AppAemContext;
import com.airline.travel.core.testcontext.Fragments;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TravelQuestionsAccordionImplTest {

    private final AemContext context = AppAemContext.newAemContext();
    private Page page;
    private int counter;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(TravelQuestionsAccordionImpl.class);
        Fragments.sixInfantQuestions(context);
        page = context.create().page("/content/airline/au/en/flying");
        context.currentPage(page);
    }

    private String create(Object... properties) {
        Object[] all = new Object[properties.length + 2];
        all[0] = "sling:resourceType";
        all[1] = TravelQuestionsAccordionImpl.RESOURCE_TYPE;
        System.arraycopy(properties, 0, all, 2, properties.length);
        String path = page.getContentResource().getPath() + "/root/content/travelquestions" + (counter++);
        context.create().resource(path, all);
        return path;
    }

    private TravelQuestionsAccordion accordion(Object... properties) {
        context.currentResource(create(properties));
        return context.request().adaptTo(TravelQuestionsAccordion.class);
    }

    @Test
    void tagModeAllRendersSixItemsInOrder() {
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion accordion = accordion("heading", " Booking for infants (under 2 years) ",
                "id", "booking-infants", "source", "tags", "tagMatch", "all",
                "tags", new String[] {"airline:travel/infants", "airline:market/au"});
        assertEquals("Booking for infants (under 2 years)", accordion.getHeading());
        assertEquals("booking-infants", accordion.getId());
        assertFalse(accordion.isEmpty());
        assertFalse(accordion.isTruncated());
        assertFalse(accordion.isExpandFirst());
        List<String> questions = accordion.getItems().stream().map(AccordionItem::getQuestion)
                .collect(Collectors.toList());
        assertEquals(List.of("Infants on domestic flights", "Infants on international flights",
                "Booking a seat for your infant", "Travelling with more than one infant per adult",
                "How soon can infants travel after birth?", "My infant isn't born yet, how do I book?"), questions);
        AccordionItem first = accordion.getItems().get(0);
        assertEquals("<p>Free on your lap.</p>", first.getAnswerHtml());
        assertFalse(first.isExpanded());
        assertTrue(first.getId().startsWith(AccordionItemImpl.ID_PREFIX + "-"));
        assertNull(first.getData(), "data layer is off unless configured");
        assertNull(accordion.getData());
        assertEquals(TravelQuestionsAccordionImpl.RESOURCE_TYPE, accordion.getExportedType());
    }

    @Test
    void itemIdsDependOnTheFragmentPathNotThePosition() {
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion all = accordion("folderPath", ROOT + "/infants");
        TravelQuestionsAccordion two = accordion("folderPath", ROOT + "/infants", "maxItems", 2L);
        assertEquals(all.getItems().get(1).getId(), two.getItems().get(1).getId());
        assertNotEquals(all.getItems().get(0).getId(), all.getItems().get(1).getId());
        assertEquals(2, two.getItems().size());
    }

    @Test
    void expandFirstOpensOnlyTheFirstItem() {
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion accordion = accordion("folderPath", ROOT + "/infants", "expandFirst", true);
        assertTrue(accordion.isExpandFirst());
        assertTrue(accordion.getItems().get(0).isExpanded());
        assertFalse(accordion.getItems().get(1).isExpanded());
    }

    @Test
    void headingLevelsComeFromThePolicyAndAreValidated() {
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion defaults = accordion("folderPath", ROOT + "/infants");
        assertEquals("h2", defaults.getHeadingElement());
        assertEquals("h3", defaults.getItemHeadingElement());

        context.contentPolicyMapping(TravelQuestionsAccordionImpl.RESOURCE_TYPE,
                "headingElement", "h3", "itemHeadingElement", "h1");
        TravelQuestionsAccordion styled = accordion("folderPath", ROOT + "/infants");
        assertEquals("h3", styled.getHeadingElement());
        assertEquals("h3", styled.getItemHeadingElement(), "h1 is not allowed, so the default applies");
    }

    @Test
    void dataLayerCarriesComponentAndItemData() {
        AppAemContext.enableDataLayer(context);
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion accordion = accordion("heading", "Booking", "id", "booking-infants",
                "folderPath", ROOT + "/infants");
        ComponentData data = accordion.getData();
        assertNotNull(data);
        assertEquals("Booking", data.getTitle());
        ComponentData item = accordion.getItems().get(0).getData();
        assertEquals(AccordionItemImpl.ITEM_TYPE, item.getType());
        assertEquals("Infants on domestic flights", item.getTitle());
        assertEquals("booking-infants", item.getParentId());
        assertEquals(accordion.getItems().get(0).getId(), item.getId());
    }

    @Test
    void unconfiguredOrMissingServiceIsEmpty() {
        TravelQuestionsAccordion noService = accordion("folderPath", ROOT + "/infants");
        assertTrue(noService.isEmpty());
        context.registerInjectActivateService(new TravelQuestionServiceImpl());
        TravelQuestionsAccordion unconfigured = accordion("heading", "Nothing yet");
        assertTrue(unconfigured.isEmpty());
        assertTrue(unconfigured.getItems().isEmpty());
    }

    @Test
    void selectionResultIsReusedWithinTheRequest() {
        TravelQuestionService service = Mockito.mock(TravelQuestionService.class);
        Mockito.when(service.find(Mockito.any(), Mockito.any())).thenReturn(SelectionResult.EMPTY);
        context.registerService(TravelQuestionService.class, service);
        String path = create("folderPath", ROOT + "/infants");
        context.currentResource(path);
        context.request().adaptTo(TravelQuestionsAccordion.class);
        context.request().adaptTo(TravelQuestionsAccordion.class);
        Mockito.verify(service, Mockito.times(1)).find(Mockito.any(), Mockito.any(QuestionSelection.class));
        assertSame(SelectionResult.EMPTY,
                SelectionCache.find(context.request(), service, null));
    }
}
