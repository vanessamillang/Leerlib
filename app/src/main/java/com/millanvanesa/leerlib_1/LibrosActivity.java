package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibrosActivity extends AppCompatActivity {

    private static final String TAG = "LibrosActivity";

    private ImageButton profileImageView;
    private ListView librosListView;
    private SearchView searchView;
    private ArrayAdapter<String> librosAdapter;
    private List<String> libroList;
    private List<String> libroIdList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libros);

        initFirebase();
        initViews();
        setupListeners();

        String categoryId = getIntent().getStringExtra("categoryId");
        String categoryName = getIntent().getStringExtra("categoryName");

        if (categoryId == null || categoryId.isEmpty() || categoryName == null || categoryName.isEmpty()) {
            showError("El ID o nombre de la categoría es nulo o vacío");
            return;
        }

        loadProfileImage();
        fetchLibros(categoryId);

        // Configurar la visibilidad de los botones según el rol del usuario
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            String position = document.getString("position");
                            // Actualizar UI según el rol del usuario
                            updateUIBasedOnRole(position);
                        }
                    });
        }
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : null;
    }

    private void initViews() {
        librosListView = findViewById(R.id.librosListView);
        searchView = findViewById(R.id.searchView);
        profileImageView = findViewById(R.id.profileButton);

        libroList = new ArrayList<>();
        libroIdList = new ArrayList<>();
        librosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, libroList);
        librosListView.setAdapter(librosAdapter);
    }

    private void setupListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterLibros(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterLibros(newText);
                return false;
            }
        });

        librosListView.setOnItemClickListener((parent, view, position, id) -> {
            String libroId = libroIdList.get(position);
            Intent intent = new Intent(LibrosActivity.this, LibroDetailActivity.class);
            intent.putExtra("libroId", libroId);
            intent.putExtra("username", currentUser != null ? currentUser.getDisplayName() : "Unknown");
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.profileButton).setOnClickListener(view -> {
            Intent intent = new Intent(LibrosActivity.this, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.addContentButton).setOnClickListener(view -> {
            Intent intent = new Intent(LibrosActivity.this, AddContentActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.myLibraryButton).setOnClickListener(view -> {
            Intent intent = new Intent(LibrosActivity.this, FavoritesActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.searchButton).setOnClickListener(view -> {
            Intent intent = new Intent(LibrosActivity.this, CategoryActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.homeButton).setOnClickListener(view -> {
            Intent intent = new Intent(LibrosActivity.this, MainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.ticketsButton).setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(LibrosActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });
    }

    private void loadProfileImage() {
        if (userId != null) {
            Log.d(TAG, "Loading profile image for UID: " + userId);
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String profileImageUrl = document.getString("imagen");
                                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                    Log.d(TAG, "Profile image URL found: " + profileImageUrl);
                                    Glide.with(LibrosActivity.this)
                                            .load(profileImageUrl)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(profileImageView);
                                } else {
                                    Log.d(TAG, "No profile image URL found, using default image.");
                                    profileImageView.setImageResource(R.drawable.perfil);
                                }
                            } else {
                                Log.d(TAG, "No such document");
                                profileImageView.setImageResource(R.drawable.perfil);
                            }
                        } else {
                            Log.d(TAG, "Get failed with ", task.getException());
                            profileImageView.setImageResource(R.drawable.perfil);
                        }
                    });
        } else {
            Log.d(TAG, "User ID is null, cannot load profile image.");
            profileImageView.setImageResource(R.drawable.perfil);
        }
    }

    private void fetchLibros(String categoryId) {
        db.collection("libros")
                .whereEqualTo("category", categoryId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            showError("Error al obtener libros: " + error.getMessage());
                            return;
                        }

                        if (value != null) {
                            libroList.clear();
                            libroIdList.clear();
                            for (DocumentSnapshot document : value.getDocuments()) {
                                String libroId = document.getId();
                                String nombre = document.getString("name");
                                Log.d(TAG, "Nombre del libro: " + nombre);
                                libroList.add(nombre);
                                libroIdList.add(libroId);
                            }
                            librosAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void filterLibros(String query) {
        ArrayList<String> filteredList = new ArrayList<>();
        for (String libro : libroList) {
            if (libro.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(libro);
            }
        }
        librosAdapter.clear();
        librosAdapter.addAll(filteredList);
        librosAdapter.notifyDataSetChanged();
    }

    private void showError(String message) {
        Toast.makeText(LibrosActivity.this, message, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    private void updateUIBasedOnRole(String position) {
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        if (position != null) {
            switch (position) {
                case "admin":
                    // Admin puede ver ambos botones
                    addButton.setVisibility(View.VISIBLE);
                    ticketsButton.setVisibility(View.VISIBLE);
                    break;
                case "support":
                    // Soporte solo puede ver el botón de tickets
                    addButton.setVisibility(View.GONE);
                    ticketsButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    // Usuarios comunes no ven ninguno de estos botones
                    addButton.setVisibility(View.GONE);
                    ticketsButton.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
