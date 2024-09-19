package com.millanvanesa.leerlib_1;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddContentActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST_CATEGORY = 1;
    private static final int PICK_IMAGE_REQUEST_DOCUMENT = 2;
    private static final int PICK_PDF_REQUEST = 3;

    private EditText etCategoryName, etDocumentName, etDocumentDescription, etAuthor, etEditorial;
    private ImageView ivCategoryImage, ivDocumentImage, ivPdfLogo;
    private Button btnSelectCategoryImage, btnSelectDocumentImage, btnSelectDocumentPdf, btnSaveCategory, btnSaveDocument;
    private TextView usernameTextView, tvPdfName;
    private Spinner spinnerCategory;

    private Uri categoryImageUri, documentImageUri, documentPdfUri;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseUser currentUser;
    private List<String> categoryNames, categoryUids;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_content);

        // Recibir el uid del usuario de la actividad anterior
        userUid = getIntent().getStringExtra("userId");
        Log.d("AddContentActivity", "User UID: " + userUid);

        // Inicializar vistas
        initializeViews();

        // Inicializar Firebase
        initializeFirebase();

        // Configurar listeners
        setupListeners();

        if (userUid != null) {
            // Cargar el nombre de usuario
            loadUsername(userUid);
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        etCategoryName = findViewById(R.id.etCategoryName);
        etDocumentName = findViewById(R.id.etDocumentName);
        etDocumentDescription = findViewById(R.id.etDocumentDescription);
        etAuthor = findViewById(R.id.etAuthor); // Nuevo campo
        etEditorial = findViewById(R.id.etPublisher); // Nuevo campo
        ivCategoryImage = findViewById(R.id.ivCategoryImage);
        ivDocumentImage = findViewById(R.id.ivDocumentImage);
        btnSelectCategoryImage = findViewById(R.id.btnSelectCategoryImage);
        btnSelectDocumentImage = findViewById(R.id.btnSelectDocumentImage);
        btnSelectDocumentPdf = findViewById(R.id.btnSelectDocumentPDF);
        btnSaveCategory = findViewById(R.id.btnSaveCategory);
        btnSaveDocument = findViewById(R.id.btnSaveDocument);
        usernameTextView = findViewById(R.id.usernameTextView);
        tvPdfName = findViewById(R.id.tvPdfName);
        ivPdfLogo = findViewById(R.id.ivPdfLogo);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        categoryNames = new ArrayList<>();
        categoryUids = new ArrayList<>();
    }

    private void initializeFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            // Redirige al usuario a la pantalla de inicio de sesión
            Intent intent = new Intent(AddContentActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Opcional: cierra la actividad actual
            return;
        }

        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    private void setupListeners() {
        btnSelectCategoryImage.setOnClickListener(v -> openFileChooser(PICK_IMAGE_REQUEST_CATEGORY));
        btnSelectDocumentImage.setOnClickListener(v -> openFileChooser(PICK_IMAGE_REQUEST_DOCUMENT));
        btnSelectDocumentPdf.setOnClickListener(v -> openFileChooser(PICK_PDF_REQUEST));
        btnSaveCategory.setOnClickListener(v -> saveCategory());
        btnSaveDocument.setOnClickListener(v -> saveDocument());

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        final CardView categoryCardView = findViewById(R.id.categoryCardView);
        final CardView documentCardView = findViewById(R.id.documentCardView);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAddCategory) {
                categoryCardView.setVisibility(View.VISIBLE);
                documentCardView.setVisibility(View.GONE);
                spinnerCategory.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbAddDocument) {
                categoryCardView.setVisibility(View.GONE);
                documentCardView.setVisibility(View.VISIBLE);
                spinnerCategory.setVisibility(View.VISIBLE);
                loadCategoriesIntoSpinner();
            }
        });

        // Configurar botones de navegación
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton searchButton = findViewById(R.id.searchButton); // Agregar el ImageButton de búsqueda
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        profileButton.setOnClickListener(v -> navigateToUserProfile());
        addContentButton.setOnClickListener(v -> navigateToAddContent());
        favoritesButton.setOnClickListener(v -> navigateToFavorites());
        searchButton.setOnClickListener(v -> navigateToCategory()); // Agregar listener para búsqueda
        homeButton.setOnClickListener(v -> navigateToMain());

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(AddContentActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType(requestCode == PICK_PDF_REQUEST ? "application/pdf" : "image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST_CATEGORY) {
                categoryImageUri = uri;
                ivCategoryImage.setImageURI(categoryImageUri);
                ivCategoryImage.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_IMAGE_REQUEST_DOCUMENT) {
                documentImageUri = uri;
                ivDocumentImage.setImageURI(documentImageUri);
                ivDocumentImage.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_PDF_REQUEST) {
                documentPdfUri = uri;
                showPdfInfo(documentPdfUri);
            }
        }
    }

    private void showPdfInfo(Uri pdfUri) {
        String pdfName = getFileName(pdfUri);
        tvPdfName.setText(pdfName);
        tvPdfName.setVisibility(View.VISIBLE);
        ivPdfLogo.setVisibility(View.VISIBLE);
        ivPdfLogo.setImageResource(R.drawable.ic_pdf_logo); // Asegúrate de tener este recurso en tu drawable
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void saveCategory() {
        String categoryName = etCategoryName.getText().toString().trim();
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa un nombre de categoría", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryImageUri != null) {
            final StorageReference categoryImageRef = storageRef.child("categories/" + System.currentTimeMillis() + ".jpg");
            categoryImageRef.putFile(categoryImageUri).addOnSuccessListener(taskSnapshot -> categoryImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("name", categoryName);
                categoryData.put("image", imageUrl);
                db.collection("categories").add(categoryData)
                        .addOnSuccessListener(documentReference -> Toast.makeText(AddContentActivity.this, "Categoría guardada", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al guardar categoría", Toast.LENGTH_SHORT).show());
            })).addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Por favor selecciona una imagen para la categoría", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveDocument() {
        String documentName = etDocumentName.getText().toString().trim();
        String documentDescription = etDocumentDescription.getText().toString().trim();
        String author = etAuthor.getText().toString().trim(); // Obtener el autor
        String editorial = etEditorial.getText().toString().trim(); // Obtener la editorial

        if (documentName.isEmpty() || documentDescription.isEmpty() || documentPdfUri == null) {
            Toast.makeText(this, "Por favor completa todos los campos y selecciona un PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        if (documentImageUri != null) {
            final StorageReference documentImageRef = storageRef.child("documents/images/" + System.currentTimeMillis() + ".jpg");
            documentImageRef.putFile(documentImageUri).addOnSuccessListener(taskSnapshot -> documentImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                final StorageReference documentPdfRef = storageRef.child("documents/pdfs/" + System.currentTimeMillis() + ".pdf");
                documentPdfRef.putFile(documentPdfUri).addOnSuccessListener(taskSnapshot1 -> documentPdfRef.getDownloadUrl().addOnSuccessListener(uri1 -> {
                    String pdfUrl = uri1.toString();
                    Map<String, Object> documentData = new HashMap<>();
                    documentData.put("name", documentName);
                    documentData.put("description", documentDescription);
                    documentData.put("author", author); // Guardar autor
                    documentData.put("editorial", editorial); // Guardar editorial
                    documentData.put("image", imageUrl);
                    documentData.put("pdf", pdfUrl);
                    documentData.put("addedAt", Timestamp.now()); // Guardar fecha y hora actuales

                    db.collection("documents").add(documentData)
                            .addOnSuccessListener(documentReference -> Toast.makeText(AddContentActivity.this, "Documento guardado", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al guardar documento", Toast.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al subir PDF", Toast.LENGTH_SHORT).show()));
            }).addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show()));
        } else {
            Toast.makeText(this, "Por favor selecciona una imagen para el documento", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadUsername(String userUid) {
        db.collection("users").document(userUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                if (username != null) {
                    usernameTextView.setText(username);
                } else {
                    usernameTextView.setText("Usuario sin nombre");
                }
            } else {
                usernameTextView.setText("Usuario no encontrado");
            }
        }).addOnFailureListener(e -> usernameTextView.setText("Error al cargar el nombre"));
    }

    private void loadCategoriesIntoSpinner() {
        db.collection("categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            categoryNames.clear();
            categoryUids.clear();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                String name = document.getString("name");
                if (name != null) {
                    categoryNames.add(name);
                    categoryUids.add(document.getId());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(AddContentActivity.this, android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        }).addOnFailureListener(e -> Toast.makeText(AddContentActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show());
    }

    private void navigateToUserProfile() {
        Intent intent = new Intent(AddContentActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", userUid);
        startActivity(intent);
    }

    private void navigateToAddContent() {
        Intent intent = new Intent(AddContentActivity.this, AddContentActivity.class);
        intent.putExtra("userId", userUid);
        startActivity(intent);
    }

    private void navigateToFavorites() {
        Intent intent = new Intent(AddContentActivity.this, FavoritesActivity.class);
        intent.putExtra("userId", userUid);
        startActivity(intent);
    }

    private void navigateToCategory() {
        Intent intent = new Intent(AddContentActivity.this, CategoryActivity.class);
        intent.putExtra("userId", userUid);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(AddContentActivity.this, MainActivity.class);
        intent.putExtra("userId", userUid);
        startActivity(intent);
    }
}
