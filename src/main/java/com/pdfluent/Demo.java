package com.pdfluent;

import com.pdfluent.builder.ContentBuilder;
import com.pdfluent.builder.Document;
import com.pdfluent.core.PageSettings;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Demo: generates a patient intake form PDF exercising all framework features.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass=com.pdfluent.Demo
 */
public class Demo {

    // ── Reusable content blocks ──────────────────────────────────────────
    // Define once, use everywhere — in pages, columns, or nested layouts.

    /** A labeled underline field. */
    static Consumer<ContentBuilder> field(String label) {
        return c -> c
            .text(label, tc -> tc.bold().fontSize(9).spaceAfter(2))
            .text("_______________________________", tc -> tc.fontSize(10).spaceAfter(10));
    }

    /** A labeled field with custom placeholder text. */
    static Consumer<ContentBuilder> field(String label, String placeholder) {
        return c -> c
            .text(label, tc -> tc.bold().fontSize(9).spaceAfter(2))
            .text(placeholder, tc -> tc.fontSize(10).spaceAfter(10));
    }

    /** A section header with a thin separator line above it. */
    static Consumer<ContentBuilder> sectionHeader(String title) {
        return c -> c
            .line().thickness(0.5f).color(Color.LIGHT_GRAY).spaceBefore(4).spaceAfter(10)
            .text(title, tc -> tc.fontSize(10).spaceAfter(10));
    }

    /** A repeated underline row for multi-line entry areas. */
    static Consumer<ContentBuilder> blankLines(int count) {
        return c -> {
            for (int i = 0; i < count; i++) {
                c.text("_______________________________", tc -> tc.spaceAfter(8));
            }
        };
    }

    public static void main(String[] args) throws Exception {

        Document.create(doc -> doc

            .footer(f -> f
                .left("Patient Intake Form1")
                .right("Page {page} of {totalPages}")
                .fontSize(8)
                .color(Color.GRAY)
            )

            .page(PageSettings.a4().margin(50).build(), page -> page

                // ── Full-width header ────────────────────────────────────
                .text("Patient Intake Form")
                    .bold().fontSize(28).alignCenter().spaceAfter(6)

                .text("Please complete all sections clearly.")
                    .fontSize(9).alignCenter().color(Color.GRAY).spaceAfter(10)

                .line().thickness(1.5f).spaceBefore(2).spaceAfter(14)

                // ── Section: Personal Details ────────────────────────────
                .text("Personal Details").bold().fontSize(12).spaceAfter(8)

                // Reusable field() blocks passed directly to columns
                .columns(cols -> cols
                    .column(50, field("First Name"))
                    .column(50, field("Last Name"))
                )

                .columns(cols -> cols
                    .column(33, field("Date of Birth", "DD / MM / YYYY"))
                    .column(33, field("Phone Number", "___________________"))
                    .column(34, field("Email Address", "___________________"))
                )

                // ── Section: Gender ──────────────────────────────────────
                .content(sectionHeader("Gender"))

                .radioGroup(rg -> rg
                    .selected(1)
                    .option("Male")
                    .option("Female")
                    .option("Non-binary")
                    .option("Prefer not to say")
                    .horizontal()
                    
                    .fontSize(10)
                    .itemSpacing(20)
                    .spaceAfter(12)
                        
                )

                // ── Section: Medical History ─────────────────────────────
                .content(sectionHeader("Medical History"))

                .text("Please tick any conditions that apply:").fontSize(9).spaceAfter(6)

                .checkboxGroup(cg -> cg
                    .item("Diabetes")
                    .item("Hypertension")
                    .item("Heart disease")
                    .item("Asthma")
                    .item("Epilepsy")
                    .item("Mental health condition")
                    .horizontal()
                    .boxSize(10)
                    .fontSize(9)
                    .itemSpacing(16)
                    .spaceAfter(12)
                )

                // ── Section: Current Medications ─────────────────────────
                .content(sectionHeader("Current Medications"))

                .columns(cols -> cols
                    .column(60, c -> c
                        .text("Medication Name", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .content(blankLines(3))
                    )
                    .column(40, c -> c
                        .text("Dosage / Frequency", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .content(blankLines(3))
                    )
                )

                // ── Section: Allergies ──────────────────────────────────
                .content(sectionHeader("Allergies"))

                .text("Please list any known allergies (medications, food, environmental):").fontSize(9).spaceAfter(6)

                .columns(cols -> cols
                    .column(50, c -> c
                        .text("Allergen", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .content(blankLines(4))
                    )
                    .column(50, c -> c
                        .text("Reaction / Severity", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .content(blankLines(4))
                    )
                )

                // ── Section: Family Medical History ─────────────────────
                .content(sectionHeader("Family Medical History"))

                .text("Please indicate if any immediate family members have been diagnosed with the following:").fontSize(9).spaceAfter(6)

                .checkboxGroup(cg -> cg
                    .item("Heart disease")
                    .item("Diabetes")
                    .item("Cancer")
                    .item("Stroke")
                    .item("High blood pressure")
                    .item("Mental health conditions")
                        .item("Any other problems")
                    .horizontal()
                    .boxSize(10)
                    .fontSize(9)
                    .itemSpacing(16)
                    .spaceAfter(12)
                )

                .columns(cols -> cols
                    .column(100, field("Please provide details if applicable"))
                )

                // ── Section: Lifestyle ──────────────────────────────────
                .content(sectionHeader("Lifestyle"))

                .text("Smoking Status").bold().fontSize(9).spaceAfter(4)

                .radioGroup(rg -> rg
                    .option("Never smoked")
                    .option("Former smoker")
                    .option("Current smoker")
                    .horizontal()
                    .fontSize(9)
                    .itemSpacing(20)
                    .spaceAfter(10)
                )

                .text("Alcohol Consumption").bold().fontSize(9).spaceAfter(4)

                .radioGroup(rg -> rg
                    .option("None")
                    .option("Occasional")
                    .option("Moderate")
                    .option("Heavy")
                    .horizontal()
                    .fontSize(9)
                    .itemSpacing(20)
                    .spaceAfter(10)
                )

                .text("Exercise Frequency").bold().fontSize(9).spaceAfter(4)

                .radioGroup(rg -> rg
                    .option("Never")
                    .option("1-2 times/week")
                    .option("3-4 times/week")
                    .option("5+ times/week")
                    .horizontal()
                    .fontSize(9)
                    .itemSpacing(20)
                    .spaceAfter(12)
                )

                // ── Section: Reason for Visit ───────────────────────────
                .content(sectionHeader("Reason for Visit"))

                .text("Please describe your symptoms or reason for today's appointment:").fontSize(9).spaceAfter(6)

                .content(blankLines(5))

                .columns(cols -> cols
                    .column(50, field("Date symptoms began", "DD / MM / YYYY"))
                    .column(50, field("Referred by", "___________________"))
                )

                // ── Section: Consent ─────────────────────────────────────
                .content(sectionHeader("Consent & Declaration"))

                .checkboxGroup(cg -> cg
                    .item("I confirm the information provided is accurate to the best of my knowledge.", false)
                    .item("I consent to my information being shared with relevant clinical staff.", false)
                    .item("I agree to receive appointment reminders by email or SMS.", false)
                    .item("I understand that I may withdraw my consent at any time in writing.", false)
                    .boxSize(10)
                    .fontSize(9)
                    .itemSpacing(6)
                    .spaceAfter(16)
                )

                // ── Signature block ─────────────────────────────────────
                .line().thickness(1f).spaceBefore(8).spaceAfter(8)

                .columns(cols -> cols
                    .column(50, field("Patient Signature"))
                    .column(50, field("Date", "DD / MM / YYYY"))
                )

                .spacer(12)

                .columns(cols -> cols
                    .column(50, field("Clinician Signature"))
                    .column(50, field("Clinician Name"))
                )
            )

        ).save("./patient-intake-form.pdf");

        System.out.println("PDF generated: patient-intake-form.pdf");
    }
}
