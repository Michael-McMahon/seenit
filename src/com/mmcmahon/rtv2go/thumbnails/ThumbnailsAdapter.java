package com.mmcmahon.rtv2go.thumbnails;

import java.util.Vector;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
//import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Manages a gallery of thumbnail video previews for the current channel. 
 * Thumbsnails are stored in a LRU cache, which can be configured to use ++/-- 
 * memory. Adapter is set up to intersperse advertisement videos into the list.
 *   
 * Summer 2012
 * @author Michael McMahon
 */
public class ThumbnailsAdapter extends BaseAdapter
{
   // The portion of memory the image cache will consume, 
   // The cache will use 1/16th of memory when this variable equals 16.
   private final int MEM_PORTION = 8;
   public static int THUMB_HEIGHT = 150, THUMB_WIDTH = 250;
   //private static final String TAG = null;
   private final ACTV_VideoPlayer actvCon;
   private final BitmapCache bitmapCache;
   private Vector<ThumbnailContent> thumbData = new Vector<ThumbnailContent>();

   public ThumbnailsAdapter(Context c)
   {
      actvCon = (ACTV_VideoPlayer) c;
      configThumbSize();
      bitmapCache = BitmapCache.createBitmapCache(c, MEM_PORTION);
   }

   /**
    * Determine the screen resolution, and set an appropriate thumbnail size.
    */
   @TargetApi(13)
   private void configThumbSize()
   {
      Display disp = actvCon.getWindowManager().getDefaultDisplay();
      Point res = new Point();
      //Get the screen's dimensions
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
      {
         disp.getSize(res);//API 13 call
      }
      else
      {
         res.x = disp.getWidth();
         res.y = disp.getHeight();
      }
      //Set thumbnail size
      THUMB_WIDTH = res.x / 3;
      THUMB_HEIGHT = res.y /3;
   }

   public int getCount()
   {
      return thumbData.size();
   }

   /**
    * Get the thumbnail content for the i'th postion in the gallery. Will return
    * data for an advertisement, if the position is an ad space.
    */
   public Object getItem(int i)
   {
      int thumbSize = thumbData.size();

      if (thumbSize == 0)// Waiting for thumbnails to arrive
      {
         return new ThumbnailContent("", "loading...", 0, "-2", 0);
      }

      return thumbData.get(i);
   }

   public long getItemId(int id)
   {
      return id;
   }

   public View getView(int pos, View v, ViewGroup gal)
   {
      ThumbnailContent content;
      String imgUrl;
      ImageView imageView = (ImageView) v;

      // Create a new view to hold a thumbnail
      if (imageView == null)
      {
         // 9-13 NOW USING ImageView (imageView), not Thumbnail (view)
         imageView = new ImageView(actvCon);
         imageView.setLayoutParams(// Use gallery layoutparams
               new Gallery.LayoutParams(THUMB_WIDTH, THUMB_HEIGHT));
         imageView.setBackgroundResource(R.drawable.thumb_bg);
         imageView.setScaleType(ImageView.ScaleType.FIT_XY);
         imageView.setAlpha(127);
      }

      content = (ThumbnailContent) getItem(pos);
      imgUrl = content.getUrl();// Tell the view to go fetch its bitmap now
      if (imgUrl != null)
      {
         bitmapCache.getBitmap(imageView, imgUrl, imgUrl);// Use url as key
      }
      return imageView;
   }

   public void addThumbnail(final String url, final String title,
         final int score, final String id, final int voted)
   {
      // Notify UI Thread of new thumbnail
      ((Activity) actvCon).runOnUiThread(new Runnable()
      {
         public void run()
         {
            thumbData.add(new ThumbnailContent(url, title, score, id, voted));
            notifyDataSetChanged();
            if (thumbData.size() == 1)// Remove loading text in title
            {
               actvCon.thumbsLoaded();
            }
         }
      });
   }

   public LruCache<String, Bitmap> getBitmapCache()
   {
      return bitmapCache;
   }
}
