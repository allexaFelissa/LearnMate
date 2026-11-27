package com.learnmate.app.ui.material;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.learnmate.app.databinding.ItemMaterialBinding;
import com.learnmate.app.model.Material;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MaterialAdapter extends RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder> {

    public interface OnMaterialClickListener {
        void onMaterialClick(Material material);
    }

    private final List<Material> items = new ArrayList<>();
    private final OnMaterialClickListener listener;
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public MaterialAdapter(OnMaterialClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Material> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MaterialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMaterialBinding binding = ItemMaterialBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MaterialViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MaterialViewHolder holder, int position) {
        Material item = items.get(position);
        holder.binding.tvMaterialTitle.setText(item.getTitle());
        String subject = item.getSubjectName() != null ? item.getSubjectName() : "General";
        String meta = subject + " • " + item.getPageCount() + " pages • "
                + dateFormat.format(new Date(item.getCreatedAt()));
        holder.binding.tvMaterialMeta.setText(meta);
        holder.itemView.setOnClickListener(v -> listener.onMaterialClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MaterialViewHolder extends RecyclerView.ViewHolder {
        final ItemMaterialBinding binding;

        MaterialViewHolder(ItemMaterialBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
