package com.airline.travel.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.airline.travel.core.models.JumpLink;
import com.airline.travel.core.models.JumpLinks;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = {JumpLinks.class, ComponentExporter.class},
        resourceType = JumpLinksImpl.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class JumpLinksImpl implements JumpLinks {

    static final String RESOURCE_TYPE = "airline/components/jumplinks";
    static final String DEFAULT_TITLE = "On this page";
    private static final String NN_LINKS = "links";

    @SlingObject
    private Resource resource;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Page currentPage;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ComponentContext componentContext;

    @ValueMapValue
    private String title;

    private List<JumpLink> links = Collections.emptyList();
    private String id;

    @PostConstruct
    protected void init() {
        title = StringUtils.defaultIfBlank(StringUtils.trim(title), DEFAULT_TITLE);
        links = readLinks(resource.getChild(NN_LINKS));
    }

    private static List<JumpLink> readLinks(Resource container) {
        if (container == null) {
            return Collections.emptyList();
        }
        List<JumpLink> result = new ArrayList<>();
        Iterator<Resource> children = container.listChildren();
        while (children.hasNext()) {
            ValueMap properties = children.next().getValueMap();
            String label = StringUtils.trimToNull(properties.get("label", String.class));
            String anchor = normaliseAnchor(properties.get("anchor", String.class));
            if (label != null && anchor != null) {
                result.add(new JumpLinkImpl(label, anchor));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Strips leading {@code #} and reduces the value to {@code [a-z0-9_-]}, so it can only ever be an
     * in-page fragment.
     */
    static String normaliseAnchor(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String anchor = StringUtils.stripStart(raw.trim(), "#")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        return anchor.isEmpty() ? null : anchor;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<JumpLink> getLinks() {
        return links;
    }

    @Override
    public boolean isEmpty() {
        return links.isEmpty();
    }

    @Override
    public String getId() {
        if (id == null) {
            id = ComponentUtils.getId(resource, currentPage, componentContext);
        }
        return id;
    }

    @Override
    @JsonProperty("dataLayer")
    public ComponentData getData() {
        if (!ComponentUtils.isDataLayerEnabled(resource)) {
            return null;
        }
        return DataLayerBuilder.forComponent()
                .withId(this::getId)
                .withType(resource::getResourceType)
                .withTitle(this::getTitle)
                .build();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
