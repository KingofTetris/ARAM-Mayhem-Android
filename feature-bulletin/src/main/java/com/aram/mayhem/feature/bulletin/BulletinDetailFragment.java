package com.aram.mayhem.feature.bulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinDetailBinding;
import com.aram.mayhem.feature.bulletin.viewmodel.BulletinDetailViewModel;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BulletinDetailFragment extends Fragment {

    private static final String ARG_BULLETIN_ID = "bulletin_id";

    private FragmentBulletinDetailBinding binding;
    private BulletinDetailViewModel viewModel;

    private OnBackListener onBackListener;

    public interface OnBackListener {
        void onBack();
    }

    public void setOnBackListener(OnBackListener listener) {
        this.onBackListener = listener;
    }

    public static BulletinDetailFragment newInstance(long bulletinId) {
        BulletinDetailFragment fragment = new BulletinDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BULLETIN_ID, bulletinId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BulletinDetailViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> {
            if (onBackListener != null) {
                onBackListener.onBack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        if (getArguments() != null) {
            long bulletinId = getArguments().getLong(ARG_BULLETIN_ID);
            viewModel.loadBulletinDetail(bulletinId);
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getBulletinDetail().observe(getViewLifecycleOwner(), this::displayBulletin);
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.progressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void displayBulletin(BulletinUiModel bulletin) {
        if (bulletin == null) return;

        binding.toolbar.setTitle(bulletin.getTypeDisplay());
        binding.textTitle.setText(bulletin.getTitle());
        binding.textType.setText(bulletin.getTypeDisplay());
        binding.textDate.setText(bulletin.getPublishedAt());
        binding.textContent.setText(bulletin.getContent());

        if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
            binding.imageBulletin.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(bulletin.getImageUrl())
                    .centerCrop()
                    .into(binding.imageBulletin);
        } else {
            binding.imageBulletin.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
