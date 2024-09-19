package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelpAndSupportActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private RecyclerView ticketsRecyclerView;
    private TicketAdapter ticketAdapter;
    private List<Ticket> ticketsList;

    private ImageView profilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_and_support);

        // Inicializar Firestore y Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No has iniciado sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Referencia a los elementos del layout
        Button submitButton = findViewById(R.id.supportTicketSubmit);
        EditText subjectEditText = findViewById(R.id.supportTicketSubject);
        EditText problemEditText = findViewById(R.id.supportTicketProblem);
        ticketsRecyclerView = findViewById(R.id.ticketsRecyclerView);
        profilePicture = findViewById(R.id.profileButton);

        // Inicializar la lista de tickets
        ticketsList = new ArrayList<>();

        // Configurar el adaptador de RecyclerView
        ticketAdapter = new TicketAdapter(ticketsList);
        ticketsRecyclerView.setAdapter(ticketAdapter);
        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Cargar los tickets del usuario actual
        loadUserTickets();

        // Cargar la foto de perfil del usuario actual
        loadUserProfile(currentUser.getUid());

        // Configurar los botones del pie de página
        setupFooterNavigation();

        // Manejar el clic del botón de envío de tickets
        submitButton.setOnClickListener(v -> {
            String subject = subjectEditText.getText().toString().trim();
            String problem = problemEditText.getText().toString().trim();

            if (!subject.isEmpty() && !problem.isEmpty()) {
                saveTicketToFirestore(subject, problem, currentUser.getUid());
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cargar los tickets del usuario actual cada vez que se reanuda la actividad
        loadUserTickets();
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
                                .into(profilePicture);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la foto de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTicketToFirestore(String subject, String problem, String userUid) {
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("subject", subject);
        ticket.put("problem", problem);
        ticket.put("status", "Pendiente");
        ticket.put("createdAt", Timestamp.now());
        ticket.put("userUid", userUid);

        db.collection("tickets")
                .add(ticket)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Ticket generado correctamente", Toast.LENGTH_SHORT).show();
                    loadUserTickets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al generar el ticket", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserTickets() {
        String uid = currentUser.getUid();

        db.collection("tickets")
                .whereEqualTo("userUid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ticketsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String ticketId = document.getId();
                            String subject = document.getString("subject");
                            String status = document.getString("status");
                            ticketsList.add(new Ticket(ticketId, subject, status));
                        }
                        ticketAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error al cargar los tickets", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupFooterNavigation() {
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(HelpAndSupportActivity.this, MainActivity.class);
            startActivity(homeIntent);
        });

        searchButton.setOnClickListener(v -> {
            Intent searchIntent = new Intent(HelpAndSupportActivity.this, CategoryActivity.class);
            startActivity(searchIntent);
        });

        addButton.setOnClickListener(v -> {
            Intent addContentIntent = new Intent(HelpAndSupportActivity.this, AddContentActivity.class);
            startActivity(addContentIntent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent favoritesIntent = new Intent(HelpAndSupportActivity.this, FavoritesActivity.class);
            startActivity(favoritesIntent);
        });

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(HelpAndSupportActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        // Verificar el rol del usuario y ajustar la visibilidad de los botones
        db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                String position = document.getString("position");

                if (position != null) {
                    switch (position) {
                        case "admin":
                            addButton.setVisibility(View.VISIBLE);
                            ticketsButton.setVisibility(View.VISIBLE);
                            break;
                        case "support":
                            addButton.setVisibility(View.GONE);
                            ticketsButton.setVisibility(View.VISIBLE);
                            break;
                        default:
                            addButton.setVisibility(View.GONE);
                            ticketsButton.setVisibility(View.GONE);
                            break;
                    }
                }
            }
        });
    }
}
