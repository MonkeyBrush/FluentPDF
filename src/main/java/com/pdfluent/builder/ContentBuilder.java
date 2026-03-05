package com.pdfluent.builder;

import com.pdfluent.components.*;
import com.pdfluent.layout.ColumnLayout;
import com.pdfluent.layout.StackLayout;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Fluent builder for composing PDF content.
 *
 * Every method returns {@code this} so calls can be chained.
 * The builder accumulates components into an underlying {@link StackLayout}.
 *
 * Example:
 * <pre>
 *   Document.create(doc -> doc
 *       .page(PageSettings.a4().margin(36).build(), page -> page
 *           .text("Patient Intake Form").bold().fontSize(16).alignCenter()
 *           .line()
 *           .spacer(8)
 *           .columns(cols -> cols
 *               .column(50, col -> col
 *                   .text("First Name").bold()
 *                   .text("________________")
 *               )
 *               .column(50, col -> col
 *                   .text("Last Name").bold()
 *                   .text("________________")
 *               )
 *           )
 *           .text("Gender")
 *           .radioGroup(rg -> rg
 *               .option("Male").option("Female").option("Other")
 *               .horizontal()
 *           )
 *       )
 *   );
 * </pre>
 */
public class ContentBuilder {

    final StackLayout stack = new StackLayout();

    // -----------------------------------------------------------------------
    // Text
    // -----------------------------------------------------------------------

    /**
     * Add a text paragraph.  Returns the {@link TextComponent} directly so
     * callers can chain text-specific options (.bold(), .fontSize(), etc.)
     * before adding the next component.
     *
     * <pre>
     *   page.text("Hello World").bold().fontSize(14).alignCenter()
     *       .text("Next paragraph here");
     * </pre>
     *
     * To keep chaining on the ContentBuilder after configuring the text
     * component, use {@link #text(String, Consumer)} instead.
     */
    public TextAdder text(String content) {
        TextComponent tc = new TextComponent(content);
        stack.add(tc);
        return new TextAdder(tc, this);
    }

    /**
     * Add a text paragraph and configure it via a consumer, returning this
     * builder for continued chaining.
     */
    public ContentBuilder text(String content, Consumer<TextComponent> configure) {
        TextComponent tc = new TextComponent(content);
        configure.accept(tc);
        stack.add(tc);
        return this;
    }

    // -----------------------------------------------------------------------
    // Line / Spacer
    // -----------------------------------------------------------------------

    public LineAdder line() {
        LineComponent lc = new LineComponent();
        stack.add(lc);
        return new LineAdder(lc, this);
    }

    public ContentBuilder spacer(float points) {
        stack.add(new SpacerComponent(points));
        return this;
    }

    // -----------------------------------------------------------------------
    // Radio & Checkbox
    // -----------------------------------------------------------------------

    public ContentBuilder radioGroup(Consumer<RadioGroupComponent> configure) {
        RadioGroupComponent rg = new RadioGroupComponent();
        configure.accept(rg);
        stack.add(rg);
        return this;
    }

    public ContentBuilder checkboxGroup(Consumer<CheckboxGroupComponent> configure) {
        CheckboxGroupComponent cg = new CheckboxGroupComponent();
        configure.accept(cg);
        stack.add(cg);
        return this;
    }

    // -----------------------------------------------------------------------
    // Column layout
    // -----------------------------------------------------------------------

    public ContentBuilder columns(Consumer<ColumnLayoutBuilder> configure) {
        ColumnLayoutBuilder clb = new ColumnLayoutBuilder();
        configure.accept(clb);
        stack.add(clb.layout);
        return this;
    }

    // -----------------------------------------------------------------------
    // Inner builders — allow text/line to chain back to ContentBuilder
    // -----------------------------------------------------------------------

    /**
     * Returned by {@link #text(String)} so the caller can set text options
     * AND then continue adding more components to the page.
     */
    public static class TextAdder extends TextComponent {

        private final ContentBuilder parent;

        TextAdder(TextComponent delegate, ContentBuilder parent) {
            super(""); // won't be used — we re-delegate
            this.parent = parent;
            // Copy delegate into stack — already done by the parent builder.
            // This class is a thin façade that forward-declares the text
            // component options while also providing access back to the parent.
        }

        // Override all TextComponent fluent methods to return TextAdder so
        // the chain stays on this type until the caller pivots back.
        @Override public TextAdder fontSize(float s)      { super.fontSize(s);   return this; }
        @Override public TextAdder bold()                  { super.bold();        return this; }
        @Override public TextAdder italic()                { super.italic();      return this; }
        @Override public TextAdder alignCenter()           { super.alignCenter(); return this; }
        @Override public TextAdder alignRight()            { super.alignRight();  return this; }
        @Override public TextAdder color(Color c)          { super.color(c);      return this; }
        @Override public TextAdder spaceBefore(float pts)  { super.spaceBefore(pts); return this; }
        @Override public TextAdder spaceAfter(float pts)   { super.spaceAfter(pts);  return this; }

        // Pivot back to ContentBuilder
        public TextAdder   text(String s)                          { return parent.text(s); }
        public LineAdder   line()                                  { return parent.line(); }
        public ContentBuilder spacer(float pts)                    { return parent.spacer(pts); }
        public ContentBuilder radioGroup(Consumer<RadioGroupComponent> c)   { return parent.radioGroup(c); }
        public ContentBuilder checkboxGroup(Consumer<CheckboxGroupComponent> c) { return parent.checkboxGroup(c); }
        public ContentBuilder columns(Consumer<ColumnLayoutBuilder> c)     { return parent.columns(c); }
    }

    public static class LineAdder extends LineComponent {

        private final ContentBuilder parent;

        LineAdder(LineComponent delegate, ContentBuilder parent) {
            this.parent = parent;
        }

        @Override public LineAdder color(Color c)         { super.color(c);       return this; }
        @Override public LineAdder thickness(float t)     { super.thickness(t);   return this; }
        @Override public LineAdder spaceBefore(float pts) { super.spaceBefore(pts); return this; }
        @Override public LineAdder spaceAfter(float pts)  { super.spaceAfter(pts);  return this; }

        // Pivot back
        public TextAdder   text(String s)                          { return parent.text(s); }
        public LineAdder   line()                                   { return parent.line(); }
        public ContentBuilder spacer(float pts)                    { return parent.spacer(pts); }
        public ContentBuilder radioGroup(Consumer<RadioGroupComponent> c)   { return parent.radioGroup(c); }
        public ContentBuilder checkboxGroup(Consumer<CheckboxGroupComponent> c) { return parent.checkboxGroup(c); }
        public ContentBuilder columns(Consumer<ColumnLayoutBuilder> c)      { return parent.columns(c); }
    }

    /**
     * Fluent builder for a {@link ColumnLayout}.
     */
    public static class ColumnLayoutBuilder {

        final ColumnLayout layout = new ColumnLayout();

        public ColumnLayoutBuilder column(float widthPercent, Consumer<ContentBuilder> configure) {
            layout.column(widthPercent, stack -> {
                ContentBuilder cb = new ContentBuilder();
                configure.accept(cb);
                // Transfer children from the inner ContentBuilder's stack
                // into the ColumnLayout's column stack.
                cb.stack.getChildren().forEach(stack::add);
            });
            return this;
        }

        public ColumnLayoutBuilder gutter(float pts)      { layout.gutter(pts);      return this; }
        public ColumnLayoutBuilder spaceBefore(float pts) { layout.spaceBefore(pts); return this; }
        public ColumnLayoutBuilder spaceAfter(float pts)  { layout.spaceAfter(pts);  return this; }
    }
}
