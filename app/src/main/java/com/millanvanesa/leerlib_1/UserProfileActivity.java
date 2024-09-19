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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, phoneEditText, roleEditText;
    private ImageView profileImageView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Inicializar Firebase y vistas
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        roleEditText = findViewById(R.id.roleEditText);
        profileImageView = findViewById(R.id.profileImageView);

        // Configurar los botones del pie de página
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);

        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(UserProfileActivity.this, MainActivity.class);
            startActivity(homeIntent);
        });

        searchButton.setOnClickListener(v -> {
            Intent searchIntent = new Intent(UserProfileActivity.this, CategoryActivity.class);
            startActivity(searchIntent);
        });

        addButton.setOnClickListener(v -> {
            Intent addContentIntent = new Intent(UserProfileActivity.this, AddContentActivity.class);
            startActivity(addContentIntent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent favoritesIntent = new Intent(UserProfileActivity.this, FavoritesActivity.class);
            startActivity(favoritesIntent);
        });

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(UserProfileActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(UserProfileActivity.this, ConfigurationActivity.class);
            startActivity(settingsIntent);
        });

        // Deshabilitar campos de texto
        nameEditText.setEnabled(false);
        emailEditText.setEnabled(false);
        phoneEditText.setEnabled(false);
        roleEditText.setEnabled(false);

        // Obtener el usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserProfile(userId);
        } else {
            Toast.makeText(UserProfileActivity.this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
            // Opcionalmente podrías redirigir al usuario a la pantalla de inicio de sesión
        }
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String userName = document.getString("username");
                    String userEmail = document.getString("email");
                    String userPhone = document.getString("phone");
                    String userRole = document.getString("position");
                    String profileImageUrl = document.getString("imagen");

                    nameEditText.setText(userName);
                    emailEditText.setText(userEmail);
                    phoneEditText.setText(userPhone);
                    roleEditText.setText(userRole);

                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .into(profileImageView);
                    }

                    // Actualizar la visibilidad de los botones según el rol del usuario
                    updateUIBasedOnRole(userRole);

                } else {
                    Toast.makeText(UserProfileActivity.this, "No se encontró el perfil del usuario", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(UserProfileActivity.this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
            }
        });
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
