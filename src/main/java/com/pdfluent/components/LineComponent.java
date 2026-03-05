package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.awt.Color;
import java.io.IOException;

/**
 * Draws a horizontal rule across the available width.
 */
public class LineComponent implements Component {

    private Color color     = Color.BLACK;
    private float thickness = 0.5f;
    private float spaceBefore = 4f;
    private float spaceAfter  = 4f;

    public LineComponent color(Color c)          { this.color = c;         return this; }
    public LineComponent thickness(float t)      { this.thickness = t;     return this; }
    public LineComponent spaceBefore(float pts)  { this.spaceBefore = pts; return this; }
    public LineComponent spaceAfter(float pts)   { this.spaceAfter = pts;  return this; }

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        return spaceBefore + thickness + spaceAfter;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            ctx.drawHorizontalLine(x, y + spaceBefore, width, color, thickness);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
