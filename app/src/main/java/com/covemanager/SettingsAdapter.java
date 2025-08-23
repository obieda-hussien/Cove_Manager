package com.covemanager;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.covemanager.databinding.ItemSettingBinding;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingViewHolder> {
    private List<Setting> settings;

    public SettingsAdapter(List<Setting> settings) {
        this.settings = settings;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSettingBinding binding = ItemSettingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SettingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        Setting setting = settings.get(position);
        holder.bind(setting);
    }

    @Override
    public int getItemCount() {
        return settings.size();
    }

    static class SettingViewHolder extends RecyclerView.ViewHolder {
        private ItemSettingBinding binding;

        public SettingViewHolder(ItemSettingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Setting setting) {
            binding.tvSettingTitle.setText(setting.getTitle());
            binding.tvSettingSubtitle.setText(setting.getSubtitle());
            binding.ivSettingIcon.setImageResource(setting.getIcon());
        }
    }
}