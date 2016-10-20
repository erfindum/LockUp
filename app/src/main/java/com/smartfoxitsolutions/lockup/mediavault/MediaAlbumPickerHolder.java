package com.smartfoxitsolutions.lockup.mediavault;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 22-09-2016.
 */

public class MediaAlbumPickerHolder extends RecyclerView.ViewHolder {

    private AppCompatImageView thumbnailView;
    private TextView infoText;
    private TextView countText;
    private OnGridItemSelectedListener listener;

    public void setOnGridItemSelectedListener(OnGridItemSelectedListener listener){
        this.listener = listener;
    }

    public MediaAlbumPickerHolder(View itemView) {
        super(itemView);
        thumbnailView = (AppCompatImageView) itemView.findViewById(R.id.vault_album_picker_recycler_item_thumbnail);
        infoText = (TextView) itemView.findViewById(R.id.vault_album_picker_recycler_item_text);
        countText = (TextView) itemView.findViewById(R.id.vault_album_picker_recycler_item_count);
        setOnclickListener();
    }

    ImageView getThumbnailView(){
        return this.thumbnailView;
    }

    TextView getInfoText(){
        return  this.infoText;
    }

    TextView getCountText(){
        return this.countText;
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
