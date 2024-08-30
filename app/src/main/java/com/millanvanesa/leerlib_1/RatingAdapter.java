package com.millanvanesa.leerlib_1;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.ViewHolder> {
    private Context context;
    private List<RatingItem> ratingList;
    private String userId;

    public RatingAdapter(Context context, List<RatingItem> ratingList, String userId) {
        this.context = context;
        this.ratingList = ratingList;
        this.userId = userId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rating, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RatingItem item = ratingList.get(position);
        holder.titleTextView.setText(item.getTitle());
        Glide.with(context).load(item.getImageUrl()).into(holder.imageView);

        if (item.isCategory()) {
            // No hacer clic en categorías
            holder.itemView.setOnClickListener(null);
        } else if (item.isInProgress()) {
            // Configuración para "Seguir Leyendo"
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, PdfViewerActivity.class); // Asumimos que abre en PdfViewerActivity
                intent.putExtra("pdfUrl", item.getImageUrl());
                intent.putExtra("bookId", item.getBookId());
                context.startActivity(intent);
            });
        } else if (item.isNew()) {
            // Configuración para recomendaciones por "Más Nuevo"
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, LibroDetailActivity.class);
                intent.putExtra("libroId", item.getBookId());
                intent.putExtra("userId", userId);
                context.startActivity(intent);
            });
        } else {
            // Configuración para hacer clic en libros normales (rating o autor)
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, LibroDetailActivity.class);
                intent.putExtra("libroId", item.getBookId());
                intent.putExtra("userId", userId);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return ratingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ratingImageView);
            titleTextView = itemView.findViewById(R.id.ratingTitleTextView);
        }
    }
}
