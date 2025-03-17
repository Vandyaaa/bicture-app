package com.example.imaginate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.lang.reflect.Field;

public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {

    public CustomSwipeRefreshLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public CustomSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        try {
            // Mengakses field circleView dari SwipeRefreshLayout
            Field circleView = SwipeRefreshLayout.class.getDeclaredField("mCircleView");
            circleView.setAccessible(true);
            View progressView = (View) circleView.get(this);

            // Set background drawable pada circle view
            if (progressView != null) {
                progressView.setBackground(
                        ContextCompat.getDrawable(getContext(), R.drawable.lingkaran_border_isi_berwarna)
                );
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
