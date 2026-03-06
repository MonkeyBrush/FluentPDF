package com.pdfluent.builder;

import com.pdfluent.components.ImageComponent;
import com.pdfluent.core.PageSettings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Configures and renders a page header with support for images, multiline text,
 * and multi-column layouts.  The header is stamped onto pages during the second
 * rendering pass, after all content pages have been created.
 *
 * <h3>Display modes</h3>
 * <ul>
 *   <li>{@link #showAll()}  — every page (default)</li>
 *   <li>{@link #showOnce()} — first page only</li>
 *   <li>{@link #showEven()} — even-numbered pages (2, 4, 6 …)</li>
 *   <li>{@link #showOdd()}  — odd-numbered pages (1, 3, 5 …)</li>
 * </ul>
 *
 * <h3>Content modes</h3>
 * Use either the simple text API or the column layout — not both:
 *
 * <p><strong>Simple text:</strong></p>
 * <pre>
 *   .header(h -&gt; h
 *       .text("Document Title", tc -&gt; tc.bold().fontSize(14).alignCenter())
 *       .text("Subtitle", tc -&gt; tc.fontSize(10).color(Color.GRAY).alignCenter())
 *   )
 * </pre>
 *
 * <p><strong>Column layout with logo:</strong></p>
 * <pre>
 *   .header(h -&gt; h
 *       .height(60)
 *       .columns(cols -&gt; cols
 *           .column(30, col -&gt; col
 *               .image("logo.png").width(80).height(40)
 *           )
 *           .column(70, col -&gt; col
 *               .text("My Company").bold().fontSize(14)
 *               .text("ABN: 12 345 678 901").fontSize(9).color(Color.GRAY)
 *           )
 *       )
 *   )
 * </pre>
 *
 * <p>Template placeholders {@code {page}} and {@code {totalPages}} are
 * resolved at render time in all text entries.</p>
 */
public class Header {

    // -----------------------------------------------------------------------
    // Display mode
    // -----------------------------------------------------------------------

    public enum DisplayMode { ALL, ONCE, EVEN, ODD }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private DisplayMode displayMode = DisplayMode.ALL;
    private float       height      = 50f;           // header area height in points
    private boolean     underline   = false;
    private Color       underlineColor = Color.LIGHT_GRAY;
    private float       underlineThickness = 0.5f;

    // Simple text entries (used when no columns are defined)
    private final List<TextEntry> textEntries = new ArrayList<>();

    // Column layout (takes precedence over textEntries if set)
    private List<HeaderColumn> columns;

    // -----------------------------------------------------------------------
    // Display mode API
    // -----------------------------------------------------------------------

    public Header showAll()  { this.displayMode = DisplayMode.ALL;  return this; }
    public Header showOnce() { this.displayMode = DisplayMode.ONCE; return this; }
    public Header showEven() { this.displayMode = DisplayMode.EVEN; return this; }
    public Header showOdd()  { this.displayMode = DisplayMode.ODD;  return this; }

    // -----------------------------------------------------------------------
    // General settings
    // -----------------------------------------------------------------------

    /** Set the header area height in points (default 50). */
    public Header height(float pts)              { this.height = pts;               return this; }

    /** Draw a thin underline below the header area. */
    public Header underline()                    { this.underline = true;           return this; }
    public Header underline(Color c)             { this.underline = true;
                                                   this.underlineColor = c;         return this; }
    public Header underlineThickness(float t)    { this.underlineThickness = t;     return this; }

    // -----------------------------------------------------------------------
    // Simple text API (for headers without columns)
    // -----------------------------------------------------------------------

    /** Add a line of text to the header. Configure style via the consumer. */
    public Header text(String content, Consumer<TextEntry> configure) {
        TextEntry te = new TextEntry(content);
        configure.accept(te);
        textEntries.add(te);
        return this;
    }

    /** Add a line of text with default styling. */
    public Header text(String content) {
        textEntries.add(new TextEntry(content));
        return this;
    }

    // -----------------------------------------------------------------------
    // Column layout API
    // -----------------------------------------------------------------------

    /** Define a column-based header layout. */
    public Header columns(Consumer<HeaderColumnBuilder> configure) {
        HeaderColumnBuilder hcb = new HeaderColumnBuilder();
        configure.accept(hcb);
        this.columns = hcb.columns;
        return this;
    }

    // -----------------------------------------------------------------------
    // Rendering — called by Document in Pass 2
    // -----------------------------------------------------------------------

    /**
     * Stamp the header onto applicable pages.
     *
     * @param pdDoc    the fully-rendered PDFBox document
     * @param settings page settings (margins, media box)
     */
    void render(PDDocument pdDoc, PageSettings settings) throws IOException {
        int totalPages = pdDoc.getNumberOfPages();
        float pageWidth  = settings.getMediaBox().getWidth();
        float pageHeight = settings.getMediaBox().getHeight();
        float marginLeft  = settings.getMarginLeft();
        float marginRight = settings.getMarginRight();
        float marginTop   = settings.getMarginTop();

        float contentLeft  = marginLeft;
        float contentWidth = pageWidth - marginLeft - marginRight;

        // The header renders in the top margin area.
        // headerTop is the PDF-Y of the top of the header area.
        // We place the header centred vertically within the top margin.
        float headerTopPdfY = pageHeight - (marginTop - height) / 2f;

        for (int i = 0; i < totalPages; i++) {
            int pageNum = i + 1;
            if (!shouldShow(pageNum, totalPages)) continue;

            PDPage page = pdDoc.getPage(i);

            try (PDPageContentStream cs = new PDPageContentStream(
                    pdDoc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                if (columns != null && !columns.isEmpty()) {
                    renderColumns(cs, pdDoc, pageNum, totalPages,
                                  contentLeft, headerTopPdfY, contentWidth);
                } else {
                    renderSimpleText(cs, pageNum, totalPages,
                                     contentLeft, headerTopPdfY, contentWidth);
                }

                // Optional underline beneath header area
                if (underline) {
                    float lineY = pageHeight - marginTop;
                    cs.setStrokingColor(underlineColor);
                    cs.setLineWidth(underlineThickness);
                    cs.moveTo(contentLeft, lineY);
                    cs.lineTo(contentLeft + contentWidth, lineY);
                    cs.stroke();
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Display mode filter
    // -----------------------------------------------------------------------

    private boolean shouldShow(int pageNum, int totalPages) {
        return switch (displayMode) {
            case ALL  -> true;
            case ONCE -> pageNum == 1;
            case EVEN -> pageNum % 2 == 0;
            case ODD  -> pageNum % 2 != 0;
        };
    }

    // -----------------------------------------------------------------------
    // Simple text rendering
    // -----------------------------------------------------------------------

    private void renderSimpleText(PDPageContentStream cs, int pageNum, int totalPages,
                                   float contentLeft, float topY, float contentWidth)
            throws IOException {

        float curY = topY;

        for (TextEntry te : textEntries) {
            PDFont font = te.bold
                    ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    : (te.italic
                        ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)
                        : new PDType1Font(Standard14Fonts.FontName.HELVETICA));

            String resolved = resolve(te.content, pageNum, totalPages);
            float lineHeight = te.fontSize * 1.3f;
            curY -= lineHeight;

            // Word-wrap the text
            List<String> lines = wrapText(resolved, font, te.fontSize, contentWidth);

            for (String line : lines) {
                float textWidth = font.getStringWidth(line) / 1000f * te.fontSize;
                float drawX = contentLeft;

                switch (te.alignment) {
                    case CENTER -> drawX = contentLeft + (contentWidth - textWidth) / 2f;
                    case RIGHT  -> drawX = contentLeft + contentWidth - textWidth;
                }

                drawText(cs, line, font, te.fontSize, te.color, drawX, curY);
                curY -= lineHeight;
            }
            // Undo the extra advance after the last line
            curY += lineHeight;
        }
    }

    // -----------------------------------------------------------------------
    // Column rendering
    // -----------------------------------------------------------------------

    private void renderColumns(PDPageContentStream cs, PDDocument pdDoc,
                                int pageNum, int totalPages,
                                float contentLeft, float topY, float contentWidth)
            throws IOException {

        float totalGutter = 12f * (columns.size() - 1);
        float usableWidth = contentWidth - totalGutter;
        float curX = contentLeft;

        for (HeaderColumn col : columns) {
            float colWidth = usableWidth * (col.widthPercent / 100f);

            renderColumnContent(cs, pdDoc, col, pageNum, totalPages,
                                curX, topY, colWidth);

            curX += colWidth + 12f; // gutter
        }
    }

    private void renderColumnContent(PDPageContentStream cs, PDDocument pdDoc,
                                      HeaderColumn col, int pageNum, int totalPages,
                                      float colX, float topY, float colWidth)
            throws IOException {

        float curY = topY;

        for (Object entry : col.entries) {
            if (entry instanceof TextEntry te) {
                PDFont font = te.bold
                        ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                        : (te.italic
                            ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)
                            : new PDType1Font(Standard14Fonts.FontName.HELVETICA));

                String resolved = resolve(te.content, pageNum, totalPages);
                float lineHeight = te.fontSize * 1.3f;
                curY -= lineHeight;

                List<String> lines = wrapText(resolved, font, te.fontSize, colWidth);

                for (String line : lines) {
                    float textWidth = font.getStringWidth(line) / 1000f * te.fontSize;
                    float drawX = colX;

                    switch (te.alignment) {
                        case CENTER -> drawX = colX + (colWidth - textWidth) / 2f;
                        case RIGHT  -> drawX = colX + colWidth - textWidth;
                    }

                    drawText(cs, line, font, te.fontSize, te.color, drawX, curY);
                    curY -= lineHeight;
                }
                curY += lineHeight; // undo extra advance
            } else if (entry instanceof ImageEntry ie) {
                PDImageXObject img;
                if (ie.imagePath != null) {
                    img = PDImageXObject.createFromFile(ie.imagePath, pdDoc);
                } else if (ie.imageBytes != null) {
                    img = PDImageXObject.createFromByteArray(pdDoc, ie.imageBytes, ie.imageType);
                } else {
                    continue;
                }

                float imgW = ie.width > 0 ? ie.width : img.getWidth();
                float imgH = ie.height > 0 ? ie.height : img.getHeight();

                // Maintain aspect ratio if only one dimension is set
                if (ie.width > 0 && ie.height <= 0) {
                    imgH = img.getHeight() * (imgW / img.getWidth());
                } else if (ie.height > 0 && ie.width <= 0) {
                    imgW = img.getWidth() * (imgH / img.getHeight());
                }

                // Clamp to column width
                if (imgW > colWidth) {
                    float scale = colWidth / imgW;
                    imgW = colWidth;
                    imgH *= scale;
                }

                curY -= imgH;
                cs.drawImage(img, colX, curY, imgW, imgH);
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

    /** Simple word-wrap into lines that fit within maxWidth. */
    private static List<String> wrapText(String text, PDFont font,
                                          float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

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

    // =======================================================================
    // Inner classes
    // =======================================================================

    // -----------------------------------------------------------------------
    // TextEntry — a styled line of text
    // -----------------------------------------------------------------------

    public static class TextEntry {
        String content;
        float  fontSize  = 10f;
        Color  color     = Color.BLACK;
        boolean bold     = false;
        boolean italic   = false;
        Alignment alignment = Alignment.LEFT;

        public TextEntry(String content) { this.content = content; }

        public TextEntry fontSize(float s)    { this.fontSize = s;             return this; }
        public TextEntry color(Color c)       { this.color = c;               return this; }
        public TextEntry bold()               { this.bold = true;             return this; }
        public TextEntry italic()             { this.italic = true;           return this; }
        public TextEntry alignLeft()          { this.alignment = Alignment.LEFT;   return this; }
        public TextEntry alignCenter()        { this.alignment = Alignment.CENTER; return this; }
        public TextEntry alignRight()         { this.alignment = Alignment.RIGHT;  return this; }
    }

    enum Alignment { LEFT, CENTER, RIGHT }

    // -----------------------------------------------------------------------
    // ImageEntry — an image in a header column
    // -----------------------------------------------------------------------

    static class ImageEntry {
        String imagePath;
        byte[] imageBytes;
        String imageType;
        float  width  = -1;
        float  height = -1;
    }

    // -----------------------------------------------------------------------
    // ImageAdder — fluent builder returned by column.image()
    // -----------------------------------------------------------------------

    public static class ImageAdder {
        private final ImageEntry entry;
        private final HeaderColumnContent parent;

        ImageAdder(ImageEntry entry, HeaderColumnContent parent) {
            this.entry  = entry;
            this.parent = parent;
        }

        public ImageAdder width(float w)  { entry.width = w;  return this; }
        public ImageAdder height(float h) { entry.height = h; return this; }

        /** Pivot back to add more content to this column. */
        public HeaderColumnContent text(String content) {
            return parent.text(content);
        }

        public HeaderColumnContent text(String content, Consumer<TextEntry> configure) {
            return parent.text(content, configure);
        }

        public ImageAdder image(String filePath) {
            return parent.image(filePath);
        }
    }

    // -----------------------------------------------------------------------
    // HeaderColumn — stores content for one column
    // -----------------------------------------------------------------------

    static class HeaderColumn {
        float widthPercent;
        List<Object> entries = new ArrayList<>(); // TextEntry or ImageEntry

        HeaderColumn(float widthPercent) {
            this.widthPercent = widthPercent;
        }
    }

    // -----------------------------------------------------------------------
    // HeaderColumnContent — fluent builder for column content
    // -----------------------------------------------------------------------

    public static class HeaderColumnContent {
        private final HeaderColumn column;

        HeaderColumnContent(HeaderColumn column) {
            this.column = column;
        }

        /** Add a styled text entry. */
        public HeaderColumnContent text(String content, Consumer<TextEntry> configure) {
            TextEntry te = new TextEntry(content);
            configure.accept(te);
            column.entries.add(te);
            return this;
        }

        /** Add a text entry with default styling. */
        public HeaderColumnContent text(String content) {
            column.entries.add(new TextEntry(content));
            return this;
        }

        /** Add an image from a file path. */
        public ImageAdder image(String filePath) {
            ImageEntry ie = new ImageEntry();
            ie.imagePath = filePath;
            column.entries.add(ie);
            return new ImageAdder(ie, this);
        }

        /** Add an image from raw bytes. */
        public ImageAdder image(byte[] data, String type) {
            ImageEntry ie = new ImageEntry();
            ie.imageBytes = data;
            ie.imageType  = type;
            column.entries.add(ie);
            return new ImageAdder(ie, this);
        }
    }

    // -----------------------------------------------------------------------
    // HeaderColumnBuilder — top-level builder for column definitions
    // -----------------------------------------------------------------------

    public static class HeaderColumnBuilder {
        final List<HeaderColumn> columns = new ArrayList<>();

        /** Add a column with a width percentage and content. */
        public HeaderColumnBuilder column(float widthPercent,
                                           Consumer<HeaderColumnContent> configure) {
            HeaderColumn col = new HeaderColumn(widthPercent);
            HeaderColumnContent hcc = new HeaderColumnContent(col);
            configure.accept(hcc);
            columns.add(col);
            return this;
        }

        /** Set gutter between columns (not yet customisable — fixed at 12pt). */
        public HeaderColumnBuilder gutter(float pts) {
            // Future: store and use custom gutter
            return this;
        }
    }
}
