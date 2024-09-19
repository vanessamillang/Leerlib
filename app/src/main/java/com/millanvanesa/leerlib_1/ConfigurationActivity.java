package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConfigurationActivity extends AppCompatActivity {

    private static final String TAG = "ConfigurationActivity";

    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencia al ImageView para la foto de perfil
        profileImageView = findViewById(R.id.profileButton);

        // Referencias a los TextView de las opciones
        TextView editProfileTextView = findViewById(R.id.editProfileTextView);
        TextView accountSettingsTextView = findViewById(R.id.accountSettingsTextView);
        // TextView subscriptionsTextView = findViewById(R.id.subscriptionsTextView);
        TextView aboutLeerlibTextView = findViewById(R.id.aboutLeerlibTextView);
        TextView helpSupportTextView = findViewById(R.id.helpSupportTextView);
        TextView logoutTextView = findViewById(R.id.logoutTextView);

        // Configurar listeners para los clics en los TextView
        editProfileTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        accountSettingsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, AccountSettingsActivity.class);
            startActivity(intent);
        });

        /* subscriptionsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, SubscriptionsActivity.class);
            startActivity(intent);
        }); */

        aboutLeerlibTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, AboutLeerlibActivity.class);
            startActivity(intent);
        });

        helpSupportTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, HelpAndSupportActivity.class);
            startActivity(intent);
        });

        logoutTextView.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ConfigurationActivity.this, "Has cerrado sesión", Toast.LENGTH_SHORT).show();
            // Redirigir al usuario a la pantalla de inicio de sesión
            Intent intent = new Intent(ConfigurationActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Opcional: cerrar la actividad actual
        });

        // Cargar la foto de perfil del usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadProfileImage(userId);
            updateUIBasedOnRole(userId); // Actualizar UI basado en el rol del usuario
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        // Configurar la navegación de los botones del footer
        setupFooterNavigation();
    }

    private void loadProfileImage(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String profileImageUrl = document.getString("imagen");
                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(new RequestOptions().transform(new CircleCrop()))
                                .into(profileImageView);
                    } else {
                        // Usa drawable por defecto si no hay URL
                        profileImageView.setImageResource(R.drawable.perfil);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIBasedOnRole(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                String position = document.getString("position"); // Obtener el rol del usuario

                // Actualizar visibilidad de botones según el rol
                ImageButton addContentButton = findViewById(R.id.addContentButton);
                ImageButton ticketsButton = findViewById(R.id.ticketsButton); // Asegúrate de que el botón de tickets está en el XML

                if (position != null) {
                    switch (position) {
                        case "admin":
                            addContentButton.setVisibility(View.VISIBLE);
                            ticketsButton.setVisibility(View.VISIBLE);
                            break;
                        case "support":
                            addContentButton.setVisibility(View.GONE);
                            ticketsButton.setVisibility(View.VISIBLE);
                            break;
                        default:
                            addContentButton.setVisibility(View.GONE);
                            ticketsButton.setVisibility(View.GONE);
                            break;
                    }
                }
            } else {
                Log.d(TAG, "Error obteniendo el rol del usuario: ", task.getException());
            }
        });
    }

    private void setupFooterNavigation() {
        // Botón de Home
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Botón de Buscar
        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        // Botón de Agregar Contenido
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        addContentButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, AddContentActivity.class);
            startActivity(intent);
        });

        // Botón de Mi Biblioteca
        ImageButton myLibraryButton = findViewById(R.id.myLibraryButton);
        myLibraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConfigurationActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });

        // Botón de Tickets
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);
        ticketsButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(ConfigurationActivity.this, TicketSoporteActivity.class);
                intent.putExtra("userId", user.getUid());
                startActivity(intent);
            } else {
                Log.e(TAG, "Intento de iniciar TicketSoporteActivity sin usuario autenticado");
            }
        });
    }
}
