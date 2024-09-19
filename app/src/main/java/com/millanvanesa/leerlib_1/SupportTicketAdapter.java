package com.millanvanesa.leerlib_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SupportTicketAdapter extends RecyclerView.Adapter<SupportTicketAdapter.SupportTicketViewHolder> {

    private List<SupportTicket> ticketList;
    private Context context;

    public SupportTicketAdapter(List<SupportTicket> ticketList, Context context) {
        this.ticketList = ticketList;
        this.context = context;
    }

    @NonNull
    @Override
    public SupportTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.support_ticket_item, parent, false);
        return new SupportTicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupportTicketViewHolder holder, int position) {
        SupportTicket ticket = ticketList.get(position);

        // Mostrar el subject del ticket
        holder.ticketSubjectTextView.setText(ticket.getSubject());
        holder.ticketProblemTextView.setText(ticket.getProblem()); // Mostrar la descripción

        // Configurar el Spinner con las opciones de estado
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                holder.itemView.getContext(),
                R.array.ticket_status_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.statusSpinner.setAdapter(adapter);

        // Seleccionar el estado actual del ticket
        int spinnerPosition = adapter.getPosition(ticket.getStatus());
        holder.statusSpinner.setSelection(spinnerPosition);

        holder.statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStatus = (String) parent.getItemAtPosition(position);

                // Actualizar el estado del ticket solo si ha cambiado
                if (!ticket.getStatus().equalsIgnoreCase(selectedStatus)) {
                    ticket.setStatus(selectedStatus);
                    ((TicketSoporteActivity) context).updateTicketStatus(ticket, selectedStatus, holder.getAdapterPosition());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public class SupportTicketViewHolder extends RecyclerView.ViewHolder {
        TextView ticketSubjectTextView;
        TextView ticketProblemTextView;
        Spinner statusSpinner;  // Asumiendo que tienes un Spinner para seleccionar el estado

        public SupportTicketViewHolder(View itemView) {
            super(itemView);
            ticketSubjectTextView = itemView.findViewById(R.id.ticketSubjectTextView);
            ticketProblemTextView = itemView.findViewById(R.id.ticketProblemTextView);
            statusSpinner = itemView.findViewById(R.id.statusSpinner);  // Spinner para cambiar estado
        }
    }
}
