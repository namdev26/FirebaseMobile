package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;

public class MovieDetailActivity extends AppCompatActivity {

    private FirebaseAnalytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        analytics = FirebaseAnalytics.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Nhận dữ liệu phim từ Intent
        String movieId       = getIntent().getStringExtra("movieId");
        String title         = getIntent().getStringExtra("movieTitle");
        String description   = getIntent().getStringExtra("movieDescription");
        String genre         = getIntent().getStringExtra("movieGenre");
        int    duration      = getIntent().getIntExtra("movieDuration", 0);
        float  rating        = getIntent().getFloatExtra("movieRating", 0f);
        String posterUrl     = getIntent().getStringExtra("moviePosterUrl");
        long   price         = getIntent().getLongExtra("moviePrice", 0L);
        ArrayList<String> showtimes = getIntent().getStringArrayListExtra("movieShowtimes");

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);

        // Bind views
        ImageView imgPoster       = findViewById(R.id.imgPoster);
        TextView  tvTitle         = findViewById(R.id.tvTitle);
        TextView  tvGenre         = findViewById(R.id.tvGenre);
        TextView  tvDuration      = findViewById(R.id.tvDuration);
        TextView  tvRating        = findViewById(R.id.tvRating);
        RatingBar ratingBar       = findViewById(R.id.ratingBar);
        TextView  tvDescription   = findViewById(R.id.tvDescription);
        TextView  tvPrice         = findViewById(R.id.tvPrice);
        Button    btnBookTicket   = findViewById(R.id.btnBookTicket);

        tvTitle.setText(title);
        tvGenre.setText(genre);
        tvDuration.setText(duration + " phút");
        tvRating.setText(rating + "/10");
        ratingBar.setRating(rating / 2f);
        tvDescription.setText(description);
        tvPrice.setText(String.format("Giá vé: %,d đ/ghế", price));

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(imgPoster);
        }

        // Log analytics: user xem chi tiết phim
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, movieId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, title);
        analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);

        // Nút đặt vé → hiện dialog chọn suất chiếu trước, rồi mở SeatSelectionActivity
        btnBookTicket.setOnClickListener(v -> showShowtimePicker(movieId, title, price, showtimes));
    }

    /**
     * Dialog chọn suất chiếu → sau đó mở SeatSelectionActivity với showtime đã chọn
     */
    private void showShowtimePicker(String movieId, String title, long price,
                                    ArrayList<String> showtimes) {
        if (showtimes == null || showtimes.isEmpty()) {
            openSeatSelection(movieId, title, price, "");
            return;
        }

        String[] items = showtimes.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("🕐 Chọn suất chiếu")
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    openSeatSelection(movieId, title, price, selected);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openSeatSelection(String movieId, String title, long price, String showtime) {
        Intent intent = new Intent(this, SeatSelectionActivity.class);
        intent.putExtra("movieId",    movieId);
        intent.putExtra("movieTitle", title);
        intent.putExtra("moviePrice", price);
        intent.putExtra("showtime",   showtime);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
