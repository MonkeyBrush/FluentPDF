package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A full-featured table component supporting headers, footers, column spanning,
 * cell-level styling overrides, borders, alternating row shading, images in
 * cells, text wrapping/clamping, and multi-page pagination with header repeat.
 *
 * <p>Tables are built via {@link com.pdfluent.builder.TableBuilder}:</p>
 * <pre>
 *   page.table(t -&gt; t
 *       .column(30, col -&gt; col.bold())
 *       .column(70)
 *       .headerRow("Name", "Description")
 *       .row("Widget", "A useful thing")
 *       .alternateRowColor(new Color(245, 245, 245))
 *   );
 * </pre>
 *
 * <p>Styling cascades three levels: table defaults → column defaults → cell
 * overrides.  The table manages its own pagination internally so it can repeat
 * header rows on each new page.</p>
 */
public class TableComponent implements Component {

    // =======================================================================
    // Inner types
    // =======================================================================

    /** Text alignment within a cell. */
    public enum TextAlign { LEFT, CENTER, RIGHT }

    /** Row purpose — determines repeat behaviour and default styling. */
    public enum RowType { HEADER, DATA, FOOTER }

    // -----------------------------------------------------------------------
    // ColumnDef
    // -----------------------------------------------------------------------

    /** Column definition: width percentage and default styling for cells. */
    public static class ColumnDef {
        float widthPercent;
        Float fontSize;
        Boolean bold;
        Boolean italic;
        Color textColor;
        TextAlign alignment;
        Color backgroundColor;
        boolean wrapText = true;

        public ColumnDef(float widthPercent) {
            this.widthPercent = widthPercent;
        }

        public ColumnDef fontSize(float s)       { this.fontSize = s;        return this; }
        public ColumnDef bold()                   { this.bold = true;         return this; }
        public ColumnDef italic()                 { this.italic = true;       return this; }
        public ColumnDef color(Color c)           { this.textColor = c;       return this; }
        public ColumnDef align(TextAlign a)       { this.alignment = a;       return this; }
        public ColumnDef alignLeft()              { return align(TextAlign.LEFT); }
        public ColumnDef alignCenter()            { return align(TextAlign.CENTER); }
        public ColumnDef alignRight()             { return align(TextAlign.RIGHT); }
        public ColumnDef backgroundColor(Color c) { this.backgroundColor = c; return this; }
        public ColumnDef wrapText(boolean w)       { this.wrapText = w;        return this; }
    }

    // -----------------------------------------------------------------------
    // BorderConfig
    // -----------------------------------------------------------------------

    /** Border styling for cells or the whole table. */
    public static class BorderConfig {
        Color color = Color.LIGHT_GRAY;
        float width = 0.5f;
        boolean top = true, right = true, bottom = true, left = true;

        public BorderConfig color(Color c)  { this.color = c;   return this; }
        public BorderConfig width(float w)  { this.width = w;   return this; }
        public BorderConfig top(boolean b)  { this.top = b;     return this; }
        public BorderConfig right(boolean b) { this.right = b;  return this; }
        public BorderConfig bottom(boolean b){ this.bottom = b; return this; }
        public BorderConfig left(boolean b)  { this.left = b;   return this; }
        public BorderConfig none() {
            top = right = bottom = left = false;
            return this;
        }
    }

    // -----------------------------------------------------------------------
    // Cell
    // -----------------------------------------------------------------------

    /** A single table cell with content and optional style overrides. */
    public static class Cell {
        // Content
        String text;
        BufferedImage image;

        // Style overrides (null = inherit from column / table)
        Float fontSize;
        Boolean bold;
        Boolean italic;
        Color textColor;
        TextAlign alignment;
        Color backgroundColor;
        BorderConfig border;
        Boolean wrapText;
        int columnSpan = 1;

        public Cell(String text)          { this.text = text; }
        public Cell(BufferedImage image)  { this.image = image; }

        public Cell fontSize(float s)          { this.fontSize = s;        return this; }
        public Cell bold()                     { this.bold = true;         return this; }
        public Cell italic()                   { this.italic = true;       return this; }
        public Cell color(Color c)             { this.textColor = c;       return this; }
        public Cell align(TextAlign a)         { this.alignment = a;       return this; }
        public Cell alignLeft()                { return align(TextAlign.LEFT); }
        public Cell alignCenter()              { return align(TextAlign.CENTER); }
        public Cell alignRight()               { return align(TextAlign.RIGHT); }
        public Cell backgroundColor(Color c)   { this.backgroundColor = c; return this; }
        public Cell border(Consumer<BorderConfig> cfg) {
            this.border = new BorderConfig();
            cfg.accept(this.border);
            return this;
        }
        public Cell noBorder() {
            this.border = new BorderConfig();
            this.border.none();
            return this;
        }
        public Cell columnSpan(int span)       { this.columnSpan = span;   return this; }
        public Cell wrapText(boolean w)        { this.wrapText = w;        return this; }
    }

    // -----------------------------------------------------------------------
    // Row
    // -----------------------------------------------------------------------

    /** A table row containing cells. */
    public static class Row {
        RowType type = RowType.DATA;
        public final List<Cell> cells = new ArrayList<>();
        Color backgroundColor;
        float minHeight = 0;

        public Row(RowType type) { this.type = type; }

        public Row backgroundColor(Color c) { this.backgroundColor = c; return this; }
        public Row minHeight(float h)        { this.minHeight = h;       return this; }
    }

    // =======================================================================
    // Table state
    // =======================================================================

    private final List<ColumnDef> columns;
    private final List<Row> rows;

    // Table-level defaults
    private float cellPadding = 4f;
    private float spaceBefore = 0f;
    private float spaceAfter  = 8f;
    private BorderConfig borderConfig;
    private Color alternateRowColor;
    private boolean keepTogether = false;
    private boolean repeatHeader = true;

    // Default text styling
    private float defaultFontSize    = 10f;
    private Color defaultTextColor   = Color.BLACK;
    private Color headerBgColor      = new Color(230, 230, 230);
    private float headerFontSize     = 10f;

    // =======================================================================
    // Constructor
    // =======================================================================

    public TableComponent(List<ColumnDef> columns, List<Row> rows) {
        this.columns = columns;
        this.rows    = rows;
    }

    // -----------------------------------------------------------------------
    // Setters (called by TableBuilder)
    // -----------------------------------------------------------------------

    public void setCellPadding(float p)          { this.cellPadding = p; }
    public void setSpaceBefore(float s)          { this.spaceBefore = s; }
    public void setSpaceAfter(float s)           { this.spaceAfter = s; }
    public void setBorderConfig(BorderConfig b)  { this.borderConfig = b; }
    public void setAlternateRowColor(Color c)    { this.alternateRowColor = c; }
    public void setKeepTogether(boolean k)       { this.keepTogether = k; }
    public void setRepeatHeader(boolean r)       { this.repeatHeader = r; }
    public void setDefaultFontSize(float s)      { this.defaultFontSize = s; }
    public void setDefaultTextColor(Color c)     { this.defaultTextColor = c; }
    public void setHeaderBgColor(Color c)        { this.headerBgColor = c; }
    public void setHeaderFontSize(float s)       { this.headerFontSize = s; }

    // =======================================================================
    // Component contract
    // =======================================================================

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float[] colWidths = resolveColumnWidths(availableWidth);
        float total = spaceBefore + spaceAfter;
        for (Row row : rows) {
            total += measureRow(ctx, row, colWidths);
        }
        return total;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            renderTable(ctx, x, y, width);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render table: " + e.getMessage(), e);
        }
    }

    // =======================================================================
    // Core rendering
    // =======================================================================

    private void renderTable(RenderContext ctx, float x, float y, float width)
            throws IOException {

        float[] colWidths = resolveColumnWidths(width);
        float totalWidth  = sumWidths(colWidths);
        float pageHeight  = ctx.getContentHeight();

        // Keep-together: if table doesn't fit, jump to next page
        if (keepTogether) {
            float fullHeight = measure(ctx, width);
            if (fullHeight > ctx.remainingHeight() && fullHeight <= pageHeight) {
                ctx.newPage();
                y = 0;
            }
        }

        float currentY = y + spaceBefore;

        // Separate header, data, and footer rows
        List<Row> headerRows = new ArrayList<>();
        List<Row> dataRows   = new ArrayList<>();
        List<Row> footerRows = new ArrayList<>();
        for (Row row : rows) {
            switch (row.type) {
                case HEADER -> headerRows.add(row);
                case DATA   -> dataRows.add(row);
                case FOOTER -> footerRows.add(row);
            }
        }

        // Render header rows on first page
        for (Row hdr : headerRows) {
            float rh = measureRow(ctx, hdr, colWidths);
            renderRow(ctx, x, currentY, colWidths, totalWidth, hdr, -1);
            currentY += rh;
        }

        // Render data rows with pagination
        int dataRowIndex = 0;
        for (Row dataRow : dataRows) {
            float rh = measureRow(ctx, dataRow, colWidths);

            // Check if row fits: remaining space below currentY
            if (rh > (pageHeight - currentY) && rh <= pageHeight) {
                ctx.newPage();
                currentY = 0;

                // Re-render header on new page
                if (repeatHeader) {
                    for (Row hdr : headerRows) {
                        float hh = measureRow(ctx, hdr, colWidths);
                        renderRow(ctx, x, currentY, colWidths, totalWidth, hdr, -1);
                        currentY += hh;
                    }
                }
            }

            renderRow(ctx, x, currentY, colWidths, totalWidth, dataRow, dataRowIndex);
            currentY += rh;
            dataRowIndex++;
        }

        // Render footer rows
        for (Row ftr : footerRows) {
            float rh = measureRow(ctx, ftr, colWidths);

            if (rh > (pageHeight - currentY) && rh <= pageHeight) {
                ctx.newPage();
                currentY = 0;
            }

            renderRow(ctx, x, currentY, colWidths, totalWidth, ftr, -1);
            currentY += rh;
        }
    }

    // =======================================================================
    // Row rendering
    // =======================================================================

    /**
     * @param dataRowIndex 0-based index among data rows only, or -1 for header/footer
     */
    private void renderRow(RenderContext ctx, float x, float y,
                           float[] colWidths, float totalWidth,
                           Row row, int dataRowIndex) throws IOException {

        float rowHeight = measureRow(ctx, row, colWidths);

        // ── Background fill ───────────────────────────────────────────────
        Color bg = resolveRowBackground(row, dataRowIndex);
        if (bg != null) {
            ctx.drawFilledRect(x, y, totalWidth, rowHeight, bg);
        }

        // ── Cells ─────────────────────────────────────────────────────────
        float cellX = x;
        int colIndex = 0;

        for (Cell cell : row.cells) {
            int span = Math.max(1, cell.columnSpan);
            float cellWidth = 0;
            for (int s = 0; s < span && (colIndex + s) < colWidths.length; s++) {
                cellWidth += colWidths[colIndex + s];
            }

            // Per-cell background (overrides row background)
            Color cellBg = resolveCellBackground(cell, row, dataRowIndex);
            if (cellBg != null && cellBg != bg) {
                ctx.drawFilledRect(cellX, y, cellWidth, rowHeight, cellBg);
            }

            // Content
            renderCellContent(ctx, cellX, y, cellWidth, rowHeight, cell,
                              colIndex < columns.size() ? columns.get(colIndex) : null,
                              row);

            // Cell border
            renderCellBorder(ctx, cellX, y, cellWidth, rowHeight, cell);

            cellX    += cellWidth;
            colIndex += span;
        }
    }

    // =======================================================================
    // Cell content rendering
    // =======================================================================

    private void renderCellContent(RenderContext ctx, float x, float y,
                                    float cellWidth, float cellHeight,
                                    Cell cell, ColumnDef col, Row row)
            throws IOException {

        float pad = cellPadding;
        float contentX = x + pad;
        float contentW = cellWidth - 2 * pad;
        float contentH = cellHeight - 2 * pad;

        if (cell.image != null) {
            renderCellImage(ctx, contentX, y + pad, contentW, contentH, cell);
        } else if (cell.text != null) {
            renderCellText(ctx, contentX, y + pad, contentW, contentH, cell, col, row);
        }
    }

    private void renderCellText(RenderContext ctx, float x, float y,
                                 float width, float height,
                                 Cell cell, ColumnDef col, Row row)
            throws IOException {

        // Resolve effective style
        float fontSize = resolveFloat(cell.fontSize,
                col != null ? col.fontSize : null,
                row.type == RowType.HEADER ? headerFontSize : defaultFontSize);
        boolean bold   = resolveBool(cell.bold,
                col != null ? col.bold : null,
                row.type == RowType.HEADER);
        boolean italic = resolveBool(cell.italic,
                col != null ? col.italic : null, false);
        Color textColor = resolveColor(cell.textColor,
                col != null ? col.textColor : null, defaultTextColor);
        TextAlign align = resolveAlign(cell.alignment,
                col != null ? col.alignment : null, TextAlign.LEFT);
        boolean wrap = resolveBool(cell.wrapText,
                col != null ? col.wrapText : null, true);

        PDFont font = resolveFont(bold, italic);

        // Build lines
        List<String> lines;
        if (wrap) {
            lines = wrapText(ctx, cell.text, font, fontSize, width);
        } else {
            lines = List.of(clampText(ctx, cell.text, font, fontSize, width));
        }

        float lineHeight = fontSize * 1.3f;
        float totalTextHeight = lines.size() * lineHeight;

        // Vertically center text in cell
        float startY = y + Math.max(0, (height - totalTextHeight) / 2f);

        for (String line : lines) {
            float drawX = x;
            try {
                float lineWidth = ctx.getTextWidth(line, font, fontSize);
                drawX = switch (align) {
                    case CENTER -> x + (width - lineWidth) / 2f;
                    case RIGHT  -> x + width - lineWidth;
                    default     -> x;
                };
            } catch (IOException ignored) {}

            ctx.drawText(line, font, fontSize, textColor, drawX, startY);
            startY += lineHeight;
        }
    }

    private void renderCellImage(RenderContext ctx, float x, float y,
                                  float width, float height, Cell cell)
            throws IOException {
        PDImageXObject img = LosslessFactory.createFromImage(
                ctx.getDocument(), cell.image);

        float natW = img.getWidth();
        float natH = img.getHeight();

        // Scale to fit within cell content area preserving aspect ratio
        float scale = Math.min(width / natW, height / natH);
        float drawW = natW * scale;
        float drawH = natH * scale;

        // Center in cell
        float drawX = x + (width - drawW) / 2f;
        float drawY = y + (height - drawH) / 2f;

        ctx.drawImage(img, drawX, drawY, drawW, drawH);
    }

    // =======================================================================
    // Cell border rendering
    // =======================================================================

    private void renderCellBorder(RenderContext ctx, float x, float y,
                                   float width, float height, Cell cell)
            throws IOException {

        BorderConfig bc = cell.border != null ? cell.border : borderConfig;
        if (bc == null) return;
        if (!bc.top && !bc.right && !bc.bottom && !bc.left) return;

        Color c = bc.color;
        float w = bc.width;

        if (bc.top)    ctx.drawHorizontalLine(x, y, width, c, w);
        if (bc.bottom) ctx.drawHorizontalLine(x, y + height, width, c, w);
        if (bc.left)   ctx.drawVerticalLine(x, y, height, c, w);
        if (bc.right)  ctx.drawVerticalLine(x + width, y, height, c, w);
    }

    // =======================================================================
    // Measurement
    // =======================================================================

    private float[] resolveColumnWidths(float totalWidth) {
        float[] widths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = totalWidth * (columns.get(i).widthPercent / 100f);
        }
        return widths;
    }

    private float measureRow(RenderContext ctx, Row row, float[] colWidths) {
        float maxH = row.minHeight;
        int colIndex = 0;

        for (Cell cell : row.cells) {
            int span = Math.max(1, cell.columnSpan);
            float cellWidth = 0;
            for (int s = 0; s < span && (colIndex + s) < colWidths.length; s++) {
                cellWidth += colWidths[colIndex + s];
            }

            float cellH = measureCellContent(ctx, cell,
                    colIndex < columns.size() ? columns.get(colIndex) : null,
                    row, cellWidth);
            if (cellH > maxH) maxH = cellH;

            colIndex += span;
        }
        return maxH;
    }

    private float measureCellContent(RenderContext ctx, Cell cell,
                                      ColumnDef col, Row row, float cellWidth) {
        float pad = cellPadding;
        float contentW = cellWidth - 2 * pad;

        if (cell.image != null) {
            // Image: use natural aspect ratio scaled to fit width, capped at 100pt
            float natW = cell.image.getWidth();
            float natH = cell.image.getHeight();
            float scale = Math.min(contentW / natW, 1f);
            float h = natH * scale;
            return Math.min(h, 100f) + 2 * pad;
        }

        if (cell.text == null || cell.text.isEmpty()) {
            return 2 * pad + resolveFloat(cell.fontSize,
                    col != null ? col.fontSize : null,
                    row.type == RowType.HEADER ? headerFontSize : defaultFontSize) * 1.3f;
        }

        // Text measurement
        float fontSize = resolveFloat(cell.fontSize,
                col != null ? col.fontSize : null,
                row.type == RowType.HEADER ? headerFontSize : defaultFontSize);
        boolean bold = resolveBool(cell.bold,
                col != null ? col.bold : null,
                row.type == RowType.HEADER);
        boolean italic = resolveBool(cell.italic,
                col != null ? col.italic : null, false);
        boolean wrap = resolveBool(cell.wrapText,
                col != null ? col.wrapText : null, true);

        PDFont font = resolveFont(bold, italic);

        int lineCount;
        if (wrap) {
            lineCount = wrapText(ctx, cell.text, font, fontSize, contentW).size();
        } else {
            lineCount = 1;
        }

        return lineCount * fontSize * 1.3f + 2 * pad;
    }

    // =======================================================================
    // Style resolution helpers
    // =======================================================================

    private Color resolveRowBackground(Row row, int dataRowIndex) {
        if (row.type == RowType.HEADER && headerBgColor != null) {
            return headerBgColor;
        }
        if (row.backgroundColor != null) return row.backgroundColor;
        if (dataRowIndex >= 0 && alternateRowColor != null && dataRowIndex % 2 == 1) {
            return alternateRowColor;
        }
        return null;
    }

    private Color resolveCellBackground(Cell cell, Row row, int dataRowIndex) {
        if (cell.backgroundColor != null) return cell.backgroundColor;
        return resolveRowBackground(row, dataRowIndex);
    }

    private static float resolveFloat(Float cellVal, Float colVal, float tableDefault) {
        if (cellVal != null) return cellVal;
        if (colVal != null) return colVal;
        return tableDefault;
    }

    private static boolean resolveBool(Boolean cellVal, Boolean colVal, boolean tableDefault) {
        if (cellVal != null) return cellVal;
        if (colVal != null) return colVal;
        return tableDefault;
    }

    private static Color resolveColor(Color cellVal, Color colVal, Color tableDefault) {
        if (cellVal != null) return cellVal;
        if (colVal != null) return colVal;
        return tableDefault;
    }

    private static TextAlign resolveAlign(TextAlign cellVal, TextAlign colVal,
                                           TextAlign tableDefault) {
        if (cellVal != null) return cellVal;
        if (colVal != null) return colVal;
        return tableDefault;
    }

    private static PDFont resolveFont(boolean bold, boolean italic) {
        if (bold && italic) return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
        if (bold)           return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        if (italic)         return new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    // =======================================================================
    // Text wrapping & clamping
    // =======================================================================

    private static List<String> wrapText(RenderContext ctx, String text,
                                          PDFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            try {
                float w = ctx.getTextWidth(candidate, font, fontSize);
                if (w > maxWidth && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            } catch (IOException e) {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static String clampText(RenderContext ctx, String text,
                                     PDFont font, float fontSize, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        try {
            float w = ctx.getTextWidth(text, font, fontSize);
            if (w <= maxWidth) return text;

            // Binary search for truncation point
            String ellipsis = "...";
            float ellipsisW = ctx.getTextWidth(ellipsis, font, fontSize);
            float available = maxWidth - ellipsisW;

            for (int i = text.length() - 1; i >= 0; i--) {
                String sub = text.substring(0, i);
                if (ctx.getTextWidth(sub, font, fontSize) <= available) {
                    return sub + ellipsis;
                }
            }
            return ellipsis;
        } catch (IOException e) {
            return text;
        }
    }

    // =======================================================================
    // Utility
    // =======================================================================

    private static float sumWidths(float[] widths) {
        float sum = 0;
        for (float w : widths) sum += w;
        return sum;
    }
}
