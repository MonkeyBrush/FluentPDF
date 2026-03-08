package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

/** Empty vertical gap. */
public class SpacerComponent implements Component {

    private final float height;

    public SpacerComponent(float height) {
        this.height = height;
    }

    @Override public float measure(RenderContext ctx, float availableWidth) { return height; }
    @Override public void render(RenderContext ctx, float x, float y, float width) { /* nothing to draw */ }
}
