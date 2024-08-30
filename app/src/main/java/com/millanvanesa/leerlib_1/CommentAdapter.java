package com.millanvanesa.leerlib_1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;

import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private final Context context;
    private final List<Comment> comments;

    public CommentAdapter(@NonNull Context context, @NonNull List<Comment> comments) {
        super(context, R.layout.comment_list_item, comments);
        this.context = context;
        this.comments = comments;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.comment_list_item, parent, false);
        }

        Comment comment = comments.get(position);

        ImageView profileImageView = convertView.findViewById(R.id.commentProfileImageView);
        TextView nameTextView = convertView.findViewById(R.id.commentNameTextView);
        TextView commentTextView = convertView.findViewById(R.id.commentTextView);

        // Cargar la imagen de perfil utilizando Picasso
        if (comment.getUserImageUrl() != null && !comment.getUserImageUrl().isEmpty()) {
            Picasso.get()
                    .load(comment.getUserImageUrl())
                    .placeholder(R.drawable.perfil) // Imagen predeterminada si falla
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.perfil);
        }

        // Configurar el nombre del usuario y el comentario
        nameTextView.setText(comment.getUsername() != null ? comment.getUsername() : "Usuario desconocido");
        commentTextView.setText(comment.getComentario());

        return convertView;
    }
}
