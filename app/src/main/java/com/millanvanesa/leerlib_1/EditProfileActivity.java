package com.millanvanesa.leerlib_1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText editName, editPosition;
    private ImageView profilePicture;
    private Button saveProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;
    private Uri profileImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Referencias a las vistas
        editName = findViewById(R.id.editName);
        profilePicture = findViewById(R.id.profilePicture);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        // Obtener el usuario actual
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserProfile(userId);

            // Configurar el botón de guardar perfil
            saveProfileButton.setOnClickListener(v -> saveUserProfile());

            // Configurar el ImageView para seleccionar una nueva imagen de perfil
            profilePicture.setOnClickListener(v -> openImagePicker());

            // Configurar la navegación de los botones del footer
            setupFooterNavigation();
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfile(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String userName = document.getString("username");
                    String profileImageUrl = document.getString("imagen");

                    editName.setText(userName);

                    if (profileImageUrl != null) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .apply(new RequestOptions().transform(new CircleCrop()))
                                .into(profilePicture);
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar el perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile() {
        String updatedName = editName.getText().toString();

        // Actualizar la información en Firestore
        db.collection("users").document(userId).update("username", updatedName)
                .addOnSuccessListener(aVoid -> {
                    if (profileImageUri != null) {
                        uploadProfileImage();
                    } else {
                        Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Cierra la actividad y regresa a la pantalla anterior
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage() {
        StorageReference profileImageRef = storage.getReference().child("profile_images/" + userId + ".jpg");

        // Convertir la imagen a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profileImageRef.putBytes(data);
        uploadTask.addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Actualizar la URL de la imagen en Firestore
            db.collection("users").document(userId).update("imagen", uri.toString())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Cierra la actividad y regresa a la pantalla anterior
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar la imagen de perfil", Toast.LENGTH_SHORT).show();
                    });
        })).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al subir la imagen de perfil", Toast.LENGTH_SHORT).show();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> imagePickerResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    profileImageUri = result.getData().getData();
                    profilePicture.setImageURI(profileImageUri);
                }
            });

    private void setupFooterNavigation() {
        // Botón de Home
        ImageButton homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Botón de Buscar
        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        // Botón de Agregar Contenido
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        addContentButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, AddContentActivity.class);
            startActivity(intent);
        });

        // Botón de Mi Biblioteca
        ImageButton myLibraryButton = findViewById(R.id.myLibraryButton);
        myLibraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });

        // Botón de Tickets
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);
        ticketsButton.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfileActivity.this, TicketSoporteActivity.class);
            startActivity(intent);
        });

        // Verificar rol del usuario y mostrar/ocultar botones según corresponda
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    String position = document.getString("position");

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
                }
            });
        }
    }
}
