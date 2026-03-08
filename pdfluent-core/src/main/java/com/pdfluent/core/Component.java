package com.pdfluent.core;

/**
 * Base interface for all renderable PDF components.
 *
 * Rendering is a two-pass process:
 *   1. measure() — report preferred height so the layout engine can paginate
 *   2. render()  — draw onto the page at the assigned position
 */
public interface Component {

    /**
     * Returns the height this component needs given a fixed available width.
     * Called before render() so the layout engine can decide if a page break
     * is required before drawing.
     *
     * @param ctx            render context (gives access to fonts etc.)
     * @param availableWidth width available in points
     * @return required height in points
     */
    float measure(RenderContext ctx, float availableWidth);

    /**
     * Draw this component.
     *
     * @param ctx    render context wrapping the PDFBox content stream
     * @param x      left edge in points (from left of page)
     * @param y      top edge in points  (from top of page — decreasing downwards)
     * @param width  allocated width in points
     */
    void render(RenderContext ctx, float x, float y, float width);
}
