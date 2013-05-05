package com.mmcmahon.rtv2go.dialogs;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.VideoInterface;
import com.mmcmahon.rtv2go.R.id;
import com.mmcmahon.rtv2go.R.layout;
import com.mmcmahon.rtv2go.video.VideoPlayer;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

/**
 * A dialog which allows the interface to overlay the html video content.
 * 
 * Summer 2012
 * @author Michael McMahon
 */
public class InterfaceDialog extends Dialog
{
   private static final String TAG = "#InterfaceDialog#";
   private VideoInterface lsiface;
   private ACTV_VideoPlayer actvCon;

   public InterfaceDialog(Context context, VideoPlayer vp)
   {
      super(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
      actvCon = (ACTV_VideoPlayer) context;
      
      setContentView(R.layout.lsiface2);// landscape_interface);
      lsiface = (VideoInterface) findViewById(R.id.lsifaceClickables);
      lsiface.setVideoPlayer(vp);
   }

   @Override
   public void onBackPressed()
   {
      if (lsiface != null)
      {
         lsiface.backPressed();
      }
   }

   @Override
   public boolean onKeyDown(int keycode, KeyEvent event)
   {
      switch (keycode)
      {
      case KeyEvent.KEYCODE_MENU:
         actvCon.openMenu();
         return true;
      case KeyEvent.KEYCODE_BACK:
         onBackPressed();
         return true;
      default:
         return false;
      }
   }

   public VideoInterface getInterfaceView()
   {
      return lsiface;
   }
}
