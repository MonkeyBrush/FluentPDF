package com.pdfluent.builder;

import com.pdfluent.core.PageSettings;
import com.pdfluent.core.RenderContext;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Top-level entry point for PDFluent.
 *
 * <pre>
 * Document.create(doc -> doc
 *     .page(PageSettings.a4().margin(36).build(), page -> page
 *         .text("My Document Title").bold().fontSize(18).alignCenter()
 *         .line()
 *         .spacer(12)
 *         .columns(cols -> cols
 *             .column(50, left -> left
 *                 .text("Left column content")
 *             )
 *             .column(50, right -> right
 *                 .text("Right column content")
 *             )
 *         )
 *     )
 * ).save("output.pdf");
 * </pre>
 */
public class Document {

    private final List<PageDef> pages = new ArrayList<>();

    private Document() {}

    // -----------------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------------

    public static Document create(Consumer<Document> configure) {
        Document doc = new Document();
        configure.accept(doc);
        return doc;
    }

    // -----------------------------------------------------------------------
    // Fluent page API
    // -----------------------------------------------------------------------

    /**
     * Add a page with the given settings and configure its content.
     */
    public Document page(PageSettings settings, Consumer<ContentBuilder> configure) {
        ContentBuilder builder = new ContentBuilder();
        configure.accept(builder);
        pages.add(new PageDef(settings, builder));
        return this;
    }

    /**
     * Convenience overload — uses A4 with 36pt margins.
     */
    public Document page(Consumer<ContentBuilder> configure) {
        return page(PageSettings.a4().margin(36).build(), configure);
    }

    // -----------------------------------------------------------------------
    // Output
    // -----------------------------------------------------------------------

    public void save(String filePath) throws IOException {
        save(new File(filePath));
    }

    public void save(File file) throws IOException {
        try (PDDocument pdDoc = buildPDDocument()) {
            pdDoc.save(file);
        }
    }

    public void save(OutputStream out) throws IOException {
        try (PDDocument pdDoc = buildPDDocument()) {
            pdDoc.save(out);
        }
    }

    // -----------------------------------------------------------------------
    // Internal rendering
    // -----------------------------------------------------------------------

    private PDDocument buildPDDocument() throws IOException {
        PDDocument pdDoc = new PDDocument();

        for (PageDef pageDef : pages) {
            RenderContext ctx = new RenderContext(pdDoc, pageDef.settings);
            pageDef.builder.stack.render(ctx, 0, 0, ctx.getContentWidth());
            ctx.close();
        }

        return pdDoc;
    }

    // -----------------------------------------------------------------------

    private static class PageDef {
        final PageSettings   settings;
        final ContentBuilder builder;

        PageDef(PageSettings settings, ContentBuilder builder) {
            this.settings = settings;
            this.builder  = builder;
        }
    }
}
