package com.pdfluent.layout;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays out components vertically, one after another.
 * Handles page breaks automatically: before rendering each child it checks
 * whether the child fits in the remaining space; if not, it triggers a new page.
 *
 * This is the root container typically used for a full page of content.
 */
public class StackLayout implements Component {

    private final List<Component> children = new ArrayList<>();

    public List<Component> getChildren() { return children; }

    public StackLayout add(Component component) {
        children.add(component);
        return this;
    }

    // -----------------------------------------------------------------------
    // Component
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float total = 0;
        for (Component c : children) {
            total += c.measure(ctx, availableWidth);
        }
        return total;
    }

    /**
     * Render children at a fixed position WITHOUT advancing the shared cursor.
     * Used by ColumnLayout so each column starts from the same Y coordinate.
     */
    public void renderInPlace(RenderContext ctx, float x, float y, float width) {
        float curY = y;
        for (Component child : children) {
            float childHeight = child.measure(ctx, width);
            child.render(ctx, x, curY, width);
            curY += childHeight;
        }
    }

    /**
     * Render all children top-to-bottom, creating new pages as needed.
     *
     * Note: x and y here are content-space coordinates (relative to the
     * content area, not the absolute page). The RenderContext handles
     * the translation to PDFBox coordinates.
     */
    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            float cursorY = ctx.getCursorY();

            for (Component child : children) {
                float childHeight = child.measure(ctx, width);

                // If the child doesn't fit on the remaining page, move to the next page.
                // Exception: if a single component is taller than a full page we render
                // it anyway (the component itself is responsible for handling that edge case).
                if (childHeight <= ctx.getContentHeight()
                        && ctx.remainingHeight() < childHeight) {
                    ctx.newPage();
                    cursorY = 0;
                }

                child.render(ctx, x, cursorY, width);
                cursorY += childHeight;
                ctx.advanceCursor(childHeight);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
