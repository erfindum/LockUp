package com.smartfoxitsolutions.lockup.mediavault;

import android.media.MediaMetadataRetriever;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by RAAJA on 03-10-2016.
 */

public class AlbumArtFetcher implements DataFetcher<InputStream> {

    private AlbumArtModel model;

    public AlbumArtFetcher(AlbumArtModel model) {
        this.model = model;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try{
            retriever.setDataSource(model.ctxt,model.albumPath);
            byte[] bitmapByteData = retriever.getEmbeddedPicture();
            if(bitmapByteData != null){
                return new ByteArrayInputStream(bitmapByteData);
            }
        }finally {
            retriever.release();
        }
        return null;
    }

    @Override
    public void cleanup() {

    }

    @Override
    public String getId() {
        return model.albumPath.toString();
    }

    @Override
    public void cancel() {

    }
}
