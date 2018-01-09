package com.roostermornings.android.custom_ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ScrollView;

/**
 * Created by bscholtz on 2017/11/17.
 */

public class SnackbarDialogLinearLayout extends ScrollView {

    public SnackbarDialogLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Necessary for showing elevation on 5.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ShadowOutline(w, h));
        }
    }

    // ShadowOutline implementation
    @TargetApi(21)
    static class ShadowOutline extends ViewOutlineProvider {

        int width;
        int height;

        ShadowOutline(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, width, height);
        }
    }
}
