package com.pdfluent.core;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;

/**
 * Wraps a PDFBox content stream and provides higher-level drawing primitives.
 * Tracks the current Y cursor position and handles page creation / overflow.
 *
 * Y coordinates internally use PDFBox convention (origin bottom-left),
 * but the public API accepts "top-down" coordinates so callers think in
 * reading order (y=0 is the top margin, increasing downward).
 */
public class RenderContext {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final PDDocument document;
    private PDPage           currentPage;
    private PDPageContentStream stream;

    /** Current Y in top-down space (0 = top margin). Increases downward. */
    private float cursorY = 0;

    private final PageSettings settings;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public RenderContext(PDDocument document, PageSettings settings) throws IOException {
        this.document = document;
        this.settings = settings;
        newPage();
    }

    // -----------------------------------------------------------------------
    // Page management
    // -----------------------------------------------------------------------

    public void newPage() throws IOException {
        if (stream != null) {
            stream.close();
        }
        currentPage = new PDPage(settings.getMediaBox());
        document.addPage(currentPage);
        stream = new PDPageContentStream(document, currentPage,
                PDPageContentStream.AppendMode.APPEND, true, true);
        cursorY = 0; // reset to top margin
    }

    /**
     * Ensures there is at least {@code requiredHeight} points of vertical space
     * remaining. If not, a new page is started.
     */
    public void ensureSpace(float requiredHeight) throws IOException {
        if (cursorY + requiredHeight > getContentHeight()) {
            newPage();
        }
    }

    public void advanceCursor(float amount) {
        cursorY += amount;
    }

    /** Remaining vertical space on the current page in points. */
    public float remainingHeight() {
        return getContentHeight() - cursorY;
    }

    // -----------------------------------------------------------------------
    // Drawing primitives — all coordinates in top-down content space
    // -----------------------------------------------------------------------

    /**
     * Draw a single line of text.
     *
     * @param text     the string to draw
     * @param font     PDFBox font
     * @param fontSize font size in points
     * @param color    text colour
     * @param x        X from left content edge
     * @param y        Y from top content edge (top of the text baseline)
     */
    public void drawText(String text, PDFont font, float fontSize,
                         Color color, float x, float y) throws IOException {
        float pdfY = toPdfY(y + fontSize); // baseline position
        stream.beginText();
        stream.setFont(font, fontSize);
        setNonStrokingColor(color);
        stream.newLineAtOffset(toAbsX(x), pdfY);
        stream.showText(text);
        stream.endText();
    }

    /**
     * Draw a horizontal line.
     */
    public void drawHorizontalLine(float x, float y, float width,
                                   Color color, float lineWidth) throws IOException {
        float pdfY = toPdfY(y);
        stream.setStrokingColor(color);
        stream.setLineWidth(lineWidth);
        stream.moveTo(toAbsX(x), pdfY);
        stream.lineTo(toAbsX(x + width), pdfY);
        stream.stroke();
    }

    /**
     * Draw a rectangle outline.
     */
    public void drawRect(float x, float y, float width, float height,
                         Color strokeColor, float lineWidth) throws IOException {
        float pdfY = toPdfY(y + height);
        stream.setStrokingColor(strokeColor);
        stream.setLineWidth(lineWidth);
        stream.addRect(toAbsX(x), pdfY, width, height);
        stream.stroke();
    }

    /**
     * Draw a filled rectangle.
     */
    public void drawFilledRect(float x, float y, float width, float height,
                                Color fillColor) throws IOException {
        float pdfY = toPdfY(y + height);
        setNonStrokingColor(fillColor);
        stream.addRect(toAbsX(x), pdfY, width, height);
        stream.fill();
    }

    /**
     * Draw a circle outline.
     *
     * @param cx   centre X in content space
     * @param cy   centre Y in content space
     * @param r    radius in points
     */
    public void drawCircle(float cx, float cy, float r,
                           Color color, float lineWidth) throws IOException {
        drawEllipse(cx, cy, r, r, color, lineWidth, false);
    }

    /**
     * Draw a filled circle.
     */
    public void drawFilledCircle(float cx, float cy, float r,
                                 Color color) throws IOException {
        drawEllipse(cx, cy, r, r, color, 0, true);
    }

    // -----------------------------------------------------------------------
    // Text measurement helpers
    // -----------------------------------------------------------------------

    public float getTextWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    public float getTextHeight(PDFont font, float fontSize) {
        return font.getFontDescriptor().getCapHeight() / 1000f * fontSize;
    }

    // -----------------------------------------------------------------------
    // Fonts — convenience accessors for standard fonts
    // -----------------------------------------------------------------------

    public PDFont getRegularFont()  { return new PDType1Font(Standard14Fonts.FontName.HELVETICA); }
    public PDFont getBoldFont()     { return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD); }
    public PDFont getItalicFont()   { return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE); }

    // -----------------------------------------------------------------------
    // Page geometry
    // -----------------------------------------------------------------------

    /** Full content width (page width minus left and right margins). */
    public float getContentWidth() {
        return settings.getMediaBox().getWidth()
                - settings.getMarginLeft()
                - settings.getMarginRight();
    }

    /** Full content height (page height minus top and bottom margins). */
    public float getContentHeight() {
        return settings.getMediaBox().getHeight()
                - settings.getMarginTop()
                - settings.getMarginBottom();
    }

    public float getCursorY()   { return cursorY; }
    public PDDocument getDocument() { return document; }
    public PageSettings getSettings() { return settings; }

    // -----------------------------------------------------------------------
    // Cleanup
    // -----------------------------------------------------------------------

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Convert top-down content Y to PDFBox bottom-up absolute Y. */
    private float toPdfY(float contentY) {
        return settings.getMediaBox().getHeight()
                - settings.getMarginTop()
                - contentY;
    }

    /** Convert content-relative X to absolute page X. */
    private float toAbsX(float contentX) {
        return settings.getMarginLeft() + contentX;
    }

    private void setNonStrokingColor(Color color) throws IOException {
        stream.setNonStrokingColor(color);
    }

    /** Approximate Bezier curve ellipse (4-arc method). */
    private void drawEllipse(float cx, float cy, float rx, float ry,
                             Color color, float lineWidth, boolean fill) throws IOException {
        float absCx = toAbsX(cx);
        float absCy = toPdfY(cy);
        final float k = 0.5523f;

        if (fill) {
            setNonStrokingColor(color);
        } else {
            stream.setStrokingColor(color);
            stream.setLineWidth(lineWidth);
        }

        stream.moveTo(absCx + rx, absCy);
        stream.curveTo(absCx + rx, absCy + ry * k, absCx + rx * k, absCy + ry, absCx, absCy + ry);
        stream.curveTo(absCx - rx * k, absCy + ry, absCx - rx, absCy + ry * k, absCx - rx, absCy);
        stream.curveTo(absCx - rx, absCy - ry * k, absCx - rx * k, absCy - ry, absCx, absCy - ry);
        stream.curveTo(absCx + rx * k, absCy - ry, absCx + rx, absCy - ry * k, absCx + rx, absCy);

        if (fill) {
            stream.fill();
        } else {
            stream.stroke();
        }
    }
}
