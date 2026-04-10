package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final Context context;
    private final List<Ticket> tickets;

    public TicketAdapter(Context context, List<Ticket> tickets) {
        this.context = context;
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.bind(ticket);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMovieTitle;
        private final TextView tvShowtime;
        private final TextView tvSeats;
        private final TextView tvCustomerName;
        private final TextView tvTotalPrice;
        private final TextView tvBookingDate;
        private final TextView tvStatus;
        private final TextView tvTicketId;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMovieTitle = itemView.findViewById(R.id.tvMovieTitle);
            tvShowtime = itemView.findViewById(R.id.tvShowtime);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
        }

        public void bind(Ticket ticket) {
            tvMovieTitle.setText(ticket.getMovieTitle());
            tvShowtime.setText("Suất: " + ticket.getShowtime());
            tvSeats.setText("Số ghế: " + ticket.getSeats());
            tvCustomerName.setText(ticket.getCustomerName());
            tvTotalPrice.setText(String.format("%,d đ", ticket.getTotalPrice()));
            tvStatus.setText("Đã xác nhận");

            if (ticket.getId() != null) {
                tvTicketId.setText("Mã vé: " + ticket.getId().substring(0, Math.min(8, ticket.getId().length())).toUpperCase());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvBookingDate.setText(sdf.format(new Date(ticket.getBookingTime())));
        }
    }
}
