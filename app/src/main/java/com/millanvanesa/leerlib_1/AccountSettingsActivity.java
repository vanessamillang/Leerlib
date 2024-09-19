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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView profileImageView;
    private EditText emailEditText, phoneEditText, currentPasswordEditText, newPasswordEditText, confirmNewPasswordEditText;
    private Button saveEmailPhoneButton, savePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Referencias a los elementos de la interfaz
        profileImageView = findViewById(R.id.profileButton);
        emailEditText = findViewById(R.id.editEmail);
        phoneEditText = findViewById(R.id.editPhoneNumber);
        currentPasswordEditText = findViewById(R.id.currentPassword);
        newPasswordEditText = findViewById(R.id.newPassword);
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPassword);
        saveEmailPhoneButton = findViewById(R.id.saveEmailPhoneButton);
        savePasswordButton = findViewById(R.id.savePasswordButton);

        // Cargar la información del usuario
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            loadUserData(userId);
            loadProfileImage(userId);
        } else {
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
        }

        // Configurar el listener para guardar los cambios de email y teléfono
        saveEmailPhoneButton.setOnClickListener(v -> updateEmailPhone());

        // Configurar el listener para cambiar la contraseña
        savePasswordButton.setOnClickListener(v -> updatePassword());

        // Configurar los botones del pie de página
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        homeButton.setOnClickListener(v -> {
            Intent homeIntent = new Intent(AccountSettingsActivity.this, MainActivity.class);
            startActivity(homeIntent);
        });

        searchButton.setOnClickListener(v -> {
            Intent searchIntent = new Intent(AccountSettingsActivity.this, CategoryActivity.class);
            startActivity(searchIntent);
        });

        addButton.setOnClickListener(v -> {
            Intent addContentIntent = new Intent(AccountSettingsActivity.this, AddContentActivity.class);
            startActivity(addContentIntent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent favoritesIntent = new Intent(AccountSettingsActivity.this, FavoritesActivity.class);
            startActivity(favoritesIntent);
        });

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(AccountSettingsActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        // Verificar el rol del usuario y ajustar la visibilidad de los botones
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String position = document.getString("position");
                        updateUIBasedOnRole(position, addButton, ticketsButton);
                    } else {
                        Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Error al obtener el rol del usuario", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
                    }
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String email = document.getString("email");
                    String phone = document.getString("phone");
                    if (email != null) emailEditText.setText(email);
                    if (phone != null) phoneEditText.setText(phone);
                } else {
                    Toast.makeText(this, "Error: El perfil no existe", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error al cargar los datos del usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmailPhone() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String newEmail = emailEditText.getText().toString();
            String newPhone = phoneEditText.getText().toString();

            db.collection("users").document(userId)
                    .update("email", newEmail, "phone", newPhone)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updatePassword() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentPassword = currentPasswordEditText.getText().toString();
            String newPassword = newPasswordEditText.getText().toString();
            String confirmNewPassword = confirmNewPasswordEditText.getText().toString();

            if (!newPassword.equals(confirmNewPassword)) {
                Toast.makeText(this, "Las nuevas contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtén el correo del usuario
            String email = currentUser.getEmail();

            if (email != null) {
                // Crear un AuthCredential con el correo y la contraseña actual
                AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

                // Reautenticar al usuario
                currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        // Actualizar la contraseña
                        currentUser.updatePassword(newPassword).addOnCompleteListener(passwordTask -> {
                            if (passwordTask.isSuccessful()) {
                                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Error de autenticación: contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Error: No se pudo obtener el correo del usuario", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método para actualizar la visibilidad de los botones según el rol
    private void updateUIBasedOnRole(String position, ImageButton addButton, ImageButton ticketsButton) {
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
