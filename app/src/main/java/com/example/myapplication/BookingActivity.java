package com.example.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private Spinner spinnerShowtime;
    private EditText etSeats, etName, etPhone;
    private TextView tvTotalPrice, tvMovieTitle;
    private Button btnConfirm;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseAnalytics analytics;

    private String movieId, movieTitle, selectedShowtime;
    private long pricePerSeat;
    private ArrayList<String> showtimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        analytics = FirebaseAnalytics.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Đặt vé");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        movieId = getIntent().getStringExtra("movieId");
        movieTitle = getIntent().getStringExtra("movieTitle");
        pricePerSeat = getIntent().getLongExtra("moviePrice", 80000L);
        showtimes = getIntent().getStringArrayListExtra("movieShowtimes");

        spinnerShowtime = findViewById(R.id.spinnerShowtime);
        etSeats = findViewById(R.id.etSeats);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvMovieTitle = findViewById(R.id.tvMovieTitle);
        btnConfirm = findViewById(R.id.btnConfirm);
        progressBar = findViewById(R.id.progressBar);

        tvMovieTitle.setText(movieTitle);
        updateTotalPrice();

        // Setup spinner suất chiếu
        if (showtimes != null && !showtimes.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, showtimes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerShowtime.setAdapter(adapter);
            selectedShowtime = showtimes.get(0);

            spinnerShowtime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedShowtime = showtimes.get(position);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Cập nhật tổng tiền khi số ghế thay đổi
        etSeats.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalPrice();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    private void updateTotalPrice() {
        int seats = 1;
        try {
            String seatsStr = etSeats.getText().toString().trim();
            if (!seatsStr.isEmpty()) {
                seats = Integer.parseInt(seatsStr);
            }
        } catch (NumberFormatException ignored) {}

        long total = seats * pricePerSeat;
        tvTotalPrice.setText(String.format("Tổng tiền: %,d đ", total));
    }

    private void confirmBooking() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(seatsStr)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
            if (seats <= 0 || seats > 10) {
                Toast.makeText(this, "Số ghế phải từ 1 đến 10", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số ghế không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long totalPrice = (long) seats * pricePerSeat;
        long bookingTime = System.currentTimeMillis();

        Ticket ticket = new Ticket(userId, movieId, movieTitle, selectedShowtime,
                seats, name, phone, totalPrice, bookingTime);

        setLoading(true);

        // Lưu vé vào Firestore
        db.collection("tickets").add(ticket)
                .addOnSuccessListener(documentReference -> {
                    ticket.setId(documentReference.getId());

                    // Log Firebase Analytics event đặt vé
                    Bundle analyticsBundle = new Bundle();
                    analyticsBundle.putString(FirebaseAnalytics.Param.ITEM_ID, movieId);
                    analyticsBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, movieTitle);
                    analyticsBundle.putLong(FirebaseAnalytics.Param.VALUE, totalPrice);
                    analyticsBundle.putString(FirebaseAnalytics.Param.CURRENCY, "VND");
                    analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, analyticsBundle);

                    // Gửi push notification cục bộ
                    MyFirebaseMessagingService.showNotification(this,
                            "Đặt vé thành công! 🎬",
                            "Vé xem \"" + movieTitle + "\" lúc " + selectedShowtime + " đã được xác nhận.");

                    // Lên lịch nhắc nhở trước giờ chiếu 30 phút
                    scheduleMovieReminder(ticket);

                    setLoading(false);
                    showSuccessDialog(ticket);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Lỗi đặt vé: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog(Ticket ticket) {
        String message = "Phim: " + ticket.getMovieTitle() + "\n" +
                "Suất: " + ticket.getShowtime() + "\n" +
                "Ghế: " + ticket.getSeats() + "\n" +
                "Khách hàng: " + ticket.getCustomerName() + "\n" +
                "Tổng tiền: " + String.format("%,d đ", ticket.getTotalPrice()) + "\n" +
                "Mã vé: " + ticket.getId().substring(0, 8).toUpperCase();

        new AlertDialog.Builder(this)
                .setTitle("🎉 Đặt vé thành công!")
                .setMessage(message)
                .setPositiveButton("Xem vé của tôi", (dialog, which) -> {
                    startActivity(new android.content.Intent(this, MyTicketsActivity.class));
                    finish();
                })
                .setNegativeButton("Đặt tiếp", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnConfirm.setEnabled(!loading);
    }

    private void scheduleMovieReminder(Ticket ticket) {
        try {
            // Parse showtime (format: HH:mm)
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date showtimeDate = sdf.parse(ticket.getShowtime());

            // Tạo Calendar cho ngày hiện tại với giờ chiếu
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(showtimeDate);
            calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

            // Nếu giờ chiếu đã qua hôm nay, đặt cho ngày mai
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Trừ 30 phút để nhắc nhở
            calendar.add(Calendar.MINUTE, -30);

            // Tạo Intent cho BroadcastReceiver
            Intent intent = new Intent(this, MovieReminderReceiver.class);
            intent.putExtra("movieTitle", ticket.getMovieTitle());
            intent.putExtra("showtime", ticket.getShowtime());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ticket.getId().hashCode(), // unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Lên lịch Alarm
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        // Nếu không được phép, dùng set() không chính xác
                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }

        } catch (ParseException e) {
            // Nếu parse lỗi, bỏ qua
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
