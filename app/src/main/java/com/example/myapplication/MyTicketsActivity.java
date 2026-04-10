package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TicketAdapter ticketAdapter;
    private List<Ticket> ticketList;
    private ProgressBar progressBar;
    private View tvEmpty;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Vé của tôi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        ticketList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(this, ticketList);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(ticketAdapter);

        loadTickets();
    }

    private void loadTickets() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        // Chỉ filter theo userId, KHÔNG orderBy để tránh cần composite index
        db.collection("tickets")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ticketList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Ticket ticket = doc.toObject(Ticket.class);
                        ticket.setId(doc.getId());
                        ticketList.add(ticket);
                    }
                    // Sort in-memory: mới nhất lên đầu
                    ticketList.sort((a, b) -> Long.compare(b.getBookingTime(), a.getBookingTime()));

                    ticketAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(ticketList.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi tải vé: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
