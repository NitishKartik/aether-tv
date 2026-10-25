package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Aether TV Futuristic Holographic Jarvis Assistant View.
 * Renders multi-layered concentric orbital rings, pulsating central arc-reactor core,
 * and audio-reactive waveforms across IDLE, LISTENING, PROCESSING, SPEAKING, and ERROR states.
 */
public final class JarvisView extends View {

    public enum State {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING,
        ERROR
    }

    private State currentState = State.IDLE;
    private float audioLevel = 0f; // 0.0 to 1.0
    private float ringAngle1 = 0f;
    private float ringAngle2 = 0f;
    private float ringAngle3 = 0f;
    private float pulsePhase = 0f;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect1 = new RectF();
    private final RectF arcRect2 = new RectF();
    private final RectF arcRect3 = new RectF();

    public JarvisView(Context context) {
        super(context);
        init();
    }

    public JarvisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3f);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(4f);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(2f);
    }

    public void setState(State state) {
        this.currentState = state;
        invalidate();
    }

    public State getState() {
        return currentState;
    }

    public void setAudioLevel(float level) {
        this.audioLevel = Math.max(0f, Math.min(1f, level));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float baseRadius = Math.min(w, h) * 0.42f;

        // Advance animation phases
        float speed = (currentState == State.PROCESSING) ? 3.5f :
                      (currentState == State.LISTENING) ? 1.8f : 0.8f;
        ringAngle1 = (ringAngle1 + 1.2f * speed) % 360f;
        ringAngle2 = (ringAngle2 - 0.9f * speed) % 360f;
        ringAngle3 = (ringAngle3 + 1.5f * speed) % 360f;
        pulsePhase = (pulsePhase + 0.05f * speed) % ((float) (Math.PI * 2));

        // Colors based on state
        int primaryColor = Color.parseColor("#00E5FF"); // Cyan Hologram
        int secondaryColor = Color.parseColor("#7C4DFF"); // Neon Purple
        if (currentState == State.LISTENING) {
            primaryColor = Color.parseColor("#00FF66"); // Active Voice Green
            secondaryColor = Color.parseColor("#00E5FF");
        } else if (currentState == State.ERROR) {
            primaryColor = Color.parseColor("#FF5252"); // Alert Red
            secondaryColor = Color.parseColor("#FF9100");
        }

        // 1. Central Pulsing Arc-Reactor Core
        float coreRadius = baseRadius * 0.28f * (1f + 0.12f * (float) Math.sin(pulsePhase) + 0.25f * audioLevel);
        RadialGradient coreGrad = new RadialGradient(
            cx, cy, coreRadius,
            new int[]{Color.WHITE, primaryColor, secondaryColor, Color.TRANSPARENT},
            new float[]{0.0f, 0.35f, 0.75f, 1.0f},
            Shader.TileMode.CLAMP
        );
        corePaint.setShader(coreGrad);
        canvas.drawCircle(cx, cy, coreRadius, corePaint);

        // 2. Audio-reactive Waveform / Pulse Burst
        if (currentState == State.LISTENING || currentState == State.SPEAKING) {
            wavePaint.setColor(primaryColor);
            int numBars = 32;
            float waveRadius = baseRadius * 0.38f;
            for (int i = 0; i < numBars; i++) {
                double angleRad = Math.toRadians((360.0 / numBars) * i + ringAngle1);
                float barHeight = 8f + (audioLevel * 45f * (float) Math.abs(Math.sin(pulsePhase + i * 0.5)));
                float x1 = cx + (float) Math.cos(angleRad) * waveRadius;
                float y1 = cy + (float) Math.sin(angleRad) * waveRadius;
                float x2 = cx + (float) Math.cos(angleRad) * (waveRadius + barHeight);
                float y2 = cy + (float) Math.sin(angleRad) * (waveRadius + barHeight);
                canvas.drawLine(x1, y1, x2, y2, wavePaint);
            }
        }

        // 3. Orbital Ring 1 (Inner Segmented)
        float r1 = baseRadius * 0.55f;
        arcRect1.set(cx - r1, cy - r1, cx + r1, cy + r1);
        ringPaint.setColor(primaryColor);
        ringPaint.setStrokeWidth(2.5f);
        canvas.drawArc(arcRect1, ringAngle1, 100, false, ringPaint);
        canvas.drawArc(arcRect1, ringAngle1 + 140, 60, false, ringPaint);
        canvas.drawArc(arcRect1, ringAngle1 + 240, 80, false, ringPaint);

        // 4. Orbital Ring 2 (Middle Gyroscope with Ticks)
        float r2 = baseRadius * 0.75f;
        arcRect2.set(cx - r2, cy - r2, cx + r2, cy + r2);
        ringPaint.setColor(secondaryColor);
        ringPaint.setStrokeWidth(3.5f);
        canvas.drawArc(arcRect2, ringAngle2, 140, false, ringPaint);
        canvas.drawArc(arcRect2, ringAngle2 + 180, 110, false, ringPaint);

        // Draw ticks around ring 2
        tickPaint.setColor(Color.argb(160, 255, 255, 255));
        for (int i = 0; i < 24; i++) {
            double tickAngle = Math.toRadians((360.0 / 24) * i + ringAngle2);
            float tx1 = cx + (float) Math.cos(tickAngle) * (r2 - 6f);
            float ty1 = cy + (float) Math.sin(tickAngle) * (r2 - 6f);
            float tx2 = cx + (float) Math.cos(tickAngle) * (r2 + 6f);
            float ty2 = cy + (float) Math.sin(tickAngle) * (r2 + 6f);
            canvas.drawLine(tx1, ty1, tx2, ty2, tickPaint);
        }

        // 5. Orbital Ring 3 (Outer Sensor Perimeter)
        float r3 = baseRadius * 0.95f;
        arcRect3.set(cx - r3, cy - r3, cx + r3, cy + r3);
        ringPaint.setColor(primaryColor);
        ringPaint.setStrokeWidth(2f);
        canvas.drawArc(arcRect3, ringAngle3, 70, false, ringPaint);
        canvas.drawArc(arcRect3, ringAngle3 + 120, 40, false, ringPaint);
        canvas.drawArc(arcRect3, ringAngle3 + 200, 100, false, ringPaint);

        // Request next animation frame for smooth 60 FPS TV rendering
        postInvalidateOnAnimation();
    }
}
