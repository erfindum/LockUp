package com.smartfoxitsolutions.lockup.mediavault;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smartfoxitsolutions.lockup.R;

/**
 * Created by RAAJA on 22-09-2016.
 */

public class ImagePickerExtBucketFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    RecyclerView imagePickerExtBuckRecycler;
    TextView loadingText;
    ProgressBar loadingProgress;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.media_vault_image_pick_ext_fragment,container,false);
        imagePickerExtBuckRecycler = (RecyclerView) parent.findViewById(R.id.media_vault_image_pick_ext_fragment_recycler);
        loadingProgress = (ProgressBar) parent.findViewById(R.id.media_vault_image_pick_ext_fragment_progress);
        loadingText = (TextView) parent.findViewById(R.id.media_vault_image_pick_ext_fragment_load_text);
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(20,null,this);
        return parent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                                ,MediaStore.Images.Media.BUCKET_ID};

        return new CursorLoader(getActivity().getBaseContext(),
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,projection,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
