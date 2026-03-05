package com.pdfluent.layout;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out components in a fixed set of columns defined by percentage widths.
 *
 * Usage pattern:
 * <pre>
 *   new ColumnLayout()
 *       .column(50, col -> col.add(nameField))
 *       .column(50, col -> col.add(dobField))
 * </pre>
 *
 * The overall height of the ColumnLayout is the height of its tallest column.
 * A small gutter between columns can be configured.
 */
public class ColumnLayout implements Component {

    private final List<ColumnDef> columns  = new ArrayList<>();
    private float gutter                   = 12f;  // gap between columns in points
    private float spaceBefore              = 0f;
    private float spaceAfter               = 8f;

    // -----------------------------------------------------------------------
    // Definition types
    // -----------------------------------------------------------------------

    public interface ColumnConfigurer {
        void configure(StackLayout column);
    }

    private static class ColumnDef {
        final float         widthPercent;
        final StackLayout   content;

        ColumnDef(float widthPercent, StackLayout content) {
            this.widthPercent = widthPercent;
            this.content      = content;
        }
    }

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    public ColumnLayout column(float widthPercent, ColumnConfigurer configurer) {
        StackLayout stack = new StackLayout();
        configurer.configure(stack);
        columns.add(new ColumnDef(widthPercent, stack));
        return this;
    }

    public ColumnLayout gutter(float pts)       { this.gutter = pts;       return this; }
    public ColumnLayout spaceBefore(float pts)  { this.spaceBefore = pts;  return this; }
    public ColumnLayout spaceAfter(float pts)   { this.spaceAfter = pts;   return this; }

    // -----------------------------------------------------------------------
    // Component
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float totalGutter = gutter * (columns.size() - 1);
        float usableWidth = availableWidth - totalGutter;
        float maxHeight   = 0;

        for (ColumnDef col : columns) {
            float colWidth = usableWidth * (col.widthPercent / 100f);
            float h = col.content.measure(ctx, colWidth);
            if (h > maxHeight) maxHeight = h;
        }
        return spaceBefore + maxHeight + spaceAfter;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        float totalGutter = gutter * (columns.size() - 1);
        float usableWidth = width - totalGutter;
        float curX = x;
        float contentY = y + spaceBefore;

        // Save the current cursor so each column starts from the same Y.
        // Each column manages its own internal cursor independently.
        float savedCursorY = ctx.getCursorY();

        for (ColumnDef col : columns) {
            float colWidth = usableWidth * (col.widthPercent / 100f);

            // Temporarily reset cursor to the top of this column row so the
            // StackLayout inside the column renders from the correct position.
            // We do NOT call ctx.advanceCursor() here — the outer StackLayout
            // that contains us will advance by our total measured height.
            col.content.renderInPlace(ctx, curX, contentY, colWidth);

            curX += colWidth + gutter;
        }

        // Restore the cursor — the parent StackLayout will advance it by
        // our measured height after this render() call returns.
        // (No action needed — we didn't modify the cursor.)
    }
}
