package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private static final String TAG = "CategoryActivity";

    private ImageButton profileButton;
    private ImageButton addContentButton;
    private ImageButton favoritesButton;
    private ImageButton homeButton;
    private ImageButton ticketsButton; // Nuevo botón para Tickets
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private BookAdapter bookAdapter; // Adaptador para libros
    private List<Category> categoryList, filteredCategoryList;
    private List<Book> bookList, filteredBookList; // Listas para libros
    private SearchView searchView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileButton = findViewById(R.id.profileButton);
        addContentButton = findViewById(R.id.addContentButton);
        favoritesButton = findViewById(R.id.myLibraryButton);
        homeButton = findViewById(R.id.homeButton);
        ticketsButton = findViewById(R.id.ticketsButton); // Inicializar el nuevo botón
        recyclerView = findViewById(R.id.categoryRecyclerView);
        searchView = findViewById(R.id.searchView);

        // Configurar RecyclerView y adaptadores
        categoryList = new ArrayList<>();
        filteredCategoryList = new ArrayList<>();
        bookList = new ArrayList<>();
        filteredBookList = new ArrayList<>();

        categoryAdapter = new CategoryAdapter(this, filteredCategoryList);
        bookAdapter = new BookAdapter(this, filteredBookList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(categoryAdapter); // Inicialmente muestra categorías

        // Obtener y mostrar las categorías y libros desde Firestore
        fetchCategories();
        fetchBooks();

        // Configurar el clic en un elemento del RecyclerView
        categoryAdapter.setOnItemClickListener(position -> {
            Category category = filteredCategoryList.get(position);
            String categoryId = category.getUid();
            String categoryName = category.getNombre();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                Intent intent = new Intent(CategoryActivity.this, LibrosActivity.class);
                intent.putExtra("categoryId", categoryId);
                intent.putExtra("categoryName", categoryName);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        bookAdapter.setOnItemClickListener(position -> {
            Book book = filteredBookList.get(position);
            String bookId = book.getUid(); // Asegúrate de que `getUid()` devuelve el ID correcto del libro

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                Intent intent = new Intent(CategoryActivity.this, LibroDetailActivity.class);
                intent.putExtra("libroId", bookId); // Cambia "bookId" a "libroId"
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        // Configurar SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterContent(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterContent(newText);
                return false;
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Usuario actual: " + userId);
            setupProfileButton(R.id.profileButton, userId);
            loadProfileImage(userId);
            updateUIBasedOnRole(userId); // Obtener el rol del usuario
        } else {
            Intent intent = new Intent(CategoryActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        // Configurar los botones de navegación
        profileButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(CategoryActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", user.getUid());
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(CategoryActivity.this, MainActivity.class);
                intent.putExtra("userId", user.getUid());
                Log.d(TAG, "Iniciando MainActivity con UID: " + user.getUid());
                startActivity(intent);
            } else {
                Log.e(TAG, "Intento de iniciar MainActivity sin usuario autenticado");
            }
        });

        addContentButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(CategoryActivity.this, AddContentActivity.class);
                intent.putExtra("userId", user.getUid());
                Log.d(TAG, "Iniciando AddContentActivity con UID: " + user.getUid());
                startActivity(intent);
            } else {
                Log.e(TAG, "Intento de iniciar AddContentActivity sin usuario autenticado");
            }
        });

        favoritesButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(CategoryActivity.this, FavoritesActivity.class);
                intent.putExtra("userId", user.getUid());
                startActivity(intent);
            }
        });

        // Configurar el botón de Tickets
        ticketsButton.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(CategoryActivity.this, TicketSoporteActivity.class);
                intent.putExtra("userId", user.getUid());
                startActivity(intent);
            } else {
                Log.e(TAG, "Intento de iniciar TicketSoporteActivity sin usuario autenticado");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchCategories();
        fetchBooks();
    }

    private void fetchCategories() {
        db.collection("categorias")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoryList.clear();
                        filteredCategoryList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String nombre = document.getString("name");
                            String imagenUrl = document.getString("image");
                            String uid = document.getId();
                            Log.d(TAG, "Categoría obtenida: " + nombre + ", " + imagenUrl);
                            Category category = new Category(uid, nombre, imagenUrl);
                            categoryList.add(category);
                        }
                        filteredCategoryList.addAll(categoryList);
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(CategoryActivity.this, "Error al obtener categorías", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchBooks() {
        db.collection("libros")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        bookList.clear();
                        filteredBookList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String nombre = document.getString("name");
                            String imagenUrl = document.getString("image");
                            String uid = document.getId();
                            Log.d(TAG, "Libro obtenido: " + nombre + ", " + imagenUrl);
                            Book book = new Book(uid, nombre, imagenUrl);
                            bookList.add(book);
                        }
                        filteredBookList.addAll(bookList);
                        bookAdapter.notifyDataSetChanged();
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(CategoryActivity.this, "Error al obtener libros", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterContent(String text) {
        filteredCategoryList.clear();
        filteredBookList.clear();
        if (text.isEmpty()) {
            filteredCategoryList.addAll(categoryList);
            filteredBookList.addAll(bookList);
        } else {
            text = text.toLowerCase();
            for (Category category : categoryList) {
                if (category.getNombre().toLowerCase().contains(text)) {
                    filteredCategoryList.add(category);
                }
            }
            for (Book book : bookList) {
                if (book.getNombre().toLowerCase().contains(text)) {
                    filteredBookList.add(book);
                }
            }
        }
        // Actualizar el adaptador del RecyclerView
        if (!filteredCategoryList.isEmpty()) {
            recyclerView.setAdapter(categoryAdapter);
        } else if (!filteredBookList.isEmpty()) {
            recyclerView.setAdapter(bookAdapter);
        }
        categoryAdapter.notifyDataSetChanged();
        bookAdapter.notifyDataSetChanged();
    }

    private void loadProfileImage(String uid) {
        Log.d(TAG, "Cargando imagen de perfil para UID: " + uid);
        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        Log.d(TAG, "Documento obtenido: " + document.getData());
                        String profileImageUrl = document.getString("imagen");

                        if (profileButton != null) {
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(CategoryActivity.this)
                                        .load(profileImageUrl)
                                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                        .into(profileButton);
                            } else {
                                // Usa drawable por defecto si no hay URL
                                profileButton.setImageResource(R.drawable.perfil);
                            }
                        } else {
                            Log.d(TAG, "El ImageButton para la imagen de perfil es null. Verifica el ID en el archivo XML.");
                        }
                    } else {
                        Toast.makeText(CategoryActivity.this, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Error obteniendo el documento: ", task.getException());
                    }
                });
    }

    private void setupProfileButton(int buttonId, String userId) {
        ImageButton profileButton = findViewById(buttonId);
        profileButton.setOnClickListener(view -> {
            Intent intent = new Intent(CategoryActivity.this, UserProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    // Método para actualizar la visibilidad de los botones según el rol
    private void updateUIBasedOnRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String position = document.getString("position"); // Obtener el rol del usuario

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
                    } else {
                        Log.d(TAG, "Error obteniendo el rol del usuario: ", task.getException());
                    }
                });
    }
}
