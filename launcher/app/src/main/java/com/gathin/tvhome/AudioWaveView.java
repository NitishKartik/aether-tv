package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Animated sound wave visualizer.
 * Displays undulating, glowing audio equalizer bars synchronized with active media playback.
 */
public final class AudioWaveView extends View {

    private static final int NUM_BARS = 6;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private boolean isPlaying = false;

    // Relative animation phases for each bar
    private final float[] phaseOffsets = {0.0f, 1.2f, 2.5f, 0.8f, 1.9f, 2.9f};
    private final float[] speedMultipliers = {1.8f, 2.4f, 1.5f, 2.7f, 2.1f, 1.9f};
    private final float[] currentHeights = new float[NUM_BARS];

    public AudioWaveView(Context context) {
        super(context);
        init();
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < NUM_BARS; i++) {
            currentHeights[i] = 0.25f;
        }
    }

    public void setPlaying(boolean playing) {
        if (this.isPlaying != playing) {
            this.isPlaying = playing;
            postInvalidateOnAnimation();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float barWidth = (float) w / (NUM_BARS * 1.8f);
        float spacing = barWidth * 0.8f;
        float totalWidth = (NUM_BARS * barWidth) + ((NUM_BARS - 1) * spacing);
        float startX = (w - totalWidth) / 2f;

        long now = SystemClock.uptimeMillis();
        float timeSec = (now % 100000L) / 1000f;

        // Gradient shader across height: Neon Cyan to Vibrant Mint
        Shader shader = new LinearGradient(
                0, h, 0, 0,
                new int[]{Color.parseColor("#00E5FF"), Color.parseColor("#00E676")},
                null,
                Shader.TileMode.CLAMP
        );
        barPaint.setShader(shader);

        float cornerRadius = barWidth / 2f;

        for (int i = 0; i < NUM_BARS; i++) {
            float targetHeight;
            if (isPlaying) {
                // Wave formula: primary harmonic + secondary octave
                float p1 = (float) Math.sin(timeSec * speedMultipliers[i] * Math.PI + phaseOffsets[i]);
                float p2 = (float) Math.sin(timeSec * speedMultipliers[i] * 1.7f * Math.PI + phaseOffsets[i] * 1.3f);
                float wave = (p1 * 0.65f + p2 * 0.35f + 1f) / 2f; // normalize 0..1
                targetHeight = 0.18f + wave * 0.78f;
            } else {
                targetHeight = 0.18f;
            }

            // Smooth interpolation
            currentHeights[i] += (targetHeight - currentHeights[i]) * 0.25f;
            float actualBarH = Math.max(barWidth, h * currentHeights[i]);

            float left = startX + i * (barWidth + spacing);
            float top = h - actualBarH;
            float right = left + barWidth;
            float bottom = h;

            barRect.set(left, top, right, bottom);
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);
        }

        if (isPlaying) {
            postInvalidateOnAnimation();
        }
    }
}
