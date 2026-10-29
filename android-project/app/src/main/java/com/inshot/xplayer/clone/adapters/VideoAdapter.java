package com.inshot.xplayer.clone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.inshot.xplayer.clone.R;
import com.inshot.xplayer.clone.models.VideoModel;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoModel video, int position);
        void onMoveToVault(VideoModel video);
    }

    private final Context context;
    private final List<VideoModel> videoList;
    private final OnVideoClickListener listener;

    public VideoAdapter(Context context, List<VideoModel> videoList, OnVideoClickListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        holder.txtTitle.setText(video.getTitle());
        holder.txtDuration.setText(video.getFormattedDuration());
        holder.txtDetails.setText(String.format("%s · %s", video.getFolder(), video.getFormattedSize()));

        Glide.with(context)
                .load(video.getUri())
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.imgThumbnail);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onVideoClick(video, holder.getAdapterPosition());
        });

        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, v);
            popup.getMenu().add("Play");
            popup.getMenu().add("Move to Private Vault");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Play")) {
                    if (listener != null) listener.onVideoClick(video, holder.getAdapterPosition());
                } else if (item.getTitle().equals("Move to Private Vault")) {
                    if (listener != null) listener.onMoveToVault(video);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView txtTitle, txtDuration, txtDetails, txtBadge;
        ImageButton btnMore;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtDetails = itemView.findViewById(R.id.txtDetails);
            txtBadge = itemView.findViewById(R.id.txtBadge);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
