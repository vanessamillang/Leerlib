package com.millanvanesa.leerlib_1;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AboutLeerlibActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_leerlib);

        // Inicializar Firebase y vistas
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.profileButton); // Asegúrate de tener este ImageView en tu layout

        // Cargar la foto de perfil del usuario
        loadUserProfileImage();

        // Configurar la navegación de los botones del footer
        setupFooterNavigation();
    }

    private void loadUserProfileImage() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Obtener el UID del usuario
            String uid = currentUser.getUid();

            // Obtener el documento del usuario
            firestore.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Obtener la URL de la imagen de perfil desde el documento
                        String profileImageUrl = document.getString("imagen");
                        if (profileImageUrl != null) {
                            // Usar Glide para cargar la imagen desde la URL
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .apply(new RequestOptions().transform(new CircleCrop()))
                                    .placeholder(R.drawable.perfil) // Imagen por defecto
                                    .error(R.drawable.perfil) // Imagen si ocurre un error
                                    .into(profileImageView);
                        } else {
                            // Mostrar una imagen predeterminada si no hay URL
                            profileImageView.setImageResource(R.drawable.perfil);
                        }
                    } else {
                        Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Manejar el caso en que el usuario no está autenticado
            Toast.makeText(this, "No se pudo obtener el usuario actual", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFooterNavigation() {
        // Botón de Home
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutLeerlibActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Botón de Buscar
        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutLeerlibActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        // Botón de Agregar Contenido
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        addContentButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutLeerlibActivity.this, AddContentActivity.class);
            startActivity(intent);
        });

        // Botón de Mi Biblioteca
        ImageButton myLibraryButton = findViewById(R.id.myLibraryButton);
        myLibraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutLeerlibActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });

        // Botón de Tickets
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);
        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(AboutLeerlibActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        // Verificar el rol del usuario y ajustar la visibilidad de los botones
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String position = document.getString("position");
                        updateUIBasedOnRole(position, addContentButton, ticketsButton);
                    } else {
                        Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Error al obtener el rol del usuario", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Método para actualizar la visibilidad de los botones según el rol
    private void updateUIBasedOnRole(String position, ImageButton addContentButton, ImageButton ticketsButton) {
        if (position != null) {
            switch (position) {
                case "admin":
                    // Admin puede ver ambos botones
                    addContentButton.setVisibility(View.VISIBLE);
                    ticketsButton.setVisibility(View.VISIBLE);
                    break;
                case "support":
                    // Soporte solo puede ver el botón de tickets
                    addContentButton.setVisibility(View.GONE);
                    ticketsButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    // Usuarios comunes no ven ninguno de estos botones
                    addContentButton.setVisibility(View.GONE);
                    ticketsButton.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
