package com.airline.travel.core.models.impl;

import com.airline.travel.core.models.JumpLink;

final class JumpLinkImpl implements JumpLink {

    private final String label;
    private final String anchor;

    JumpLinkImpl(String label, String anchor) {
        this.label = label;
        this.anchor = anchor;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getAnchor() {
        return anchor;
    }
}
