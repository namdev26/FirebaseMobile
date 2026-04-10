package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final Context context;
    private final List<Movie> movies;
    private final OnMovieClickListener listener;

    public MovieAdapter(Context context, List<Movie> movies, OnMovieClickListener listener) {
        this.context = context;
        this.movies = movies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPoster;
        private final TextView tvTitle;
        private final TextView tvGenre;
        private final TextView tvDuration;
        private final RatingBar ratingBar;
        private final TextView tvPrice;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvGenre = itemView.findViewById(R.id.tvGenre);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        public void bind(Movie movie) {
            tvTitle.setText(movie.getTitle());
            tvGenre.setText(movie.getGenre());
            tvDuration.setText(movie.getDuration() + " phút");
            ratingBar.setRating(movie.getRating() / 2f); // Convert 10-scale to 5-star
            tvPrice.setText(String.format("%,d đ", movie.getPrice()));

            if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
                Glide.with(context)
                        .load(movie.getPosterUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(imgPoster);
            } else {
                imgPoster.setImageResource(R.drawable.ic_launcher_background);
            }

            itemView.setOnClickListener(v -> listener.onMovieClick(movie));
        }
    }
}
