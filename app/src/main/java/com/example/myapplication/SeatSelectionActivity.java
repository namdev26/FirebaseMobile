package com.example.myapplication;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeatSelectionActivity extends AppCompatActivity {

    // ── Cấu hình rạp ──────────────────────────────────────────────────
    private static final String[] ROW_LABELS    = {"A", "B", "C", "D", "E", "F", "G", "H"};
    private static final int      SEATS_PER_ROW = 10;
    private static final int      AISLE_AFTER   = 5;   // lối đi sau ghế số 5

    // Màu ghế
    private static final int COLOR_AVAILABLE = Color.parseColor("#546E7A"); // xanh xám
    private static final int COLOR_SELECTED  = Color.parseColor("#FFD700"); // vàng gold
    private static final int COLOR_BOOKED    = Color.parseColor("#C62828"); // đỏ đậm

    // ── Dữ liệu phim / suất chiếu ─────────────────────────────────────
    private String movieId, movieTitle, showtime;
    private long   pricePerSeat;

    // ── State ghế ─────────────────────────────────────────────────────
    private final List<String>              selectedSeats = new ArrayList<>();
    private final Map<String, ImageButton>  seatButtonMap = new HashMap<>();

    // ── Views ─────────────────────────────────────────────────────────
    private TextView    tvSelectedSeats, tvTotalPrice, tvShowtimeInfo;
    private Button      btnBook;
    private ProgressBar progressBar;
    private EditText    etName, etPhone;
    private LinearLayout seatsContainer;

    // ── Firebase ──────────────────────────────────────────────────────
    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics analytics;

    // ─────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        mAuth     = FirebaseAuth.getInstance();
        db        = FirebaseFirestore.getInstance();
        analytics = FirebaseAnalytics.getInstance(this);

        // Nhận dữ liệu từ MovieDetailActivity
        movieId      = getIntent().getStringExtra("movieId");
        movieTitle   = getIntent().getStringExtra("movieTitle");
        showtime     = getIntent().getStringExtra("showtime");
        pricePerSeat = getIntent().getLongExtra("moviePrice", 80000L);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(movieTitle);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Bind views
        tvShowtimeInfo  = findViewById(R.id.tvShowtimeInfo);
        tvSelectedSeats = findViewById(R.id.tvSelectedSeats);
        tvTotalPrice    = findViewById(R.id.tvTotalPrice);
        btnBook         = findViewById(R.id.btnBook);
        progressBar     = findViewById(R.id.progressBar);
        seatsContainer  = findViewById(R.id.seatsContainer);
        etName          = findViewById(R.id.etName);
        etPhone         = findViewById(R.id.etPhone);

        tvShowtimeInfo.setText("Suất chiếu: " + showtime + "  •  " + String.format("%,d đ/ghế", pricePerSeat));
        updateBottomPanel();

        btnBook.setOnClickListener(v -> confirmBooking());

        // Xin quyền POST_NOTIFICATIONS trên Android 13+
        requestNotificationPermission();

        // Tải ghế đã đặt từ Firestore rồi vẽ sơ đồ
        loadBookedSeats();
    }

    // ─────────────────────────────────────────────────────────────────
    //  1. Tải danh sách ghế đã đặt từ Firestore
    // ─────────────────────────────────────────────────────────────────
    private void loadBookedSeats() {
        progressBar.setVisibility(View.VISIBLE);

        // Key: movieId_HH_MM  (dấu ":" thay bằng "_" vì Firestore không cho phép ":" trong docId)
        String docId = movieId + "_" + showtime.replace(":", "_");

        db.collection("seatMaps").document(docId).get()
                .addOnSuccessListener(doc -> {
                    List<String> booked = new ArrayList<>();
                    if (doc.exists() && doc.get("bookedSeats") != null) {
                        //noinspection unchecked
                        booked = (List<String>) doc.get("bookedSeats");
                    }
                    buildSeatGrid(booked);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // Không lấy được → vẽ lưới trống (không ghế nào bị khóa)
                    buildSeatGrid(new ArrayList<>());
                    progressBar.setVisibility(View.GONE);
                });
    }

    // ─────────────────────────────────────────────────────────────────
    //  2. Vẽ toàn bộ sơ đồ ghế
    // ─────────────────────────────────────────────────────────────────
    private void buildSeatGrid(List<String> bookedSeats) {
        seatsContainer.removeAllViews();

        // Hàng số thứ tự cột (1 … 5  6 … 10)
        addColumnNumberRow();

        for (String rowLabel : ROW_LABELS) {
            LinearLayout rowLayout = createRowLayout();

            // Nhãn hàng (A, B, C…)
            rowLayout.addView(makeRowLabel(rowLabel));

            for (int num = 1; num <= SEATS_PER_ROW; num++) {
                // Khoảng cách lối đi giữa ghế 5 và 6
                if (num == AISLE_AFTER + 1) {
                    rowLayout.addView(makeAisle());
                }

                String seatId = rowLabel + num;
                boolean isBooked = bookedSeats.contains(seatId);

                ImageButton btn = makeSeatButton(seatId, isBooked);
                seatButtonMap.put(seatId, btn);
                rowLayout.addView(btn);
            }

            seatsContainer.addView(rowLayout);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers: tạo từng thành phần UI
    // ─────────────────────────────────────────────────────────────────

    /** Hàng số cột 1-10 ở đầu bảng */
    private void addColumnNumberRow() {
        LinearLayout row = createRowLayout();

        // Ô trống thay cho label hàng
        Space labelSpace = new Space(this);
        labelSpace.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(20)));
        row.addView(labelSpace);

        for (int num = 1; num <= SEATS_PER_ROW; num++) {
            if (num == AISLE_AFTER + 1) row.addView(makeAisle());

            TextView tv = new TextView(this);
            tv.setText(String.valueOf(num));
            tv.setTextColor(Color.parseColor("#666666"));
            tv.setTextSize(9f);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(20)));
            row.addView(tv);
        }

        seatsContainer.addView(row);
    }

    private LinearLayout createRowLayout() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(p);
        return row;
    }

    private TextView makeRowLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(38)));
        return tv;
    }

    private Space makeAisle() {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(14), dp(38)));
        return s;
    }

    /** Tạo nút ghế với trạng thái tương ứng */
    private ImageButton makeSeatButton(String seatId, boolean isBooked) {
        ImageButton btn = new ImageButton(this);
        btn.setImageResource(R.drawable.ic_seat);
        btn.setBackground(null);
        btn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btn.setPadding(dp(2), dp(2), dp(2), dp(2));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(34), dp(38));
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        btn.setLayoutParams(p);

        if (isBooked) {
            btn.setColorFilter(COLOR_BOOKED, PorterDuff.Mode.SRC_IN);
            btn.setEnabled(false);
            btn.setAlpha(0.75f);
        } else {
            btn.setColorFilter(COLOR_AVAILABLE, PorterDuff.Mode.SRC_IN);
            btn.setOnClickListener(v -> onSeatClicked(btn, seatId));
        }

        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    //  3. Xử lý click chọn / bỏ chọn ghế
    // ─────────────────────────────────────────────────────────────────
    private void onSeatClicked(ImageButton btn, String seatId) {
        if (selectedSeats.contains(seatId)) {
            // Bỏ chọn
            selectedSeats.remove(seatId);
            btn.setColorFilter(COLOR_AVAILABLE, PorterDuff.Mode.SRC_IN);
            animateSeat(btn, 1.0f);
        } else {
            // Chọn ghế
            selectedSeats.add(seatId);
            btn.setColorFilter(COLOR_SELECTED, PorterDuff.Mode.SRC_IN);
            animateSeat(btn, 1.15f);
        }
        updateBottomPanel();
    }

    /** Hiệu ứng pop nhỏ khi chọn ghế */
    private void animateSeat(View v, float scale) {
        v.animate()
                .scaleX(scale).scaleY(scale)
                .setDuration(120)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();
    }

    // ─────────────────────────────────────────────────────────────────
    //  4. Cập nhật thanh thông tin dưới màn hình
    // ─────────────────────────────────────────────────────────────────
    private void updateBottomPanel() {
        if (selectedSeats.isEmpty()) {
            tvSelectedSeats.setText("Chưa chọn ghế nào");
            tvTotalPrice.setText("0 đ");
            btnBook.setEnabled(false);
        } else {
            // Sắp xếp ghế cho dễ đọc
            List<String> sorted = new ArrayList<>(selectedSeats);
            java.util.Collections.sort(sorted);
            tvSelectedSeats.setText("Ghế: " + TextUtils.join(", ", sorted));
            tvTotalPrice.setText(String.format("%,d đ", selectedSeats.size() * pricePerSeat));
            btnBook.setEnabled(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  5. Xác nhận đặt vé → lưu Firestore + gửi notification
    // ─────────────────────────────────────────────────────────────────
    private void confirmBooking() {
        String name  = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId    = mAuth.getCurrentUser().getUid();
        long   total     = selectedSeats.size() * pricePerSeat;
        long   timestamp = System.currentTimeMillis();

        List<String> sortedSeats = new ArrayList<>(selectedSeats);
        java.util.Collections.sort(sortedSeats);

        Ticket ticket = new Ticket(userId, movieId, movieTitle, showtime,
                sortedSeats.size(), name, phone, total, timestamp);
        ticket.setSelectedSeatIds(sortedSeats);

        setLoading(true);

        db.collection("tickets").add(ticket)
                .addOnSuccessListener(ref -> {
                    // setLoading(false) PHẢI được gọi dù bất kỳ lỗi nào xảy ra sau đây
                    try {
                        ticket.setId(ref.getId());

                        // Đánh dấu ghế vừa đặt trong seatMaps
                        markSeatsAsBooked(sortedSeats);

                        // Firebase Analytics
                        try {
                            Bundle b = new Bundle();
                            b.putString(FirebaseAnalytics.Param.ITEM_ID, movieId);
                            b.putString(FirebaseAnalytics.Param.ITEM_NAME, movieTitle);
                            b.putDouble(FirebaseAnalytics.Param.VALUE, total);
                            b.putString(FirebaseAnalytics.Param.CURRENCY, "VND");
                            analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, b);
                        } catch (Exception analyticsEx) {
                            Log.w("SeatSelection", "Analytics error (non-fatal): " + analyticsEx.getMessage());
                        }

                        // Push notification cục bộ
                        try {
                            MyFirebaseMessagingService.showNotification(this,
                                    "Đặt vé thành công!",
                                    "Ghe " + TextUtils.join(", ", sortedSeats)
                                            + " - " + movieTitle + " luc " + showtime);
                        } catch (Exception notifEx) {
                            Log.w("SeatSelection", "Notification error (non-fatal): " + notifEx.getMessage());
                        }

                        showSuccessDialog(ticket);

                    } catch (Exception e) {
                        Log.e("SeatSelection", "Loi sau khi luu ve: " + e.getMessage(), e);
                        Toast.makeText(this, "Da luu ve! (loi hien thi: " + e.getMessage() + ")",
                                Toast.LENGTH_LONG).show();
                    } finally {
                        // LUON luon tat loading du thanh cong hay that bai
                        setLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e("SeatSelection", "Firestore write failed: " + e.getMessage(), e);
                    Toast.makeText(this,
                            "Loi dat ve: " + e.getMessage() + "\n\nKiem tra Firestore Security Rules!",
                            Toast.LENGTH_LONG).show();
                });
    }

    /** Lưu ghế vừa đặt vào collection seatMaps để lần sau hiển thị đỏ */
    private void markSeatsAsBooked(List<String> seats) {
        String docId = movieId + "_" + showtime.replace(":", "_");
        Map<String, Object> data = new HashMap<>();
        data.put("bookedSeats", FieldValue.arrayUnion(seats.toArray()));
        db.collection("seatMaps").document(docId)
                .set(data, SetOptions.merge());
    }

    // ─────────────────────────────────────────────────────────────────
    //  6. Dialog thành công
    // ─────────────────────────────────────────────────────────────────
    private void showSuccessDialog(Ticket ticket) {
        List<String> sorted = ticket.getSelectedSeatIds();
        if (sorted == null) sorted = new ArrayList<>();

        String msg =
                "🎬  " + ticket.getMovieTitle() + "\n" +
                "⏰  Suất chiếu: " + ticket.getShowtime() + "\n" +
                "💺  Ghế: " + TextUtils.join(", ", sorted) + "\n" +
                "👤  " + ticket.getCustomerName() + "\n" +
                "💰  " + String.format("%,d đ", ticket.getTotalPrice()) + "\n\n" +
                "Mã vé: " + ticket.getId().substring(0, 8).toUpperCase();

        new AlertDialog.Builder(this)
                .setTitle("🎉 Đặt vé thành công!")
                .setMessage(msg)
                .setPositiveButton("Xem vé của tôi", (d, w) -> {
                    startActivity(new android.content.Intent(this, MyTicketsActivity.class));
                    finish();
                })
                .setNegativeButton("OK", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────
    //  Utilities
    // ─────────────────────────────────────────────────────────────────

    /** Xin quyền POST_NOTIFICATIONS bắt buộc trên Android 13+ */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnBook.setEnabled(!loading && !selectedSeats.isEmpty());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
