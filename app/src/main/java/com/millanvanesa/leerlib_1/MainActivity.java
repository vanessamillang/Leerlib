package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ImageButton profileButton;
    private RecyclerView topRatedRecyclerView;
    private RecyclerView favoritesAuthorsRecyclerView;
    private RecyclerView categoriesRecyclerView;
    private RecyclerView continueReadingRecyclerView;
    private RecyclerView newestBooksRecyclerView;
    private RatingAdapter topRatedAdapter;
    private RatingAdapter favoritesAuthorsAdapter;
    private RatingAdapter categoryAdapter;
    private RatingAdapter continueReadingAdapter;
    private RatingAdapter newestBooksAdapter;
    private List<RatingItem> topRatedList;
    private List<RatingItem> favoritesAuthorsList;
    private List<RatingItem> categoryList;
    private List<RatingItem> continueReadingList;
    private List<RatingItem> newestBooksList;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileButton = findViewById(R.id.profileButton);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            loadProfileImage(currentUser.getUid());
        } else {
            Log.w(TAG, "Usuario no autenticado.");
        }

        // Configurar RecyclerView para recomendaciones por rating
        topRatedRecyclerView = findViewById(R.id.topRatedRecyclerView);
        topRatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        topRatedList = new ArrayList<>();
        topRatedAdapter = new RatingAdapter(this, topRatedList, currentUser != null ? currentUser.getUid() : "");
        topRatedRecyclerView.setAdapter(topRatedAdapter);

        // Configurar RecyclerView para recomendaciones por autores
        favoritesAuthorsRecyclerView = findViewById(R.id.favoritesAuthorsRecyclerView);
        favoritesAuthorsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        favoritesAuthorsList = new ArrayList<>();
        favoritesAuthorsAdapter = new RatingAdapter(this, favoritesAuthorsList, currentUser != null ? currentUser.getUid() : "");
        favoritesAuthorsRecyclerView.setAdapter(favoritesAuthorsAdapter);

        // Configurar RecyclerView para recomendaciones por categorías
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryList = new ArrayList<>();
        categoryAdapter = new RatingAdapter(this, categoryList, currentUser != null ? currentUser.getUid() : "");
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Configurar RecyclerView para 'Seguir Leyendo'
        continueReadingRecyclerView = findViewById(R.id.continueReadingRecyclerView);
        continueReadingRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        continueReadingList = new ArrayList<>();
        continueReadingAdapter = new RatingAdapter(this, continueReadingList, currentUser != null ? currentUser.getUid() : "");
        continueReadingRecyclerView.setAdapter(continueReadingAdapter);

        // Configurar RecyclerView para los libros más nuevos
        newestBooksRecyclerView = findViewById(R.id.newestBooksRecyclerView);
        newestBooksRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        newestBooksList = new ArrayList<>();
        newestBooksAdapter = new RatingAdapter(this, newestBooksList, currentUser != null ? currentUser.getUid() : "");
        newestBooksRecyclerView.setAdapter(newestBooksAdapter);

        fetchTopRatedBooks();
        fetchBooksByAuthors();
        fetchBooksByCategories();
        fetchBooksInProgress();
        fetchNewestBooks();
        setupFooterButtons();
    }

    private void fetchTopRatedBooks() {
        db.collection("ratings")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> bookIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String uidLibro = document.getString("uidLibro");
                            if (uidLibro != null) {
                                bookIds.add(uidLibro);
                            }
                        }
                        fetchBookDetails(bookIds, true); // 'true' para recomendaciones por rating
                    } else {
                        Log.w(TAG, "Error obteniendo documentos de ratings.", task.getException());
                    }
                });
    }

    private void fetchBookDetails(List<String> bookIds, boolean isForRating) {
        if (bookIds.isEmpty()) {
            Log.d(TAG, "No hay IDs de libros para obtener detalles.");
            return; // Evita hacer una solicitud con una lista vacía
        }

        db.collection("libros")
                .whereIn("uid", bookIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (isForRating) {
                            topRatedList.clear(); // Limpia la lista antes de agregar nuevos elementos
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            String name = document.getString("name");
                            String imageUrl = document.getString("image");

                            if (name != null && imageUrl != null) {
                                if (isForRating) {
                                    topRatedList.add(new RatingItem(name, imageUrl, bookId, false, false, false,false));                                }
                            }
                        }

                        if (isForRating) {
                            Log.d(TAG, "Top rated books list: " + topRatedList);
                            topRatedAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo los detalles de los libros.", task.getException());
                    }
                });
    }

    private void fetchBooksByAuthors() {
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado.");
            return;
        }

        db.collection("userBooks")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", "read")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> bookIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getString("bookId");
                            if (bookId != null) {
                                bookIds.add(bookId);
                            }
                        }
                        Log.d(TAG, "Book IDs for read books: " + bookIds);
                        if (!bookIds.isEmpty()) {
                            fetchAuthorsFromBooks(bookIds);
                        } else {
                            Log.d(TAG, "No se encontraron libros leídos por el usuario.");
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo los libros leídos.", task.getException());
                    }
                });
    }

    private void fetchAuthorsFromBooks(List<String> readBookIds) {
        db.collection("libros")
                .whereIn("uid", readBookIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> authorsList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String author = document.getString("author");
                            if (author != null && !authorsList.contains(author)) {
                                authorsList.add(author);
                            }
                        }
                        Log.d(TAG, "Authors list: " + authorsList);
                        if (!authorsList.isEmpty()) {
                            fetchBooksByAuthorList(authorsList, readBookIds);
                        } else {
                            Log.d(TAG, "No se encontraron autores basados en los libros leídos por el usuario.");
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo los autores de los libros.", task.getException());
                    }
                });
    }

    private void fetchBooksByAuthorList(List<String> authorsList, List<String> readBookIds) {
        db.collection("libros")
                .whereIn("author", authorsList)
                .limit(15)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoritesAuthorsList.clear(); // Asegúrate de que la lista esté vacía antes de agregar nuevos elementos
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            if (!readBookIds.contains(bookId)) { // Excluir los libros ya leídos
                                String name = document.getString("name");
                                String imageUrl = document.getString("image");
                                favoritesAuthorsList.add(new RatingItem(name, imageUrl, bookId, true, false, false,false));  // 'true' para recomendaciones basadas en autores
                            }
                        }
                        Log.d(TAG, "Favorites authors list: " + favoritesAuthorsList);
                        favoritesAuthorsAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error obteniendo los libros por autores.", task.getException());
                    }
                });
    }

    private void fetchBooksByCategories() {
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado.");
            return;
        }

        db.collection("userBooks")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", "read")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> bookIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getString("bookId");
                            if (bookId != null) {
                                bookIds.add(bookId);
                            }
                        }
                        Log.d(TAG, "Book IDs for read books: " + bookIds);
                        if (!bookIds.isEmpty()) {
                            fetchCategoriesFromBooks(bookIds);
                        } else {
                            Log.d(TAG, "No se encontraron libros leídos por el usuario.");
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo los libros leídos.", task.getException());
                    }
                });
    }

    private void fetchCategoriesFromBooks(List<String> readBookIds) {
        db.collection("libros")
                .whereIn("uid", readBookIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> categoriesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String category = document.getString("category");
                            if (category != null && !categoriesList.contains(category)) {
                                categoriesList.add(category);
                            }
                        }
                        Log.d(TAG, "Categories list: " + categoriesList);
                        if (!categoriesList.isEmpty()) {
                            fetchBooksByCategoryList(categoriesList, readBookIds);
                        } else {
                            Log.d(TAG, "No se encontraron categorías basadas en los libros leídos por el usuario.");
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo las categorías de los libros.", task.getException());
                    }
                });
    }

    private void fetchBooksByCategoryList(List<String> categoriesList, List<String> readBookIds) {
        db.collection("libros")
                .whereIn("category", categoriesList)
                .limit(15)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoryList.clear(); // Asegúrate de que la lista esté vacía antes de agregar nuevos elementos
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            if (!readBookIds.contains(bookId)) { // Excluir los libros ya leídos
                                String name = document.getString("name");
                                String imageUrl = document.getString("image");
                                categoryList.add(new RatingItem(name, imageUrl, bookId, false, true,false,false));  // 'true' para recomendaciones basadas en categorías
                            }
                        }
                        Log.d(TAG, "Categories books list: " + categoryList);
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error obteniendo los libros por categorías.", task.getException());
                    }
                });
    }

    private void fetchBooksInProgress() {
        if (currentUser == null) {
            Log.w(TAG, "Usuario no autenticado.");
            return;
        }

        db.collection("userBooks")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", "inProgress")
                .limit(15)  // Limitar a 15 resultados
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> bookIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getString("bookId");
                            if (bookId != null) {
                                bookIds.add(bookId);
                            }
                        }
                        Log.d(TAG, "Book IDs in progress: " + bookIds);
                        if (!bookIds.isEmpty()) {
                            fetchBookDetailsForInProgress(bookIds);
                        } else {
                            Log.d(TAG, "No se encontraron libros en progreso para el usuario.");
                        }
                    } else {
                        Log.w(TAG, "Error obteniendo los libros en progreso.", task.getException());
                    }
                });
    }


    private void fetchBookDetailsForInProgress(List<String> bookIds) {
        if (bookIds.isEmpty()) {
            Log.d(TAG, "No hay IDs de libros para obtener detalles en progreso.");
            return;
        }

        db.collection("libros")
                .whereIn("uid", bookIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        continueReadingList.clear(); // Limpia la lista antes de agregar nuevos elementos
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            String name = document.getString("name");
                            String imageUrl = document.getString("image");

                            Log.d(TAG, "In-progress book details: ID=" + bookId + ", Name=" + name + ", Image URL=" + imageUrl);

                            if (name != null && imageUrl != null) {
                                continueReadingList.add(new RatingItem(name, imageUrl, bookId, false, false, true,false));
                            }
                        }
                        Log.d(TAG, "In progress books list: " + continueReadingList);
                        continueReadingAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error obteniendo los detalles de los libros en progreso.", task.getException());
                    }
                });
    }

    private void fetchNewestBooks() {
        db.collection("libros")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        newestBooksList.clear(); // Limpia la lista antes de agregar nuevos elementos
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String bookId = document.getId();
                            String name = document.getString("name");
                            String imageUrl = document.getString("image");

                            if (name != null && imageUrl != null) {
                                newestBooksList.add(new RatingItem(name, imageUrl, bookId, false, false, false,true)); // Ajusta los valores booleanos según sea necesario
                            }
                        }

                        Log.d(TAG, "Newest books list: " + newestBooksList);
                        newestBooksAdapter.notifyDataSetChanged(); // Actualiza el adaptador
                    } else {
                        Log.w(TAG, "Error obteniendo los libros más nuevos.", task.getException());
                    }
                });
    }


    private void loadProfileImage(String userId) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String imageUrl = document.getString("imagen");
                                if (imageUrl != null) {
                                    Glide.with(MainActivity.this)
                                            .load(imageUrl)
                                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                            .into(profileButton);
                                } else {
                                    Log.d(TAG, "No se encontró una imagen de perfil para el usuario.");
                                }
                            } else {
                                Log.d(TAG, "No se encontró un documento para el usuario.");
                            }
                        } else {
                            Log.w(TAG, "Error obteniendo el documento del usuario.", task.getException());
                        }
                    }
                });
    }

    private void setupFooterButtons() {
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addButton = findViewById(R.id.addContentButton);
        ImageButton favoritesButton = findViewById(R.id.favoritesButton);
        ImageButton userProfileButton = findViewById(R.id.profileButton);

        homeButton.setOnClickListener(v -> {
            // Acción para botón Home
            startActivity(new Intent(MainActivity.this, MainActivity.class));
        });

        searchButton.setOnClickListener(v -> {
            // Acción para botón Search
            startActivity(new Intent(MainActivity.this, CategoryActivity.class));
        });

        addButton.setOnClickListener(v -> {
            // Acción para botón Add
            startActivity(new Intent(MainActivity.this, AddContentActivity.class));
        });

        favoritesButton.setOnClickListener(v -> {
            // Acción para botón Favorites
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        });

        userProfileButton.setOnClickListener(v -> {
            // Acción para botón Profile
            startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
        });
    }
}
