package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Aether TV Unique Offline Arcade Game: Orbital Defender.
 * High-performance 60 FPS arcade loop designed specifically for Android TV remote D-pad controls.
 * Protect the planetary core from orbital meteors, hostile drones, and collect energy orbs!
 */
public final class OrbitalDefenderView extends View {

    public enum GameState {
        START,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private static final String PREFS_NAME = "aether_orbital_game";
    private static final String KEY_HIGH_SCORE = "high_score";

    private final SharedPreferences prefs;
    private final Random random = new Random();

    private GameState state = GameState.START;
    private int score = 0;
    private int highScore = 0;
    private int wave = 1;
    private float coreHealth = 100f; // 0 to 100
    private float plasmaEnergy = 100f; // 0 to 100

    // Ship orbit parameters
    private float shipAngle = 270f; // degrees
    private float shipOrbitRadius = 130f;
    private float targetShipAngle = 270f;

    // Entities
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<EnergyOrb> energyOrbs = new ArrayList<>();
    private final List<SparkParticle> sparks = new ArrayList<>();

    // Paints
    private final Paint planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint projectilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hazardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private long lastSpawnTime = 0;
    private long lastFrameTime = 0;

    public OrbitalDefenderView(Context context) {
        super(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        init();
    }

    public OrbitalDefenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.argb(50, 0, 229, 255));
        ringPaint.setStrokeWidth(2f);

        shipPaint.setColor(Color.parseColor("#00E5FF"));
        shipPaint.setStyle(Paint.Style.FILL);

        projectilePaint.setColor(Color.parseColor("#00FF66"));
        projectilePaint.setStyle(Paint.Style.FILL);

        hazardPaint.setColor(Color.parseColor("#FF5252"));
        hazardPaint.setStyle(Paint.Style.FILL);

        orbPaint.setColor(Color.parseColor("#FFD700"));
        orbPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);

        barPaint.setStyle(Paint.Style.FILL);
    }

    public void startNewGame() {
        score = 0;
        wave = 1;
        coreHealth = 100f;
        plasmaEnergy = 100f;
        shipAngle = 270f;
        targetShipAngle = 270f;
        projectiles.clear();
        hazards.clear();
        energyOrbs.clear();
        sparks.clear();
        state = GameState.PLAYING;
        invalidate();
    }

    public void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
        invalidate();
    }

    public GameState getGameState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (state == GameState.START || state == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                startNewGame();
                return true;
            }
        } else if (state == GameState.PLAYING) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                targetShipAngle -= 24f;
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                targetShipAngle += 24f;
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                shipOrbitRadius = Math.min(220f, shipOrbitRadius + 20f);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                shipOrbitRadius = Math.max(90f, shipOrbitRadius - 20f);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                firePlasma();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                togglePause();
                return true;
            }
        } else if (state == GameState.PAUSED) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MENU) {
                togglePause();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void firePlasma() {
        if (plasmaEnergy < 8f) return;
        plasmaEnergy = Math.max(0f, plasmaEnergy - 8f);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        double rad = Math.toRadians(shipAngle);
        float sx = cx + (float) Math.cos(rad) * shipOrbitRadius;
        float sy = cy + (float) Math.sin(rad) * shipOrbitRadius;

        float speed = 14f;
        float vx = (float) Math.cos(rad) * speed;
        float vy = (float) Math.sin(rad) * speed;

        projectiles.add(new Projectile(sx, sy, vx, vy));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;

        // Update physics if playing
        if (state == GameState.PLAYING) {
            updateGame(w, h, cx, cy);
        }

        // Draw deep space background
        canvas.drawColor(Color.parseColor("#FF050A14"));

        // Draw orbital tracks
        canvas.drawCircle(cx, cy, shipOrbitRadius, ringPaint);
        canvas.drawCircle(cx, cy, 60f, ringPaint);

        // Draw Planetary Core
        float coreRadius = 45f;
        RadialGradient coreGrad = new RadialGradient(
            cx, cy, coreRadius,
            new int[]{Color.WHITE, Color.parseColor("#00E5FF"), Color.parseColor("#7C4DFF"), Color.TRANSPARENT},
            new float[]{0.0f, 0.4f, 0.85f, 1.0f},
            Shader.TileMode.CLAMP
        );
        planetPaint.setShader(coreGrad);
        canvas.drawCircle(cx, cy, coreRadius, planetPaint);

        // Draw Energy Orbs
        for (EnergyOrb orb : energyOrbs) {
            canvas.drawCircle(orb.x, orb.y, orb.radius, orbPaint);
        }

        // Draw Hazards (Asteroids)
        for (Hazard hazard : hazards) {
            hazardPaint.setColor(hazard.isDrone ? Color.parseColor("#FF1744") : Color.parseColor("#FF9100"));
            canvas.drawCircle(hazard.x, hazard.y, hazard.radius, hazardPaint);
        }

        // Draw Projectiles
        for (Projectile p : projectiles) {
            canvas.drawCircle(p.x, p.y, 4f, projectilePaint);
        }

        // Draw Sparks
        for (SparkParticle s : sparks) {
            barPaint.setColor(s.color);
            barPaint.setAlpha((int) (255 * (s.life / s.maxLife)));
            canvas.drawCircle(s.x, s.y, 3f, barPaint);
        }

        // Draw Player Starship
        double shipRad = Math.toRadians(shipAngle);
        float sx = cx + (float) Math.cos(shipRad) * shipOrbitRadius;
        float sy = cy + (float) Math.sin(shipRad) * shipOrbitRadius;
        canvas.save();
        canvas.translate(sx, sy);
        canvas.rotate(shipAngle + 90f);
        android.graphics.Path shipPath = new android.graphics.Path();
        shipPath.moveTo(0, -14);
        shipPath.lineTo(-10, 10);
        shipPath.lineTo(0, 4);
        shipPath.lineTo(10, 10);
        shipPath.close();
        canvas.drawPath(shipPath, shipPaint);
        canvas.restore();

        // Draw HUD
        drawHud(canvas, w, h);

        // Draw States Overlay
        if (state == GameState.START) {
            drawOverlay(canvas, w, h, "ORBITAL DEFENDER", "Press DPAD OK to Launch Mission\nUse DPAD Left/Right to Orbit, OK to Fire Plasma");
        } else if (state == GameState.PAUSED) {
            drawOverlay(canvas, w, h, "MISSION PAUSED", "Press DPAD OK or MENU to Resume");
        } else if (state == GameState.GAME_OVER) {
            drawOverlay(canvas, w, h, "CORE COMPROMISED", "Final Score: " + score + "\nPress DPAD OK to Re-engage");
        }

        if (state == GameState.PLAYING) {
            postInvalidateOnAnimation();
        }
    }

    private void updateGame(int w, int h, float cx, float cy) {
        long now = System.currentTimeMillis();

        // Smooth ship rotation
        shipAngle += (targetShipAngle - shipAngle) * 0.25f;

        // Recharge plasma energy
        plasmaEnergy = Math.min(100f, plasmaEnergy + 0.35f);

        // Spawn hazards
        long spawnInterval = Math.max(700, 2200 - (wave * 150));
        if (now - lastSpawnTime > spawnInterval) {
            lastSpawnTime = now;
            spawnHazard(w, h, cx, cy);
            if (random.nextFloat() < 0.3f) {
                spawnOrb(cx, cy);
            }
        }

        // Update Projectiles
        Iterator<Projectile> pIter = projectiles.iterator();
        while (pIter.hasNext()) {
            Projectile p = pIter.next();
            p.x += p.vx;
            p.y += p.vy;
            if (p.x < 0 || p.x > w || p.y < 0 || p.y > h) {
                pIter.remove();
            }
        }

        // Update Energy Orbs
        double shipRad = Math.toRadians(shipAngle);
        float sx = cx + (float) Math.cos(shipRad) * shipOrbitRadius;
        float sy = cy + (float) Math.sin(shipRad) * shipOrbitRadius;

        Iterator<EnergyOrb> orbIter = energyOrbs.iterator();
        while (orbIter.hasNext()) {
            EnergyOrb orb = orbIter.next();
            orb.x += orb.vx;
            orb.y += orb.vy;

            // Distance to ship
            float distToShip = (float) Math.hypot(orb.x - sx, orb.y - sy);
            if (distToShip < 24f) {
                score += 50;
                coreHealth = Math.min(100f, coreHealth + 15f);
                plasmaEnergy = Math.min(100f, plasmaEnergy + 30f);
                spawnSparks(orb.x, orb.y, Color.parseColor("#FFD700"), 12);
                orbIter.remove();
            }
        }

        // Update Hazards & Collisions
        Iterator<Hazard> hIter = hazards.iterator();
        while (hIter.hasNext()) {
            Hazard hz = hIter.next();
            hz.x += hz.vx;
            hz.y += hz.vy;

            // Check hit with projectiles
            boolean destroyed = false;
            Iterator<Projectile> projIter = projectiles.iterator();
            while (projIter.hasNext()) {
                Projectile p = projIter.next();
                if (Math.hypot(p.x - hz.x, p.y - hz.y) < hz.radius + 6f) {
                    projIter.remove();
                    hz.hp--;
                    spawnSparks(hz.x, hz.y, hz.isDrone ? Color.RED : Color.parseColor("#FF9100"), 8);
                    if (hz.hp <= 0) {
                        destroyed = true;
                        score += hz.points;
                        if (score > highScore) {
                            highScore = score;
                            prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
                        }
                        if (score >= wave * 500) {
                            wave++;
                        }
                        break;
                    }
                }
            }

            if (destroyed) {
                hIter.remove();
                continue;
            }

            // Check hit with core
            if (Math.hypot(hz.x - cx, hz.y - cy) < 45f + hz.radius) {
                coreHealth -= hz.damage;
                spawnSparks(cx, cy, Color.RED, 20);
                hIter.remove();
                if (coreHealth <= 0f) {
                    coreHealth = 0f;
                    state = GameState.GAME_OVER;
                }
            }
        }

        // Update Sparks
        Iterator<SparkParticle> sIter = sparks.iterator();
        while (sIter.hasNext()) {
            SparkParticle s = sIter.next();
            s.x += s.vx;
            s.y += s.vy;
            s.life -= 1f;
            if (s.life <= 0f) {
                sIter.remove();
            }
        }
    }

    private void spawnHazard(int w, int h, float cx, float cy) {
        float angle = random.nextFloat() * 360f;
        double rad = Math.toRadians(angle);
        float dist = Math.max(w, h) * 0.6f;
        float x = cx + (float) Math.cos(rad) * dist;
        float y = cy + (float) Math.sin(rad) * dist;

        float targetX = cx + (random.nextFloat() - 0.5f) * 40f;
        float targetY = cy + (random.nextFloat() - 0.5f) * 40f;
        float speed = 1.8f + (wave * 0.35f);
        float dx = targetX - x;
        float dy = targetY - y;
        float len = (float) Math.hypot(dx, dy);

        boolean isDrone = (wave >= 2) && random.nextFloat() < 0.35f;
        hazards.add(new Hazard(x, y, (dx / len) * speed, (dy / len) * speed,
            isDrone ? 12f : 16f, isDrone ? 1 : 2, isDrone ? 15f : 10f, isDrone ? 150 : 100, isDrone));
    }

    private void spawnOrb(float cx, float cy) {
        float angle = random.nextFloat() * 360f;
        double rad = Math.toRadians(angle);
        float r = 80f + random.nextFloat() * 100f;
        float x = cx + (float) Math.cos(rad) * r;
        float y = cy + (float) Math.sin(rad) * r;
        energyOrbs.add(new EnergyOrb(x, y, (float) Math.cos(rad + Math.PI/2) * 0.6f, (float) Math.sin(rad + Math.PI/2) * 0.6f, 8f));
    }

    private void spawnSparks(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float ang = random.nextFloat() * (float) Math.PI * 2f;
            float spd = 2f + random.nextFloat() * 5f;
            sparks.add(new SparkParticle(x, y, (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd, color, 15f + random.nextFloat() * 15f));
        }
    }

    private void drawHud(Canvas canvas, int w, int h) {
        // Score & Wave
        textPaint.setTextSize(22f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("SCORE: " + score, 48f, 52f, textPaint);
        canvas.drawText("WAVE: " + wave, 48f, 82f, textPaint);

        textPaint.setColor(Color.parseColor("#FF00E5FF"));
        canvas.drawText("HIGH SCORE: " + highScore, 48f, 112f, textPaint);

        // Core Health Bar (Bottom Left)
        float barWidth = 180f;
        float barHeight = 12f;
        barPaint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRect(48f, h - 54f, 48f + barWidth, h - 54f + barHeight, barPaint);

        barPaint.setColor(coreHealth > 30f ? Color.parseColor("#00E676") : Color.RED);
        canvas.drawRect(48f, h - 54f, 48f + (barWidth * (coreHealth / 100f)), h - 54f + barHeight, barPaint);

        textPaint.setTextSize(14f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("CORE SHIELD: " + (int) coreHealth + "%", 48f, h - 64f, textPaint);

        // Plasma Ammo Bar (Bottom Right)
        barPaint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRect(w - 228f, h - 54f, w - 48f, h - 54f + barHeight, barPaint);

        barPaint.setColor(Color.parseColor("#00E5FF"));
        canvas.drawRect(w - 228f, h - 54f, w - 228f + (barWidth * (plasmaEnergy / 100f)), h - 54f + barHeight, barPaint);

        canvas.drawText("PLASMA ENERGY: " + (int) plasmaEnergy + "%", w - 228f, h - 64f, textPaint);
    }

    private void drawOverlay(Canvas canvas, int w, int h, String title, String subtitle) {
        barPaint.setColor(Color.argb(190, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, barPaint);

        textPaint.setTextSize(40f);
        textPaint.setColor(Color.parseColor("#00E5FF"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, w / 2f, h / 2f - 40f, textPaint);

        textPaint.setTextSize(18f);
        textPaint.setColor(Color.WHITE);
        String[] lines = subtitle.split("\n");
        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], w / 2f, h / 2f + 16f + (i * 28f), textPaint);
        }
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    // Entity Data Classes
    private static class Projectile {
        float x, y, vx, vy;
        Projectile(float x, float y, float vx, float vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }

    private static class Hazard {
        float x, y, vx, vy, radius, hp, damage;
        int points;
        boolean isDrone;
        Hazard(float x, float y, float vx, float vy, float radius, float hp, float damage, int points, boolean isDrone) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.radius = radius; this.hp = hp; this.damage = damage;
            this.points = points; this.isDrone = isDrone;
        }
    }

    private static class EnergyOrb {
        float x, y, vx, vy, radius;
        EnergyOrb(float x, float y, float vx, float vy, float radius) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius;
        }
    }

    private static class SparkParticle {
        float x, y, vx, vy, life, maxLife;
        int color;
        SparkParticle(float x, float y, float vx, float vy, int color, float maxLife) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.life = maxLife; this.maxLife = maxLife;
        }
    }
}
