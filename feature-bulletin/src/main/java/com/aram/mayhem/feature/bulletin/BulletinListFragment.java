package com.aram.mayhem.feature.bulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinListBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BulletinListFragment extends Fragment {

    private FragmentBulletinListBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
