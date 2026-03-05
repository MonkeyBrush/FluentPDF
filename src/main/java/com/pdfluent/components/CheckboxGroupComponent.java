package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renders a list of checkbox items (square boxes with optional tick mark).
 * Purely visual — not AcroForm interactive.
 */
public class CheckboxGroupComponent implements Component {

    private final List<String>  labels   = new ArrayList<>();
    private final Set<Integer>  checked  = new HashSet<>();

    private float boxSize     = 10f;
    private float fontSize    = 10f;
    private Color color       = Color.BLACK;
    private float spacing     = 6f;   // gap between box and label
    private float itemSpacing = 6f;   // vertical gap between items
    private float spaceBefore = 4f;
    private float spaceAfter  = 4f;
    private boolean horizontal = false;

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    public CheckboxGroupComponent item(String label)         { labels.add(label); return this; }
    public CheckboxGroupComponent item(String label, boolean isChecked) {
        if (isChecked) checked.add(labels.size());
        labels.add(label);
        return this;
    }
    public CheckboxGroupComponent check(int index)           { checked.add(index); return this; }
    public CheckboxGroupComponent horizontal()               { this.horizontal = true; return this; }
    public CheckboxGroupComponent boxSize(float s)           { this.boxSize = s;    return this; }
    public CheckboxGroupComponent fontSize(float fs)         { this.fontSize = fs;  return this; }
    public CheckboxGroupComponent color(Color c)             { this.color = c;      return this; }
    public CheckboxGroupComponent spacing(float s)           { this.spacing = s;    return this; }
    public CheckboxGroupComponent itemSpacing(float s)       { this.itemSpacing = s; return this; }
    public CheckboxGroupComponent spaceBefore(float pts)     { this.spaceBefore = pts; return this; }
    public CheckboxGroupComponent spaceAfter(float pts)      { this.spaceAfter = pts;  return this; }

    // -----------------------------------------------------------------------
    // Component
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float itemHeight = Math.max(boxSize, fontSize);
        if (horizontal) {
            int rows = countRows(ctx, availableWidth);
            return spaceBefore + rows * (itemHeight + itemSpacing) - itemSpacing + spaceAfter;
        }
        return spaceBefore + labels.size() * (itemHeight + itemSpacing) - itemSpacing + spaceAfter;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            float itemHeight = Math.max(boxSize, fontSize);
            float curX = x;
            float curY = y + spaceBefore;

            for (int i = 0; i < labels.size(); i++) {

                // Horizontal wrapping: if this item would overflow, move to next row
                if (horizontal && i > 0) {
                    float itemWidth = measureItem(ctx, i);
                    if (curX + itemWidth > x + width) {
                        curX = x;
                        curY += itemHeight + itemSpacing;
                    }
                }

                float boxY = curY + (itemHeight - boxSize) / 2f;

                // Box outline
                ctx.drawRect(curX, boxY, boxSize, boxSize, color, 1f);

                // Tick if checked — drawn as two lines forming a checkmark
                if (checked.contains(i)) {
                    drawTick(ctx, curX, boxY);
                }

                // Label
                float textX = curX + boxSize + spacing;
                float textY = curY + (itemHeight - fontSize) / 2f;
                ctx.drawText(labels.get(i), ctx.getRegularFont(), fontSize, color, textX, textY);

                if (horizontal) {
                    float labelWidth;
                    try {
                        labelWidth = ctx.getTextWidth(labels.get(i), ctx.getRegularFont(), fontSize);
                    } catch (IOException e) {
                        labelWidth = 60f;
                    }
                    curX += boxSize + spacing + labelWidth + itemSpacing;
                } else {
                    curY += itemHeight + itemSpacing;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Measure the width of a single item (box + spacing + label + itemSpacing). */
    private float measureItem(RenderContext ctx, int index) {
        try {
            float labelWidth = ctx.getTextWidth(labels.get(index), ctx.getRegularFont(), fontSize);
            return boxSize + spacing + labelWidth + itemSpacing;
        } catch (IOException e) {
            return boxSize + spacing + 60f + itemSpacing;
        }
    }

    /** Count how many rows are needed for horizontal layout at the given width. */
    private int countRows(RenderContext ctx, float availableWidth) {
        int rows = 1;
        float curX = 0;
        for (int i = 0; i < labels.size(); i++) {
            float itemWidth = measureItem(ctx, i);
            if (i > 0 && curX + itemWidth > availableWidth) {
                rows++;
                curX = 0;
            }
            curX += itemWidth;
        }
        return rows;
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    /** Draw a simple tick inside the box at (boxX, boxY). */
    private void drawTick(RenderContext ctx, float boxX, float boxY) throws IOException {
        float pad = boxSize * 0.2f;
        // Tick: bottom-left to mid-bottom, then up to top-right
        float x1 = boxX + pad;
        float y1 = boxY + boxSize * 0.5f;
        float x2 = boxX + boxSize * 0.4f;
        float y2 = boxY + pad;
        float x3 = boxX + boxSize - pad;
        float y3 = boxY + boxSize - pad;

        // We draw tick as two line segments via drawHorizontalLine — 
        // but RenderContext only has drawHorizontalLine, so we use drawRect 
        // with zero height as a workaround for now. A full implementation 
        // would expose moveTo/lineTo directly.
        // For now: fill a small inner rect to represent checked state clearly.
        float innerPad = boxSize * 0.25f;
        ctx.drawFilledRect(boxX + innerPad, boxY + innerPad,
                           boxSize - innerPad * 2, boxSize - innerPad * 2,
                           color);
    }
}
