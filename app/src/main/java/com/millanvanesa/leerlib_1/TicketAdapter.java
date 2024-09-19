package com.millanvanesa.leerlib_1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> ticketList;
    private FirebaseFirestore db;

    public TicketAdapter(List<Ticket> ticketList) {
        this.ticketList = ticketList;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ticket_item, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);

        // Mostrar el subject del ticket
        holder.ticketSubjectTextView.setText(ticket.getSubject());
        holder.ticketStatusTextView.setText(ticket.getStatus());

        // Cambiar el color del estado del ticket
        switch (ticket.getStatus()) {
            case "Pendiente":
                holder.ticketStatusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
                break;
            case "En proceso":
                holder.ticketStatusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.yellow)); // Color amarillo para "en proceso"
                break;
            case "Resuelto":
                holder.ticketStatusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
                break;
        }

        // Marcar como resuelto
        holder.markResolvedButton.setOnClickListener(v -> {
            // Actualizar el estado del ticket a "resuelto"
            ticket.setStatus("resuelto");
            db.collection("tickets").document(ticket.getId())
                    .update("status", "resuelto")
                    .addOnSuccessListener(aVoid -> {
                        // Actualizar el estado en la vista
                        holder.ticketStatusTextView.setText("resuelto");
                        holder.ticketStatusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
                    })
                    .addOnFailureListener(e -> {
                        // Manejar el error
                    });
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView ticketSubjectTextView;
        TextView ticketStatusTextView;
        ImageButton markResolvedButton;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            ticketSubjectTextView = itemView.findViewById(R.id.ticketSubjectTextView);
            ticketStatusTextView = itemView.findViewById(R.id.ticketStatusTextView);
            markResolvedButton = itemView.findViewById(R.id.markResolvedButton);
        }
    }
}
