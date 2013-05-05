package com.mmcmahon.rtv2go.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SlidingDrawer;

/**
 * A horizontal sliding drawer. Obtained from this repository:
 * http://code.google.com/p/android-misc-widgets/ 
 * Oct 2012
 */
public class PullOut extends SlidingDrawer
{
   public PullOut(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
   }

   public PullOut(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
   {

      int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
      int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

      int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
      int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

      // 10-2 Now overrides to handle unspecified measure specs
      if (widthSpecMode == MeasureSpec.UNSPECIFIED)
      {
         widthSpecMode = MeasureSpec.EXACTLY;
      }
      if (heightSpecMode == MeasureSpec.UNSPECIFIED)
      {// throw new
       // RuntimeException("SlidingDrawer cannot have UNSPECIFIED dimensions");
         heightSpecMode = MeasureSpec.EXACTLY;
      }
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
            widthSpecMode);
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSpecSize,
            heightSpecMode);

      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
   }
}