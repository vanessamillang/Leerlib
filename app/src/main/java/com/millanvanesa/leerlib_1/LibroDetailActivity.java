package com.millanvanesa.leerlib_1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibroDetailActivity extends AppCompatActivity {

    private static final String TAG = "LibroDetailActivity";

    private TextView libroNombreTextView;
    private TextView libroDescripcionTextView;
    private ImageView libroImageView;
    private ImageView userProfileImageView;
    private TextView userNameTextView;
    private ImageButton favoriteButton;
    private ImageButton profileButton;
    private String pdf;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String libroId;
    private String userId;
    private boolean isFavorite = false;

    private ImageView userCommentProfileImageView;
    private TextView userCommentNameTextView;
    private EditText commentEditText;
    private ImageButton sendCommentButton;

    private LinearLayout commentsList;

    private ImageButton star1, star2, star3, star4, star5;
    private int currentRating = 0; // Para almacenar la calificación actual

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libro_detail);

        // Inicializar Firestore y Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        libroNombreTextView = findViewById(R.id.libroNombreTextView);
        libroDescripcionTextView = findViewById(R.id.libroDescripcionTextView);
        libroImageView = findViewById(R.id.libroImageView);
        userProfileImageView = findViewById(R.id.userProfileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        favoriteButton = findViewById(R.id.favoriteButton);
        userCommentProfileImageView = findViewById(R.id.userCommentProfileImageView);
        userCommentNameTextView = findViewById(R.id.userCommentNameTextView);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);
        commentsList = findViewById(R.id.commentsList);

        libroId = getIntent().getStringExtra("libroId");
        userId = getIntent().getStringExtra("userId");

        // Cargar la imagen
        loadProfileImage(userId);
        loadUserInfo();
        fetchLibroDetails(libroId);
        loadComments();

        Button verPdfButton = findViewById(R.id.verPdfButton);
        verPdfButton.setOnClickListener(v -> openPdfViewerActivity());

        // PARA EL APARTADO DE RATING
        // Inicializar las estrellas
        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);

        // Establecer listeners para las estrellas
        star1.setOnClickListener(v -> setRating(1));
        star2.setOnClickListener(v -> setRating(2));
        star3.setOnClickListener(v -> setRating(3));
        star4.setOnClickListener(v -> setRating(4));
        star5.setOnClickListener(v -> setRating(5));

        // Mostrar rating del libro
        calculateAndDisplayAverageRating();
        getUserRating();

        profileButton = findViewById(R.id.profileButton); // Inicializar el ImageButton profileButton
        profileButton.setOnClickListener(v -> {
            if (userId != null) {
                Intent intent = new Intent(LibroDetailActivity.this, UserProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                Toast.makeText(LibroDetailActivity.this, "UID de usuario no disponible", Toast.LENGTH_SHORT).show();
            }
        });

        favoriteButton.setOnClickListener(v -> {
            if (isFavorite) {
                removeFavorite(libroId, currentUser.getUid());
            } else {
                addFavorite(libroId, currentUser.getUid());
            }
        });

        // Configurar los botones del pie de página
        ImageButton categoryButton = findViewById(R.id.searchButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton favoritesButton = findViewById(R.id.myLibraryButton);
        ImageButton addContentButton = findViewById(R.id.addContentButton);
        ImageButton ticketsButton = findViewById(R.id.ticketsButton);

        categoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibroDetailActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibroDetailActivity.this, MainActivity.class);
            startActivity(intent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibroDetailActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });

        addContentButton.setOnClickListener(v -> {
            Intent intent = new Intent(LibroDetailActivity.this, AddContentActivity.class);
            startActivity(intent);
        });

        sendCommentButton.setOnClickListener(v -> {
            String commentText = commentEditText.getText().toString().trim();
            if (!commentText.isEmpty()) {
                sendComment(commentText);
            } else {
                Toast.makeText(LibroDetailActivity.this, "Escribe un comentario", Toast.LENGTH_SHORT).show();
            }
        });

        ticketsButton.setOnClickListener(v -> {
            Intent ticketsIntent = new Intent(LibroDetailActivity.this, TicketSoporteActivity.class);
            startActivity(ticketsIntent);
        });

        // Configurar la visibilidad de los botones según el rol del usuario
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    String position = document.getString("position");

                    // Actualizar UI según el rol del usuario
                    updateUIBasedOnRole(position, addContentButton, ticketsButton);
                }
            });
        }
    }

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

    @Override
    protected void onResume() {
        super.onResume();

        // Llamar a calculateAndDisplayAverageRating() en onResume para actualizar el rating
        calculateAndDisplayAverageRating();
        // Llamar a getUserRating() para mostrar la calificación del usuario
        getUserRating();
    }
    //Metodo para mostrar el rating de un libro
    private void calculateAndDisplayAverageRating() {
        if (libroId != null) {
            db.collection("ratings")
                    .whereEqualTo("uidLibro", libroId)
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Error al obtener calificaciones.", e);
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            int totalRatings = 0;
                            int count = 0;

                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Integer rating = document.getLong("rating").intValue();
                                if (rating != null) {
                                    totalRatings += rating;
                                    count++;
                                }
                            }

                            if (count > 0) {
                                double averageRating = (double) totalRatings / count;
                                updateStarRating(averageRating);
                            } else {
                                // No hay calificaciones, puedes manejar esto si es necesario
                                updateStarRating(0);
                            }
                        }
                    });
        }
    }
    private void updateStarRating(double averageRating) {
        ImageButton[] stars = {
                findViewById(R.id.Star_1),
                findViewById(R.id.Star_2),
                findViewById(R.id.Star_3),
                findViewById(R.id.Star_4),
                findViewById(R.id.Star_5)
        };

        int filledStars = (int) Math.floor(averageRating);
        double fractionalPart = averageRating - filledStars;

        for (int i = 0; i < stars.length; i++) {
            if (i < filledStars) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else if (i == filledStars && fractionalPart > 0) {
                stars[i].setImageResource(R.drawable.ic_star_half);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline);
            }
        }
    }
    // Método para actualizar la interfaz de usuario según la calificación
    private void setRating(int rating) {
        currentRating = rating;

        // Restablecer todas las estrellas a la imagen desmarcada
        star1.setImageResource(R.drawable.ic_star_outline);
        star2.setImageResource(R.drawable.ic_star_outline);
        star3.setImageResource(R.drawable.ic_star_outline);
        star4.setImageResource(R.drawable.ic_star_outline);
        star5.setImageResource(R.drawable.ic_star_outline);

        // Marcar las estrellas seleccionadas
        if (rating >= 1) star1.setImageResource(R.drawable.ic_star_filled);
        if (rating >= 2) star2.setImageResource(R.drawable.ic_star_filled);
        if (rating >= 3) star3.setImageResource(R.drawable.ic_star_filled);
        if (rating >= 4) star4.setImageResource(R.drawable.ic_star_filled);
        if (rating >= 5) star5.setImageResource(R.drawable.ic_star_filled);

        // Guardar la calificación en Firestore
        saveRatingToFirestore();
        calculateAndDisplayAverageRating(); // Actualizar las estrellas

    }
    // Método para cargar la calificacion del usuario
    private void getUserRating() {
        if (libroId != null && userId != null) {
            db.collection("ratings")
                    .whereEqualTo("uidLibro", libroId)
                    .whereEqualTo("uidUser", userId)

                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                // Obtener el primer documento de la consulta
                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                Integer userRating = document.getLong("rating").intValue();
                                if (userRating != null) {
                                    updateUserRating(userRating);
                                }
                            }
                        } else {
                            Log.w(TAG, "Error al obtener la calificación del usuario.", task.getException());
                        }
                    });
        }
    }
    private void updateUserRating(int userRating) {
        ImageButton[] stars = {
                findViewById(R.id.star1),
                findViewById(R.id.star2),
                findViewById(R.id.star3),
                findViewById(R.id.star4),
                findViewById(R.id.star5)
        };

        for (int i = 0; i < stars.length; i++) {
            if (i < userRating) {
                stars[i].setImageResource(R.drawable.ic_star_filled);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline);
            }
        }
    }


    // Método para guardar la calificación en Firestore
    private void saveRatingToFirestore() {
        if (currentUser != null && libroId != null) {
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("uidLibro", libroId);
            ratingData.put("uidUser", currentUser.getUid());
            ratingData.put("rating", currentRating);
            ratingData.put("timestamp", System.currentTimeMillis());

            // Guardar o actualizar la calificación del usuario para este libro
            db.collection("ratings")
                    .whereEqualTo("uidLibro", libroId)
                    .whereEqualTo("uidUser", currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Si ya existe una calificación, actualizarla
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            db.collection("ratings").document(document.getId())
                                    .update(ratingData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Rating actualizado"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar rating", e));
                        } else {
                            // Si no existe, crear un nuevo documento
                            db.collection("ratings")
                                    .add(ratingData)
                                    .addOnSuccessListener(documentReference -> Log.d(TAG, "Rating guardado"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error al guardar rating", e));
                        }
                    });
        } else {
            Log.e(TAG, "Usuario o ID del libro no disponible");
        }
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
                                Glide.with(LibroDetailActivity.this)
                                        .load(profileImageUrl)
                                        .transform(new CircleCrop())
                                        .into(profileButton);
                            } else {
                                profileButton.setImageResource(R.drawable.perfil);
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "Get failed with ", task.getException());
                    }
                });
    }

// PARA MOSTRAR LA INFORMACION DEL USUSARIO EN LA SECCIÓN COMENTARIOS
    private void loadUserInfo() {
        Log.d(TAG, "Cargando información del usuario");
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        Log.d(TAG, "Documento obtenido: " + document.getData());
                        String username = document.getString("username");
                        String userImageUrl = document.getString("imagen");

                        if (username != null) {
                            userCommentNameTextView.setText(username); // Mostrar nombre
                            Log.d(TAG, "Nombre de usuario cargado: " + username);
                        } else {
                            userCommentNameTextView.setText("Usuario desconocido");
                            Log.d(TAG, "El campo username está vacío");
                        }

                        if (userImageUrl != null && !userImageUrl.isEmpty()) {
                            Picasso.get().load(userImageUrl).placeholder(R.drawable.perfil).into(userCommentProfileImageView);
                        } else {
                            userCommentProfileImageView.setImageResource(R.drawable.perfil);
                        }
                    } else {
                        Log.d(TAG, "Error obteniendo el documento: ", task.getException());
                        userCommentNameTextView.setText("Error al cargar usuario");
                        userCommentProfileImageView.setImageResource(R.drawable.perfil);
                    }
                });
    }

    private void fetchLibroDetails(String libroId) {
        db.collection("libros").document(libroId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String nombre = document.getString("name");
                            String descripcion = document.getString("description");
                            String imagenUrl = document.getString("image");
                            String uidUser = document.getString("uidUser");
                            pdf = document.getString("pdf");

                            libroNombreTextView.setText(nombre);
                            libroDescripcionTextView.setText(descripcion);

                            if (imagenUrl != null && !imagenUrl.isEmpty()) {
                                Picasso.get().load(imagenUrl).into(libroImageView);
                            }

                            if (uidUser != null) {
                                fetchUserDetails(uidUser);
                            }

                            if (currentUser != null) {
                                checkIfFavorite(libroId, currentUser.getUid());
                            }
                        } else {
                            Log.d(TAG, "No se encontró el documento");
                        }
                    } else {
                        Log.d(TAG, "Error al obtener el documento", task.getException());
                    }
                });
    }

    private void fetchUserDetails(String uidUser) {
        db.collection("users").document(uidUser)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String username = document.getString("username");
                            String imageUrl = document.getString("imagen");

                            if (username != null && !username.isEmpty()) {
                                userNameTextView.setText(username);
                            } else {
                                userNameTextView.setText("Usuario desconocido");
                            }

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(imageUrl)
                                        .placeholder(R.drawable.perfil)
                                        .into(userProfileImageView);
                            } else {
                                userProfileImageView.setImageResource(R.drawable.perfil);
                            }
                        } else {
                            userNameTextView.setText("Usuario desconocido");
                            userProfileImageView.setImageResource(R.drawable.perfil);
                        }
                    } else {
                        Log.d(TAG, "Error al obtener los detalles del usuario", task.getException());
                        userNameTextView.setText("Error al cargar usuario");
                        userProfileImageView.setImageResource(R.drawable.perfil);
                    }
                });
    }

    private void checkIfFavorite(String libroId, String userId) {
        db.collection("favoritos")
                .whereEqualTo("uidUser", userId)
                .whereEqualTo("uidDoc", libroId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isFavorite = !task.getResult().isEmpty();
                        // Actualiza el estado del botón de favorito
                        favoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
                    } else {
                        Log.d(TAG, "Error al verificar favoritos", task.getException());
                    }
                });
    }

    private void addFavorite(String libroId, String userId) {
        Map<String, Object> favorite = new HashMap<>();
        favorite.put("uidUser", userId);
        favorite.put("uidDoc", libroId);

        db.collection("favoritos").add(favorite)
                .addOnSuccessListener(documentReference -> {
                    isFavorite = true;
                    favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                    Toast.makeText(LibroDetailActivity.this, "Agregado a favoritos", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Error al agregar a favoritos", e);
                    Toast.makeText(LibroDetailActivity.this, "Error al agregar a favoritos", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFavorite(String libroId, String userId) {
        db.collection("favoritos")
                .whereEqualTo("uidUser", userId)
                .whereEqualTo("uidDoc", libroId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            db.collection("favoritos").document(document.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        isFavorite = false;
                                        favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                        Toast.makeText(LibroDetailActivity.this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d(TAG, "Error al eliminar de favoritos", e);
                                        Toast.makeText(LibroDetailActivity.this, "Error al eliminar de favoritos", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.d(TAG, "Error al verificar favoritos", task.getException());
                    }
                });
    }

    private void openPdfViewerActivity() {
        if (pdf != null && !pdf.isEmpty()) {
            Intent intent = new Intent(LibroDetailActivity.this, PdfViewerActivity.class);
            intent.putExtra("pdfUrl", pdf);
            intent.putExtra("bookId", libroId);  // Agregado
            startActivity(intent);
        } else {
            Toast.makeText(LibroDetailActivity.this, "URL del PDF no disponible", Toast.LENGTH_SHORT).show();
        }
    }


    private void sendComment(String commentText) {
        if (currentUser != null) {
            Map<String, Object> comment = new HashMap<>();
            comment.put("comentario", commentText);
            comment.put("uidUser", currentUser.getUid());
            comment.put("uidLibro", libroId);

            db.collection("comentarios").add(comment)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(LibroDetailActivity.this, "Comentario enviado", Toast.LENGTH_SHORT).show();
                        commentEditText.setText("");
                        loadComments(); // Recargar comentarios para actualizar la vista
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Error al enviar comentario", e);
                        Toast.makeText(LibroDetailActivity.this, "Error al enviar comentario", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(LibroDetailActivity.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadComments() {
        db.collection("comentarios")
                .whereEqualTo("uidLibro", libroId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        commentsList.removeAllViews(); // Limpiar comentarios existentes
                        boolean hasComments = false; // Bandera para verificar si hay comentarios

                        for (DocumentSnapshot document : task.getResult()) {
                            hasComments = true; // Hay al menos un comentario
                            String comentario = document.getString("comentario");
                            String uidUser = document.getString("uidUser");

                            if (uidUser != null) {
                                db.collection("users").document(uidUser).get()
                                        .addOnCompleteListener(userTask -> {
                                            if (userTask.isSuccessful()) {
                                                DocumentSnapshot userDocument = userTask.getResult();
                                                String username = userDocument.getString("username");
                                                String userImageUrl = userDocument.getString("imagen");

                                                View commentView = getLayoutInflater().inflate(R.layout.comment_list_item, commentsList, false);
                                                TextView commentNameTextView = commentView.findViewById(R.id.commentNameTextView);
                                                ImageView commentProfileImageView = commentView.findViewById(R.id.commentProfileImageView);
                                                TextView commentTextView = commentView.findViewById(R.id.commentTextView);

                                                commentNameTextView.setText(username != null ? username : "Usuario desconocido");
                                                commentTextView.setText(comentario);

                                                if (userImageUrl != null && !userImageUrl.isEmpty()) {
                                                    Picasso.get().load(userImageUrl).placeholder(R.drawable.perfil).into(commentProfileImageView);
                                                } else {
                                                    commentProfileImageView.setImageResource(R.drawable.perfil);
                                                }

                                                commentsList.addView(commentView);

                                                // Mostrar la sección de comentarios después de agregar la vista
                                                commentsList.setVisibility(View.VISIBLE);
                                            }
                                        });
                            }
                        }

                        // Si no hay comentarios, ocultar la sección de comentarios
                        if (!hasComments) {
                            commentsList.setVisibility(View.GONE);
                        }
                    } else {
                        Log.d(TAG, "Error al cargar comentarios", task.getException());
                        commentsList.setVisibility(View.GONE);
                    }
                });
    }

}
