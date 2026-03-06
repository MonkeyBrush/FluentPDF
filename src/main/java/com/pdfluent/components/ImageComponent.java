package com.pdfluent.components;

import com.pdfluent.core.Component;
import com.pdfluent.core.RenderContext;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Renders an image (PNG, JPEG, etc.) within the content flow.
 *
 * <p>The image can be loaded from a file path or from raw byte data.
 * Width and height can be set explicitly; if only one dimension is given
 * the other is calculated to maintain aspect ratio.  If neither is given
 * the image renders at its natural pixel size (1 px = 1 pt).</p>
 *
 * <p>Usage inside ContentBuilder:</p>
 * <pre>
 *   page.image("logo.png", img -&gt; img.width(120).height(40).spaceAfter(10));
 * </pre>
 */
public class ImageComponent implements Component {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private String imagePath;
    private byte[] imageBytes;
    private String imageType;          // e.g. "png", "jpg" — used with byte[] constructor

    private float requestedWidth  = -1;
    private float requestedHeight = -1;
    private float spaceBefore     = 0;
    private float spaceAfter      = 4;

    // Cached PDFBox image (created on first render)
    private PDImageXObject cachedImage;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** Create an image component from a file path. */
    public ImageComponent(String filePath) {
        this.imagePath = filePath;
    }

    /** Create an image component from raw bytes (specify type e.g. "png", "jpg"). */
    public ImageComponent(byte[] data, String type) {
        this.imageBytes = data;
        this.imageType  = type;
    }

    // -----------------------------------------------------------------------
    // Fluent API
    // -----------------------------------------------------------------------

    public ImageComponent width(float w)           { this.requestedWidth = w;  return this; }
    public ImageComponent height(float h)           { this.requestedHeight = h; return this; }
    public ImageComponent spaceBefore(float pts)    { this.spaceBefore = pts;   return this; }
    public ImageComponent spaceAfter(float pts)     { this.spaceAfter = pts;    return this; }

    // -----------------------------------------------------------------------
    // Component contract
    // -----------------------------------------------------------------------

    @Override
    public float measure(RenderContext ctx, float availableWidth) {
        float[] dim = resolvedDimensions(ctx, availableWidth);
        return spaceBefore + dim[1] + spaceAfter;
    }

    @Override
    public void render(RenderContext ctx, float x, float y, float width) {
        try {
            PDImageXObject img = loadImage(ctx);
            float[] dim = resolvedDimensions(ctx, width);
            float drawW = dim[0];
            float drawH = dim[1];

            ctx.drawImage(img, x, y + spaceBefore, drawW, drawH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render image: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private PDImageXObject loadImage(RenderContext ctx) throws IOException {
        if (cachedImage != null) return cachedImage;

        if (imagePath != null) {
            cachedImage = PDImageXObject.createFromFile(imagePath, ctx.getDocument());
        } else if (imageBytes != null) {
            cachedImage = PDImageXObject.createFromByteArray(
                    ctx.getDocument(), imageBytes, imageType);
        } else {
            throw new IllegalStateException("No image source set");
        }
        return cachedImage;
    }

    /**
     * Resolve the actual draw width and height.
     *
     * <ul>
     *   <li>Both set → use as-is</li>
     *   <li>Only width set → scale height proportionally</li>
     *   <li>Only height set → scale width proportionally</li>
     *   <li>Neither set → natural size, clamped to available width</li>
     * </ul>
     */
    private float[] resolvedDimensions(RenderContext ctx, float availableWidth) {
        float natW, natH;
        try {
            PDImageXObject img = loadImage(ctx);
            natW = img.getWidth();
            natH = img.getHeight();
        } catch (IOException e) {
            // Fallback: use requested or zero
            natW = requestedWidth > 0 ? requestedWidth : 100;
            natH = requestedHeight > 0 ? requestedHeight : 100;
        }

        float w, h;

        if (requestedWidth > 0 && requestedHeight > 0) {
            w = requestedWidth;
            h = requestedHeight;
        } else if (requestedWidth > 0) {
            w = requestedWidth;
            h = natH * (w / natW);
        } else if (requestedHeight > 0) {
            h = requestedHeight;
            w = natW * (h / natH);
        } else {
            w = natW;
            h = natH;
        }

        // Clamp to available width
        if (w > availableWidth) {
            float scale = availableWidth / w;
            w = availableWidth;
            h = h * scale;
        }

        return new float[]{w, h};
    }

    // -----------------------------------------------------------------------
    // Accessors (used by Header for direct rendering)
    // -----------------------------------------------------------------------

    public String getImagePath()       { return imagePath; }
    public byte[] getImageBytes()      { return imageBytes; }
    public String getImageType()       { return imageType; }
    public float getRequestedWidth()   { return requestedWidth; }
    public float getRequestedHeight()  { return requestedHeight; }
}
