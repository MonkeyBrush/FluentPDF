package com.pdfluent.builder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;

/**
 * Configures and renders a page footer with optional left, centre, and right
 * sections.  Template strings may contain placeholders that are resolved at
 * render time:
 *
 * <ul>
 *   <li>{@code {page}}       — current page number (1-based)</li>
 *   <li>{@code {totalPages}} — total number of pages in the document</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   Document.create(doc -&gt; doc
 *       .footer(f -&gt; f
 *           .center("Page {page} of {totalPages}")
 *           .fontSize(8)
 *           .color(Color.GRAY)
 *       )
 *       .page(page -&gt; page
 *           .text("Hello world")
 *       )
 *   ).save("output.pdf");
 * </pre>
 */
public class Footer {

    private String left;
    private String center;
    private String right;
    private float  fontSize = 8f;
    private Color  color    = Color.GRAY;
    private boolean bold    = false;

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    /** Text for the left side of the footer. Supports {page} and {totalPages}. */
    public Footer left(String text)     { this.left = text;      return this; }

    /** Text for the centre of the footer. Supports {page} and {totalPages}. */
    public Footer center(String text)   { this.center = text;    return this; }

    /** Text for the right side of the footer. Supports {page} and {totalPages}. */
    public Footer right(String text)    { this.right = text;     return this; }

    public Footer fontSize(float size)  { this.fontSize = size;  return this; }
    public Footer color(Color c)        { this.color = c;        return this; }
    public Footer bold()                { this.bold = true;      return this; }

    // -----------------------------------------------------------------------
    // Rendering — called by Document after all content pages are built
    // -----------------------------------------------------------------------

    /**
     * Stamp the footer onto every page in the document.
     *
     * @param pdDoc          the fully-rendered PDFBox document
     * @param marginLeft     left margin in points
     * @param marginRight    right margin in points
     * @param marginBottom   bottom margin in points
     */
    void render(PDDocument pdDoc, float marginLeft, float marginRight,
                float marginBottom) throws IOException {

        int totalPages = pdDoc.getNumberOfPages();
        PDFont font = bold
                ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        for (int i = 0; i < totalPages; i++) {
            PDPage page = pdDoc.getPage(i);
            float pageWidth  = page.getMediaBox().getWidth();

            // Position the baseline inside the bottom margin, roughly centred.
            float baselineY = marginBottom * 0.4f;

            float contentLeft  = marginLeft;
            float contentRight = pageWidth - marginRight;

            try (PDPageContentStream cs = new PDPageContentStream(
                    pdDoc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                int pageNum = i + 1;

                if (left != null) {
                    String resolved = resolve(left, pageNum, totalPages);
                    drawText(cs, resolved, font, fontSize, color,
                             contentLeft, baselineY);
                }

                if (center != null) {
                    String resolved = resolve(center, pageNum, totalPages);
                    float textWidth = font.getStringWidth(resolved) / 1000f * fontSize;
                    float cx = contentLeft + (contentRight - contentLeft - textWidth) / 2f;
                    drawText(cs, resolved, font, fontSize, color, cx, baselineY);
                }

                if (right != null) {
                    String resolved = resolve(right, pageNum, totalPages);
                    float textWidth = font.getStringWidth(resolved) / 1000f * fontSize;
                    drawText(cs, resolved, font, fontSize, color,
                             contentRight - textWidth, baselineY);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String resolve(String template, int page, int totalPages) {
        return template
                .replace("{page}", String.valueOf(page))
                .replace("{totalPages}", String.valueOf(totalPages));
    }

    private static void drawText(PDPageContentStream cs, String text,
                                 PDFont font, float fontSize, Color color,
                                 float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }
}
