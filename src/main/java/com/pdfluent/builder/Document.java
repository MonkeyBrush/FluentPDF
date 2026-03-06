package com.pdfluent.builder;

import com.pdfluent.core.PageSettings;
import com.pdfluent.core.RenderContext;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
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
    private Header header;
    private Footer footer;

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
    // Header
    // -----------------------------------------------------------------------

    /**
     * Configure a header that is stamped onto pages during the second render
     * pass.  Supports images, multiline text, column layouts, and display
     * modes ({@code showAll}, {@code showOnce}, {@code showEven}, {@code showOdd}).
     *
     * <pre>
     *   Document.create(doc -&gt; doc
     *       .header(h -&gt; h
     *           .height(60).showAll().underline()
     *           .columns(cols -&gt; cols
     *               .column(30, col -&gt; col.image("logo.png").width(80).height(40))
     *               .column(70, col -&gt; col
     *                   .text("My Company", tc -&gt; tc.bold().fontSize(14))
     *                   .text("ABN: 12 345 678 901", tc -&gt; tc.fontSize(9).color(Color.GRAY))
     *               )
     *           )
     *       )
     *       .page(page -&gt; page.text("Hello"))
     *   ).save("output.pdf");
     * </pre>
     */
    public Document header(Consumer<Header> configure) {
        Header h = new Header();
        configure.accept(h);
        this.header = h;
        return this;
    }

    // -----------------------------------------------------------------------
    // Footer
    // -----------------------------------------------------------------------

    /**
     * Configure a footer that appears on every page of the document.
     *
     * The footer supports left, centre, and right sections, each of which
     * can contain the placeholders {@code {page}} and {@code {totalPages}}.
     *
     * <pre>
     *   Document.create(doc -> doc
     *       .footer(f -> f.center("Page {page} of {totalPages}").fontSize(8))
     *       .page(page -> page.text("Hello"))
     *   ).save("output.pdf");
     * </pre>
     */
    public Document footer(Consumer<Footer> configure) {
        Footer f = new Footer();
        configure.accept(f);
        this.footer = f;
        return this;
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

    /**
     * Render the document and return the PDF as a byte array.
     *
     * Ideal for web applications where the PDF is written straight to an
     * HTTP response:
     * <pre>
     *   byte[] pdf = Document.create(doc -> doc
     *       .page(page -> page.text("Hello"))
     *   ).toByteArray();
     *
     *   // Spring MVC
     *   return ResponseEntity.ok()
     *       .contentType(MediaType.APPLICATION_PDF)
     *       .header("Content-Disposition", "attachment; filename=\"report.pdf\"")
     *       .body(pdf);
     *
     *   // Servlet
     *   response.setContentType("application/pdf");
     *   response.getOutputStream().write(pdf);
     * </pre>
     */
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        save(baos);
        return baos.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Internal rendering
    // -----------------------------------------------------------------------

    private PDDocument buildPDDocument() throws IOException {
        PDDocument pdDoc = new PDDocument();

        // Pass 1: render all page content (may create extra pages via auto-pagination)
        for (PageDef pageDef : pages) {
            RenderContext ctx = new RenderContext(pdDoc, pageDef.settings);
            pageDef.builder.stack.render(ctx, 0, 0, ctx.getContentWidth());
            ctx.close();
        }

        // Pass 2: stamp header and footer (now that total page count is known)
        if (!pages.isEmpty()) {
            PageSettings settings = pages.get(0).settings;

            if (header != null) {
                header.render(pdDoc, settings);
            }

            if (footer != null) {
                footer.render(pdDoc,
                        settings.getMarginLeft(),
                        settings.getMarginRight(),
                        settings.getMarginBottom());
            }
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
