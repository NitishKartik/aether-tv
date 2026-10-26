package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * Full-window immersive App Library matching Aether TV UI aesthetic and layout effects.
 */
final class AppLibraryDialog {

    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private static final Map<String, Known> KNOWN = new HashMap<>();

    private static final class Known {
        final long updated;
        final String label;

        Known(long updated, String label) {
            this.updated = updated;
            this.label = label;
        }
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Drawable> icons = new HashMap<>();
    private final Set<String> iconsLoading = new HashSet<>();
    private final Runnable dismissed;
    private boolean closed;
    private final LibraryLoads loads = new LibraryLoads();
    private final Activity activity;
    private final Predicate<String> onHome;
    private final Runnable changed;
    private final LinearLayout panel;
    private final boolean reducedMotion;
    private final boolean selectionMode;
    private final boolean okOpens;
    private final boolean animations;
    private final boolean rtl;
    private TextView instructions;
    private int totalApps;
    final PopupWindow popup;
    private final float density;
    private List<ResolveInfo> apps = new ArrayList<>();
    private String selectedPackage;
    private int selectedPosition;
    private final Map<String, Long> installTimes = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();
    private long catalogTime;
    private PopupWindow actionPopup;
    private RecyclerView catalog;

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), 0x22FFFFFF);
        return bg;
    }

    AppLibraryDialog(Activity activity, boolean animations, boolean reducedMotion, boolean selectionMode,
                     boolean okOpens, Predicate<String> onHome, Runnable changed, Runnable dismissed) {
        this.activity = activity;
        this.animations = animations;
        this.rtl = activity.getResources().getBoolean(R.bool.rtl_app_grid);
        this.reducedMotion = reducedMotion;
        this.selectionMode = selectionMode;
        this.okOpens = okOpens && !selectionMode;
        this.onHome = onHome;
        this.changed = changed;
        this.dismissed = dismissed;
        this.density = activity.getResources().getDisplayMetrics().density;

        panel = new LinearLayout(activity);
        panel.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xF5080C16);
        panel.setPadding(dp(56), dp(26), dp(56), dp(28));

        popup = new PopupWindow(panel, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setOnDismissListener(() -> {
            closed = true;
            dismissActions();
            icons.clear();
            dismissed.run();
        });

        panel.setFocusableInTouchMode(true);
        panel.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                close();
                return true;
            }
            return false;
        });
    }

    boolean isOpen() {
        return popup != null && popup.isShowing();
    }

    void close() {
        dismissActions();
        if (popup.isShowing()) {
            popup.dismiss();
        } else if (!closed) {
            closed = true;
            dismissed.run();
        }
    }

    void show() {
        load(true);
    }

    private void reveal() {
        popup.showAtLocation(activity.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        View container = panel.getRootView();
        if (container.getLayoutParams() instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) container.getLayoutParams();
            params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            params.dimAmount = 0.65f;
            activity.getWindowManager().updateViewLayout(container, params);
        }
        panel.setAlpha(0f);
        panel.animate().alpha(1f).setDuration(reducedMotion ? Motion.REDUCED_MS : 200).start();
    }

    private final Set<String> tvPackages = new HashSet<>();

    void refresh() {
        load(false);
    }

    private void load(boolean reveal) {
        dismissActions();
        int load = loads.start(reveal);
        PackageManager pm = activity.getPackageManager();
        String self = activity.getPackageName();
        LOADER.execute(() -> {
            Map<String, ResolveInfo> unique = new LinkedHashMap<>();
            Set<String> tv = new HashSet<>();
            for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                for (ResolveInfo info : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                    if (info.activityInfo == null || !info.activityInfo.exported) continue;
                    String pkg = info.activityInfo.packageName;
                    if (Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)) tv.add(pkg);
                    if (!pkg.equals(self) && !unique.containsKey(pkg)) unique.put(pkg, info);
                }
            }
            Map<String, String> names = new HashMap<>();
            Map<String, Long> installed = new HashMap<>();
            for (Map.Entry<String, ResolveInfo> entry : unique.entrySet()) {
                String pkg = entry.getKey();
                long updated = 0;
                try {
                    PackageInfo info = pm.getPackageInfo(pkg, 0);
                    installed.put(pkg, info.firstInstallTime);
                    updated = info.lastUpdateTime;
                } catch (PackageManager.NameNotFoundException | RuntimeException unavailable) {
                    installed.put(pkg, 0L);
                }
                Known known = KNOWN.get(pkg);
                if (known == null || known.updated != updated) {
                    known = new Known(updated, loadLabel(pm, entry.getValue()));
                    KNOWN.put(pkg, known);
                }
                names.put(pkg, known.label);
            }
            KNOWN.keySet().retainAll(unique.keySet());
            main.post(() -> {
                if (closed || !loads.isLatest(load)) return;
                tvPackages.clear();
                tvPackages.addAll(tv);
                installTimes.clear();
                installTimes.putAll(installed);
                labels.clear();
                labels.putAll(names);
                fill(new ArrayList<>(unique.values()));
                if (loads.takeReveal()) reveal();
            });
        });
    }

    private void fill(List<ResolveInfo> catalogue) {
        totalApps = catalogue.size();
        apps = catalogue;
        Map<String, Boolean> home = new HashMap<>();
        for (ResolveInfo app : apps) {
            String pkg = app.activityInfo.packageName;
            home.put(pkg, onHome.test(pkg));
        }
        catalogTime = System.currentTimeMillis();
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> {
            String pa = a.activityInfo.packageName, pb = b.activityInfo.packageName;
            if (selectionMode) return LibraryApps.compareCatalog(home.get(pa), label(a), pa,
                    home.get(pb), label(b), pb, collator);
            int priority = LibraryApps.comparePriority(home.get(pa), installTimes.get(pa),
                    home.get(pb), installTimes.get(pb), catalogTime);
            if (priority != 0) return priority;
            int byLabel = collator.compare(label(a), label(b));
            return byLabel != 0 ? byLabel : pa.compareTo(pb);
        });
        showCatalog();
    }

    private String label(ResolveInfo app) {
        String pkg = app.activityInfo.packageName;
        String cached = labels.get(pkg);
        if (cached != null) return cached;
        String value = loadLabel(activity.getPackageManager(), app);
        labels.put(pkg, value);
        return value;
    }

    private static String loadLabel(PackageManager pm, ResolveInfo app) {
        String pkg = app.activityInfo.packageName;
        try { return Cards.cleanLabel(String.valueOf(app.loadLabel(pm)), pkg); }
        catch (RuntimeException unavailable) { return pkg; }
    }

    private void bindIcon(ImageView icon, ResolveInfo app) {
        String pkg = app.activityInfo.packageName;
        icon.setTag(pkg);
        Drawable cached = icons.get(pkg);
        icon.setImageDrawable(cached);
        if (cached != null || !iconsLoading.add(pkg)) return;
        PackageManager pm = activity.getPackageManager();
        LOADER.execute(() -> {
            Drawable loaded;
            try { loaded = app.loadIcon(pm); }
            catch (RuntimeException unavailable) { loaded = null; }
            Drawable result = loaded;
            main.post(() -> {
                iconsLoading.remove(pkg);
                if (closed) return;
                Drawable shown = result != null ? result : activity.getDrawable(R.drawable.ic_apps);
                icons.put(pkg, shown);
                if (pkg.equals(icon.getTag())) icon.setImageDrawable(shown);
            });
        });
    }

    private String hint(boolean home) {
        if (selectionMode) return activity.getString(android.R.string.ok) + ": "
                + activity.getString(home ? R.string.library_remove_shortcut : R.string.library_add_home);
        return "PRESS [OK] TO LAUNCH • HOLD [OK] FOR DOCK & SHORTCUT OPTIONS";
    }

    private void showCatalog() {
        panel.removeAllViews();

        // 1. Full-Window Header matching launcher top bar
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setClipChildren(false);

        // Left block (Greeting & Title)
        LinearLayout leftBlock = new LinearLayout(activity);
        leftBlock.setOrientation(LinearLayout.VERTICAL);
        leftBlock.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView category = text("ALL APPLICATIONS // AETHER LIBRARY", 12, 0x8CFFFFFF);
        category.setTypeface(UiFont.get(activity, R.string.font_medium));
        category.setLetterSpacing(0.22f);
        leftBlock.addView(category);

        TextView title = text(activity.getString(R.string.app_library), 36, Color.WHITE);
        title.setTypeface(UiFont.get(activity, R.string.font_light));
        title.setLetterSpacing(-0.02f);
        leftBlock.addView(title);

        topBar.addView(leftBlock);

        // Right block: App counter pill and Back pill
        LinearLayout rightBlock = new LinearLayout(activity);
        rightBlock.setOrientation(LinearLayout.HORIZONTAL);
        rightBlock.setGravity(Gravity.CENTER_VERTICAL);

        TextView countPill = text(totalApps + " APPS INSTALLED", 12, 0xFF00E5FF);
        countPill.setTypeface(UiFont.get(activity, R.string.font_medium));
        countPill.setLetterSpacing(0.08f);
        countPill.setBackgroundResource(R.drawable.glass_pill_bg);
        countPill.setPadding(dp(14), dp(6), dp(14), dp(6));
        rightBlock.addView(countPill);

        TextView exitPill = text("BACK TO EXIT", 12, 0xB3FFFFFF);
        exitPill.setTypeface(UiFont.get(activity, R.string.font_medium));
        exitPill.setLetterSpacing(0.08f);
        exitPill.setBackgroundResource(R.drawable.glass_pill_bg);
        exitPill.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        exitLp.setMarginStart(dp(10));
        exitPill.setLayoutParams(exitLp);
        exitPill.setFocusable(true);
        exitPill.setOnClickListener(v -> close());
        rightBlock.addView(exitPill);

        topBar.addView(rightBlock);
        panel.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. Instructions row
        instructions = text(hint(false), 12, 0x80FFFFFF);
        instructions.setTypeface(UiFont.get(activity, R.string.font_regular));
        instructions.setLetterSpacing(0.06f);
        instructions.setSingleLine(true);
        instructions.setEllipsize(TextUtils.TruncateAt.END);
        instructions.setPadding(0, dp(8), 0, dp(12));
        panel.addView(instructions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (apps.isEmpty()) {
            TextView empty = text(activity.getString(R.string.library_empty), 18, 0x8CFFFFFF);
            empty.setGravity(Gravity.CENTER);
            panel.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));
            return;
        }

        // 3. Multi-column 6-column Leanback-style App Grid
        RecyclerView list = new RecyclerView(activity);
        list.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        catalog = list;
        list.setLayoutManager(new GridLayoutManager(activity, 6));
        list.setItemAnimator(null);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setClipToPadding(false);
        list.setClipChildren(false);
        list.setPadding(dp(2), dp(4), dp(2), dp(24));
        list.setAdapter(new Catalog());

        panel.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        int position = Math.min(selectedPosition, apps.size() - 1);
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).activityInfo.packageName.equals(selectedPackage)) {
                position = i;
                break;
            }
        }
        list.scrollToPosition(position);
        final int target = position;
        list.post(() -> {
            if (catalog != list || !popup.isShowing()) return;
            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(target);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private void dismissActions() {
        PopupWindow menu = actionPopup;
        actionPopup = null;
        if (menu != null) menu.dismiss();
    }

    private void restoreCardFocus() {
        if (catalog == null || !popup.isShowing()) return;
        catalog.post(() -> {
            if (catalog == null || !popup.isShowing() || actionPopup != null) return;
            RecyclerView.ViewHolder holder = catalog.findViewHolderForAdapterPosition(selectedPosition);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private void changeHome(ResolveInfo app, boolean add) {
        if (!LibraryApps.save(activity, app.activityInfo.packageName, add)) {
            Toast.makeText(activity, R.string.library_save_failed, Toast.LENGTH_LONG).show();
            return;
        }
        dismissActions();
        changed.run();
        refresh();
    }

    private void open(ResolveInfo app) {
        close();
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(
                new ComponentName(app.activityInfo.packageName, app.activityInfo.name));
        if (tvPackages.contains(app.activityInfo.packageName)) intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, activity.getString(R.string.cant_open, label(app)), Toast.LENGTH_SHORT).show();
        }
    }

    private void showActions(ResolveInfo app, View anchor) {
        dismissActions();
        selectedPackage = app.activityInfo.packageName;
        String pkg = selectedPackage;
        View menuPanel = activity.getLayoutInflater().inflate(R.layout.popup_inputs, panel, false);
        LinearLayout rows = menuPanel.findViewById(R.id.rows);
        PopupWindow menu = new PopupWindow(menuPanel, dp(230), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        actionPopup = menu;
        menu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        menu.setOutsideTouchable(true);
        menu.setAnimationStyle(0);
        menu.setElevation(0f);
        menu.setOnDismissListener(() -> {
            if (actionPopup == menu) actionPopup = null;
            restoreCardFocus();
        });

        // 1. Open
        addAction(rows, R.string.library_open, () -> open(app));

        // 2. 📌 Add to Dock
        addAction(rows, R.string.tile_add_to_dock, () -> {
            close();
            if (activity instanceof MainActivity) {
                String key = new ComponentName(app.activityInfo.packageName, app.activityInfo.name).flattenToShortString();
                ((MainActivity) activity).pinAppToDock(key);
            }
        });

        // 3. ➕ Add to Home / Remove Shortcut
        boolean home = onHome.test(pkg);
        addAction(rows, home ? R.string.library_remove_shortcut : R.string.library_add_home,
                () -> changeHome(app, !home));

        // 4. ℹ️ App info
        Intent info = SystemSettings.appInfo(activity, pkg);
        if (info != null) addAction(rows, R.string.library_app_info, () -> {
            close();
            try {
                activity.startActivity(info);
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(activity, activity.getString(R.string.cant_open, label(app)), Toast.LENGTH_SHORT).show();
            }
        });

        int menuWidth = dp(230);
        for (int i = 0; i < rows.getChildCount(); i++) {
            TextView title = rows.getChildAt(i).findViewById(R.id.title);
            menuWidth = Math.max(menuWidth, (int) Math.ceil(title.getPaint().measureText(title.getText().toString())) + dp(58));
            title.setEllipsize(TextUtils.TruncateAt.END);
        }
        menu.setWidth(menuWidth);
        menuPanel.measure(View.MeasureSpec.makeMeasureSpec(menuWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int x = Math.max(dp(20), Math.min(location[0] + anchor.getWidth() / 2 - menuWidth / 2,
                panel.getWidth() - menuWidth - dp(20)));
        int y = Math.max(dp(20), location[1] - dp(10));
        menuPanel.setAlpha(0f);
        menuPanel.setScaleY(0.92f);
        menu.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP | Gravity.START, x, y);
        menuPanel.animate().alpha(1f).scaleY(1f).setDuration(reducedMotion ? Motion.REDUCED_MS : 200).start();
        rows.getChildAt(0).requestFocus();
    }

    private void addAction(LinearLayout rows, int label, Runnable action) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((TextView) row.findViewById(R.id.title)).setText(label);
        row.setFocusableInTouchMode(true);
        row.findViewById(R.id.icon).setVisibility(View.GONE);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(v -> action.run());
        rows.addView(row);
        row.setOnKeyListener((v, key, event) -> {
            int index = rows.indexOfChild(v);
            return event.getAction() == KeyEvent.ACTION_DOWN && (key == KeyEvent.KEYCODE_DPAD_LEFT
                    || key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_UP && index == 0
                    || key == KeyEvent.KEYCODE_DPAD_DOWN && index == rows.getChildCount() - 1);
        });
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(UiFont.get(activity, R.string.font_medium));
        view.setLetterSpacing(0.01f);
        view.setIncludeFontPadding(false);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }

    private final class Catalog extends RecyclerView.Adapter<Row> {

        @NonNull
        @Override
        public Row onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            FrameLayout card = new FrameLayout(activity);
            GridLayoutManager.LayoutParams params = new GridLayoutManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(136));
            params.setMargins(dp(6), dp(6), dp(6), dp(6));
            card.setLayoutParams(params);
            card.setPadding(dp(10), dp(12), dp(10), dp(10));
            card.setFocusable(true);
            card.setClickable(true);
            card.setBackgroundResource(R.drawable.glass_card_bg);

            LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setGravity(Gravity.CENTER_HORIZONTAL);
            card.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            ImageView icon = new ImageView(activity);
            icon.setId(R.id.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            content.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));

            TextView name = text("", 13, Color.WHITE);
            name.setId(R.id.title);
            name.setTypeface(UiFont.get(activity, R.string.font_medium));
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = dp(8);
            content.addView(name, nameParams);

            // Badge pill in top right corner (DOCK or HOME)
            TextView badge = text("", 9, Color.WHITE);
            badge.setId(R.id.dot);
            badge.setTypeface(UiFont.get(activity, R.string.font_medium));
            badge.setLetterSpacing(0.08f);
            badge.setPadding(dp(6), dp(2), dp(6), dp(2));
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            badgeParams.setMargins(0, dp(4), dp(4), 0);
            card.addView(badge, badgeParams);

            return new Row(card);
        }

        @Override
        public void onBindViewHolder(@NonNull Row holder, int position) {
            ResolveInfo app = apps.get(position);
            String pkg = app.activityInfo.packageName;
            bindIcon(holder.itemView.findViewById(R.id.icon), app);
            TextView name = holder.itemView.findViewById(R.id.title);
            name.setText(label(app));

            boolean home = onHome.test(pkg);
            TextView badge = holder.itemView.findViewById(R.id.dot);
            if (home) {
                badge.setText("HOME");
                badge.setBackground(surface(0x3300E676, 6));
                badge.setTextColor(0xFF00E676);
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                boolean onHomeNow = onHome.test(pkg);
                if (LibraryApps.okOpens(selectionMode, okOpens, onHomeNow)) open(app);
                else changeHome(app, !onHomeNow);
            });

            holder.itemView.setOnLongClickListener(selectionMode ? null : v -> {
                showActions(app, v);
                return true;
            });

            holder.itemView.setOnFocusChangeListener((v, focused) -> {
                if (focused) {
                    selectedPackage = pkg;
                    int index = holder.getBindingAdapterPosition();
                    if (index != RecyclerView.NO_POSITION) selectedPosition = index;
                    instructions.setText(hint(onHome.test(pkg)));
                    v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(140).start();
                    SoundManager.getInstance(activity).playSelect();
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start();
                }
            });

            holder.itemView.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    close();
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }
    }

    private static final class Row extends RecyclerView.ViewHolder {
        Row(View view) {
            super(view);
        }
    }
}
