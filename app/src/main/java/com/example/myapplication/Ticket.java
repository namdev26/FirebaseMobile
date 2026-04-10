package com.example.myapplication;

import java.util.List;

public class Ticket {
    private String id;
    private String userId;
    private String movieId;
    private String movieTitle;
    private String showtime;
    private int seats;
    private String customerName;
    private String customerPhone;
    private long totalPrice;
    private long bookingTime;
    private String status;
    private List<String> selectedSeatIds; // danh sách mã ghế cụ thể: ["A1","A2",...]

    public Ticket() {
        // Firestore cần constructor rỗng
    }

    public Ticket(String userId, String movieId, String movieTitle, String showtime,
                  int seats, String customerName, String customerPhone,
                  long totalPrice, long bookingTime) {
        this.userId = userId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.showtime = showtime;
        this.seats = seats;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.totalPrice = totalPrice;
        this.bookingTime = bookingTime;
        this.status = "confirmed";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getShowtime() { return showtime; }
    public void setShowtime(String showtime) { this.showtime = showtime; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(long totalPrice) { this.totalPrice = totalPrice; }

    public long getBookingTime() { return bookingTime; }
    public void setBookingTime(long bookingTime) { this.bookingTime = bookingTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getSelectedSeatIds() { return selectedSeatIds; }
    public void setSelectedSeatIds(List<String> selectedSeatIds) { this.selectedSeatIds = selectedSeatIds; }
}
