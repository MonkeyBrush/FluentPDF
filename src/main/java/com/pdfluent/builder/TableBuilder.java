package com.pdfluent.builder;

import com.pdfluent.components.TableComponent;
import com.pdfluent.components.TableComponent.BorderConfig;
import com.pdfluent.components.TableComponent.Cell;
import com.pdfluent.components.TableComponent.ColumnDef;
import com.pdfluent.components.TableComponent.Row;
import com.pdfluent.components.TableComponent.RowType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for constructing {@link TableComponent} instances.
 *
 * <p>Usage:</p>
 * <pre>
 *   page.table(t -&gt; t
 *       .column(30, col -&gt; col.bold())
 *       .column(40)
 *       .column(30, col -&gt; col.alignRight())
 *
 *       .headerRow("ID", "Item", "Price")
 *
 *       .row("001", "Widget A", "$29.99")
 *       .row("002", "Widget B", "$49.99")
 *       .row(r -&gt; r
 *           .cell("Total", c -&gt; c.columnSpan(2).bold().alignRight())
 *           .cell("$79.98", c -&gt; c.bold())
 *       )
 *
 *       .alternateRowColor(new Color(245, 245, 245))
 *       .cellPadding(5)
 *       .repeatHeader(true)
 *   );
 * </pre>
 */
public class TableBuilder {

    private final List<ColumnDef> columns = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    // Table-level style
    private float cellPadding = 4f;
    private float spaceBefore = 0f;
    private float spaceAfter  = 8f;
    private BorderConfig borderConfig;
    private Color alternateRowColor;
    private boolean keepTogether = false;
    private boolean repeatHeader = true;
    private Color headerBgColor = new Color(230, 230, 230);
    private float headerFontSize = 10f;
    private float defaultFontSize = 10f;
    private Color defaultTextColor = Color.BLACK;

    // -----------------------------------------------------------------------
    // Column definition
    // -----------------------------------------------------------------------

    /** Add a column with a width percentage and default styling. */
    public TableBuilder column(float widthPercent) {
        columns.add(new ColumnDef(widthPercent));
        return this;
    }

    /** Add a column with a width percentage and custom defaults. */
    public TableBuilder column(float widthPercent, Consumer<ColumnDef> configure) {
        ColumnDef col = new ColumnDef(widthPercent);
        configure.accept(col);
        columns.add(col);
        return this;
    }

    // -----------------------------------------------------------------------
    // Header rows
    // -----------------------------------------------------------------------

    /** Add a header row with simple text cells. */
    public TableBuilder headerRow(String... cellTexts) {
        Row row = new Row(RowType.HEADER);
        for (String text : cellTexts) {
            row.cells.add(new Cell(text));
        }
        rows.add(row);
        return this;
    }

    /** Add a header row with full cell control. */
    public TableBuilder headerRow(Consumer<RowBuilder> configure) {
        Row row = new Row(RowType.HEADER);
        RowBuilder rb = new RowBuilder(row);
        configure.accept(rb);
        rows.add(row);
        return this;
    }

    // -----------------------------------------------------------------------
    // Data rows
    // -----------------------------------------------------------------------

    /** Add a data row with simple text cells. */
    public TableBuilder row(String... cellTexts) {
        Row row = new Row(RowType.DATA);
        for (String text : cellTexts) {
            row.cells.add(new Cell(text));
        }
        rows.add(row);
        return this;
    }

    /** Add a data row with full cell control. */
    public TableBuilder row(Consumer<RowBuilder> configure) {
        Row row = new Row(RowType.DATA);
        RowBuilder rb = new RowBuilder(row);
        configure.accept(rb);
        rows.add(row);
        return this;
    }

    // -----------------------------------------------------------------------
    // Footer rows
    // -----------------------------------------------------------------------

    /** Add a footer row with simple text cells. */
    public TableBuilder footerRow(String... cellTexts) {
        Row row = new Row(RowType.FOOTER);
        for (String text : cellTexts) {
            row.cells.add(new Cell(text));
        }
        rows.add(row);
        return this;
    }

    /** Add a footer row with full cell control. */
    public TableBuilder footerRow(Consumer<RowBuilder> configure) {
        Row row = new Row(RowType.FOOTER);
        RowBuilder rb = new RowBuilder(row);
        configure.accept(rb);
        rows.add(row);
        return this;
    }

    // -----------------------------------------------------------------------
    // Table-level style
    // -----------------------------------------------------------------------

    /** Set internal cell padding in points (default 4). */
    public TableBuilder cellPadding(float pts) {
        this.cellPadding = pts;
        return this;
    }

    /** Configure table-wide border. */
    public TableBuilder border(Consumer<BorderConfig> configure) {
        this.borderConfig = new BorderConfig();
        configure.accept(this.borderConfig);
        return this;
    }

    /** Disable all borders. */
    public TableBuilder noBorder() {
        this.borderConfig = new BorderConfig();
        this.borderConfig.none();
        return this;
    }

    /** Set alternating data row background colour. */
    public TableBuilder alternateRowColor(Color color) {
        this.alternateRowColor = color;
        return this;
    }

    /** Set header row background colour (default light grey). */
    public TableBuilder headerBackground(Color color) {
        this.headerBgColor = color;
        return this;
    }

    /** Set header font size (default 10). */
    public TableBuilder headerFontSize(float pts) {
        this.headerFontSize = pts;
        return this;
    }

    /** Set default font size for data cells (default 10). */
    public TableBuilder fontSize(float pts) {
        this.defaultFontSize = pts;
        return this;
    }

    /** Set default text colour (default black). */
    public TableBuilder textColor(Color color) {
        this.defaultTextColor = color;
        return this;
    }

    /** Keep the entire table on one page if it fits (default false). */
    public TableBuilder keepTogether(boolean keep) {
        this.keepTogether = keep;
        return this;
    }

    /** Repeat header rows on each new page (default true). */
    public TableBuilder repeatHeader(boolean repeat) {
        this.repeatHeader = repeat;
        return this;
    }

    /** Space before the table (default 0). */
    public TableBuilder spaceBefore(float pts) {
        this.spaceBefore = pts;
        return this;
    }

    /** Space after the table (default 8). */
    public TableBuilder spaceAfter(float pts) {
        this.spaceAfter = pts;
        return this;
    }

    // -----------------------------------------------------------------------
    // Build
    // -----------------------------------------------------------------------

    /** Construct the {@link TableComponent}. */
    public TableComponent build() {
        TableComponent table = new TableComponent(columns, rows);
        table.setCellPadding(cellPadding);
        table.setSpaceBefore(spaceBefore);
        table.setSpaceAfter(spaceAfter);
        table.setBorderConfig(borderConfig);
        table.setAlternateRowColor(alternateRowColor);
        table.setKeepTogether(keepTogether);
        table.setRepeatHeader(repeatHeader);
        table.setHeaderBgColor(headerBgColor);
        table.setHeaderFontSize(headerFontSize);
        table.setDefaultFontSize(defaultFontSize);
        table.setDefaultTextColor(defaultTextColor);
        return table;
    }

    // =======================================================================
    // RowBuilder
    // =======================================================================

    /**
     * Fluent builder for adding cells to a row.
     */
    public static class RowBuilder {

        private final Row row;

        RowBuilder(Row row) {
            this.row = row;
        }

        /** Add a text cell. */
        public RowBuilder cell(String text) {
            row.cells.add(new Cell(text));
            return this;
        }

        /** Add a text cell with custom styling. */
        public RowBuilder cell(String text, Consumer<Cell> configure) {
            Cell cell = new Cell(text);
            configure.accept(cell);
            row.cells.add(cell);
            return this;
        }

        /** Add an image cell. */
        public RowBuilder cell(BufferedImage image) {
            row.cells.add(new Cell(image));
            return this;
        }

        /** Add an image cell with custom styling. */
        public RowBuilder cell(BufferedImage image, Consumer<Cell> configure) {
            Cell cell = new Cell(image);
            configure.accept(cell);
            row.cells.add(cell);
            return this;
        }

        /** Set the row's background colour. */
        public RowBuilder backgroundColor(Color color) {
            row.backgroundColor(color);
            return this;
        }

        /** Set the row's minimum height. */
        public RowBuilder minHeight(float pts) {
            row.minHeight(pts);
            return this;
        }
    }
}
