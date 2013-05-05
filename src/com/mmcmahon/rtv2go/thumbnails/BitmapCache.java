package com.mmcmahon.rtv2go.thumbnails;

import java.io.InputStream;
import java.net.URL;

import com.mmcmahon.rtv2go.R;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * Standard Android library LRUCache, with light modifications for loading 
 * video thumbnails.
 * @author Michael McMahon
 */
public class BitmapCache extends LruCache<String, Bitmap>
{
   private static final String TAG = "#BitmapCache#";

   private Context actvCon;

   public BitmapCache(int maxSize, Context avp)
   {
      super(maxSize);
      actvCon = avp;
   }

   /*
    * TODO: A better solution is to have a hardcoded size. The size of a bitmap
    * is hardcoded, and the cache will never hold more than a hundred or so
    * entries (unless the javascript is modified to retrieve more). Only thing
    * left to do is determine the DYNAMIC? bytes-per-pixel size...
    */
   /*
    * Create an instance of this class for storing bitmaps with string keys,
    * using 1/pth portion of memory
    */
   public static BitmapCache createBitmapCache(Context actvCon, int p)
   {
      // Get memory class of this device, exceeding this amount will throw an
      // OutOfMemory exception.
      final int memClass = ((ActivityManager) actvCon
            .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

      // Use 1/pth of the available memory for this memory cache.
      final int cacheSize = 1024 * 1024 * memClass / p;

      return new BitmapCache(cacheSize, actvCon);
   }

   protected int sizeOf(String key, Bitmap bitmap)
   {
      // The cache size will be measured in bytes rather than number of items.
      return bitmap.getRowBytes() * bitmap.getHeight();// getByteCount();
   }

   /*
    * To be called when this thumbnail is going to be displayed in a scrolling
    * list, and the image must be processed.
    */
   public void getBitmap(ImageView imageView, String imageURL, String imageKey)
   {
      BitmapWorkerTask bitmapDecoder;
      final Bitmap bitmap = (Bitmap) get(imageKey);// Lookup

      if (bitmap != null)// Hit
      {
         imageView.setImageBitmap(bitmap);
      } else// Miss
      {
         bitmapDecoder = new BitmapWorkerTask(imageView);
         bitmapDecoder.execute(imageURL, imageKey);
      }
   }

   /* Decodes bitmap from inputstream: MEMORY INTENTSIVE */
   class BitmapWorkerTask extends AsyncTask<String, Integer, Bitmap>
   {
      private ImageView imgView;

      public BitmapWorkerTask(ImageView imageView)
      {
         imgView = imageView;
      }

      // Decode image in background. params = {String url, String cache key}
      protected Bitmap doInBackground(String... params)
      {
         if (params.length < 2)
         {
            return null;
         }

         Bitmap bitmap;
         InputStream imgContent = getImageContent(params[0]);
         String imgKey = params[1];

         if (imgContent == null)
         {
            return null;
         }

         bitmap = BitmapFactory.decodeStream(imgContent);
         bitmap = Bitmap.createScaledBitmap(bitmap, ThumbnailsAdapter.THUMB_WIDTH,
               ThumbnailsAdapter.THUMB_HEIGHT, true);
         // Log.e(TAG, "size::::: " + bitmap.getHeight() + " row: " +
         // bitmap.getRowBytes());
         put(imgKey, bitmap);// Now store the new bitmap

         return bitmap;
      }

      @Override
      protected void onPostExecute(Bitmap bitmap)
      {
         //View gets updated when task has completed
         imgView.setImageBitmap(bitmap);
      }
   }

   /*
    * Get the bytes of an image at url.
    */
   public InputStream getImageContent(String url)
   {
      URL imgUrl;
      InputStream imgContent = null;
      /*
       * if(url.equals(SHOW_LOADING))//special key for returning a loading
       * animation { return actvCon.getResources().openRawResource(android.R.);
       * }
       */

      try
      {
         imgUrl = new URL(url);
         imgContent = (InputStream) imgUrl.getContent();
      } catch (Exception e)
      {
         Log.e(TAG + "getImageContent", e.toString());
         imgContent = actvCon.getResources().openRawResource(
               R.drawable.video_icon2);
      }

      return imgContent;
   }
}
