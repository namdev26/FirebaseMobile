package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Movie> movieList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BottomNavigationView bottomNav;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // Kiểm tra đăng nhập
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        // RecyclerView
        movieList = new ArrayList<>();
        movieAdapter = new MovieAdapter(this, movieList, movie -> {
            Intent intent = new Intent(this, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getId());
            intent.putExtra("movieTitle", movie.getTitle());
            intent.putExtra("movieDescription", movie.getDescription());
            intent.putExtra("movieGenre", movie.getGenre());
            intent.putExtra("movieDuration", movie.getDuration());
            intent.putExtra("movieRating", movie.getRating());
            intent.putExtra("moviePosterUrl", movie.getPosterUrl());
            intent.putExtra("moviePrice", movie.getPrice());
            if (movie.getShowtimes() != null) {
                intent.putStringArrayListExtra("movieShowtimes", new ArrayList<>(movie.getShowtimes()));
            }
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(movieAdapter);

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_movies) {
                recyclerView.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(this, MyTicketsActivity.class));
                return true;
            }
            return false;
        });

        loadMovies();
    }

    private void loadMovies() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("movies")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Xóa dữ liệu cũ và seed lại
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                    seedSampleMovies();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải phim: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void seedSampleMovies() {
        // Tạo dữ liệu mẫu vào Firestore lần đầu
        List<Movie> samples = new ArrayList<>();
        samples.add(new Movie(null, "Dư Chấn Thành Phố",
                "Một đội phản ứng nhanh phải giải mã chuỗi tín hiệu bí ẩn trước khi hệ thống giao thông thông minh của thành phố bị tê liệt hoàn toàn.",
                "Hành động / Hình sự", 132, 8.1f,
                "https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg",
                Arrays.asList("08:45", "11:15", "13:50", "16:30", "19:10", "21:45"), 89000L));

        samples.add(new Movie(null, "Bản Đồ Sao Đêm",
                "Một nữ phi công trẻ nhận nhiệm vụ hộ tống chuyến bay nghiên cứu cực quang và phát hiện bí mật liên quan đến gia đình mình.",
                "Phiêu lưu / Khoa học viễn tưởng", 124, 7.9f,
                "https://image.tmdb.org/t/p/w500/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg",
                Arrays.asList("09:20", "12:10", "15:00", "18:00", "20:50"), 92000L));

        samples.add(new Movie(null, "Gió Nghịch Mùa",
                "Giữa mùa bão biển miền Trung, một thuyền trưởng và con gái phải hợp lực cứu đội cứu hộ mắc kẹt ngoài khơi.",
                "Chính kịch / Sinh tồn", 118, 8.0f,
                "https://image.tmdb.org/t/p/w500/62HCnUTziyWcpDaBO2i1DX17ljH.jpg",
                Arrays.asList("10:00", "12:40", "15:20", "18:10", "21:10"), 84000L));

        samples.add(new Movie(null, "Mật Mã Số 7",
                "Một lập trình viên an ninh mạng bị cuốn vào trò chơi săn đuổi khi dữ liệu quốc gia bị rò rỉ qua một ứng dụng tưởng chừng vô hại.",
                "Giật gân / Công nghệ", 127, 8.3f,
                "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg",
                Arrays.asList("09:40", "12:30", "15:40", "18:40", "21:30"), 95000L));

        samples.add(new Movie(null, "Khúc Hát Trên Mây",
                "Một ban nhạc indie vô tình nổi tiếng sau một đêm livestream và phải đối diện với cái giá của sự nổi tiếng quá nhanh.",
                "Âm nhạc / Tình cảm", 112, 7.7f,
                "https://image.tmdb.org/t/p/w500/9Gtg2DzBhmYamXBS1hKAhiwbBKS.jpg",
                Arrays.asList("08:30", "11:00", "13:40", "16:20", "19:00", "21:20"), 79000L));

        // Lưu tất cả vào Firestore
        for (Movie movie : samples) {
            db.collection("movies").add(movie)
                    .addOnSuccessListener(docRef -> {
                        movie.setId(docRef.getId());
                        movieList.add(movie);
                        movieAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.GONE);
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
