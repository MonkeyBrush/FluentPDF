package com.pdfluent.core;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Immutable page configuration: paper size and margins (in points).
 */
public class PageSettings {

    private final PDRectangle mediaBox;
    private final float marginTop;
    private final float marginBottom;
    private final float marginLeft;
    private final float marginRight;

    private PageSettings(Builder b) {
        this.mediaBox     = b.mediaBox;
        this.marginTop    = b.marginTop;
        this.marginBottom = b.marginBottom;
        this.marginLeft   = b.marginLeft;
        this.marginRight  = b.marginRight;
    }

    public PDRectangle getMediaBox()    { return mediaBox; }
    public float getMarginTop()         { return marginTop; }
    public float getMarginBottom()      { return marginBottom; }
    public float getMarginLeft()        { return marginLeft; }
    public float getMarginRight()       { return marginRight; }

    // -----------------------------------------------------------------------

    public static Builder a4() {
        return new Builder(PDRectangle.A4);
    }

    public static Builder letter() {
        return new Builder(PDRectangle.LETTER);
    }

    public static class Builder {

        private PDRectangle mediaBox;
        private float marginTop    = 36;
        private float marginBottom = 36;
        private float marginLeft   = 36;
        private float marginRight  = 36;

        public Builder(PDRectangle mediaBox) {
            this.mediaBox = mediaBox;
        }

        public Builder margin(float all) {
            return margin(all, all, all, all);
        }

        public Builder margin(float topBottom, float leftRight) {
            return margin(topBottom, leftRight, topBottom, leftRight);
        }

        public Builder margin(float top, float right, float bottom, float left) {
            this.marginTop    = top;
            this.marginRight  = right;
            this.marginBottom = bottom;
            this.marginLeft   = left;
            return this;
        }

        public PageSettings build() {
            return new PageSettings(this);
        }
    }
}
