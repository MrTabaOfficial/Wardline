package io.github.mrtabaofficial.wardline.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.Nullable;

public final class ImageUtils {

    private ImageUtils() {
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
