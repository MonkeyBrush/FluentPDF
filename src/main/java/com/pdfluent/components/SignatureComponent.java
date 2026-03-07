package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders handwritten signature strokes captured from a touch/stylus device.
 *
 * <p>Supports two input formats used by TATworks:</p>
 * <ul>
 *   <li><strong>SIGNATURE</strong> (JSON lines format):
 *       {@code {"lines":[[[x,y],[x,y],...],...]}}
 *       — parsed by {@link #parseSignatureJson(String)}</li>
 *   <li><strong>SIGNATURESVG</strong> (SVG path format):
 *       {@code {"paths":["M99.8 484.1L99.8 484.1...", ...]}}
 *       — parsed by {@link #parseSignatureSvg(String)}</li>
 * </ul>
 *
 * <p>Both parsers produce a {@code List<List<float[]>>} of strokes, where each
 * stroke is a list of {@code {x, y}} coordinate pairs.  The component scales
 * the signature to fit its allocated area while preserving aspect ratio, and
 * centres it horizontally.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   List&lt;List&lt;float[]&gt;&gt; strokes = SignatureComponent.parseSignatureJson(jsonStr);
 *   page.signature(strokes, sig -&gt; sig.height(60).color(Color.DARK_GRAY));
 * </pre>
 */
public class SignatureComponent implements Component {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final List<List<float[]>> strokes;

    private float height       = 60f;
    private float strokeWidth  = 1.2f;
    private Color color        = new Color(20, 20, 80);  // dark blue-black ink
    private float spaceBefore  = 4f;
    private float spaceAfter   = 4f;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Create a signature component from pre-parsed strokes.
     *
     * @param strokes list of strokes; each stroke is a list of {@code {x, y}} pairs
     */
    public SignatureComponent(List<List<float[]>> strokes) {
        this.strokes = strokes != null ? strokes : List.of();
    }

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    /** Render height for the signature area in points (default 60). */
    public SignatureComponent height(float h)          { this.height = h;          return this; }

    /** Stroke width for signature lines in points (default 1.2). */
    public SignatureComponent strokeWidth(float w)     { this.strokeWidth = w;     return this; }

    /** Ink colour (default dark blue-black). */
    public SignatureComponent color(Color c)           { this.color = c;           return this; }

    public SignatureComponent spaceBefore(float pts)   { this.spaceBefore = pts;   return this; }
    public SignatureComponent spaceAfter(float pts)    { this.spaceAfter = pts;    return this; }

    // -----------------------------------------------------------------------
    // Component contract
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        return spaceBefore + height + spaceAfter;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        if (strokes.isEmpty()) return;

        try {
            // 1. Calculate bounding box of all signature points
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

            for (List<float[]> stroke : strokes) {
                for (float[] pt : stroke) {
                    if (pt[0] < minX) minX = pt[0];
                    if (pt[1] < minY) minY = pt[1];
                    if (pt[0] > maxX) maxX = pt[0];
                    if (pt[1] > maxY) maxY = pt[1];
                }
            }

            float sigWidth  = maxX - minX;
            float sigHeight = maxY - minY;

            if (sigWidth <= 0 || sigHeight <= 0) return;

            // 2. Scale to fit, preserving aspect ratio
            float scaleX = width / sigWidth;
            float scaleY = height / sigHeight;
            float scale  = Math.min(scaleX, scaleY);

            // 3. Centre horizontally within available width
            float scaledW  = sigWidth * scale;
            float offsetX  = (width - scaledW) / 2f;

            // 4. Centre vertically within allocated height
            float scaledH  = sigHeight * scale;
            float offsetY  = (height - scaledH) / 2f;

            // 5. Draw each stroke as a polyline
            float baseX = x + offsetX;
            float baseY = y + spaceBefore + offsetY;

            for (List<float[]> stroke : strokes) {
                if (stroke.size() < 2) continue;

                float[][] points = new float[stroke.size()][2];
                for (int i = 0; i < stroke.size(); i++) {
                    float[] pt = stroke.get(i);
                    points[i][0] = baseX + (pt[0] - minX) * scale;
                    points[i][1] = baseY + (pt[1] - minY) * scale;
                }

                ctx.drawPolyline(points, color, strokeWidth);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to render signature: " + e.getMessage(), e);
        }
    }

    // =======================================================================
    // Static parsers
    // =======================================================================

    // -----------------------------------------------------------------------
    // SIGNATURE format — {"lines":[[[x,y],[x,y],...],...]]}
    // -----------------------------------------------------------------------

    /** Regex for one stroke: an array of [x,y] pairs. */
    private static final Pattern STROKE_PATTERN =
            Pattern.compile("(\\[(?:,?\\[-?[\\d.]+,-?[\\d.]+\\])+\\])");

    /** Regex for individual [x,y] point inside a stroke. */
    private static final Pattern POINT_PATTERN =
            Pattern.compile("\\[(-?[\\d.]+),(-?[\\d.]+)\\]");

    /**
     * Parse the SIGNATURE JSON format into a list of strokes.
     *
     * <p>Input format: {@code {"lines":[[[29,122.83],[30,121.83],...],[[x,y],...]]]}}</p>
     *
     * @param jsonEncoding the raw JSON string from {@code fieldData.plainValue}
     * @return list of strokes, each stroke is a list of {@code {x, y}} pairs;
     *         empty list if input is null, blank, or unparseable
     */
    public static List<List<float[]>> parseSignatureJson(String jsonEncoding) {
        List<List<float[]>> lines = new ArrayList<>();
        if (jsonEncoding == null || jsonEncoding.isBlank()) return lines;

        Matcher strokeMatcher = STROKE_PATTERN.matcher(jsonEncoding);
        while (strokeMatcher.find()) {
            Matcher pointMatcher = POINT_PATTERN.matcher(strokeMatcher.group(1));
            List<float[]> stroke = new ArrayList<>();
            while (pointMatcher.find()) {
                float x = Float.parseFloat(pointMatcher.group(1));
                float y = Float.parseFloat(pointMatcher.group(2));
                stroke.add(new float[]{x, y});
            }
            if (!stroke.isEmpty()) {
                lines.add(stroke);
            }
        }
        return lines;
    }

    // -----------------------------------------------------------------------
    // SIGNATURESVG format — {"paths":["M99.8 484.1L99.8 484.1...", ...]}
    // -----------------------------------------------------------------------

    /**
     * Parse the SIGNATURESVG format into a list of strokes.
     *
     * <p>Input format: {@code {"paths":["M99.8 484.1L99.8 484.1L...", ...]}}</p>
     *
     * <p>Supports both Xamarin-style ({@code M99.805 484.145L99.805 484.145})
     * and Flutter-style ({@code M823.4 476.4 L830.5 469.4}) SVG path strings.</p>
     *
     * @param jsonEncoding the raw JSON string from {@code fieldData.plainValue}
     * @return list of strokes; empty list if input is null or unparseable
     */
    public static List<List<float[]>> parseSignatureSvg(String jsonEncoding) {
        List<List<float[]>> lines = new ArrayList<>();
        if (jsonEncoding == null || jsonEncoding.isBlank()) return lines;

        // Extract the "paths" array values — simple regex since we don't want
        // to depend on a specific JSON library here.
        // Matches strings inside the paths array: "paths":["...", "..."]
        Pattern pathStringPattern = Pattern.compile("\"paths\"\\s*:\\s*\\[([^\\]]+)\\]");
        Matcher pathArrayMatcher = pathStringPattern.matcher(jsonEncoding);
        if (!pathArrayMatcher.find()) return lines;

        String pathsContent = pathArrayMatcher.group(1);

        // Extract each individual path string
        Pattern singlePathPattern = Pattern.compile("\"([^\"]+)\"");
        Matcher singlePathMatcher = singlePathPattern.matcher(pathsContent);

        while (singlePathMatcher.find()) {
            String sPath = singlePathMatcher.group(1);
            List<float[]> currentPoints = parseSvgPath(sPath);
            if (!currentPoints.isEmpty()) {
                lines.add(currentPoints);
            }
        }

        return lines;
    }

    /**
     * Parse a single SVG path string like "M99.8 484.1L99.8 484.1L99.8 484.1".
     * Supports both forms:
     * <ul>
     *   <li>Xamarin: {@code M99.805 484.145L99.805 484.145}</li>
     *   <li>Flutter: {@code M823.4 476.4 L830.5 469.4}</li>
     * </ul>
     */
    private static List<float[]> parseSvgPath(String sPath) {
        List<float[]> points = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        boolean readingX = false;
        boolean readingY = false;
        float x = 0, y = 0;

        for (int i = 0; i < sPath.length(); i++) {
            char c = sPath.charAt(i);

            switch (c) {
                case 'M':
                    // Start of a new sub-path
                    buf = new StringBuilder();
                    readingX = true;
                    readingY = false;
                    break;

                case 'L':
                    if (readingY) {
                        // Completed Y by hitting L (no space before L — Xamarin style)
                        y = parseFloatSafe(buf.toString());
                        points.add(new float[]{x, y});
                        buf = new StringBuilder();
                        readingX = true;
                        readingY = false;
                    } else {
                        // Completed Y via space before L — start new X
                        buf = new StringBuilder();
                        readingX = true;
                        readingY = false;
                    }
                    break;

                case ' ':
                    if (readingX) {
                        // Completed X
                        x = parseFloatSafe(buf.toString());
                        buf = new StringBuilder();
                        readingX = false;
                        readingY = true;
                    } else if (readingY) {
                        // Completed Y (Flutter style with space before L)
                        y = parseFloatSafe(buf.toString());
                        points.add(new float[]{x, y});
                        buf = new StringBuilder();
                        readingX = false;
                        readingY = false;
                    }
                    break;

                default:
                    buf.append(c);
                    break;
            }
        }

        // Handle trailing point (path may end without a trailing L or space)
        if (readingY && !buf.isEmpty()) {
            y = parseFloatSafe(buf.toString());
            points.add(new float[]{x, y});
        }

        return points;
    }

    private static float parseFloatSafe(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
