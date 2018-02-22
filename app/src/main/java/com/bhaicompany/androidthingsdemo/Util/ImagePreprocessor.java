package com.bhaicompany.androidthingsdemo.Util;

/**
 * Created by ashwin-4529 on 18/02/18.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import junit.framework.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Class that process an Image and extracts a Bitmap in a format appropriate
 */
public class ImagePreprocessor {
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private Bitmap rgbFrameBitmap;
    private Bitmap croppedBitmap;

    public ImagePreprocessor() {
        this.croppedBitmap = Bitmap.createBitmap(224, 224,
                Bitmap.Config.ARGB_8888);
        this.rgbFrameBitmap = Bitmap.createBitmap(CameraHandler.IMAGE_WIDTH,
                CameraHandler.IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    public Bitmap preprocessImage(final Image image) {
        if (image == null) {
            return null;
        }

        Assert.assertEquals("Invalid size width", rgbFrameBitmap.getWidth(), image.getWidth());
        Assert.assertEquals("Invalid size height", rgbFrameBitmap.getHeight(), image.getHeight());

        if (croppedBitmap != null && rgbFrameBitmap != null) {
            ByteBuffer bb = image.getPlanes()[0].getBuffer();
            rgbFrameBitmap = BitmapFactory.decodeStream(new ByteBufferBackedInputStream(bb));
            cropAndRescaleBitmap(rgbFrameBitmap, croppedBitmap, 0);
        }

        image.close();

        // For debugging
        if (SAVE_PREVIEW_BITMAP) {
            saveBitmap(croppedBitmap);
        }
        return croppedBitmap;
    }

    private static class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    public static void saveBitmap(final Bitmap bitmap) {
        final File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "preview.png");
        Log.d("ImageHelper", String.format("Saving %dx%d bitmap to %s.",
                bitmap.getWidth(), bitmap.getHeight(), file.getAbsolutePath()));

        if (file.exists()) {
            file.delete();
        }
        try (FileOutputStream fs = new FileOutputStream(file);
             BufferedOutputStream out = new BufferedOutputStream(fs)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
        } catch (final Exception e) {
            Log.w("ImageHelper", "Could not save image for debugging. " + e.getMessage());
        }
    }

    public static void cropAndRescaleBitmap(final Bitmap src, final Bitmap dst, int sensorOrientation) {
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (sensorOrientation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(sensorOrientation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }
}