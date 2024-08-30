package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText nameEditText, emailEditText, phoneEditText, roleEditText;
    private Button saveButton;
    private Button changePhotoButton;
    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri imageUri;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Inicializar Firebase y vistas
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        roleEditText = findViewById(R.id.roleEditText);
        saveButton = findViewById(R.id.saveButton);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        profileImageView = findViewById(R.id.profileImageView);

        // Obtener el userId del Intent
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");

        if (userId != null) {
            loadUserProfile(userId);

            changePhotoButton.setOnClickListener(v -> openImageSelector());

            saveButton.setOnClickListener(v -> {
                String userName = nameEditText.getText().toString();
                String userEmail = emailEditText.getText().toString();
                String userPhone = phoneEditText.getText().toString();
                String userRole = roleEditText.getText().toString();
                saveUserProfile(userId, userName, userEmail, userPhone, userRole);
                if (imageUri != null) {
                    uploadImage(userId);
                }
            });

            // Configurar el botón de logout
            Button logoutButton = findViewById(R.id.logoutButton);
            logoutButton.setOnClickListener(v -> {
                mAuth.signOut();
                Intent logoutIntent = new Intent(UserProfileActivity.this, LoginActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                finish();
            });

            // Configurar el botón para ir a la pantalla de favoritos
            ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
            favoritesButton.setOnClickListener(v -> {
                if (userId != null) {
                    Intent favoritesIntent = new Intent(UserProfileActivity.this, FavoritesActivity.class);
                    favoritesIntent.putExtra("userId", userId); // Agregado userId
                    startActivity(favoritesIntent);
                }
            });

            // Configurar el botón para ir a la pantalla de agregar contenido
            ImageButton addContentButton = findViewById(R.id.addContentButton);
            addContentButton.setOnClickListener(v -> {
                if (userId != null) {
                    Intent addContentIntent = new Intent(UserProfileActivity.this, AddContentActivity.class);
                    addContentIntent.putExtra("userId", userId); // Agregado userId
                    startActivity(addContentIntent);
                }
            });
        } else {
            Toast.makeText(UserProfileActivity.this, "Error: No se recibió el ID de usuario", Toast.LENGTH_SHORT).show();
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
                }
            } else {
                Toast.makeText(UserProfileActivity.this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfile(String userId, String userName, String userEmail, String userPhone, String userRole) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", userName);
        user.put("email", userEmail);
        user.put("phone", userPhone);
        user.put("position", userRole);

        db.collection("users").document(userId).update(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(UserProfileActivity.this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UserProfileActivity.this, "Error al actualizar perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImage(String userId) {
        if (imageUri != null) {
            String imageFileName = "profile_images/" + userId + "_" + UUID.randomUUID().toString();
            StorageReference imageRef = storageReference.child(imageFileName);
            imageRef.putFile(imageUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    imageRef.getDownloadUrl().addOnCompleteListener(uriTask -> {
                        if (uriTask.isSuccessful()) {
                            String downloadUri = uriTask.getResult().toString();
                            db.collection("users").document(userId).update("imagen", downloadUri).addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(UserProfileActivity.this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                                    Glide.with(this)
                                            .load(downloadUri)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(profileImageView); // Actualiza la imagen del perfil en el ImageView
                                } else {
                                    Toast.makeText(UserProfileActivity.this, "Error al actualizar la foto de perfil", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else {
                    Toast.makeText(UserProfileActivity.this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
