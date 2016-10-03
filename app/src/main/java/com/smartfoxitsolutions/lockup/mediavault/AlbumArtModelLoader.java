package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;

import java.io.InputStream;

/**
 * Created by RAAJA on 03-10-2016.
 */

public class AlbumArtModelLoader implements StreamModelLoader<AlbumArtModel> {
    @Override
    public DataFetcher<InputStream> getResourceFetcher(AlbumArtModel model, int width, int height) {
        return new AlbumArtFetcher(model);
    }

    static class Factory implements ModelLoaderFactory<AlbumArtModel,InputStream>{
        @Override
        public ModelLoader<AlbumArtModel, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new AlbumArtModelLoader();
        }

        @Override
        public void teardown() {

        }
    }
}
