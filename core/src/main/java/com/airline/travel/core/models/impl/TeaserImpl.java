package com.airline.travel.core.models.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.Teaser;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Joey Club teaser (ADR-003): the Core Teaser v2 with one change. Links that open in a new tab get
 * {@code rel="noopener noreferrer"}. Core HTL is reused unchanged and resolves this model because it
 * is registered for the more specific resource type.
 */
@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = {Teaser.class, ComponentExporter.class},
        resourceType = TeaserImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class TeaserImpl implements Teaser {

    static final String RESOURCE_TYPE = "airline/components/teaser";

    @Self
    @Via(type = ResourceSuperType.class)
    private Teaser delegate;

    @SlingObject
    private Resource resource;

    @Override
    @SuppressWarnings("rawtypes")
    public Link getLink() {
        return RelSafeLink.of(delegate.getLink());
    }

    @Override
    public List<ListItem> getActions() {
        List<ListItem> actions = delegate.getActions();
        if (actions == null || actions.isEmpty()) {
            return Collections.emptyList();
        }
        return actions.stream().map(RelSafeListItem::new).collect(Collectors.toList());
    }

    @Override
    public boolean isActionsEnabled() {
        return delegate.isActionsEnabled();
    }

    @Override
    @JsonIgnore
    @Deprecated
    public String getLinkURL() {
        return delegate.getLinkURL();
    }

    @Override
    @JsonIgnore
    public Resource getImageResource() {
        return delegate.getImageResource();
    }

    @Override
    public boolean isImageLinkHidden() {
        return delegate.isImageLinkHidden();
    }

    @Override
    public String getPretitle() {
        return delegate.getPretitle();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public boolean isTitleLinkHidden() {
        return delegate.isTitleLinkHidden();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public String getTitleType() {
        return delegate.getTitleType();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    @JsonProperty("dataLayer")
    public ComponentData getData() {
        ComponentData data = delegate.getData();
        if (data == null) {
            return null;
        }
        return DataLayerBuilder.extending(data).asComponent().withType(resource::getResourceType).build();
    }

    @Override
    @JsonProperty("appliedCssClassNames")
    public String getAppliedCssClasses() {
        return delegate.getAppliedCssClasses();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }
}
