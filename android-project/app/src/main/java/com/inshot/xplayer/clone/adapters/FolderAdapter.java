package com.inshot.xplayer.clone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.inshot.xplayer.clone.R;
import com.inshot.xplayer.clone.models.FolderModel;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderModel folder);
    }

    private final Context context;
    private final List<FolderModel> folderList;
    private final OnFolderClickListener listener;

    public FolderAdapter(Context context, List<FolderModel> folderList, OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        FolderModel folder = folderList.get(position);
        holder.txtFolderName.setText(folder.getName());
        holder.txtFolderCount.setText(String.format("%d videos", folder.getVideoCount()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFolderClick(folder);
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView txtFolderName, txtFolderCount;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFolderName = itemView.findViewById(R.id.txtFolderName);
            txtFolderCount = itemView.findViewById(R.id.txtFolderCount);
        }
    }
}
