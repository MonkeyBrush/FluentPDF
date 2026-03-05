package com.pdfluent;

import com.pdfluent.builder.Document;
import com.pdfluent.core.PageSettings;

import java.awt.Color;

/**
 * Demo: generates a patient intake form PDF exercising all framework features.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass=com.pdfluent.Demo
 */
public class Demo {

    public static void main(String[] args) throws Exception {

        Document.create(doc -> doc

            .page(PageSettings.a4().margin(10).build(), page -> page

                // ── Full-width header ────────────────────────────────────
                .text("Patient Intake Form")
                    .bold().fontSize(20).alignCenter().spaceAfter(6)

                .text("Please complete all sections clearly.")
                    .fontSize(9).alignCenter().color(Color.GRAY).spaceAfter(10)

                .line().thickness(1.5f).spaceBefore(2).spaceAfter(14)

                // ── Section: Personal Details ────────────────────────────
                .text("Personal Details").bold().fontSize(12).spaceAfter(8)

                .columns(cols -> cols
                    .column(50, left -> left
                        .text("First Name", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("_______________________________", tc -> tc.fontSize(10).spaceAfter(10))
                    )
                    .column(50, right -> right
                        .text("Last Name", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("_______________________________", tc -> tc.fontSize(10).spaceAfter(10))
                    )
                )

                .columns(cols -> cols
                    .column(33, c -> c
                        .text("Date of Birth", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("DD / MM / YYYY", tc -> tc.fontSize(10).spaceAfter(10))
                    )
                    .column(33, c -> c
                        .text("Phone Number", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("___________________", tc -> tc.fontSize(10).spaceAfter(10))
                    )
                    .column(34, c -> c
                        .text("Email Address", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("___________________", tc -> tc.fontSize(10).spaceAfter(10))
                    )
                )

                // ── Section: Gender ──────────────────────────────────────
                .line().thickness(0.5f).color(Color.LIGHT_GRAY).spaceBefore(4).spaceAfter(10)

                .text("Gender").bold().fontSize(12).spaceAfter(8)

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
                .line().thickness(0.5f).color(Color.LIGHT_GRAY).spaceBefore(4).spaceAfter(10)

                .text("Medical History").bold().fontSize(12).spaceAfter(8)

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
                .line().thickness(0.5f).color(Color.LIGHT_GRAY).spaceBefore(4).spaceAfter(10)

                .text("Current Medications").bold().fontSize(12).spaceAfter(8)

                .columns(cols -> cols
                    .column(60, left -> left
                        .text("Medication Name", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("_______________________________", tc -> tc.spaceAfter(8))
                        .text("_______________________________", tc -> tc.spaceAfter(8))
                        .text("_______________________________", tc -> tc.spaceAfter(8))
                    )
                    .column(40, right -> right
                        .text("Dosage / Frequency", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("___________________", tc -> tc.spaceAfter(8))
                        .text("___________________", tc -> tc.spaceAfter(8))
                        .text("___________________", tc -> tc.spaceAfter(8))
                    )
                )

                // ── Section: Consent ─────────────────────────────────────
                .line().thickness(0.5f).color(Color.LIGHT_GRAY).spaceBefore(4).spaceAfter(10)

                .text("Consent & Declaration").bold().fontSize(12).spaceAfter(8)

                .checkboxGroup(cg -> cg
                    .item("I confirm the information provided is accurate to the best of my knowledge.", false)
                    .item("I consent to my information being shared with relevant clinical staff.", false)
                    .item("I agree to receive appointment reminders by email or SMS.", false)
                    .boxSize(10)
                    .fontSize(9)
                    .itemSpacing(6)
                    .spaceAfter(16)
                )

                // ── Full-width footer ────────────────────────────────────
                .line().thickness(1f).spaceBefore(8).spaceAfter(8)

                .columns(cols -> cols
                    .column(50, left -> left
                        .text("Patient Signature", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("_______________________________", tc -> tc.fontSize(10))
                    )
                    .column(50, right -> right
                        .text("Date", tc -> tc.bold().fontSize(9).spaceAfter(2))
                        .text("DD / MM / YYYY", tc -> tc.fontSize(10))
                    )
                )
            )

        ).save("./patient-intake-form.pdf");

        System.out.println("PDF generated: patient-intake-form.pdf");
    }
}
