package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Aether Drift: Cyber Highway TV Arcade Racer.
 * 60 FPS neon driving experience with full Android TV Remote D-pad compatibility.
 */
public final class AetherDriftView extends View {

    public enum GameState {
        READY, PLAYING, PAUSED, GAME_OVER
    }

    private static final String PREFS_NAME = "aether_drift_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";

    private final SharedPreferences prefs;
    private final Random random = new Random();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path carPath = new Path();

    private GameState state = GameState.READY;
    private int score = 0;
    private int highScore = 0;
    private float distanceMeters = 0f;
    private float speed = 90f; // km/h
    private boolean isBoosting = false;
    private boolean isBraking = false;

    // Road & Lanes (3 lanes: 0 = left, 1 = center, 2 = right)
    private int playerLane = 1;
    private float playerX = 0f;
    private float targetPlayerX = 0f;
    private float playerY = 0f;
    private float laneWidth = 0f;
    private float roadLeft = 0f;
    private float roadRight = 0f;
    private float roadScroll = 0f;

    // Traffic obstacles
    private static class TrafficCar {
        float x;
        float y;
        int lane;
        float speed;
        int color;
        int type; // 0 = sports, 1 = truck, 2 = cyber
        boolean passed;
    }

    // Energy orbs
    private static class EnergyOrb {
        float x;
        float y;
        int lane;
        boolean collected;
    }

    // Particle explosions
    private static class SparkParticle {
        float x, y, vx, vy;
        int color;
        float alpha;
        float size;
    }

    private final List<TrafficCar> traffic = new ArrayList<>();
    private final List<EnergyOrb> orbs = new ArrayList<>();
    private final List<SparkParticle> sparks = new ArrayList<>();

    private long lastFrameTime = 0;
    private long lastTrafficSpawn = 0;
    private long lastOrbSpawn = 0;

    public AetherDriftView(Context context) {
        this(context, null);
    }

    public AetherDriftView(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public GameState getGameState() {
        return state;
    }

    public void startGame() {
        state = GameState.PLAYING;
        score = 0;
        distanceMeters = 0f;
        speed = 90f;
        playerLane = 1;
        targetPlayerX = getLaneCenterX(1);
        playerX = targetPlayerX;
        traffic.clear();
        orbs.clear();
        sparks.clear();
        lastFrameTime = SystemClock.elapsedRealtime();
        lastTrafficSpawn = lastFrameTime;
        lastOrbSpawn = lastFrameTime;
        invalidate();
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            lastFrameTime = SystemClock.elapsedRealtime();
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float roadWidth = Math.min(w * 0.65f, 640f);
        roadLeft = (w - roadWidth) / 2f;
        roadRight = roadLeft + roadWidth;
        laneWidth = roadWidth / 3f;
        playerY = h * 0.78f;
        targetPlayerX = getLaneCenterX(playerLane);
        playerX = targetPlayerX;
    }

    private float getLaneCenterX(int lane) {
        return roadLeft + (lane + 0.5f) * laneWidth;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (state == GameState.READY || state == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_SPACE) {
                startGame();
                return true;
            }
        } else if (state == GameState.PLAYING) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (playerLane > 0) {
                        playerLane--;
                        targetPlayerX = getLaneCenterX(playerLane);
                        SoundManager.getInstance(getContext()).playGameDrift();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (playerLane < 2) {
                        playerLane++;
                        targetPlayerX = getLaneCenterX(playerLane);
                        SoundManager.getInstance(getContext()).playGameDrift();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    isBoosting = true;
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    isBraking = true;
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    togglePause();
                    return true;
            }
        } else if (state == GameState.PAUSED) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                togglePause();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            isBoosting = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            isBraking = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        long now = SystemClock.elapsedRealtime();
        float dt = (lastFrameTime > 0) ? (now - lastFrameTime) / 1000f : 0.016f;
        if (dt > 0.05f) dt = 0.05f; // prevent simulation explosion
        lastFrameTime = now;

        if (state == GameState.PLAYING) {
            updateGame(dt, now, h);
        }

        drawBackdrop(canvas, w, h);
        drawRoad(canvas, h);
        drawOrbs(canvas);
        drawTraffic(canvas);
        drawPlayerCar(canvas);
        drawSparks(canvas);
        drawHud(canvas, w, h);

        if (state == GameState.READY) {
            drawOverlayBanner(canvas, w, h, "AETHER DRIFT", "HIGHWAY CYBER RACER", "Press [OK] on Remote D-pad to Start\nUse [←] and [→] to Steer, [↑] Nitro Boost, [↓] Brake");
        } else if (state == GameState.PAUSED) {
            drawOverlayBanner(canvas, w, h, "PAUSED", "CRUISING AT " + (int) speed + " KM/H", "Press [OK] to Resume");
        } else if (state == GameState.GAME_OVER) {
            drawOverlayBanner(canvas, w, h, "TERMINAL IMPACT", "FINAL SCORE: " + score + "  //  " + (int) distanceMeters + " METERS", "Press [OK] on Remote to Race Again");
        }

        if (state == GameState.PLAYING) {
            postInvalidateOnAnimation();
        }
    }

    private void updateGame(float dt, long now, int h) {
        // Adjust speed based on boost/brake
        float targetSpeed = isBoosting ? 175f : (isBraking ? 50f : 100f);
        speed += (targetSpeed - speed) * 4f * dt;

        float pixelsPerMeter = 5.5f;
        float speedPixelsPerSec = (speed * 1000f / 3600f) * pixelsPerMeter;
        roadScroll = (roadScroll + speedPixelsPerSec * dt) % 80f;
        distanceMeters += (speed * 1000f / 3600f) * dt;
        score += Math.round((speed / 20f) * dt * 10f);

        // Smooth steering interpolation
        playerX += (targetPlayerX - playerX) * 14f * dt;

        // Spawn traffic
        long spawnInterval = isBoosting ? 1200 : 1600;
        if (now - lastTrafficSpawn > spawnInterval) {
            lastTrafficSpawn = now;
            TrafficCar tc = new TrafficCar();
            tc.lane = random.nextInt(3);
            tc.x = getLaneCenterX(tc.lane);
            tc.y = -120f;
            tc.type = random.nextInt(3);
            tc.speed = 35f + random.nextInt(35); // relative speed
            int[] palette = {0xFFFF3366, 0xFFFFCC00, 0xFF9933FF, 0xFF00E5FF};
            tc.color = palette[random.nextInt(palette.length)];
            traffic.add(tc);
        }

        // Spawn energy orbs
        if (now - lastOrbSpawn > 2200) {
            lastOrbSpawn = now;
            EnergyOrb orb = new EnergyOrb();
            orb.lane = random.nextInt(3);
            orb.x = getLaneCenterX(orb.lane);
            orb.y = -80f;
            orbs.add(orb);
        }

        // Update traffic positions
        Iterator<TrafficCar> it = traffic.iterator();
        while (it.hasNext()) {
            TrafficCar tc = it.next();
            float relativeSpeed = (speed - tc.speed);
            tc.y += (relativeSpeed * 1000f / 3600f) * pixelsPerMeter * dt;

            // Check collision with player car (car width ~44, height ~80)
            if (Math.abs(tc.x - playerX) < 42f && Math.abs(tc.y - playerY) < 76f) {
                triggerGameOver(playerX, playerY);
                return;
            }

            if (!tc.passed && tc.y > playerY + 80f) {
                tc.passed = true;
                score += 150; // Close pass bonus!
            }

            if (tc.y > h + 150f || tc.y < -300f) {
                it.remove();
            }
        }

        // Update energy orbs
        Iterator<EnergyOrb> orbIt = orbs.iterator();
        while (orbIt.hasNext()) {
            EnergyOrb orb = orbIt.next();
            orb.y += (speed * 1000f / 3600f) * pixelsPerMeter * dt;

            if (Math.abs(orb.x - playerX) < 44f && Math.abs(orb.y - playerY) < 50f) {
                orbIt.remove();
                score += 350;
                SoundManager.getInstance(getContext()).playGameCoin();
                spawnCollectSparks(orb.x, orb.y);
                continue;
            }

            if (orb.y > h + 50f) {
                orbIt.remove();
            }
        }

        // Update sparks
        Iterator<SparkParticle> sIt = sparks.iterator();
        while (sIt.hasNext()) {
            SparkParticle sp = sIt.next();
            sp.x += sp.vx * dt;
            sp.y += sp.vy * dt;
            sp.alpha -= dt * 1.5f;
            if (sp.alpha <= 0f) sIt.remove();
        }
    }

    private void triggerGameOver(float cx, float cy) {
        state = GameState.GAME_OVER;
        SoundManager.getInstance(getContext()).playGameCrash();
        if (score > highScore) {
            highScore = score;
            prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
        }
        for (int i = 0; i < 40; i++) {
            SparkParticle sp = new SparkParticle();
            sp.x = cx;
            sp.y = cy;
            float angle = (float) (random.nextDouble() * Math.PI * 2);
            float vel = 80f + random.nextFloat() * 300f;
            sp.vx = (float) Math.cos(angle) * vel;
            sp.vy = (float) Math.sin(angle) * vel;
            sp.color = (random.nextBoolean()) ? 0xFFFF3366 : 0xFFFFCC00;
            sp.alpha = 1f;
            sp.size = 4f + random.nextFloat() * 8f;
            sparks.add(sp);
        }
    }

    private void spawnCollectSparks(float cx, float cy) {
        for (int i = 0; i < 16; i++) {
            SparkParticle sp = new SparkParticle();
            sp.x = cx;
            sp.y = cy;
            float angle = (float) (random.nextDouble() * Math.PI * 2);
            float vel = 60f + random.nextFloat() * 150f;
            sp.vx = (float) Math.cos(angle) * vel;
            sp.vy = (float) Math.sin(angle) * vel;
            sp.color = 0xFF00E5FF;
            sp.alpha = 1f;
            sp.size = 3f + random.nextFloat() * 5f;
            sparks.add(sp);
        }
    }

    private void drawBackdrop(Canvas canvas, int w, int h) {
        // Deep cyber night gradient
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, h, 0xFF050813, 0xFF0D1424, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Ambient cyber speed lines on the sides
        paint.setColor(0x1800E5FF);
        paint.setStrokeWidth(1.5f);
        for (int i = 0; i < 8; i++) {
            float x1 = roadLeft * (i / 8f);
            canvas.drawLine(x1, 0, x1 * 0.7f, h, paint);
            float x2 = roadRight + (w - roadRight) * (i / 8f);
            canvas.drawLine(x2, 0, w - (w - x2) * 0.7f, h, paint);
        }
    }

    private void drawRoad(Canvas canvas, int h) {
        // Asphalt fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF101726);
        canvas.drawRect(roadLeft, 0, roadRight, h, paint);

        // Neon road shoulder glow
        paint.setColor(0xFF00E5FF);
        paint.setStrokeWidth(3.5f);
        canvas.drawLine(roadLeft, 0, roadLeft, h, paint);
        canvas.drawLine(roadRight, 0, roadRight, h, paint);

        // Dashed lane dividers
        paint.setColor(0x66FFFFFF);
        paint.setStrokeWidth(2f);
        for (int l = 1; l <= 2; l++) {
            float lx = roadLeft + l * laneWidth;
            float dashLen = 30f;
            float dashGap = 25f;
            float step = dashLen + dashGap;
            float startY = -step + (roadScroll % step);
            while (startY < h) {
                canvas.drawLine(lx, Math.max(0, startY), lx, Math.min(h, startY + dashLen), paint);
                startY += step;
            }
        }
    }

    private void drawPlayerCar(Canvas canvas) {
        float cx = playerX;
        float cy = playerY;

        // Neon underglow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isBoosting ? 0x66FF3366 : 0x6600E5FF);
        canvas.drawCircle(cx, cy, 38f, paint);

        // Turbo exhaust flames when boosting
        if (isBoosting) {
            paint.setColor(0xFFFF5252);
            canvas.drawRect(cx - 12f, cy + 38f, cx - 4f, cy + 54f + random.nextFloat() * 14f, paint);
            canvas.drawRect(cx + 4f, cy + 38f, cx + 12f, cy + 54f + random.nextFloat() * 14f, paint);
            paint.setColor(0xFFFFD700);
            canvas.drawRect(cx - 10f, cy + 38f, cx - 6f, cy + 46f, paint);
            canvas.drawRect(cx + 6f, cy + 38f, cx + 10f, cy + 46f, paint);
        }

        // Car chassis
        paint.setColor(0xFF00E5FF);
        RectF body = new RectF(cx - 18f, cy - 36f, cx + 18f, cy + 36f);
        canvas.drawRoundRect(body, 8f, 8f, paint);

        // Windshield and cockpit
        paint.setColor(0xFF0A0F1D);
        RectF glass = new RectF(cx - 13f, cy - 16f, cx + 13f, cy + 14f);
        canvas.drawRoundRect(glass, 4f, 4f, paint);

        // Headlights (beaming forward)
        paint.setColor(0xFFFFFFFF);
        canvas.drawRect(cx - 16f, cy - 36f, cx - 10f, cy - 32f, paint);
        canvas.drawRect(cx + 10f, cy - 36f, cx + 16f, cy - 32f, paint);

        // Taillights
        paint.setColor(isBraking ? 0xFFFF1744 : 0xCCFF1744);
        canvas.drawRect(cx - 16f, cy + 32f, cx - 8f, cy + 36f, paint);
        canvas.drawRect(cx + 8f, cy + 32f, cx + 16f, cy + 36f, paint);
    }

    private void drawTraffic(Canvas canvas) {
        for (TrafficCar tc : traffic) {
            float cx = tc.x;
            float cy = tc.y;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(tc.color);
            RectF body = new RectF(cx - 17f, cy - 34f, cx + 17f, cy + 34f);
            canvas.drawRoundRect(body, 6f, 6f, paint);

            // Roof glass
            paint.setColor(0xFF0A0F1D);
            RectF glass = new RectF(cx - 12f, cy - 14f, cx + 12f, cy + 12f);
            canvas.drawRoundRect(glass, 3f, 3f, paint);

            // Taillights
            paint.setColor(0xFFFF1744);
            canvas.drawRect(cx - 14f, cy + 30f, cx - 8f, cy + 34f, paint);
            canvas.drawRect(cx + 8f, cy + 30f, cx + 14f, cy + 34f, paint);
        }
    }

    private void drawOrbs(Canvas canvas) {
        for (EnergyOrb orb : orbs) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x4400E5FF);
            canvas.drawCircle(orb.x, orb.y, 18f, paint);
            paint.setColor(0xFF00E5FF);
            canvas.drawCircle(orb.x, orb.y, 9f, paint);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(orb.x, orb.y, 4f, paint);
        }
    }

    private void drawSparks(Canvas canvas) {
        for (SparkParticle sp : sparks) {
            paint.setStyle(Paint.Style.FILL);
            int alphaInt = Math.max(0, Math.min(255, (int) (sp.alpha * 255)));
            paint.setColor((alphaInt << 24) | (sp.color & 0x00FFFFFF));
            canvas.drawCircle(sp.x, sp.y, sp.size, paint);
        }
    }

    private void drawHud(Canvas canvas, int w, int h) {
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(20f);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Top left: Score & Distance
        canvas.drawText("SCORE: " + score, 40f, 44f, textPaint);
        textPaint.setColor(0x99FFFFFF);
        textPaint.setTextSize(14f);
        canvas.drawText("DIST: " + (int) distanceMeters + " M  //  HIGH: " + highScore, 40f, 68f, textPaint);

        // Top right: Speedometer & Turbo
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(isBoosting ? 0xFFFF3366 : (isBraking ? 0xFFFFCC00 : 0xFF00E5FF));
        textPaint.setTextSize(26f);
        canvas.drawText((int) speed + " KM/H", w - 40f, 48f, textPaint);
        textPaint.setColor(0x99FFFFFF);
        textPaint.setTextSize(12f);
        canvas.drawText(isBoosting ? "● NITRO BOOST ACTIVE" : "CRUISING", w - 40f, 68f, textPaint);
    }

    private void drawOverlayBanner(Canvas canvas, int w, int h, String title, String subtitle, String prompt) {
        // Frosted scrim
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xCC050914);
        canvas.drawRect(0, 0, w, h, paint);

        // Glass banner card
        float cardW = Math.min(w * 0.7f, 600f);
        float cardH = 220f;
        float left = (w - cardW) / 2f;
        float top = (h - cardH) / 2f;
        RectF card = new RectF(left, top, left + cardW, top + cardH);

        paint.setColor(0x33101E36);
        canvas.drawRoundRect(card, 20f, 20f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x4D00E5FF);
        paint.setStrokeWidth(2f);
        canvas.drawRoundRect(card, 20f, 20f, paint);

        // Text
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF00E5FF);
        textPaint.setTextSize(28f);
        canvas.drawText(title, w / 2f, top + 55f, textPaint);

        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(16f);
        canvas.drawText(subtitle, w / 2f, top + 95f, textPaint);

        textPaint.setColor(0xCCFFFFFF);
        textPaint.setTextSize(13f);
        String[] lines = prompt.split("\n");
        float py = top + 140f;
        for (String line : lines) {
            canvas.drawText(line, w / 2f, py, textPaint);
            py += 22f;
        }
    }
}
