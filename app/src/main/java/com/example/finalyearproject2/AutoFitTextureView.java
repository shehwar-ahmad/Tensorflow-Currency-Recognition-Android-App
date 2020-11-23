package com.example.finalyearproject2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {


    private int measured_width = 0;
    private int measured_height = 0;

    public AutoFitTextureView(Context context) {
        super(context);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


  //override function to set Length and Width of Texture View
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        //first time
        if (0 == measured_width || 0 == measured_height) {
            setMeasuredDimension(width, height);
        } else {
            //when called from function texture_length_width
            if (width < height * measured_width / measured_height) {
                setMeasuredDimension(width, width * measured_height / measured_width);
            } else {
                setMeasuredDimension(height * measured_width / measured_height, height);
            }
        }
    }


    public void texture_length_width(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("- Error");
        }
        measured_width = width;
        measured_height = height;
        //to call overRide function above
        requestLayout();
    }

}
