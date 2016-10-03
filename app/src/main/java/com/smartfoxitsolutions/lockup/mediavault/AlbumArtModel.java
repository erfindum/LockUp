package com.smartfoxitsolutions.lockup.mediavault;

import android.content.Context;
import android.net.Uri;

/**
 * Created by RAAJA on 03-10-2016.
 */

public class AlbumArtModel {
    Uri albumPath;
    Context ctxt;
    public AlbumArtModel(Uri path, Context ctxt) {
        this.albumPath = path;
        this.ctxt = ctxt;
    }
}
