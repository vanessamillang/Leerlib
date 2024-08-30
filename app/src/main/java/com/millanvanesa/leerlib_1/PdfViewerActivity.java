package com.millanvanesa.leerlib_1;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private String pdfUrl;
    private String bookId;
    private static final String TAG = "PdfViewerActivity";
    private static final String PREFS_NAME = "PdfPrefs";
    private static final String KEY_PAGE_NUMBER = "page_number";
    private int savedPageNumber = 0;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        // Obtener la URL del PDF y el ID del libro desde Intent
        pdfUrl = getIntent().getStringExtra("pdfUrl");
        bookId = getIntent().getStringExtra("bookId");

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Obtener el número de página guardado
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        savedPageNumber = prefs.getInt(KEY_PAGE_NUMBER, 0);

        Log.d(TAG, "URL del PDF: " + pdfUrl);
        Log.d(TAG, "ID del libro: " + bookId);
        Log.d(TAG, "Número de página guardado: " + savedPageNumber);

        if (pdfUrl != null && !pdfUrl.isEmpty()) {
            // Mostrar el PDF utilizando PDFView
            displayPdfFromUrl(pdfUrl);
        } else {
            Log.e(TAG, "La URL del PDF es nula o está vacía.");
            Toast.makeText(this, "No se proporcionó una URL válida para el PDF", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Marcar el libro como "en proceso" al abrirlo
        markBookAsInProgress();

        if (pdfView != null) {
            pdfView.fromFile(new File(getCacheDir(), "downloaded.pdf"))
                    .enableSwipe(true) // Permite el deslizamiento horizontal para cambiar de página
                    .swipeHorizontal(false) // Deslizamiento vertical por defecto
                    .enableDoubletap(true) // Habilita el doble toque para hacer zoom
                    .defaultPage(savedPageNumber) // Página inicial que se muestra
                    .onPageChange((page, pageCount) -> {
                        // Guardar la página actual cada vez que cambie
                        saveCurrentPage(page);

                        // Verificar si es la última página
                        if (page + 1 == pageCount) {
                            Log.d(TAG, "Última página alcanzada. Marcando el libro como leído.");
                            markBookAsRead();
                        }
                    })
                    .load();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Guardar el número de página actual cuando la actividad está en pausa
        if (pdfView != null) {
            int currentPage = pdfView.getCurrentPage();
            saveCurrentPage(currentPage);
        }
    }

    private void saveCurrentPage(int page) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PAGE_NUMBER, page);
        editor.apply();

        Log.d(TAG, "Página actual guardada: " + page);
    }

    private void markBookAsRead() {
        if (bookId == null) {
            Log.e(TAG, "ID del libro es nulo.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Usuario no autenticado.");
            return;
        }

        Log.d(TAG, "ID del usuario: " + user.getUid());

        // Obtener el autor y la categoría del libro desde la colección "libros"
        db.collection("libros").document(bookId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot bookSnapshot = task.getResult();
                if (bookSnapshot.exists()) {
                    String author = bookSnapshot.getString("author");
                    String categoryUid = bookSnapshot.getString("category");
                    Log.d(TAG, "Autor del libro: " + author);
                    Log.d(TAG, "UID de la categoría del libro: " + categoryUid);

                    // Obtener el nombre de la categoría desde la colección "categorias"
                    db.collection("categorias").document(categoryUid).get().addOnCompleteListener(categoryTask -> {
                        if (categoryTask.isSuccessful()) {
                            DocumentSnapshot categorySnapshot = categoryTask.getResult();
                            if (categorySnapshot.exists()) {
                                String categoryName = categorySnapshot.getString("name");
                                Log.d(TAG, "Nombre de la categoría: " + categoryName);

                                db.collection("userBooks")
                                        .whereEqualTo("userId", user.getUid())
                                        .whereEqualTo("bookId", bookId)
                                        .get()
                                        .addOnCompleteListener(userBookTask -> {
                                            if (userBookTask.isSuccessful() && !userBookTask.getResult().isEmpty()) {
                                                DocumentSnapshot document = userBookTask.getResult().getDocuments().get(0);
                                                db.collection("userBooks").document(document.getId())
                                                        .update("status", "read", "author", author, "category", categoryName)
                                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Libro marcado como leído, autor y categoría guardados."))
                                                        .addOnFailureListener(e -> Log.e(TAG, "Error al marcar el libro como leído.", e));
                                            } else {
                                                Log.d(TAG, "Libro no encontrado en la base de datos. Añadiendo como leído.");
                                                addBookAsRead(author, categoryName);
                                            }
                                        });
                            } else {
                                Log.e(TAG, "Categoría no encontrada en la colección 'categorias'.");
                            }
                        } else {
                            Log.e(TAG, "Error al obtener la categoría.", categoryTask.getException());
                        }
                    });
                } else {
                    Log.e(TAG, "Libro no encontrado en la colección 'libros'.");
                }
            } else {
                Log.e(TAG, "Error al obtener el autor del libro.", task.getException());
            }
        });
    }

    private void addBookAsRead(String author, String categoryName) {
        if (bookId == null) {
            Log.e(TAG, "ID del libro es nulo.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Usuario no autenticado.");
            return;
        }

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("userId", user.getUid());
        bookData.put("bookId", bookId);
        bookData.put("status", "read");
        bookData.put("author", author);
        bookData.put("category", categoryName);

        db.collection("userBooks")
                .add(bookData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Libro añadido como leído con autor y categoría."))
                .addOnFailureListener(e -> Log.e(TAG, "Error al añadir el libro como leído.", e));
    }

    private void markBookAsInProgress() {
        if (bookId == null) {
            Log.e(TAG, "ID del libro es nulo.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Usuario no autenticado.");
            return;
        }

        Log.d(TAG, "ID del usuario: " + user.getUid());

        // Verificar si el libro ya existe en la colección "userBooks"
        db.collection("userBooks")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("bookId", bookId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Si el libro ya existe, actualizar el estado a "inProgress"
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        db.collection("userBooks").document(document.getId())
                                .update("status", "inProgress")
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Estado del libro actualizado a 'en proceso'."))
                                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar el estado del libro.", e));
                    } else {
                        // Si el libro no existe, añadirlo con el estado "inProgress"
                        Map<String, Object> bookData = new HashMap<>();
                        bookData.put("userId", user.getUid());
                        bookData.put("bookId", bookId);
                        bookData.put("status", "inProgress");

                        db.collection("userBooks")
                                .add(bookData)
                                .addOnSuccessListener(documentReference -> Log.d(TAG, "Libro añadido con estado 'en proceso'."))
                                .addOnFailureListener(e -> Log.e(TAG, "Error al añadir el libro como 'en proceso'.", e));
                    }
                });
    }

    private void displayPdfFromUrl(String pdfUrl) {
        new DownloadPdfTask().execute(pdfUrl);
    }

    private class DownloadPdfTask extends AsyncTask<String, Void, File> {

        @Override
        protected File doInBackground(String... strings) {
            String urlString = strings[0];
            File pdfFile = new File(getCacheDir(), "downloaded.pdf");

            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                return pdfFile;

            } catch (MalformedURLException e) {
                Log.e(TAG, "URL del PDF es incorrecta: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Error al descargar el PDF: " + e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(File pdfFile) {
            if (pdfFile != null) {
                pdfView.fromFile(pdfFile)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(savedPageNumber)
                        .onPageChange((page, pageCount) -> saveCurrentPage(page))
                        .load();
            } else {
                Toast.makeText(PdfViewerActivity.this, "Error al cargar el PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
