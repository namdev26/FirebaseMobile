package com.example.myapplication;

import java.util.List;

public class Movie {
    private String id;
    private String title;
    private String description;
    private String genre;
    private int duration;
    private float rating;
    private String posterUrl;
    private List<String> showtimes;
    private long price;

    public Movie() {
        // Firestore cần constructor rỗng
    }

    public Movie(String id, String title, String description, String genre,
                 int duration, float rating, String posterUrl,
                 List<String> showtimes, long price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.duration = duration;
        this.rating = rating;
        this.posterUrl = posterUrl;
        this.showtimes = showtimes;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public List<String> getShowtimes() { return showtimes; }
    public void setShowtimes(List<String> showtimes) { this.showtimes = showtimes; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
}
