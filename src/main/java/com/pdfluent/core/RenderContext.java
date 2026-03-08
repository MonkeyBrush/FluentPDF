package com.pdfluent.core;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.util.function.IntFunction;

/**
 * Wraps a PDFBox content stream and provides higher-level drawing primitives.
 * Tracks the current Y cursor position and handles page creation / overflow.
 *
 * Y coordinates internally use PDFBox convention (origin bottom-left),
 * but the public API accepts "top-down" coordinates so callers think in
 * reading order (y=0 is the top margin, increasing downward).
 *
 * <p>Supports a per-page <em>top offset</em> that reclaims unused header
 * space on pages where no header is rendered.  When the offset is non-zero
 * the effective top margin shrinks, giving components more vertical room.</p>
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

    /**
     * Function that returns the top-margin offset for a given 1-based page
     * number.  A positive value means "reclaim this many points of top
     * margin" (i.e. the content area starts higher on the page).
     * Defaults to zero (no offset) for every page.
     */
    private final IntFunction<Float> topOffsetFn;

    /** Cached offset for the current page (recomputed in {@link #newPage()}). */
    private float topOffset = 0;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public RenderContext(PDDocument document, PageSettings settings) throws IOException {
        this(document, settings, pageNum -> 0f);
    }

    /**
     * Create a render context with a per-page top-margin offset function.
     *
     * @param topOffsetFn given a 1-based page number, returns how many points
     *                    of top margin to reclaim (0 = use full margin)
     */
    public RenderContext(PDDocument document, PageSettings settings,
                         IntFunction<Float> topOffsetFn) throws IOException {
        this.document    = document;
        this.settings    = settings;
        this.topOffsetFn = topOffsetFn;
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

        // Determine this page's top offset (page number is 1-based)
        int pageNum = document.getNumberOfPages();
        topOffset = topOffsetFn.apply(pageNum);

        cursorY = 0; // reset to top of (effective) content area
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
     * Draw a vertical line.
     */
    public void drawVerticalLine(float x, float y, float height,
                                  Color color, float lineWidth) throws IOException {
        float pdfY1 = toPdfY(y);
        float pdfY2 = toPdfY(y + height);
        stream.setStrokingColor(color);
        stream.setLineWidth(lineWidth);
        stream.moveTo(toAbsX(x), pdfY1);
        stream.lineTo(toAbsX(x), pdfY2);
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
    // Polyline / path drawing
    // -----------------------------------------------------------------------

    /**
     * Draw a connected series of line segments (polyline) in top-down
     * content space.  Each point is {@code {x, y}} relative to the
     * content area origin.
     *
     * @param points    array of {@code {x, y}} pairs — at least 2 required
     * @param color     stroke colour
     * @param lineWidth stroke width in points
     */
    public void drawPolyline(float[][] points, Color color, float lineWidth)
            throws IOException {
        if (points == null || points.length < 2) return;

        stream.setStrokingColor(color);
        stream.setLineWidth(lineWidth);
        stream.setLineJoinStyle(1);  // round join for smooth signature strokes
        stream.setLineCapStyle(1);   // round caps

        stream.moveTo(toAbsX(points[0][0]), toPdfY(points[0][1]));
        for (int i = 1; i < points.length; i++) {
            stream.lineTo(toAbsX(points[i][0]), toPdfY(points[i][1]));
        }
        stream.stroke();
    }

    // -----------------------------------------------------------------------
    // Image drawing
    // -----------------------------------------------------------------------

    /**
     * Draw an image at the given position in top-down content space.
     *
     * @param image  the PDFBox image object
     * @param x      X from left content edge
     * @param y      Y from top content edge (top of image)
     * @param width  draw width in points
     * @param height draw height in points
     */
    public void drawImage(PDImageXObject image, float x, float y,
                          float width, float height) throws IOException {
        float pdfY = toPdfY(y + height);
        stream.drawImage(image, toAbsX(x), pdfY, width, height);
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

    /**
     * Full content height for the current page.
     * Accounts for per-page top offset: on pages without a header the
     * effective top margin is reduced, yielding more content space.
     */
    public float getContentHeight() {
        return settings.getMediaBox().getHeight()
                - effectiveMarginTop()
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

    /**
     * Effective top margin for the current page.  On pages where no header
     * is rendered, the top offset reclaims space, moving content upward.
     * Clamped to a minimum of 10pt to prevent content touching the page edge.
     */
    private float effectiveMarginTop() {
        return Math.max(settings.getMarginTop() - topOffset, 10f);
    }

    /** Convert top-down content Y to PDFBox bottom-up absolute Y. */
    private float toPdfY(float contentY) {
        return settings.getMediaBox().getHeight()
                - effectiveMarginTop()
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
