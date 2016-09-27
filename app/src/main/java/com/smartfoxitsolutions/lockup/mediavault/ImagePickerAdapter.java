package com.smartfoxitsolutions.lockup.mediavault;

import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.LinkedHashMap;
import java.util.LinkedList;


/**
 * Created by RAAJA on 22-09-2016.
 */

public class ImagePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private Cursor resultCursor;
    private ContentResolver contentResolver;
    private ImagePickerExtBucketFragment imageFragment;
    private LinkedHashMap<Integer,String> bucketIdNameMap;
    private LinkedList<Integer> bucketCount;
    private LinkedList<Integer> bucketThumbnailId;

    public ImagePickerAdapter(Cursor cursor, ContentResolver contentResolver, ImagePickerExtBucketFragment frag) {
        resultCursor = cursor;
        this.contentResolver = contentResolver;
        imageFragment = frag;
        setupImageBuckets();
    }

    private void setupImageBuckets(){

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

}
