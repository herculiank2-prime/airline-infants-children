package com.airline.travel.core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ValueMap;

/**
 * What an accordion asks for: a folder (all descendants) or exact tag IDs under a search root.
 * Built from the component's properties by one factory, so the accordion and the FAQ structured data
 * always make the same request (ADR-014).
 */
public final class QuestionSelection {

    public enum Mode { FOLDER, TAGS }

    public static final String PN_SOURCE = "source";
    public static final String PN_FOLDER_PATH = "folderPath";
    public static final String PN_ROOT_PATH = "rootPath";
    public static final String PN_TAGS = "tags";
    public static final String PN_TAG_MATCH = "tagMatch";
    public static final String PN_MAX_ITEMS = "maxItems";

    private final Mode mode;
    private final String folderPath;
    private final String rootPath;
    private final List<String> tagIds;
    private final boolean matchAll;
    private final int limit;

    public QuestionSelection(Mode mode, String folderPath, String rootPath, List<String> tagIds, boolean matchAll,
            int limit) {
        this.mode = mode;
        this.folderPath = StringUtils.trimToNull(folderPath);
        this.rootPath = StringUtils.trimToNull(rootPath);
        this.tagIds = tagIds == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tagIds));
        this.matchAll = matchAll;
        this.limit = Math.max(0, limit);
    }

    /**
     * @param properties the travel questions component's properties
     * @return the selection, or {@code null} when the author has not configured a source yet
     */
    public static QuestionSelection fromValueMap(ValueMap properties) {
        int limit = properties.get(PN_MAX_ITEMS, 0);
        if ("tags".equals(properties.get(PN_SOURCE, String.class))) {
            String[] tags = properties.get(PN_TAGS, new String[0]);
            List<String> tagIds = new ArrayList<>();
            Arrays.stream(tags).map(StringUtils::trimToNull).filter(Objects::nonNull).distinct().forEach(tagIds::add);
            if (tagIds.isEmpty()) {
                return null;
            }
            return new QuestionSelection(Mode.TAGS, null, properties.get(PN_ROOT_PATH, String.class), tagIds,
                    "all".equals(properties.get(PN_TAG_MATCH, "any")), limit);
        }
        String folder = properties.get(PN_FOLDER_PATH, String.class);
        if (StringUtils.isBlank(folder)) {
            return null;
        }
        return new QuestionSelection(Mode.FOLDER, folder, null, Collections.emptyList(), false, limit);
    }

    public Mode getMode() {
        return mode;
    }

    public String getFolderPath() {
        return folderPath;
    }

    /** Search root for tag mode; {@code null} means the service's allowed root. */
    public String getRootPath() {
        return rootPath;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    /** Requested maximum; 0 means the service maximum. */
    public int getLimit() {
        return limit;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuestionSelection)) {
            return false;
        }
        QuestionSelection that = (QuestionSelection) other;
        return matchAll == that.matchAll && limit == that.limit && mode == that.mode
                && Objects.equals(folderPath, that.folderPath) && Objects.equals(rootPath, that.rootPath)
                && tagIds.equals(that.tagIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, folderPath, rootPath, tagIds, matchAll, limit);
    }

    @Override
    public String toString() {
        return mode == Mode.FOLDER
                ? "folder " + folderPath + " limit " + limit
                : "tags " + tagIds + (matchAll ? " (all)" : " (any)") + " under " + rootPath + " limit " + limit;
    }
}
