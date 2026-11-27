package com.airline.travel.core.services.impl;

import static com.airline.travel.core.testcontext.Fragments.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.QuestionSelection.Mode;
import com.airline.travel.core.services.SelectionResult;
import com.airline.travel.core.services.TravelQuestion;
import com.airline.travel.core.testcontext.AppAemContext;
import com.airline.travel.core.testcontext.Fragments;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class TravelQuestionServiceImplTest {

    private static final String INFANTS = "airline:travel/infants";
    private static final String AU = "airline:market/au";

    private final AemContext context = AppAemContext.newAemContext();
    private TravelQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        Fragments.sixInfantQuestions(context);
        service = context.registerInjectActivateService(new TravelQuestionServiceImpl());
    }

    private SelectionResult find(QuestionSelection selection) {
        return service.find(context.resourceResolver(), selection);
    }

    private static QuestionSelection folder(String path, int limit) {
        return new QuestionSelection(Mode.FOLDER, path, null, Collections.emptyList(), false, limit);
    }

    private static QuestionSelection tags(boolean all, String... ids) {
        return new QuestionSelection(Mode.TAGS, null, null, List.of(ids), all, 0);
    }

    private static List<String> questions(SelectionResult result) {
        return result.getQuestions().stream().map(TravelQuestion::getQuestion).collect(Collectors.toList());
    }

    @Test
    void folderModeReturnsDescendantsInSortOrder() {
        SelectionResult result = find(folder(ROOT + "/infants", 0));
        assertEquals(List.of("Infants on domestic flights", "Infants on international flights",
                "Booking a seat for your infant", "Travelling with more than one infant per adult",
                "How soon can infants travel after birth?", "My infant isn't born yet, how do I book?"),
                questions(result));
        assertFalse(result.isTruncated());
        TravelQuestion first = result.getQuestions().get(0);
        assertEquals(ROOT + "/infants/infants-domestic-flights", first.getPath());
        assertEquals("<p>Free on your lap.</p>", first.getAnswerHtml());
        assertEquals(10, first.getSortOrder());
        assertEquals(List.of(INFANTS, AU, "airline:market/nz"), first.getTags());
    }

    @Test
    void folderModeDescendsIntoSubfoldersAndAcceptsTrailingSlash() {
        Fragments.folder(context, ROOT + "/infants/extra");
        Fragments.fragment(context, ROOT + "/infants/extra/nested", "Nested", "<p>N</p>", 5L);
        assertEquals("Nested", questions(find(folder(ROOT + "/infants/", 0))).get(0));
    }

    @Test
    void equalSortOrderFallsBackToQuestion() {
        Fragments.folder(context, ROOT + "/ties");
        Fragments.fragment(context, ROOT + "/ties/b", "beta", "<p>b</p>", 1L);
        Fragments.fragment(context, ROOT + "/ties/a", "Alpha", "<p>a</p>", 1L);
        Fragments.fragment(context, ROOT + "/ties/z", "Zulu without order", "<p>z</p>", null);
        assertEquals(List.of("Alpha", "beta", "Zulu without order"), questions(find(folder(ROOT + "/ties", 0))));
    }

    @Test
    void tagModeAnyAndAllUseExactIds() {
        Fragments.folder(context, ROOT + "/children");
        Fragments.fragment(context, ROOT + "/children/kids", "Kids", "<p>k</p>", 1L, "airline:travel/children", AU);
        assertEquals(7, find(tags(false, INFANTS, "airline:travel/children")).getQuestions().size());
        assertEquals(1, find(tags(true, "airline:travel/children", AU)).getQuestions().size());
        assertEquals(6, find(tags(true, INFANTS, AU)).getQuestions().size());
        assertTrue(find(tags(false, "airline:travel")).getQuestions().isEmpty(), "parent tag must not match");
    }

    @Test
    void limitIsCappedByDialogAndConfiguration() {
        assertEquals(2, find(folder(ROOT, 2)).getQuestions().size());
        TravelQuestionServiceImpl small = context.registerInjectActivateService(new TravelQuestionServiceImpl(),
                Map.of("max.items", 3));
        assertEquals(3, small.find(context.resourceResolver(), folder(ROOT, 10)).getQuestions().size());
    }

    @Test
    void pathsOutsideTheAllowedRootAreIgnored() {
        context.create().resource("/content/dam/other", "jcr:primaryType", "sling:Folder");
        assertSame(SelectionResult.EMPTY, find(folder("/content/dam/other", 0)));
        assertSame(SelectionResult.EMPTY, find(folder(ROOT + "/../other", 0)));
        assertSame(SelectionResult.EMPTY, find(folder(ROOT + "-evil", 0)));
        assertSame(SelectionResult.EMPTY, find(new QuestionSelection(Mode.TAGS, null, "/content", List.of(AU), false, 0)));
        assertSame(SelectionResult.EMPTY, find(folder(ROOT + "/missing", 0)));
        assertSame(SelectionResult.EMPTY, find(null));
    }

    @Test
    void otherModelsAndIncompleteFragmentsAreSkipped() {
        Fragments.folder(context, ROOT + "/mixed");
        Fragments.fragment(context, ROOT + "/mixed/other-model", "/conf/other/model", "Other", "<p>o</p>", 1L);
        Fragments.fragment(context, ROOT + "/mixed/no-answer", "No answer", " ", 1L);
        context.create().resource(ROOT + "/mixed/image.jpg", "jcr:primaryType", "dam:Asset");
        context.create().resource(ROOT + "/mixed/no-master", "jcr:primaryType", "dam:Asset");
        context.create().resource(ROOT + "/mixed/no-master/jcr:content/data", "cq:model", Fragments.MODEL);
        context.create().resource(ROOT + "/mixed/notes", "jcr:primaryType", "nt:unstructured");
        assertTrue(find(folder(ROOT + "/mixed", 0)).getQuestions().isEmpty());
    }

    @Test
    void scanCapReportsTruncation() {
        TravelQuestionServiceImpl capped = context.registerInjectActivateService(new TravelQuestionServiceImpl(),
                Map.of("max.fragments.scanned", 4));
        SelectionResult result = capped.find(context.resourceResolver(), folder(ROOT, 0));
        assertTrue(result.isTruncated());
        assertEquals(4, result.getQuestions().size());

        TravelQuestionServiceImpl exact = context.registerInjectActivateService(new TravelQuestionServiceImpl(),
                Map.of("max.fragments.scanned", 6));
        assertFalse(exact.find(context.resourceResolver(), folder(ROOT, 0)).isTruncated());
    }
}
