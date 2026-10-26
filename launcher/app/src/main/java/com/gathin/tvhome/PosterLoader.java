package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance, zero-dependency asynchronous movie poster image loader.
 * Features 2-tier caching (memory + local disk) with high-res fallback generation.
 */
public final class PosterLoader {

    private static PosterLoader instance;

    private final LruCache<String, Bitmap> memoryCache;
    private final File diskCacheDir;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private PosterLoader(Context context) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.max(1024, maxMemory / 8); // 1/8th of heap
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        diskCacheDir = new File(context.getCacheDir(), "posters");
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
    }

    public static synchronized PosterLoader get(Context context) {
        if (instance == null) {
            instance = new PosterLoader(context.getApplicationContext());
        }
        return instance;
    }

    public void loadPoster(String url, String title, double rating, ImageView target) {
        if (target == null) return;
        target.setTag(url);

        // 1. Check memory cache
        if (url != null && !url.isEmpty()) {
            Bitmap memBmp = memoryCache.get(url);
            if (memBmp != null) {
                target.setImageBitmap(memBmp);
                return;
            }
        }

        // 2. Set placeholder while loading
        Bitmap placeholder = createFallbackPoster(title, rating);
        target.setImageBitmap(placeholder);

        if (url == null || url.isEmpty()) return;

        // 3. Background fetch (disk cache -> network)
        executor.execute(() -> {
            Bitmap bmp = loadFromDisk(url);
            if (bmp == null) {
                bmp = downloadFromNetwork(url);
                if (bmp != null) {
                    saveToDisk(url, bmp);
                }
            }

            if (bmp != null) {
                memoryCache.put(url, bmp);
                final Bitmap finalBmp = bmp;
                mainHandler.post(() -> {
                    if (url.equals(target.getTag())) {
                        target.setImageBitmap(finalBmp);
                    }
                });
            }
        });
    }

    private String getDiskCacheKey(String url) {
        return "p_" + Math.abs(url.hashCode()) + ".jpg";
    }

    private Bitmap loadFromDisk(String url) {
        try {
            File file = new File(diskCacheDir, getDiskCacheKey(url));
            if (file.exists() && file.length() > 0) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void saveToDisk(String url, Bitmap bmp) {
        try {
            File file = new File(diskCacheDir, getDiskCacheKey(url));
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();
        } catch (Exception ignored) {}
    }

    private Bitmap downloadFromNetwork(String urlStr) {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                in = conn.getInputStream();
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565; // save 50% memory on Android TV
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception ignored) {
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public static Bitmap createFallbackPoster(String title, double rating) {
        int w = 180;
        int h = 270;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);

        // Poster gradient
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, w, h, 0xFF141F32, 0xFF050811, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // Border
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0x3300E5FF);
        p.setStrokeWidth(3f);
        canvas.drawRect(1.5f, 1.5f, w - 1.5f, h - 1.5f, p);

        // Title initial
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF00E5FF);
        p.setTextSize(44f);
        p.setTextAlign(Paint.Align.CENTER);
        String initial = (title != null && !title.isEmpty()) ? title.substring(0, 1).toUpperCase() : "M";
        canvas.drawText(initial, w / 2f, h / 2f - 10f, p);

        // Rating pill at bottom
        p.setColor(0xCC000000);
        RectF pill = new RectF(20f, h - 50f, w - 20f, h - 18f);
        canvas.drawRoundRect(pill, 12f, 12f, p);

        p.setColor(0xFFFFD700);
        p.setTextSize(18f);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("★ " + rating, w / 2f, h - 28f, p);

        return bmp;
    }
}
