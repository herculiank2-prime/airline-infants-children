package com.airline.travel.core.testcontext;

import io.wcm.testing.mock.aem.junit5.AemContext;

/**
 * Creates travel-question fragments in the same JCR shape the sample package installs.
 */
public final class Fragments {

    public static final String ROOT = "/content/dam/airline/travel-questions";
    public static final String MODEL = "/conf/airline/settings/dam/cfm/models/travel-question";

    private Fragments() {
    }

    public static void folder(AemContext context, String path) {
        context.create().resource(path, "jcr:primaryType", "sling:OrderedFolder");
    }

    public static void fragment(AemContext context, String path, String question, String answer, Long sortOrder,
            String... tags) {
        fragment(context, path, MODEL, question, answer, sortOrder, tags);
    }

    public static void fragment(AemContext context, String path, String model, String question, String answer,
            Long sortOrder, String... tags) {
        context.create().resource(path, "jcr:primaryType", "dam:Asset");
        context.create().resource(path + "/jcr:content", "jcr:primaryType", "dam:AssetContent",
                "contentFragment", true);
        context.create().resource(path + "/jcr:content/data", "jcr:primaryType", "nt:unstructured",
                "cq:model", model);
        Object[] master = sortOrder == null
                ? new Object[] {"question", question, "answer", answer, "tags", tags}
                : new Object[] {"question", question, "answer", answer, "tags", tags, "sortOrder", sortOrder};
        context.create().resource(path + "/jcr:content/data/master", master);
    }

    /** The six sample fragments, in a deliberately shuffled JCR order. */
    public static void sixInfantQuestions(AemContext context) {
        folder(context, ROOT);
        folder(context, ROOT + "/infants");
        String[] tags = {"airline:travel/infants", "airline:market/au", "airline:market/nz"};
        fragment(context, ROOT + "/infants/infant-not-born", "My infant isn't born yet, how do I book?",
                "<p>Add them later.</p>", 60L, tags);
        fragment(context, ROOT + "/infants/infant-seat", "Booking a seat for your infant", "<p>Child fare.</p>",
                30L, tags);
        fragment(context, ROOT + "/infants/infants-after-birth", "How soon can infants travel after birth?",
                "<p>Usually 7 days.</p>", 50L, tags);
        fragment(context, ROOT + "/infants/infants-domestic-flights", "Infants on domestic flights",
                "<p>Free on your lap.</p>", 10L, tags);
        fragment(context, ROOT + "/infants/infants-international-flights", "Infants on international flights",
                "<p>Reduced fare.</p>", 20L, tags);
        fragment(context, ROOT + "/infants/more-than-one-infant", "Travelling with more than one infant per adult",
                "<p>Book a seat.</p>", 40L, tags);
    }
}
