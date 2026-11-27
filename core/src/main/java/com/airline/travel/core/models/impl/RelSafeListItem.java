package com.airline.travel.core.models.impl;

import java.util.Calendar;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Teaser action that exposes its link through {@link RelSafeLink}; everything else is the Core item.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RelSafeListItem implements ListItem {

    private final ListItem delegate;

    RelSafeListItem(ListItem delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Link getLink() {
        return RelSafeLink.of(delegate.getLink());
    }

    @Override
    @JsonIgnore
    @Deprecated
    public String getURL() {
        return delegate.getURL();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public Calendar getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    @JsonIgnore
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    @JsonIgnore
    public String getName() {
        return delegate.getName();
    }

    @Override
    @JsonIgnore
    public Resource getTeaserResource() {
        return delegate.getTeaserResource();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    @JsonProperty("dataLayer")
    public ComponentData getData() {
        return delegate.getData();
    }

    @Override
    @JsonIgnore
    public String getAppliedCssClasses() {
        return delegate.getAppliedCssClasses();
    }

    @Override
    @JsonIgnore
    public String getExportedType() {
        return delegate.getExportedType();
    }
}
