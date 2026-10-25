package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * iPhone-style Horizontal Recent Apps Switcher & Task Cleaner.
 * Supports D-pad UP / Flick to close, and persistent clean state.
 */
public class RecentAppsDialog extends Dialog {

    private final List<RecentAppsManager.RecentApp> apps = new ArrayList<>();
    private RecentsAdapter adapter;
    private RecyclerView recentsList;
    private View emptyRecents;
    private TextView btnCleanAll;
    private final float density;

    public RecentAppsDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        setContentView(R.layout.dialog_recent_apps);

        recentsList = findViewById(R.id.recentsList);
        emptyRecents = findViewById(R.id.emptyRecents);
        btnCleanAll = findViewById(R.id.btnCleanAll);

        View dialogBackdrop = findViewById(R.id.dialogBackdrop);
        if (dialogBackdrop != null) {
            dialogBackdrop.setOnClickListener(v -> dismiss());
        }

        TextView recentsTitle = findViewById(R.id.recentsTitle);
        if (recentsTitle != null) recentsTitle.setText("RECENT APPS");

        TextView recentsSubtitle = findViewById(R.id.recentsSubtitle);
        if (recentsSubtitle != null) recentsSubtitle.setText("iOS Switcher • Flick up to close");

        TextView recentsHint = findViewById(R.id.recentsHint);
        if (recentsHint != null) recentsHint.setText("[OK] Switch   •   [UP] Swipe to Close   •   [Back] Exit");

        ViewGroup.LayoutParams rLp = recentsList.getLayoutParams();
        if (rLp != null) {
            rLp.height = (int) (260 * density);
            recentsList.setLayoutParams(rLp);
        }

        LinearLayoutManager lm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recentsList.setLayoutManager(lm);
        recentsList.setHasFixedSize(true);

        adapter = new RecentsAdapter();
        recentsList.setAdapter(adapter);

        recentsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                applyCardTilt();
            }
        });

        btnCleanAll.setOnClickListener(v -> cleanAllApps());
        btnCleanAll.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(160).start();
        });

        loadApps();
    }

    private void loadApps() {
        apps.clear();
        List<RecentAppsManager.RecentApp> loaded = RecentAppsManager.getRecentApps(getContext());
        apps.addAll(loaded);
        adapter.notifyDataSetChanged();

        boolean empty = apps.isEmpty();
        emptyRecents.setVisibility(empty ? View.VISIBLE : View.GONE);
        recentsList.setVisibility(empty ? View.GONE : View.VISIBLE);
        btnCleanAll.setEnabled(!empty);
        btnCleanAll.setAlpha(empty ? 0.4f : 1f);

        if (!empty) {
            recentsList.post(() -> {
                if (recentsList.getChildCount() > 0) {
                    View first = recentsList.getChildAt(0).findViewById(R.id.cardBody);
                    if (first != null) first.requestFocus();
                    applyCardTilt();
                }
            });
        } else {
            btnCleanAll.requestFocus();
        }
    }

    private void cleanAllApps() {
        int count = RecentAppsManager.killAll(getContext(), apps);
        Toast.makeText(getContext(), getContext().getString(R.string.apps_cleaned_toast) + " (" + count + ")", Toast.LENGTH_SHORT).show();
        apps.clear();
        adapter.notifyDataSetChanged();
        emptyRecents.setVisibility(View.VISIBLE);
        recentsList.setVisibility(View.GONE);
        btnCleanAll.setEnabled(false);
        btnCleanAll.setAlpha(0.4f);
        recentsList.postDelayed(this::dismiss, 900);
    }

    private void dismissCard(int position, View itemView, RecentAppsManager.RecentApp app) {
        if (position == RecyclerView.NO_POSITION || position >= apps.size()) return;
        RecentAppsManager.killApp(getContext(), app.packageName);
        Toast.makeText(getContext(), app.label + " closed", Toast.LENGTH_SHORT).show();

        itemView.animate().translationY(-160f * density).alpha(0f).scaleX(0.7f).scaleY(0.7f)
                .setDuration(200)
                .withEndAction(() -> {
                    int currentPos = apps.indexOf(app);
                    if (currentPos != -1) {
                        apps.remove(currentPos);
                        adapter.notifyItemRemoved(currentPos);
                        if (apps.isEmpty()) {
                            emptyRecents.setVisibility(View.VISIBLE);
                            recentsList.setVisibility(View.GONE);
                            btnCleanAll.setEnabled(false);
                            btnCleanAll.setAlpha(0.4f);
                        } else {
                            recentsList.post(this::applyCardTilt);
                        }
                    }
                }).start();
    }

    private void applyCardTilt() {
        int count = recentsList.getChildCount();
        if (count == 0) return;
        float centerX = recentsList.getWidth() / 2f;
        for (int i = 0; i < count; i++) {
            View child = recentsList.getChildAt(i);
            float childCenter = (child.getLeft() + child.getRight()) / 2f;
            float diff = (childCenter - centerX) / Math.max(1f, centerX);
            diff = Math.max(-1f, Math.min(1f, diff));

            float scale = 1f - Math.abs(diff) * 0.10f;
            float transZ = (1f - Math.abs(diff)) * 14f * density;

            child.setCameraDistance(8000 * density);
            child.setRotationY(-diff * 12f);
            child.setTranslationX(-diff * 5f * density);
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setTranslationZ(transZ);
        }
    }

    private class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_card, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            RecentAppsManager.RecentApp app = apps.get(position);
            h.appName.setText(app.label);
            h.previewLogo.setImageDrawable(app.icon);
            h.appTag.setText(app.isRunning ? "Active" : "Recent");
            h.appTag.setTextColor(app.isRunning ? 0xFF00FF66 : 0xFF88CCFF);
            h.statusDot.setBackgroundColor(app.isRunning ? 0xFF00FF66 : 0xFF5588AA);

            h.cardBody.setOnClickListener(v -> {
                try {
                    getContext().startActivity(app.launchIntent);
                    dismiss();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Cannot open app", Toast.LENGTH_SHORT).show();
                }
            });

            // iOS-style Swipe / Flick Up to close
            h.cardBody.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    dismissCard(h.getBindingAdapterPosition(), h.itemView, app);
                    return true;
                }
                return false;
            });

            h.cardBody.setOnFocusChangeListener((v, focused) -> {
                h.appName.setSelected(focused);
                v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f)
                        .translationY(focused ? -6f * density : 0f)
                        .setDuration(180)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
                if (focused) {
                    int pos = h.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        recentsList.smoothScrollToPosition(pos);
                    }
                    recentsList.post(RecentAppsDialog.this::applyCardTilt);
                }
            });

            h.btnCloseApp.setOnClickListener(v -> {
                dismissCard(h.getBindingAdapterPosition(), h.itemView, app);
            });

            h.btnCloseApp.setOnFocusChangeListener((v, focused) -> {
                v.animate().scaleX(focused ? 1.12f : 1f).scaleY(focused ? 1.12f : 1f).setDuration(140).start();
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final View cardBody;
            final ImageView previewLogo;
            final TextView appName;
            final TextView appTag;
            final View statusDot;
            final TextView btnCloseApp;

            Holder(@NonNull View itemView) {
                super(itemView);
                cardBody = itemView.findViewById(R.id.cardBody);
                previewLogo = itemView.findViewById(R.id.previewLogo);
                appName = itemView.findViewById(R.id.appName);
                appTag = itemView.findViewById(R.id.appTag);
                statusDot = itemView.findViewById(R.id.statusDot);
                btnCloseApp = itemView.findViewById(R.id.btnCloseApp);
            }
        }
    }
}
