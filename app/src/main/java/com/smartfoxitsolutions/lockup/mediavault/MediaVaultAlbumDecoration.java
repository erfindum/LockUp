package com.smartfoxitsolutions.lockup.mediavault;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by RAAJA on 29-12-2016.
 */

public class MediaVaultAlbumDecoration extends RecyclerView.ItemDecoration {
    private int itemMargin;

    public MediaVaultAlbumDecoration(int margin) {
        this.itemMargin = margin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(itemMargin,itemMargin,itemMargin,itemMargin);
    }
}
