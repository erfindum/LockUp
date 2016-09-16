package com.smartfoxitsolutions.lockup;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by RAAJA for SmartFoxItSolutions on 28-08-2016.
 *
 * Utility Class for converting Dp to Pixels or Pixels to Dp
 */
public class DimensionConverter  {

    /**
     * Converts dimension in Dp to Pixel
     *
     * @param dp The Dp dimension for pixel conversion
     * @param ctxt The Context for getting the density of the display at runtime
     * @return The Pixel dimension in float
     */

    public static float convertDpToPixel(float dp, Context ctxt){
        DisplayMetrics metrics = ctxt.getResources().getDisplayMetrics();
        return dp*(metrics.densityDpi/DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Converts dimesnion in Pixel to Dp
     *
     * @param pixel The Pixel dimension for Dp Conversion
     * @param ctxt The Context for getting the density of the display at runtime
     * @return The Dp Dimension in float
     */

    public  static float convertPixelToDp(float pixel, Context ctxt){
        int density = ctxt.getResources().getDisplayMetrics().densityDpi;
        return (pixel*160f)/density;
    }
}
