package com.mmcmahon.rtv2go.channels;

import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.R.anim;

import android.content.Context;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * Provides name and location of a subreddit. Also defines an animation for
 * when the channel appears in a list. Custom animations can also be plugged 
 * in. A one time flag can be set on construction, and accessed via the isNew() 
 * method. The flag will be permanently flipped after the first call to isNew().
 * 
 * Summer 2012
 * @author Michael McMahon
 */
public class Channel
{
   private static final String TAG = "#Channel#";
   private final String name;
   private final String feed;
   // Url of subreddit's header image
   // private String logo = "logourlhere";
   private Animation anim;
   private boolean defAnim = false;
   private boolean isNew = false;

   public static enum HasVideos
   {
      NOT_SURE, EMPTY, NOT_EMPTY
   };

   private HasVideos empty = HasVideos.NOT_SURE;

   public Channel(String n)
   {
      this(n, "/r/" + n + "/");
   }

   /**
    * For search-added channels, which show a green flash as they appear.
    * @param n Name of channel
    * @param showGreen True if channel was added by searching
    */
   public Channel(String n, boolean searchChan)
   {
      this(n);
      isNew = searchChan;//isNew can only be set to true here!
   }
   
   public Channel(String n, String f)
   {
      name = n;
      feed = f;
      readyFlashIn();// Create an animation to run when this channel appears in
                     // a list
   }

   /* Returns true the first time it's called, and false every time after that. */
   public boolean isNew()
   {
      if (isNew)// Can happen only ONE TIME in this object's lifetime
      {
         isNew = false;
         return true;
      }
      return false;
   }

   public String getName()
   {
      return name;
   }

   public String getFeed()
   {
      return feed;
   }

   public HasVideos isEmpty()
   {
      return empty;
   }

   public void setEmpty(HasVideos e)
   {
      empty = e;
   }

   /*
    * Get any animation prepared for displaying this channel. Will set
    * animationReady to false
    */
   public Animation getAnimation()
   {
      Animation ret = anim;

      if (!defAnim)// Reset animation, only show current animation one time.
      {
         readyFlashIn();
      }

      return ret;
   }

   /**
    * Ready an animation which causes a view to light up for a moment.
    */
   private void readyFlashIn()
   {
      anim = new AlphaAnimation((float) 1.0, (float) 0.3);
      anim.setDuration(250);
      anim.setRepeatMode(Animation.REVERSE);
      anim.setRepeatCount(1);
      defAnim = true;
   }

   /*
    * Set a new animation to be shown on views displaying this object. Animation
    * can be set as a one time occurance (i.e. Channel added to list), or the
    * default recurring behavior (i.e. Channel displayed in list).
    */
   public void setAnimation(Animation a, boolean isDefault)
   {
      anim = a;
      defAnim = isDefault;
   }

   /*
    * Forget any animation previously set, and return to using the original
    * default animation.
    */
   public void clearAnimation()
   {
      readyFlashIn();
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }

      if (!(o instanceof Channel))
      {
         return false;
      }

      Channel c = (Channel) o;

      return c.getFeed() == feed && c.getName() == name;
   }

   public static Animation createAddAnimation(Context actvCon)
   {
      Animation addAnim = AnimationUtils.loadAnimation(actvCon,
            R.anim.fade_inout);

      return addAnim;
   }
}
