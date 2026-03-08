package com.pdfluent.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pdfluent.builder.ContentBuilder;
import com.pdfluent.builder.Document;
import com.pdfluent.components.SignatureComponent;
import com.pdfluent.core.PageSettings;

import java.awt.Color;
import java.io.Reader;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Renders a TATworks JSONFormDesign as a PDF using the PDFluent builder.
 *
 * <p>Supported field types:</p>
 * <ul>
 *   <li>{@code HEADER}         — instruction / header text block</li>
 *   <li>{@code SECTIONSTART}   — section divider with bold title</li>
 *   <li>{@code SECTIONEND}     — section close (spacer)</li>
 *   <li>{@code TEXT}           — single-line text field</li>
 *   <li>{@code NAMEENTRY}      — name entry (rendered as text field)</li>
 *   <li>{@code DATE}           — date field (ISO instant or yyyy-MM-dd)</li>
 *   <li>{@code TIME}           — time field</li>
 *   <li>{@code NUMBER_INT}     — numeric field</li>
 *   <li>{@code COMMENT}        — multi-line text area</li>
 *   <li>{@code YESNO}          — Yes / No radio</li>
 *   <li>{@code YESNONA}        — Yes / No / N/A radio</li>
 *   <li>{@code GENDERMF}       — Male / Female radio</li>
 *   <li>{@code CHECKBOX}       — single on/off checkbox</li>
 *   <li>{@code MULTICHOICE}    — single or multi-choice (radio or checkbox group)</li>
 *   <li>{@code DROPDOWN}       — dropdown selection</li>
 *   <li>{@code SIGNATURE}      — signature line</li>
 *   <li>{@code PHOTO}          — photo placeholder</li>
 *   <li>{@code JSARL}          — Job Safety Analysis row (hazard / control / achieved)</li>
 *   <li>{@code WEBLINK}        — web link with description</li>
 *   <li>{@code QUESTIONSUMMARY}— quiz score summary</li>
 *   <li>{@code DUPLICATE}      — section duplicate marker (rendered as spacer)</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   Document doc = JsonFormRenderer.render(jsonString);
 *   doc.save("output.pdf");
 *
 *   byte[] pdf = JsonFormRenderer.render(jsonString).toByteArray();
 * </pre>
 */
public class JsonFormRenderer {

    // -----------------------------------------------------------------------
    // Style constants
    // -----------------------------------------------------------------------

    private static final float LABEL_FONT_SIZE    = 9f;
    private static final float VALUE_FONT_SIZE    = 10f;
    private static final float SECTION_FONT_SIZE  = 13f;
    private static final float TITLE_FONT_SIZE    = 22f;
    private static final float HEADER_FONT_SIZE   = 10f;
    private static final float FOOTER_FONT_SIZE   = 8f;

    private static final Color VALUE_COLOR   = new Color(30, 30, 30);
    private static final Color HEADER_COLOR  = new Color(60, 60, 60);
    private static final Color FOOTER_COLOR  = Color.GRAY;
    private static final Color DIVIDER_COLOR = Color.LIGHT_GRAY;
    private static final Color LINK_COLOR    = new Color(0, 102, 204);

    private static final String BLANK_LINE = "_______________________________";

    private static final DateTimeFormatter DISPLAY_DATE_FMT =
            DateTimeFormatter.ofPattern("dd / MM / yyyy");

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Render a JSONFormDesign from a JSON string. */
    public static Document render(String json) {
        return render(JsonParser.parseString(json).getAsJsonObject());
    }

    /** Render a JSONFormDesign from a JSON string with an external completedBy name. */
    public static Document render(String json, String completedBy) {
        return render(JsonParser.parseString(json).getAsJsonObject(), completedBy);
    }

    /** Render a JSONFormDesign from a Reader (file, stream, etc.). */
    public static Document render(Reader reader) {
        return render(JsonParser.parseReader(reader).getAsJsonObject());
    }

    /** Render a JSONFormDesign from a Reader with an external completedBy name. */
    public static Document render(Reader reader, String completedBy) {
        return render(JsonParser.parseReader(reader).getAsJsonObject(), completedBy);
    }

    /** Render a JSONFormDesign from a pre-parsed JsonObject. */
    public static Document render(JsonObject root) {
        return render(root, (String) null);
    }

    /** Render a JSONFormDesign from a pre-parsed JsonObject with an external completedBy name. */
    public static Document render(JsonObject root, String completedBy) {
        return render(root, PageSettings.a4().margin(70, 50, 50, 50).build(), completedBy);
    }

    /** Render a JSONFormDesign with custom page settings. */
    public static Document render(JsonObject root, PageSettings settings) {
        return render(root, settings, null);
    }

    /** Render a JSONFormDesign with custom page settings and an external completedBy name. */
    public static Document render(JsonObject root, PageSettings settings, String completedBy) {
        String formName      = getString(root, "name", "Untitled Form");
        String resolvedBy    = (completedBy != null && !completedBy.isEmpty())
                ? completedBy : getString(root, "completedBy", "");
        String completedDate = formatDate(getString(root, "completedDate", ""));
        JsonArray fields     = root.getAsJsonObject("formBody").getAsJsonArray("fields");

        return Document.create(doc -> doc

            .header(h -> h
                .height(36).showAll().underline()
                .columns(cols -> cols
                    .column(50, col -> col
                        .text("TATworks", tc -> tc.bold().fontSize(HEADER_FONT_SIZE))
                    )
                    .column(50, col -> {
                        if (!resolvedBy.isEmpty()) {
                            col.text("Completed by: " + resolvedBy,
                                    tc -> tc.fontSize(HEADER_FONT_SIZE).alignRight());
                        }
                        col.text("Date: " + completedDate,
                                tc -> tc.fontSize(HEADER_FONT_SIZE).alignRight());
                    })
                )
            )

            .footer(f -> f
                .left(formName)
                .right("Page {page} of {totalPages}")
                .fontSize(FOOTER_FONT_SIZE)
                .color(FOOTER_COLOR)
            )

            .page(settings, page -> {
                // Form title
                page.text(formName).bold().fontSize(TITLE_FONT_SIZE).alignCenter().spaceAfter(6);
                page.line().thickness(1.5f).spaceBefore(2).spaceAfter(14);

                // Walk each field
                for (int i = 0; i < fields.size(); i++) {
                    JsonObject field = fields.get(i).getAsJsonObject();
                    renderField(page, field);
                }
            })
        );
    }

    // -----------------------------------------------------------------------
    // Field dispatch
    // -----------------------------------------------------------------------

    private static void renderField(ContentBuilder page, JsonObject field) {
        String type = getString(field, "type", "");
        JsonObject data = field.has("fieldData") ? field.getAsJsonObject("fieldData") : null;
        if (data == null) return;

        switch (type) {
            // Structure
            case "HEADER"       -> renderHeader(page, data);
            case "SECTIONSTART" -> renderSectionStart(page, data);
            case "SECTIONEND"   -> renderSectionEnd(page);
            case "DUPLICATE"    -> renderDuplicate(page);

            // Text input types
            case "TEXT"         -> renderTextField(page, data);
            case "NAMEENTRY"    -> renderTextField(page, data);
            case "COMMENT"      -> renderCommentField(page, field);
            case "NUMBER_INT"   -> renderTextField(page, data);

            // Date / time
            case "DATE"         -> renderDateField(page, data);
            case "TIME"         -> renderTimeField(page, data);

            // Yes/No variants & gender
            case "YESNO"        -> renderYesNo(page, data, "Yes", "No");
            case "YESNONA"      -> renderYesNo(page, data, "Yes", "No", "N/A");
            case "GENDERMF"     -> renderGenderField(page, data);

            // Single checkbox (on/off toggle)
            case "CHECKBOX"     -> renderSingleCheckbox(page, data);

            // Multi-choice (radio or checkbox depending on isSingleChoiceOnly)
            case "MULTICHOICE"  -> renderMultiChoice(page, data);

            // Dropdown
            case "DROPDOWN"     -> renderDropdownField(page, data);

            // Signature (JSON lines or SVG paths)
            case "SIGNATURE"    -> renderSignatureField(page, field, false);
            case "SIGNATURESVG" -> renderSignatureField(page, field, true);

            // Photo placeholder
            case "PHOTO"        -> renderPhotoField(page, data);

            // Job Safety Analysis row
            case "JSARL"        -> renderJsarl(page, data);

            // Web link
            case "WEBLINK"      -> renderWebLink(page, data);

            // Questionnaire summary
            case "QUESTIONSUMMARY" -> renderQuestionSummary(page, data);

            default -> {
                // Fallback: render as text field if it has a question
                String q = getString(data, "questionText", "");
                if (!q.isEmpty()) {
                    renderTextField(page, data);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Structure renderers
    // -----------------------------------------------------------------------

    private static void renderHeader(ContentBuilder page, JsonObject data) {
        String text = getString(data, "plainValue", "");
        if (!text.isEmpty()) {
            page.text(text, tc -> tc.fontSize(HEADER_FONT_SIZE).color(HEADER_COLOR)
                    .spaceAfter(10).spaceBefore(4));
        }
    }

    private static void renderSectionStart(ContentBuilder page, JsonObject data) {
        String title = getString(data, "plainValue", "Section");
        page.line().thickness(0.5f).color(DIVIDER_COLOR).spaceBefore(6).spaceAfter(10);
        page.text(title, tc -> tc.bold().fontSize(SECTION_FONT_SIZE).spaceAfter(8));
    }

    private static void renderSectionEnd(ContentBuilder page) {
        page.spacer(6);
    }

    private static void renderDuplicate(ContentBuilder page) {
        page.spacer(4);
    }

    // -----------------------------------------------------------------------
    // Text / number / name entry
    // -----------------------------------------------------------------------

    private static void renderTextField(ContentBuilder page, JsonObject data) {
        page.content(labeledField(data));
    }

    // -----------------------------------------------------------------------
    // Comment (multi-line)
    // -----------------------------------------------------------------------

    private static void renderCommentField(ContentBuilder page, JsonObject field) {
        JsonObject data = field.getAsJsonObject("fieldData");
        String label = getString(data, "questionText", "");
        String value = getString(data, "plainValue", "");

        // Use field.title as label if questionText is empty
        String displayLabel = label.isEmpty() ? getString(field, "title", "") : label;

        if (!displayLabel.isEmpty()) {
            page.text(displayLabel, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(2));
        }

        if (value.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                page.text(BLANK_LINE, tc -> tc.fontSize(VALUE_FONT_SIZE).spaceAfter(8));
            }
        } else {
            page.text(value, tc -> tc.fontSize(VALUE_FONT_SIZE).spaceAfter(10));
        }
    }

    // -----------------------------------------------------------------------
    // Date / Time
    // -----------------------------------------------------------------------

    private static void renderDateField(ContentBuilder page, JsonObject data) {
        String label = getString(data, "questionText", "Date:");
        String raw   = getString(data, "plainValue", "");
        String display = formatDate(raw);
        page.content(labeledFieldWithValue(label, display));
    }

    private static void renderTimeField(ContentBuilder page, JsonObject data) {
        String label = getString(data, "questionText", "Time:");
        String raw   = getString(data, "plainValue", "");
        String display = raw.isEmpty() ? "HH : MM" : raw;
        page.content(labeledFieldWithValue(label, display));
    }

    // -----------------------------------------------------------------------
    // Yes/No variants & Gender
    // -----------------------------------------------------------------------

    private static void renderYesNo(ContentBuilder page, JsonObject data, String... options) {
        String label    = getString(data, "questionText", "");
        String selected = getString(data, "plainValue", "").toLowerCase();

        if (!label.isEmpty()) {
            page.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(4));
        }

        page.radioGroup(rg -> {
            for (String opt : options) {
                rg.option(opt);
            }
            // Match selection
            for (int i = 0; i < options.length; i++) {
                if (options[i].toLowerCase().startsWith(selected)) {
                    rg.selected(i);
                    break;
                }
            }
            rg.horizontal().fontSize(LABEL_FONT_SIZE).itemSpacing(24).spaceAfter(10);
        });
    }

    private static void renderGenderField(ContentBuilder page, JsonObject data) {
        String label    = getString(data, "questionText", "");
        String selected = getString(data, "plainValue", "").toUpperCase();

        if (!label.isEmpty()) {
            page.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(4));
        }

        page.radioGroup(rg -> {
            rg.option("Male").option("Female");
            if ("M".equals(selected))      rg.selected(0);
            else if ("F".equals(selected)) rg.selected(1);
            rg.horizontal().fontSize(LABEL_FONT_SIZE).itemSpacing(24).spaceAfter(10);
        });
    }

    // -----------------------------------------------------------------------
    // Single checkbox (on/off toggle)
    // -----------------------------------------------------------------------

    private static void renderSingleCheckbox(ContentBuilder page, JsonObject data) {
        String label   = getString(data, "questionText", "");
        String value   = getString(data, "plainValue", "");
        boolean isOn   = "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);

        page.checkboxGroup(cg -> {
            cg.item(label.isEmpty() ? "Checked" : label, isOn);
            cg.boxSize(10).fontSize(LABEL_FONT_SIZE).spaceAfter(10);
        });
    }

    // -----------------------------------------------------------------------
    // Multi-choice (radio if single, checkbox if multi)
    // -----------------------------------------------------------------------

    private static void renderMultiChoice(ContentBuilder page, JsonObject data) {
        String label = getString(data, "questionText", "");
        if (!label.isEmpty()) {
            page.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(4));
        }

        if (!data.has("multiChoice")) return;
        JsonObject mc = data.getAsJsonObject("multiChoice");
        boolean isSingle = mc.has("isSingleChoiceOnly") && mc.get("isSingleChoiceOnly").getAsBoolean();
        JsonArray options = mc.has("options") ? mc.getAsJsonArray("options") : new JsonArray();

        if (isSingle) {
            // Render as radio group
            page.radioGroup(rg -> {
                for (int i = 0; i < options.size(); i++) {
                    JsonObject opt = options.get(i).getAsJsonObject();
                    rg.option(getString(opt, "displayText", "Option " + (i + 1)));
                    if (opt.has("isSelected") && opt.get("isSelected").getAsBoolean()) {
                        rg.selected(i);
                    }
                }
                rg.horizontal().fontSize(LABEL_FONT_SIZE).itemSpacing(20).spaceAfter(10);
            });
        } else {
            // Render as checkbox group
            page.checkboxGroup(cg -> {
                for (int i = 0; i < options.size(); i++) {
                    JsonObject opt = options.get(i).getAsJsonObject();
                    String text = getString(opt, "displayText", "Option " + (i + 1));
                    boolean selected = opt.has("isSelected") && opt.get("isSelected").getAsBoolean();
                    cg.item(text, selected);
                }
                cg.horizontal().boxSize(10).fontSize(LABEL_FONT_SIZE)
                  .itemSpacing(16).spaceAfter(10);
            });
        }
    }

    // -----------------------------------------------------------------------
    // Dropdown
    // -----------------------------------------------------------------------

    private static void renderDropdownField(ContentBuilder page, JsonObject data) {
        String label     = getString(data, "questionText", "");
        String rawValue  = getString(data, "plainValue", "");

        // Resolve display text from the dropdown definition
        String display = rawValue;
        if (data.has("dropDown")) {
            JsonObject dd = data.getAsJsonObject("dropDown");
            if (dd.has("selectValue") && dd.get("selectValue").isJsonArray()) {
                for (JsonElement e : dd.getAsJsonArray("selectValue")) {
                    JsonObject opt = e.getAsJsonObject();
                    if (getString(opt, "value", "").equals(rawValue)) {
                        display = getString(opt, "displayText", rawValue);
                        break;
                    }
                }
            }
        }

        page.content(labeledFieldWithValue(label, display.isEmpty() ? BLANK_LINE : display));
    }

    // -----------------------------------------------------------------------
    // Signature
    // -----------------------------------------------------------------------

    private static void renderSignatureField(ContentBuilder page, JsonObject field, boolean isSvg) {
        JsonObject data = field.getAsJsonObject("fieldData");
        String label = getString(data, "questionText", "");
        String title = getString(field, "title", "");
        String displayLabel = label.isEmpty() ? title : label;
        String rawValue = getString(data, "plainValue", "");

        // Show label if present
        if (!displayLabel.isEmpty()) {
            page.text(displayLabel, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(4));
        }

        // Parse strokes
        List<List<float[]>> strokes = isSvg
                ? SignatureComponent.parseSignatureSvg(rawValue)
                : SignatureComponent.parseSignatureJson(rawValue);

        if (!strokes.isEmpty()) {
            // Render actual signature strokes
            page.signature(strokes, sig -> sig
                    .height(60)
                    .strokeWidth(1.2f)
                    .spaceAfter(2)
            );
        }

        // Signature line below
        page.text("Signature: " + BLANK_LINE, tc -> tc.fontSize(VALUE_FONT_SIZE).spaceAfter(10));
    }

    // -----------------------------------------------------------------------
    // Photo placeholder
    // -----------------------------------------------------------------------

    private static void renderPhotoField(ContentBuilder page, JsonObject data) {
        String label = getString(data, "questionText", "Photo");
        page.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(2));
        page.text("[Photo attachment]", tc -> tc.italic().fontSize(LABEL_FONT_SIZE)
                .color(Color.GRAY).spaceAfter(10));
    }

    // -----------------------------------------------------------------------
    // JSARL — Job Safety Analysis row
    // -----------------------------------------------------------------------

    private static void renderJsarl(ContentBuilder page, JsonObject data) {
        if (!data.has("jsarl")) return;
        JsonObject jsarl = data.getAsJsonObject("jsarl");

        String hazard  = getString(jsarl, "hazard", "");
        String control = getString(jsarl, "control", "");
        String achieved = getString(jsarl, "controlAchieved", "");

        page.columns(cols -> cols
            .column(40, c -> c.content(labeledFieldWithValue("Hazard", hazard.isEmpty() ? BLANK_LINE : hazard)))
            .column(40, c -> c.content(labeledFieldWithValue("Control", control.isEmpty() ? BLANK_LINE : control)))
            .column(20, c -> c.content(labeledFieldWithValue("Achieved", achieved.isEmpty() ? BLANK_LINE : achieved)))
        );
    }

    // -----------------------------------------------------------------------
    // Web link
    // -----------------------------------------------------------------------

    private static void renderWebLink(ContentBuilder page, JsonObject data) {
        String description = getString(data, "questionText", "");
        String url         = getString(data, "plainValue", "");
        String linkText    = getString(data, "commentText", url);

        if (!description.isEmpty()) {
            page.text(description, tc -> tc.fontSize(LABEL_FONT_SIZE).spaceAfter(2));
        }
        if (!linkText.isEmpty()) {
            page.text(linkText, tc -> tc.fontSize(LABEL_FONT_SIZE).color(LINK_COLOR).spaceAfter(10));
        }
    }

    // -----------------------------------------------------------------------
    // Questionnaire summary
    // -----------------------------------------------------------------------

    private static void renderQuestionSummary(ContentBuilder page, JsonObject data) {
        if (!data.has("questionSummary")) return;
        JsonObject qs = data.getAsJsonObject("questionSummary");

        String passPercent = getString(qs, "percentageForAchieved", "");

        page.line().thickness(0.5f).color(DIVIDER_COLOR).spaceBefore(8).spaceAfter(10);
        page.text("Questionnaire Summary", tc -> tc.bold().fontSize(SECTION_FONT_SIZE).spaceAfter(6));

        if (!passPercent.isEmpty()) {
            page.text("Pass threshold: " + passPercent + "%",
                    tc -> tc.fontSize(VALUE_FONT_SIZE).spaceAfter(10));
        }
    }

    // -----------------------------------------------------------------------
    // Reusable content blocks
    // -----------------------------------------------------------------------

    private static Consumer<ContentBuilder> labeledField(JsonObject data) {
        String label = getString(data, "questionText", "");
        String value = getString(data, "plainValue", "");

        return c -> {
            if (!label.isEmpty()) {
                c.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(2));
            }
            if (value.isEmpty()) {
                c.text(BLANK_LINE, tc -> tc.fontSize(VALUE_FONT_SIZE).color(VALUE_COLOR).spaceAfter(10));
            } else {
                c.text(value, tc -> tc.fontSize(VALUE_FONT_SIZE).color(VALUE_COLOR).spaceAfter(10));
            }
        };
    }

    private static Consumer<ContentBuilder> labeledFieldWithValue(String label, String displayValue) {
        return c -> {
            if (label != null && !label.isEmpty()) {
                c.text(label, tc -> tc.bold().fontSize(LABEL_FONT_SIZE).spaceAfter(2));
            }
            c.text(displayValue, tc -> tc.fontSize(VALUE_FONT_SIZE).color(VALUE_COLOR).spaceAfter(10));
        };
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String getString(JsonObject obj, String key, String defaultValue) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            JsonElement el = obj.get(key);
            if (el.isJsonPrimitive()) return el.getAsString();
        }
        return defaultValue;
    }

    /** Format a date string — handles both ISO instants and yyyy-MM-dd. */
    private static String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "DD / MM / YYYY";
        try {
            // Try ISO instant first (e.g. "2024-09-18T02:58:18.098Z")
            return DISPLAY_DATE_FMT.format(Instant.parse(raw).atZone(ZoneId.systemDefault()));
        } catch (Exception e1) {
            try {
                // Try plain date (e.g. "2026-03-05")
                return DISPLAY_DATE_FMT.format(LocalDate.parse(raw));
            } catch (Exception e2) {
                return raw;
            }
        }
    }
}
