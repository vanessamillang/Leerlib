package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayList<String> documentNames;
    private ArrayList<String> documentIds;
    private ArrayAdapter<String> adapter;
    private ImageView profileImageButton;
    private ImageButton searchButton;
    private ImageButton homeButton;

    private ListView documentosListView;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        documentosListView = findViewById(R.id.documentosListView);
        profileImageButton = findViewById(R.id.profileButton);
        searchButton = findViewById(R.id.searchButton);
        homeButton = findViewById(R.id.homeButton);
        documentNames = new ArrayList<>();
        documentIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, documentNames);
        documentosListView.setAdapter(adapter);

        if (user != null) {
            uid = user.getUid();
            loadProfileImage(uid);
            loadFavoriteDocuments(uid);
            setupFooterNavigation();
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }

        documentosListView.setOnItemClickListener((parent, view, position, id) -> {
            String libroId = documentIds.get(position);
            Intent intent = new Intent(FavoritesActivity.this, LibroDetailActivity.class);
            intent.putExtra("libroId", libroId);
            intent.putExtra("userId", uid);
            startActivity(intent);
        });
    }

    private void loadFavoriteDocuments(String uid) {
        db.collection("favoritos")
                .whereEqualTo("uidUser", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        documentNames.clear();
                        documentIds.clear();
                        List<String> pendingDocuments = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String uidDoc = document.getString("uidDoc");
                            if (uidDoc != null) {
                                pendingDocuments.add(uidDoc);
                            }
                        }

                        if (!pendingDocuments.isEmpty()) {
                            for (String docId : pendingDocuments) {
                                db.collection("libros").document(docId).get()
                                        .addOnCompleteListener(docTask -> {
                                            if (docTask.isSuccessful()) {
                                                DocumentSnapshot docSnapshot = docTask.getResult();
                                                if (docSnapshot.exists()) {
                                                    String documentName = docSnapshot.getString("name");
                                                    documentIds.add(docId);
                                                    documentNames.add(documentName);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                Log.d(TAG, "Failed to get document details: ", docTask.getException());
                                            }
                                        });
                            }
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private void loadProfileImage(String userId) {
        Log.d(TAG, "Loading profile image for UID: " + userId);
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String profileImageUrl = document.getString("imagen");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(FavoritesActivity.this)
                                        .load(profileImageUrl)
                                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                        .into(profileImageButton);
                            } else {
                                profileImageButton.setImageResource(R.drawable.perfil); // Reemplaza con un drawable por defecto si es necesario
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "Get failed with ", task.getException());
                    }
                });
    }

    private void setupFooterNavigation() {
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        ImageButton myLibraryButton = findViewById(R.id.myLibraryButton);
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        addContentButton.setOnClickListener(v -> {
            if (uid != null) {
                Intent intent = new Intent(FavoritesActivity.this, AddContentActivity.class);
                intent.putExtra("userId", uid);
                startActivity(intent);
            }
        });

        myLibraryButton.setOnClickListener(v -> {
            if (uid != null) {
                Intent intent = new Intent(FavoritesActivity.this, FavoritesActivity.class);
                intent.putExtra("userId", uid);
                startActivity(intent);
            }
        });

        profileButton.setOnClickListener(v -> {
            if (uid != null) {
                Intent intent = new Intent(FavoritesActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", uid);
                startActivity(intent);
            }
        });

        searchButton.setOnClickListener(v -> {
            if (uid != null) {
                Intent intent = new Intent(FavoritesActivity.this, CategoryActivity.class);
                intent.putExtra("userId", uid);
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(FavoritesActivity.this, MainActivity.class);
            if (uid != null) {
                intent.putExtra("userId", uid);
            }
            startActivity(intent);
        });

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
