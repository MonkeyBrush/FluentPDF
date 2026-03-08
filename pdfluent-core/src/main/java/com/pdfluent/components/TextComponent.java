package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a single paragraph of text with automatic line-wrapping.
 */
public class TextComponent implements Component {

    private String text;
    private float  fontSize   = 10f;
    private Color  color      = Color.BLACK;
    private boolean bold      = false;
    private boolean italic    = false;
    private float  lineSpacing = 1.3f;  // multiplier
    private Align  align      = Align.LEFT;
    private float  spaceBefore = 0f;
    private float  spaceAfter  = 4f;

    public enum Align { LEFT, CENTER, RIGHT }

    public TextComponent(String text) {
        this.text = text;
    }

    // -----------------------------------------------------------------------
    // Fluent setters
    // -----------------------------------------------------------------------

    public TextComponent fontSize(float size)      { this.fontSize = size;    return this; }
    public TextComponent color(Color c)            { this.color = c;          return this; }
    public TextComponent bold()                    { this.bold = true;        return this; }
    public TextComponent italic()                  { this.italic = true;      return this; }
    public TextComponent lineSpacing(float ls)     { this.lineSpacing = ls;   return this; }
    public TextComponent alignCenter()             { this.align = Align.CENTER; return this; }
    public TextComponent alignRight()              { this.align = Align.RIGHT;  return this; }
    public TextComponent spaceBefore(float pts)    { this.spaceBefore = pts;  return this; }
    public TextComponent spaceAfter(float pts)     { this.spaceAfter = pts;   return this; }

    // -----------------------------------------------------------------------
    // Component
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        try {
            PDFont font = resolveFont(ctx);
            List<String> lines = wrap(text, font, fontSize, availableWidth);
            float lineHeight = fontSize * lineSpacing;
            return spaceBefore + lines.size() * lineHeight + spaceAfter;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            PDFont font = resolveFont(ctx);
            List<String> lines = wrap(text, font, fontSize, width);
            float lineHeight = fontSize * lineSpacing;
            float drawY = y + spaceBefore + fontSize; // baseline of first line

            for (String line : lines) {
                float drawX = x;
                if (align == Align.CENTER) {
                    float lineWidth = ctx.getTextWidth(line, font, fontSize);
                    drawX = x + (width - lineWidth) / 2f;
                } else if (align == Align.RIGHT) {
                    float lineWidth = ctx.getTextWidth(line, font, fontSize);
                    drawX = x + width - lineWidth;
                }
                ctx.drawText(line, font, fontSize, color, drawX, drawY - fontSize);
                drawY += lineHeight;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PDFont resolveFont(RenderContext ctx) {
        if (bold)   return ctx.getBoldFont();
        if (italic) return ctx.getItalicFont();
        return ctx.getRegularFont();
    }

    /** Word-wrap text to fit within maxWidth points. */
    private List<String> wrap(String text, PDFont font, float fontSize,
                               float maxWidth) throws IOException {
        List<String> lines  = new ArrayList<>();
        String[] words      = text.split(" ");
        StringBuilder line  = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float w = font.getStringWidth(candidate) / 1000f * fontSize;
            if (w > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
