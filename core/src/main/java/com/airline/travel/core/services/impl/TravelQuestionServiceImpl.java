package com.airline.travel.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airline.travel.core.services.QuestionSelection;
import com.airline.travel.core.services.SelectionResult;
import com.airline.travel.core.services.TravelQuestion;
import com.airline.travel.core.services.TravelQuestionService;

/**
 * Walks the DAM below an allowed root instead of running a JCR query: no index dependency and a
 * predictable cost, capped at {@code max.fragments.scanned} assets (design section 4).
 */
@Component(service = TravelQuestionService.class)
@Designate(ocd = TravelQuestionServiceImpl.Config.class)
public class TravelQuestionServiceImpl implements TravelQuestionService {

    @ObjectClassDefinition(name = "Airline - Travel question service",
            description = "Selects travel-question Content Fragments for accordions and FAQ structured data.")
    public @interface Config {

        @AttributeDefinition(name = "Allowed root path", description = "Folder and tag searches must stay below this path.")
        String allowed_root_path() default "/content/dam/airline/travel-questions";

        @AttributeDefinition(name = "Model path", description = "Only fragments of this Content Fragment model are returned.")
        String model_path() default "/conf/airline/settings/dam/cfm/models/travel-question";

        @AttributeDefinition(name = "Maximum items", description = "Upper bound for any accordion, whatever the dialog says.")
        int max_items() default 20;

        @AttributeDefinition(name = "Maximum fragments scanned", description = "The walk stops after this many assets and reports truncation.")
        int max_fragments_scanned() default 100;
    }

    static final String NN_DATA = "jcr:content/data";
    static final String NN_MASTER = "master";
    static final String PN_MODEL = "cq:model";
    static final String PN_QUESTION = "question";
    static final String PN_ANSWER = "answer";
    static final String PN_TAGS = "tags";
    static final String PN_SORT_ORDER = "sortOrder";

    private static final Logger LOG = LoggerFactory.getLogger(TravelQuestionServiceImpl.class);
    private static final Set<String> FOLDER_TYPES = Set.of("sling:Folder", "sling:OrderedFolder", "nt:folder");
    private static final String DAM_ASSET = "dam:Asset";

    /** Same order as the persisted queries' {@code sort: "sortOrder ASC, question ASC"}. */
    static final Comparator<TravelQuestion> ORDER = Comparator.comparingInt(TravelQuestion::getSortOrder)
            .thenComparing(TravelQuestion::getQuestion);

    private String allowedRoot;
    private String modelPath;
    private int maxItems;
    private int maxScanned;

    @Activate
    @Modified
    protected void activate(Config config) {
        allowedRoot = StringUtils.removeEnd(config.allowed_root_path(), "/");
        modelPath = config.model_path();
        maxItems = Math.max(1, config.max_items());
        maxScanned = Math.max(1, config.max_fragments_scanned());
    }

    @Override
    public SelectionResult find(ResourceResolver resolver, QuestionSelection selection) {
        if (selection == null) {
            return SelectionResult.EMPTY;
        }
        String start = selection.getMode() == QuestionSelection.Mode.FOLDER
                ? selection.getFolderPath()
                : StringUtils.defaultIfBlank(selection.getRootPath(), allowedRoot);
        String normalised = start == null ? null : ResourceUtil.normalize(StringUtils.removeEnd(start, "/"));
        if (!isAllowed(normalised)) {
            LOG.warn("Ignoring travel question selection outside {}: {}", allowedRoot, selection);
            return SelectionResult.EMPTY;
        }
        Resource root = resolver.getResource(normalised);
        if (root == null) {
            return SelectionResult.EMPTY;
        }
        Walk walk = new Walk(selection);
        walk.visitChildren(root);
        if (walk.truncated) {
            LOG.warn("Travel question scan stopped after {} assets under {}; results may be incomplete", maxScanned,
                    normalised);
        }
        List<TravelQuestion> found = walk.found;
        found.sort(ORDER);
        int limit = selection.getLimit() > 0 ? Math.min(selection.getLimit(), maxItems) : maxItems;
        return new SelectionResult(found.subList(0, Math.min(limit, found.size())), walk.truncated);
    }

    private boolean isAllowed(String path) {
        return path != null && (path.equals(allowedRoot) || path.startsWith(allowedRoot + "/"));
    }

    private final class Walk {
        private final QuestionSelection selection;
        private final List<TravelQuestion> found = new ArrayList<>();
        private int scanned;
        private boolean truncated;

        Walk(QuestionSelection selection) {
            this.selection = selection;
        }

        void visitChildren(Resource parent) {
            Iterator<Resource> children = parent.listChildren();
            while (children.hasNext() && !truncated) {
                Resource child = children.next();
                String type = child.getValueMap().get("jcr:primaryType", String.class);
                if (DAM_ASSET.equals(type)) {
                    if (scanned == maxScanned) {
                        truncated = true;
                        return;
                    }
                    scanned++;
                    TravelQuestion question = read(child);
                    if (question != null && matches(question)) {
                        found.add(question);
                    }
                } else if (FOLDER_TYPES.contains(type)) {
                    visitChildren(child);
                }
            }
        }

        private boolean matches(TravelQuestion question) {
            if (selection.getMode() == QuestionSelection.Mode.FOLDER) {
                return true;
            }
            List<String> wanted = selection.getTagIds();
            return selection.isMatchAll()
                    ? question.getTags().containsAll(wanted)
                    : wanted.stream().anyMatch(question.getTags()::contains);
        }
    }

    /**
     * Reads the master variation of a structured fragment, or {@code null} for other assets, other models
     * and fragments missing a required field.
     */
    TravelQuestion read(Resource asset) {
        Resource data = asset.getChild(NN_DATA);
        if (data == null || !modelPath.equals(data.getValueMap().get(PN_MODEL, String.class))) {
            return null;
        }
        Resource master = data.getChild(NN_MASTER);
        if (master == null) {
            return null;
        }
        ValueMap values = master.getValueMap();
        String question = StringUtils.trimToNull(values.get(PN_QUESTION, String.class));
        String answer = StringUtils.trimToNull(values.get(PN_ANSWER, String.class));
        if (question == null || answer == null) {
            return null;
        }
        Long sortOrder = values.get(PN_SORT_ORDER, Long.class);
        int order = sortOrder == null ? Integer.MAX_VALUE
                : (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, sortOrder));
        return new TravelQuestion(asset.getPath(), question, answer,
                Arrays.asList(values.get(PN_TAGS, new String[0])), order);
    }
}
