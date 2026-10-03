package io.github.mrtabaofficial.wardline.utilities;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ImageUtils {

    private static final int MAX_DIMENSION = 256;
    private static final int JPEG_QUALITY = 75;

    private ImageUtils() {
    }

    public static Bitmap load(ContentResolver contentResolver, Uri uri) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            int largestSide = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
            decoder.setTargetSampleSize(Math.max(1, largestSide / (MAX_DIMENSION * 2)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }

    public static String encode(Bitmap bitmap) {
        float scale = Math.min(1f, (float) MAX_DIMENSION / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)),
                true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }

    @Nullable
    public static Bitmap decode(@Nullable String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
