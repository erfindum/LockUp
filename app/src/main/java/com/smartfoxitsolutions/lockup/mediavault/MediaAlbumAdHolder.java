package com.smartfoxitsolutions.lockup.mediavault;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 16-10-2016.
 */

public class MediaAlbumAdHolder extends RecyclerView.ViewHolder {
    private AppCompatImageView thumbnailView;
    private OnGridItemSelectedListener listener;

    public void setOnGridItemSelectedListener(OnGridItemSelectedListener listener){
        this.listener = listener;
    }

    public MediaAlbumAdHolder(View itemView) {
        super(itemView);
        thumbnailView = (AppCompatImageView) itemView.findViewById(R.id.vault_album_picker_ads_recycler_item_placeholder);
        setOnclickListener();
    }

    private void setOnclickListener(){
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onGridItemSelected(getLayoutPosition());
            }
        });
    }
}
