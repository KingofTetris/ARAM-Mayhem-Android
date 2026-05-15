package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class BulletinCarouselView extends LinearLayout {

    private ViewPager2 viewPager;
    private LinearLayout indicatorContainer;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private static final long AUTO_SCROLL_INTERVAL = 5000;
    private List<BulletinUiModel> bulletins = new ArrayList<>();
    private OnBulletinClickListener listener;

    public interface OnBulletinClickListener {
        void onBulletinClick(BulletinUiModel bulletin);
    }

    public BulletinCarouselView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        viewPager = new ViewPager2(context);
        viewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(viewPager);

        indicatorContainer = new LinearLayout(context);
        indicatorContainer.setOrientation(HORIZONTAL);
        indicatorContainer.setGravity(android.view.Gravity.CENTER);
        LayoutParams indicatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        indicatorParams.topMargin = 8;
        indicatorContainer.setLayoutParams(indicatorParams);
        addView(indicatorContainer);

        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            if (bulletins.size() > 1) {
                int nextItem = (viewPager.getCurrentItem() + 1) % bulletins.size();
                viewPager.setCurrentItem(nextItem, true);
            }
            startAutoScroll();
        };
    }

    public void setBulletins(List<BulletinUiModel> data) {
        this.bulletins = data != null ? data : new ArrayList<>();
        setupAdapter();
        setupIndicators();
        updateIndicatorSelection(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicatorSelection(position);
            }
        });
    }

    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    public void startAutoScroll() {
        stopAutoScroll();
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_INTERVAL);
    }

    public void stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    private void setupAdapter() {
        CarouselAdapter adapter = new CarouselAdapter();
        viewPager.setAdapter(adapter);
    }

    private void setupIndicators() {
        indicatorContainer.removeAllViews();
        for (int i = 0; i < bulletins.size(); i++) {
            View dot = new View(getContext());
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            indicatorContainer.addView(dot);
        }
    }

    private void updateIndicatorSelection(int position) {
        int activeColor = 0xFF1976D2;
        int inactiveColor = 0xFFBDBDBD;
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            View dot = indicatorContainer.getChildAt(i);
            dot.setBackgroundColor(i == position ? activeColor : inactiveColor);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

    private class CarouselAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CarouselViewHolder> {

        @NonNull
        @Override
        public CarouselViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new CarouselViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
            BulletinUiModel bulletin = bulletins.get(position);
            if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
                Glide.with(getContext())
                        .load(bulletin.getImageUrl())
                        .centerCrop()
                        .into(holder.imageView);
            }
            holder.imageView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBulletinClick(bulletin);
                }
            });
        }

        @Override
        public int getItemCount() {
            return bulletins.size();
        }
    }

    private static class CarouselViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView imageView;

        CarouselViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}
