package com.mmcmahon.rtv2go.dialogs;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.R.id;
import com.mmcmahon.rtv2go.R.layout;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Toast;

/**
 * A dialog which houses menu type buttons, exit or logout, for instance.
 * 
 * Summer 2012
 * @author Michael McMahon
 */
public class MenuDialog extends Dialog
{
   private static final int VIB_DUR = 100;

   private ACTV_VideoPlayer actvCon;
   private View logout, exit;

   public MenuDialog(Context context)
   {
      super(context, android.R.style.Theme_Translucent_NoTitleBar);
      setContentView(R.layout.menu_popup);
      actvCon = (ACTV_VideoPlayer) context;
      logout = findViewById(R.id.menuLogout);
      exit = findViewById(R.id.menuExit);
      
      setListeners();
      this.setLoggedIn(false);// So logout is not clickable until logged in
   }

   @Override
   public void onBackPressed()
   {
      actvCon.closeMenu();
   }

   public void setLoggedIn(boolean loggedIn)
   {
      AlphaAnimation fade = loggedIn ? new AlphaAnimation((float) 0.2,
            (float) 1.0) : // Fade in
            new AlphaAnimation((float) 1.0, (float) 0.2);// Fade out
      fade.setFillAfter(true);
      fade.setDuration(0);
      logout.startAnimation(fade);
      logout.setClickable(loggedIn);
   }

   private void setListeners()
   {
      // And set on-click listeners
      logout.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View arg0)
         {
            ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
                  .vibrate(VIB_DUR);
            actvCon.redditLogout();
            actvCon.closeMenu();
         }
      });

      exit.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View arg0)
         {
            ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
                  .vibrate(VIB_DUR);
            actvCon.finish();
         }
      });
   }
}
