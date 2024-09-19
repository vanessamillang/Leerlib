package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TicketSoporteActivity extends AppCompatActivity {

    private RecyclerView ticketsRecyclerView;
    private SupportTicketAdapter adapter;
    private List<SupportTicket> ticketList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ImageView profileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_soporte);

        ticketsRecyclerView = findViewById(R.id.ticketRecyclerView);
        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ticketList = new ArrayList<>();
        adapter = new SupportTicketAdapter(ticketList, this); // Pasar el contexto 'this'
        ticketsRecyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Cargar los tickets
        loadTickets();

        // Configurar los botones del pie de página
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);
        profileButton = findViewById(R.id.profileButton);

        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(TicketSoporteActivity.this, MainActivity.class);
            startActivity(homeIntent);
        });

        searchButton.setOnClickListener(v -> {
            Intent searchIntent = new Intent(TicketSoporteActivity.this, CategoryActivity.class);
            startActivity(searchIntent);
        });

        addButton.setOnClickListener(v -> {
            Intent addContentIntent = new Intent(TicketSoporteActivity.this, AddContentActivity.class);
            startActivity(addContentIntent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent favoritesIntent = new Intent(TicketSoporteActivity.this, FavoritesActivity.class);
            startActivity(favoritesIntent);
        });

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(TicketSoporteActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        profileButton.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent profileIntent = new Intent(TicketSoporteActivity.this, UserProfileActivity.class);
                profileIntent.putExtra("userId", currentUser.getUid());
                startActivity(profileIntent);
            } else {
                Toast.makeText(TicketSoporteActivity.this, "No se puede obtener el UID del usuario", Toast.LENGTH_SHORT).show();
            }
        });

        // Cargar la foto de perfil del usuario actual
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cargar los tickets nuevamente al reanudar la actividad para actualizar la lista
        loadTickets();
    }

    private void loadTickets() {
        db.collection("tickets")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ticketList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SupportTicket ticket = document.toObject(SupportTicket.class);
                            ticket.setId(document.getId());  // Establecer el ID del documento

                            // Asegurarse de que se cargue el problema (description), si está presente
                            if (document.contains("problem")) {
                                ticket.setProblem(document.getString("problem"));
                            }

                            ticketList.add(ticket);
                        }
                        filterTickets(); // Aplicar filtro después de cargar todos los tickets
                    }
                });
    }

    private void filterTickets() {
        List<SupportTicket> filteredList = new ArrayList<>();
        for (SupportTicket ticket : ticketList) {
            if (!"Resuelto".equalsIgnoreCase(ticket.getStatus())) {
                filteredList.add(ticket);
            }
        }
        ticketList.clear();
        ticketList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    public void updateTicketStatus(SupportTicket ticket, String selectedStatus, int position) {
        // Actualizar el estado del ticket en Firestore
        db.collection("tickets").document(ticket.getId())
                .update("status", selectedStatus)  // Actualizar con el estado seleccionado
                .addOnSuccessListener(aVoid -> {
                    if ("Resuelto".equalsIgnoreCase(selectedStatus)) {
                        // Si el ticket se resuelve, eliminarlo de la lista
                        ticketList.remove(position);
                        adapter.notifyItemRemoved(position); // Notificar al adaptador que se ha eliminado un ítem
                    } else {
                        // Si no se resuelve, solo notificar que los datos han cambiado
                        adapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar el error
                });
    }


    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String profileImageUrl = document.getString("imagen");

                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .into(profileButton);
                    }
                }
            }
        });
    }
}
