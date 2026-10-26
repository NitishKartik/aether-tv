package com.gathin.tvhome;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Aether TV Cinematic Entertainment Discovery Dialog.
 * Features IMDb metadata browsing, Hindi & English filters, and local watchlist persistence.
 */
public final class EntertainmentDialog extends Dialog {

    private final EntertainmentManager manager;
    private RecyclerView cinemaRecycler;
    private Button btnClose;
    private Button[] tabs;
    private String currentCategory = "ALL";
    private List<EntertainmentModel> items;
    private CinemaAdapter adapter;

    public EntertainmentDialog(Context context, EntertainmentManager manager) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.manager = manager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_entertainment);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        cinemaRecycler = findViewById(R.id.cinemaRecycler);
        btnClose = findViewById(R.id.btnCloseCinema);

        cinemaRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        items = manager.getByCategory(currentCategory);
        adapter = new CinemaAdapter();
        cinemaRecycler.setAdapter(adapter);

        setupTabs();
        btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupTabs() {
        Button tabAll = findViewById(R.id.tabAll);
        Button tabMovies = findViewById(R.id.tabMovies);
        Button tabTv = findViewById(R.id.tabTv);
        Button tabHindi = findViewById(R.id.tabHindi);
        Button tabTopImdb = findViewById(R.id.tabTopImdb);
        Button tabWatchlist = findViewById(R.id.tabWatchlist);

        tabs = new Button[]{tabAll, tabMovies, tabTv, tabHindi, tabTopImdb, tabWatchlist};
        String[] categories = {"ALL", "MOVIES", "TV_SHOWS", "HINDI", "TOP_IMDB", "WATCHLIST"};

        for (int i = 0; i < tabs.length; i++) {
            final String cat = categories[i];
            final Button tabBtn = tabs[i];
            tabBtn.setOnClickListener(v -> {
                currentCategory = cat;
                for (Button b : tabs) {
                    b.setTextColor(Color.parseColor("#B3FFFFFF"));
                }
                tabBtn.setTextColor(Color.parseColor("#FF00E5FF"));
                items = manager.getByCategory(cat);
                adapter.notifyDataSetChanged();
            });
        }
    }

    private class CinemaAdapter extends RecyclerView.Adapter<CinemaViewHolder> {

        @NonNull
        @Override
        public CinemaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entertainment_card, parent, false);
            return new CinemaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CinemaViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private class CinemaViewHolder extends RecyclerView.ViewHolder {
        ImageView moviePoster;
        TextView movieTitle, ratingPill, movieMeta, moviePlot, watchlistBadge;

        CinemaViewHolder(View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.moviePoster);
            movieTitle = itemView.findViewById(R.id.movieTitle);
            ratingPill = itemView.findViewById(R.id.ratingPill);
            movieMeta = itemView.findViewById(R.id.movieMeta);
            moviePlot = itemView.findViewById(R.id.moviePlot);
            watchlistBadge = itemView.findViewById(R.id.watchlistBadge);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    showDetailsDialog(items.get(pos));
                }
            });
        }

        void bind(EntertainmentModel item) {
            movieTitle.setText(item.title);
            ratingPill.setText("★ " + item.imdbRating);
            movieMeta.setText(item.year + " · " + item.duration + " · " + item.genre);
            moviePlot.setText(item.plot);
            watchlistBadge.setVisibility(item.isWatchlist ? View.VISIBLE : View.GONE);

            if (moviePoster != null) {
                PosterLoader.get(itemView.getContext()).loadPoster(item.posterUrl, item.title, item.imdbRating, moviePoster);
            }
        }
    }

    private void showDetailsDialog(EntertainmentModel item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle(item.title + " (" + item.year + ")");

        StringBuilder sb = new StringBuilder();
        sb.append("IMDb Rating: ★ ").append(item.imdbRating).append(" / 10\n");
        sb.append("Runtime: ").append(item.duration).append("\n");
        sb.append("Genre: ").append(item.genre).append("\n");
        sb.append("Cast: ").append(item.cast).append("\n\n");
        sb.append("Plot Summary:\n").append(item.plot);

        builder.setMessage(sb.toString());

        String watchlistAction = item.isWatchlist ? "Remove from Watchlist" : "Add to Watchlist";
        builder.setPositiveButton(watchlistAction, (dialog, which) -> {
            manager.toggleWatchlist(item.id);
            items = manager.getByCategory(currentCategory);
            adapter.notifyDataSetChanged();
        });

        builder.setNeutralButton("Back", null);
        builder.show();
    }
}
