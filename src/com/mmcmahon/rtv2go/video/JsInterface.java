package com.mmcmahon.rtv2go.video;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.thumbnails.ThumbnailsAdapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 *  An interface for this app to communicate with javascript. Handles non-channel related tasks. 
 *  
 *  Summer 2012
 *  Michael McMahon
 */
class JsInterface
{
   private static final String TAG = "#JsInterface#";
   private ACTV_VideoPlayer actvCon;
   private int width = 0, height = 0;
   private ThumbnailsAdapter tnsAdapter;

   public JsInterface(Context c)
   {
      actvCon = (ACTV_VideoPlayer) c;
   }

   public void setHeight(int h)
   {
      height = h;
   }

   public void setWidth(int w)
   {
      width = w;
   }

   public int getHeight()
   {
      return height;
   }

   public int getWidth()
   {
      return width;
   }

   public void setTitle(final String title)
   {
      ((Activity) actvCon).runOnUiThread(new Runnable()
      {
         public void run()
         {
            ((Activity) actvCon).setTitle(title);
         }
      });
   }

   public void androidLog(String msg)
   {
      Log.e(TAG, "SEENIT JS CONSOLE: " + msg);
   }

   public void addThumbnail(String url, String title, int score, String id,
         int voted)
   {
      if (tnsAdapter != null)
      {
         tnsAdapter.addThumbnail(url, title, score, id, voted);
      }
   }

   public void setTnsAdapter(ThumbnailsAdapter tnsa)
   {
      tnsAdapter = tnsa;
   }

   /**
    * Called when tv.js has found no videos while attempting to load a channel.
    */
   public void promptNoVideos(String chan)
   {
      actvCon.promptNoVideos(chan);
   }
}
