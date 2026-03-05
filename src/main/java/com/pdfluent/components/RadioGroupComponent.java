package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a group of radio button options, either horizontally or vertically.
 *
 * Visual style: drawn circle (outline) with a filled inner circle when selected.
 * These are purely visual — not interactive AcroForm fields.
 */
public class RadioGroupComponent implements Component {

    private final List<String> options    = new ArrayList<>();
    private int  selectedIndex            = -1;   // -1 = nothing pre-selected
    private boolean horizontal            = false;
    private float radius                  = 5f;
    private float fontSize                = 10f;
    private Color color                   = Color.BLACK;
    private float spacing                 = 6f;   // gap between radio and label
    private float itemSpacing             = 14f;  // gap between items
    private float spaceBefore             = 4f;
    private float spaceAfter              = 4f;

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    public RadioGroupComponent option(String label)     { options.add(label);         return this; }
    public RadioGroupComponent selected(int index)      { this.selectedIndex = index; return this; }
    public RadioGroupComponent horizontal()             { this.horizontal = true;     return this; }
    public RadioGroupComponent vertical()               { this.horizontal = false;    return this; }
    public RadioGroupComponent radius(float r)          { this.radius = r;            return this; }
    public RadioGroupComponent fontSize(float fs)       { this.fontSize = fs;         return this; }
    public RadioGroupComponent color(Color c)           { this.color = c;             return this; }
    public RadioGroupComponent spacing(float s)         { this.spacing = s;           return this; }
    public RadioGroupComponent itemSpacing(float s)     { this.itemSpacing = s;       return this; }
    public RadioGroupComponent spaceBefore(float pts)   { this.spaceBefore = pts;     return this; }
    public RadioGroupComponent spaceAfter(float pts)    { this.spaceAfter = pts;      return this; }

    // -----------------------------------------------------------------------
    // Component
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float itemHeight = Math.max(radius * 2, fontSize);
        if (horizontal) {
            int rows = countRows(ctx, availableWidth);
            return spaceBefore + rows * (itemHeight + itemSpacing) - itemSpacing + spaceAfter;
        } else {
            return spaceBefore + options.size() * (itemHeight + itemSpacing) - itemSpacing + spaceAfter;
        }
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            float itemHeight = Math.max(radius * 2, fontSize);
            float curX = x;
            float curY = y + spaceBefore;

            for (int i = 0; i < options.size(); i++) {

                // Horizontal wrapping: if this item would overflow, move to next row
                if (horizontal && i > 0) {
                    float itemWidth = measureItem(ctx, i);
                    if (curX + itemWidth > x + width) {
                        curX = x;
                        curY += itemHeight + itemSpacing;
                    }
                }

                float circleCx = curX + radius;
                float circleCy = curY + itemHeight / 2f;

                // Outer circle
                ctx.drawCircle(circleCx, circleCy, radius, color, 1f);

                // Inner filled circle if selected
                if (i == selectedIndex) {
                    ctx.drawFilledCircle(circleCx, circleCy, radius * 0.5f, color);
                }

                // Label
                float textX = curX + radius * 2 + spacing;
                float textY = curY + (itemHeight - fontSize) / 2f;
                ctx.drawText(options.get(i), ctx.getRegularFont(), fontSize, color, textX, textY);

                if (horizontal) {
                    float labelWidth;
                    try {
                        labelWidth = ctx.getTextWidth(options.get(i), ctx.getRegularFont(), fontSize);
                    } catch (IOException e) {
                        labelWidth = 60f;
                    }
                    curX += radius * 2 + spacing + labelWidth + itemSpacing;
                } else {
                    curY += itemHeight + itemSpacing;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Measure the width of a single item (circle + spacing + label + itemSpacing). */
    private float measureItem(RenderContext ctx, int index) {
        try {
            float labelWidth = ctx.getTextWidth(options.get(index), ctx.getRegularFont(), fontSize);
            return radius * 2 + spacing + labelWidth + itemSpacing;
        } catch (IOException e) {
            return radius * 2 + spacing + 60f + itemSpacing;
        }
    }

    /** Count how many rows are needed for horizontal layout at the given width. */
    private int countRows(RenderContext ctx, float availableWidth) {
        int rows = 1;
        float curX = 0;
        for (int i = 0; i < options.size(); i++) {
            float itemWidth = measureItem(ctx, i);
            if (i > 0 && curX + itemWidth > availableWidth) {
                rows++;
                curX = 0;
            }
            curX += itemWidth;
        }
        return rows;
    }
}
