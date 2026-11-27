package com.airline.travel.core.models.impl;

import com.airline.travel.core.models.HeroImage;

final class HeroImageImpl implements HeroImage {

    private final String src;
    private final String srcset;
    private final String sizes;
    private final int width;
    private final int height;
    private final String alt;
    private final boolean decorative;

    HeroImageImpl(String src, String srcset, String sizes, int width, int height, String alt, boolean decorative) {
        this.src = src;
        this.srcset = srcset;
        this.sizes = sizes;
        this.width = width;
        this.height = height;
        this.alt = alt;
        this.decorative = decorative;
    }

    @Override
    public String getSrc() {
        return src;
    }

    @Override
    public String getSrcset() {
        return srcset;
    }

    @Override
    public String getSizes() {
        return sizes;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getAlt() {
        return alt;
    }

    @Override
    public boolean isDecorative() {
        return decorative;
    }
}
