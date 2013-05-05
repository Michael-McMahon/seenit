package com.mmcmahon.rtv2go;

import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.channels.Channel;
import com.mmcmahon.rtv2go.channels.ChannelsAdapter;
import com.mmcmahon.rtv2go.channels.ChannelsJSInterface;
import com.mmcmahon.rtv2go.dialogs.InterfaceDialog;
import com.mmcmahon.rtv2go.dialogs.MenuDialog;
import com.mmcmahon.rtv2go.dialogs.RemoveDialog;
import com.mmcmahon.rtv2go.video.VideoPlayer;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * The Activity in this app which creates and configures all views.
 * This class serves as a central hub for communication between the major view
 * centers. These view centers are: the video player webview, the touch screen
 * interface overlay, and the MENU button screen. Summer 2012
 * 
 * @author Michael McMahon
 */
public class ACTV_VideoPlayer extends Activity
{
   //Shared Preferences keys
   private static final String SP_DEF_CHAN = "default-channels";
   private static final String SP_DEF_CHAN_NAMES = "default-channels-names";

   private final String TAG = "#ACTV_VideoPlayer#";

   //Extended WebView plays videos
   private VideoPlayer videoPlayer;
   //ViewGroup which manages the interface
   private VideoInterface videoIface;
   //Video player interface is presented in a transparent dialog
   private InterfaceDialog ifaceDialog;

   private boolean hasLoggedIn = false;

   private SharedPreferences defaultChannels;
   //Javascript interface manages subreddit channels
   private ChannelsJSInterface chanInterface = null;
   //Manages the list of subreddit channels
   private ChannelsAdapter chanAdapter;
   private MenuDialog menuPopup;
   //Position in thumbnails list of last video played
   private int playPos = 0;

   public void onCreate(Bundle sis)
   {
      super.onCreate(sis);
      
      //Created Adapter class for managing subreddit channels      
      chanAdapter = new ChannelsAdapter(this);
      
      //Get default channels from Shared Preferences
      defaultChannels = getSharedPreferences(SP_DEF_CHAN, 0);
      
      // Now set up views
      menuPopup = new MenuDialog(this);
      videoPlayer = new VideoPlayer(this);
      setContentView(videoPlayer.getLayout());                            
      createInterface(videoPlayer);
   }

   public void closeMenu()
   {
      if (menuPopup != null)
      {
         menuPopup.hide();
      }
      if(ifaceDialog != null)
      {
         ifaceDialog.show();
      }
   }

   public void openMenu()
   {
      if (menuPopup != null)
      {
         menuPopup.show();
      }
      if(ifaceDialog != null)
      {
         ifaceDialog.hide();
      }
   }

   
   private void createInterface(VideoPlayer vp)
   {
      ifaceDialog = new InterfaceDialog(this, vp);
      videoIface = ifaceDialog.getInterfaceView();
      ifaceDialog.show();
   }

   public void redditLogout()
   {
      hasLoggedIn = false;
      setLoggedIn(false);
      
      videoIface.sendLoginResult(0, "");
      RedditAPI.logout();
   }

   public void login(String u, String p)
   {
      redditLogin(u, p);
   }

   private void redditLogin(final String u, final String p)
   {
      new Thread(new Runnable()
      {
         public void run()
         {
            hasLoggedIn = RedditAPI.login(u, p);
            setLoggedIn(hasLoggedIn);
            if (hasLoggedIn)
            {
               // Clear out current channel list and load user's channels
               RedditAPI.getUserChannels(ACTV_VideoPlayer.this);
               videoIface.sendLoginResult(1, "");
            } else
            {
               videoIface.sendLoginResult(-1, RedditAPI.getError());
               Log.e(TAG, "Login failed: " + RedditAPI.getError());
            }
         }
      }).run();
   }

   public void setLoggedIn(boolean loggedIn)
   {
      if (videoIface != null)
      {
         videoIface.setLoggedIn(loggedIn);
      }
      menuPopup.setLoggedIn(loggedIn);
   }

   public boolean isLoggedIn()
   {
      return hasLoggedIn;
   }

   /* Loads the channel name provided in the video player */
   public void loadChannel(final Channel channel)
   {
      // currChan = channel;
      runOnUiThread(new Runnable()
      {
         public void run()
         {
            // Make thumbnails list empty
            videoIface.hideNoVideosViews();
            videoIface.addThumbnails();
         }
      });
      videoPlayer.loadChannel(channel);
   }

   public void playVideo(int pos)
   {
      playPos = pos;
      videoPlayer.playVideo(pos);
   }

   public void playNext()
   {
      videoPlayer.playNext();
      playPos++;
   }

   public void playPrev()
   {
      videoPlayer.playPrev();
      playPos--;
   }

   /* Return the postion in the thumbnails list of the last video played */
   public int getPlayPos()
   {
      return playPos;
   }

   /* Add a new channel to top of list, optionally load it */
   public void pushChannel(String ch, boolean load)
   {
      Channel newChan = new Channel(ch, true);
      // Ready new channel to show a one-time, added-to-list animation
      // newChan.setAnimation(Channel.createAddAnimation(this), false);
      chanAdapter.addChannel(newChan, 0);
      videoPlayer.addChannel(0);
      if (load)
      {
         loadChannel((Channel) chanAdapter.getItem(0));
      }
   }

   public void removeJSChannel(int ch)
   {
      videoPlayer.removeChannel(ch);
   }

   public void addJSChannel(int ch)
   {
      videoPlayer.addChannel(ch);
   }

   private ChannelsJSInterface getDefaultChannels()
   {
      String chanNames = defaultChannels.getString(SP_DEF_CHAN_NAMES, null);

      // Create default channels shared preferences, if it does not exist
      // already
      if (chanNames == null)
      {
         String chans[] = getResources().getStringArray(
               R.array.packaged_channel_names);
         int len = chans.length;
         // Save the packaged channels
         for (int i = 0; i < len; spSaveChannel(chans[i++]))
            ;
         // Retrieve the token delimited string list
         chanNames = defaultChannels.getString(SP_DEF_CHAN_NAMES, null);
      }

      // Create channel set from a '/' separated string of channel names
      return new ChannelsJSInterface(chanAdapter, chanNames);
   }

   public ChannelsJSInterface getChannelInterface()
   {
      if (chanInterface == null)
      {
         chanInterface = getDefaultChannels();
      }

      return chanInterface;
   }

   /* Add a new channel to the shared preferences default list */
   public void spSaveChannel(String name)
   {
      // Get the current list
      String curr = defaultChannels.getString(SP_DEF_CHAN_NAMES, "");
      // Add '/' delimited entry
      if (!curr.contains(name))
      {
         defaultChannels.edit().putString(SP_DEF_CHAN_NAMES, curr + "/" + name)
               .commit();
      }
   }

   /* Remove a channel from the shared preferences default list */
   public void spDeleteChannel(String name)
   {
      String curr = defaultChannels.getString(SP_DEF_CHAN_NAMES, "");
      curr = curr.replace("/" + name + "/", "/");
      defaultChannels.edit().putString(SP_DEF_CHAN_NAMES, curr).commit();
   }

   public ChannelsAdapter getChannelsAdapter()
   {
      if (chanAdapter == null)
      {
         chanAdapter = new ChannelsAdapter(this);
      }

      return chanAdapter;
   }

   public boolean hideSoftKeyboard(final IBinder winToken)
   {
      Configuration config = getResources().getConfiguration();
      boolean wasShowing = // True if the softkeyboard was showing
      (config.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO)
            && (config.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO);
      /* TODO: ASSUMED that config should have UNDEFINED on devices which don't
       * have keyboards or which can't hide keyboards.*/
      final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      runOnUiThread(new Runnable()
      {
         public void run()
         {
            imm.hideSoftInputFromWindow(winToken, 0, null);
         }
      });
      return wasShowing;
   }

   public void promptNoVideos(final String chan)
   {
      runOnUiThread(new Runnable()
      {
         public void run()
         {
            videoIface.noVideosFound(chan);
         }
      });
   }

   public void checkIfEmpty(int i, String name)
   {
      videoPlayer.checkIfEmpty(i, name);
   }

   /* No fading animation, just set visibility */
   public void showRemove(final boolean show, final RemoveDialog rd)
   {
      runOnUiThread(new Runnable()
      {
         public void run()
         {
            if (show)
            {
               // Log.e(TAG, "Hiding iface");
               ifaceDialog.hide();
               rd.show();
            } else
            {
               // Log.e(TAG, "Showinging iface");
               rd.hide();
               ifaceDialog.show();
            }
         }
      });
   }

   public void setLoadProgress(int p)
   {
      videoIface.setLoadProgress(p);
   }

   public void thumbsLoaded()
   {
      videoIface.thumbsLoaded();
   }
}
