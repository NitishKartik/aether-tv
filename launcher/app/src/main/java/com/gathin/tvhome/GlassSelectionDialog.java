package com.gathin.tvhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Reusable Frosted Glass selection/dropdown dialog for Android TV.
 * Replaces stark default AlertDialog pickers with living room 10-foot scale,
 * high-contrast cyan border highlights, and D-pad remote navigation.
 */
public class GlassSelectionDialog extends Dialog {

    public interface OnItemSelectedListener {
        void onItemSelected(GlassSelectionDialog dialog, int which);
    }

    private final String title;
    private final String subtitle;
    private final String[] items;
    private final int selectedIndex;
    private final OnItemSelectedListener listener;

    private String neutralText;
    private View.OnClickListener neutralListener;

    public GlassSelectionDialog(Context context, String title, String[] items, int selectedIndex, OnItemSelectedListener listener) {
        this(context, title, null, items, selectedIndex, listener);
    }

    public GlassSelectionDialog(Context context, String title, String subtitle, String[] items, int selectedIndex, OnItemSelectedListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.title = title;
        this.subtitle = subtitle;
        this.items = items;
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    public GlassSelectionDialog setNeutralButton(String text, View.OnClickListener listener) {
        this.neutralText = text;
        this.neutralListener = listener;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setContentView(R.layout.dialog_glass_selection);

        TextView txtTitle = findViewById(R.id.dialogTitle);
        if (txtTitle != null && title != null) {
            txtTitle.setText(title);
        }

        TextView txtSub = findViewById(R.id.dialogSubtitle);
        if (txtSub != null) {
            if (subtitle != null && !subtitle.isEmpty()) {
                txtSub.setText(subtitle);
                txtSub.setVisibility(View.VISIBLE);
            } else {
                txtSub.setVisibility(View.GONE);
            }
        }

        TextView btnNeutral = findViewById(R.id.btnNeutralAction);
        if (btnNeutral != null) {
            if (neutralText != null) {
                btnNeutral.setText(neutralText);
                btnNeutral.setVisibility(View.VISIBLE);
                btnNeutral.setOnClickListener(v -> {
                    SoundManager.getInstance(getContext()).playSelect();
                    dismiss();
                    if (neutralListener != null) neutralListener.onClick(v);
                });
                attachFocusAnimation(btnNeutral);
            } else {
                btnNeutral.setVisibility(View.GONE);
            }
        }

        TextView btnCancel = findViewById(R.id.btnCancelAction);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                SoundManager.getInstance(getContext()).playSelect();
                dismiss();
            });
            attachFocusAnimation(btnCancel);
        }

        LinearLayout container = findViewById(R.id.optionsContainer);
        ScrollView scroll = findViewById(R.id.dialogScroll);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View initialFocusView = null;

        if (container != null && items != null) {
            for (int i = 0; i < items.length; i++) {
                final int index = i;
                View row = inflater.inflate(R.layout.item_glass_selection_row, container, false);

                TextView rowIcon = row.findViewById(R.id.rowIcon);
                TextView rowTitle = row.findViewById(R.id.rowTitle);
                TextView rowCheck = row.findViewById(R.id.rowCheck);

                String itemText = items[i];
                if (rowTitle != null) {
                    rowTitle.setText(itemText);
                }

                // Check if item contains an emoji at the start
                if (rowIcon != null) {
                    if (itemText != null && itemText.length() > 2 && Character.isSurrogate(itemText.charAt(0))) {
                        rowIcon.setVisibility(View.GONE); // Icon is already in text
                    } else {
                        rowIcon.setText(index == selectedIndex ? "◉" : "○");
                        rowIcon.setTextColor(index == selectedIndex ? Color.parseColor("#FF00E5FF") : Color.parseColor("#80FFFFFF"));
                    }
                }

                boolean isSelected = (index == selectedIndex);
                row.setSelected(isSelected);

                if (rowCheck != null) {
                    rowCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }

                row.setOnClickListener(v -> {
                    SoundManager.getInstance(getContext()).playSelect();
                    dismiss();
                    if (listener != null) {
                        listener.onItemSelected(GlassSelectionDialog.this, index);
                    }
                });

                attachFocusAnimation(row);
                container.addView(row);

                if (isSelected) {
                    initialFocusView = row;
                } else if (initialFocusView == null && i == 0) {
                    initialFocusView = row;
                }
            }
        }

        final View focusTarget = initialFocusView;
        if (focusTarget != null) {
            focusTarget.post(() -> {
                focusTarget.requestFocus();
                if (scroll != null) {
                    scroll.scrollTo(0, focusTarget.getTop());
                }
            });
        }
    }

    private void attachFocusAnimation(View v) {
        v.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                view.setScaleX(1.025f);
                view.setScaleY(1.025f);
                SoundManager.getInstance(getContext()).playKeyTap();
            } else {
                view.setScaleX(1.0f);
                view.setScaleY(1.0f);
            }
        });
    }
}
