package com.adnet.archat.Item;

import android.graphics.Color;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by TanZhegui on 9/17/2016.
 */
public class FeatureItem {
    public String img_gray;
    public float x, y, width, height;
    public int img_width, img_height;
    public float[] ptArray;
    public int color;

    public FeatureItem(){
        x = 0; y = 0;
        width = 0; height = 0;
        img_width = 0; img_height = 0;
        img_gray = "";
        color = Color.GREEN;
    }

}
