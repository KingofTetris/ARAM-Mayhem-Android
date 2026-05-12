package com.aram.mayhem.feature.hero.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.hero.databinding.ItemSkillBinding;
import com.aram.mayhem.ui.model.HeroDetailUiModel;

import java.util.ArrayList;
import java.util.List;

public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    private List<HeroDetailUiModel.SkillUiModel> skills = new ArrayList<>();

    public void setSkills(List<HeroDetailUiModel.SkillUiModel> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSkillBinding binding = ItemSkillBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SkillViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        HeroDetailUiModel.SkillUiModel skill = skills.get(position);
        holder.bind(skill);
    }

    @Override
    public int getItemCount() {
        return skills.size();
    }

    static class SkillViewHolder extends RecyclerView.ViewHolder {

        private final ItemSkillBinding binding;

        SkillViewHolder(ItemSkillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HeroDetailUiModel.SkillUiModel skill) {
            binding.textSkillKey.setText(skill.getKey().toUpperCase());
            binding.textSkillName.setText(skill.getName());
            binding.textSkillDescription.setText(skill.getDescription());
        }
    }
}
