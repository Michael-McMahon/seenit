package com.mmcmahon.rtv2go;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Vibrator;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.Timer;
import java.util.TimerTask;

import com.mmcmahon.rtv2go.channels.ChannelsAdapter;
import com.mmcmahon.rtv2go.channels.HorizontalListView;
import com.mmcmahon.rtv2go.thumbnails.ThumbnailContent;
import com.mmcmahon.rtv2go.thumbnails.ThumbnailsAdapter;
import com.mmcmahon.rtv2go.ui.VerticalButton;
import com.mmcmahon.rtv2go.video.VideoPlayer;

/**
 * The root view for just about every interactive element in the app.
 * Should appear as an overlay on top of the html video content. This view 
 * houses interfaces for interacting with the underlaying video player, such as
 *  subreddit selection, thumbnail browsing, vote casting, and much more...
 *  
 *  Summer 2012
 * @author Michael McMahon
 */
public class VideoInterface extends RelativeLayout
{
   //private final String TAG = "#VideoInterface#";
   private final int ANIM_MS = 750;// Duration of button animations
   private static final int UP = 1, DOWN = -1, CANCEL = 0;
   private static final int HIDE_DELAY = 10000;// Milliseconds to show interface
   private final static String REDDIT_URL = "http://www.reddit.com/";
   
   private ACTV_VideoPlayer actvCon;
   private ImageButton upBtn, downBtn, nextBtn, prevBtn, searchBtn;
   private Gallery thumbnails;
   private VideoPlayer player;
   private View noVidsViews;//, topPanel;

   private ChannelsAdapter chanAdapter;
   private HorizontalListView channelList;
   private EditText subredditSearch, usernameEntry, passwordEntry;
   private int voteState = 0;
   private boolean hidden = false;//, topHidden = false, sideHidden = false;
   private Timer hideTimer = new Timer();
   protected boolean drawersOpen;
   private ThumbnailsAdapter tnsAdapter;
   private TextView titleDisplay;
   private SlidingDrawer loginDrawer;
   private View loginBtn;
   private TextView scoreDisplay;
   private OnItemSelectedListener thumbSelected;
   //private View sidePanel;
   private OnClickListener linkToReddit;
   
   public VideoInterface(Context context)
   {
      super(context);
//      actvCon = (ACTV_VideoPlayer) getContext();
//      onConstruct();
   }

   public VideoInterface(Context context, AttributeSet attrs)
   {
      super(context, attrs);
     /* actvCon = (ACTV_VideoPlayer) getContext();
      onConstruct();*/
   }

   public VideoInterface(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
//      actvCon = (ACTV_VideoPlayer) getContext();
//      onConstruct();
   }

   /*
   private void onConstruct()
   {
      if (isInEditMode())
      {
         return;
      }
      player = (VideoPlayer) ((Activity) actvCon)
            .findViewById(R.id.playerVideo);
      this.setVideoPlayer(player);
   }*/
   
   /* UI reactions to next or previous play button press. */
   private void onNextOrPrev(int pos, ImageButton iBtn)
   {
      AlphaAnimation fadeAnim = new AlphaAnimation((float) 1.0, (float) 0.3);
      fadeAnim.setDuration(ANIM_MS);
      fadeAnim.setRepeatMode(Animation.REVERSE);
      fadeAnim.setRepeatCount(1);
      iBtn.startAnimation(fadeAnim);
      thumbnails.onKeyDown(pos, null);
   }

   private void setButtonListeners()
   {
      // Next Button
      nextBtn = (ImageButton) this.findViewById(R.id.lsifaceNext);
      nextBtn.setOnClickListener(new OnClickListener()
      {
         public void onClick(View v)
         {
            int pos = actvCon.getPlayPos(), count = tnsAdapter.getCount();
            if (++pos < count)// Do nothing if positioned at last video
            {
               onNextOrPrev(KeyEvent.KEYCODE_DPAD_RIGHT, (ImageButton) v);// UI
                                                                          // animations
               actvCon.playNext();// Send call to tv.js
            } else
            // At last video
            {
               ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
                     .vibrate(250);// Shake rattle and roll thumbnails gallery
               thumbnails.setSelection(count - 1);
            }
         }
      });
      // Previous Button
      prevBtn = (ImageButton) this.findViewById(R.id.lsifacePrev);
      prevBtn.setOnClickListener(new OnClickListener()
      {
         public void onClick(View v)
         {
            int pos = actvCon.getPlayPos();
            if (pos > 0)// Do nothing if at first video
            {
               onNextOrPrev(KeyEvent.KEYCODE_DPAD_LEFT, (ImageButton) v);// UI
                                                                         // animations
               actvCon.playPrev();// Send call to tv.js
            } else
            // At first video
            {// Shake rattle and roll thumbnails gallery
               ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
                     .vibrate(250);
               thumbnails.setSelection(0);
            }
         }
      });
      // Up Button
      upBtn = (ImageButton) this.findViewById(R.id.lsifaceUp);
      upBtn.setOnClickListener(new OnClickListener()
      {
         public void onClick(View v)
         {
            if (actvCon.isLoggedIn())// Can't vote unless logged in
            {
               castVote(UP);
            } else
            {
               Toast loginToast = Toast
                     .makeText(actvCon, "Login to vote", Toast.LENGTH_LONG);
               loginToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                     0, 0);
               loginToast.show();
               loginDrawer.open();
            }
         }
      });
      // Down Button
      downBtn = (ImageButton) this.findViewById(R.id.lsifaceDown);
      downBtn.setOnClickListener(new OnClickListener()
      {
         public void onClick(View v)
         {
            if (actvCon.isLoggedIn())// Can't vote unless logged in
            {
               castVote(DOWN);
            } else
            {
              Toast loginToast = Toast
                     .makeText(actvCon, "Login to vote", Toast.LENGTH_LONG);
               loginToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                     0, 0);
               loginToast.show();
               loginDrawer.open();
            }
         }
      });
      
      //Open a reddit post in an external browser on a click
      linkToReddit = new OnClickListener(){
         public void onClick(View arg0)
         {
            if(thumbnails == null)
            {
               return;
            }
            int vPos = thumbnails.getSelectedItemPosition();
            final String vId = ((ThumbnailContent) tnsAdapter.getItem(vPos)).getId();
            new Thread(){
               public void run()
               {
                     Intent launchBrowser = 
                        new Intent(Intent.ACTION_VIEW, Uri.parse(REDDIT_URL+vId));
                     actvCon.startActivity(launchBrowser);
               }
            }.run();
         }};
      titleDisplay.setOnClickListener(linkToReddit);
      
      configLogin();
      configSearch();
   }

   private void castVote(int vote)
   {
      // Perform UI changes
      if ((voteState > 0 && vote > 0) || (voteState < 0 && vote < 0))
      {
         vote = CANCEL;
      }
      setVoteState(vote);

      // Now logical changes
      int vPos = thumbnails.getSelectedItemPosition();
      final ThumbnailContent tc = (ThumbnailContent) tnsAdapter.getItem(vPos);
      //final int oldState = tc.getVoteState();
      tc.setVoteState(vote);// Vote gets remembered

      // Send new vote to reddit
      new Thread(new Runnable()
      {
         public void run()
         {
            RedditAPI.vote(voteState, tc.getId());
            /*This is laggy, improve before including vote response
            String result = RedditAPI.vote(voteState, tc.getId());
            if(result.length() > 0)//If errored, revert to old state
            {
               Toast voteErr = Toast.makeText(actvCon, "Voting Failed:"+ result, Toast.LENGTH_SHORT);
               voteErr.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
               voteErr.show();
               tc.setVoteState(oldState);
            }*/
         }
      }).run();
   }

   private void configLogin()
   {
      // Get views
      loginDrawer = (SlidingDrawer) findViewById(R.id.lsifaceLoginDrawer);
      usernameEntry = (EditText) findViewById(R.id.lsifaceUsername);
      passwordEntry = (EditText) findViewById(R.id.lsifacePassword);
      loginBtn = findViewById(R.id.lsifaceLoginBtn);

      // Set listeners on views
      VerticalButton loginHandle = (VerticalButton) findViewById(R.id.lsifaceLoginHandle);
      loginDrawer.setOnDrawerCloseListener(new LoginClosed(loginHandle));
      loginDrawer.setOnDrawerOpenListener(new LoginOpened(loginHandle));
      loginBtn.setOnClickListener(new OnClickListener()
      {
         public void onClick(View arg0)
         {
            attemptLogin();
         }
      });
      usernameEntry.setOnKeyListener(new UnameKeyListener());
      passwordEntry.setOnKeyListener(new OnKeyListener()
      {
         public boolean onKey(View v, int keycode, KeyEvent event)
         {
            VideoInterface.this.hideInterface(false);// Keep interface up while
                                                     // typing
            switch (keycode)
            {// Login on enter key
            case KeyEvent.KEYCODE_ENTER:
               attemptLogin();
               return true;
            default:
               return false;
            }
         }
      });

      // Hide login prompt if already logged in
      if (actvCon.isLoggedIn())
      {
         loginDrawer.setVisibility(View.GONE);
      }
   }


   private void attemptLogin()
   {
      // Replace login fields with loading animation
      findViewById(R.id.lsifaceLoginViews).setVisibility(View.GONE);
      findViewById(R.id.lsifaceLoginLoading).setVisibility(View.VISIBLE);

      // Attempt to login
      actvCon.login(usernameEntry.getText().toString(), passwordEntry.getText()
            .toString());
      passwordEntry.setText("");// Clear password text
      /*
       * Login request will happen in separate thread; this method returns and
       * login result is listened for concurrently.
       */
   }

   /**
    * Display a login result message.
    * 
    * @param result
    *           Pass a positive value for successful login, negative for failed
    *           login, 0 for a log out.
    */
   public void sendLoginResult(int result, String msg)
   {
      Toast confirm;
      String message;

      if (result > 0)// Login succeeded
      {
         message = "Logged in as: " + usernameEntry.getText().toString();
      } else if (result == 0)
      {
         message = "Logged out";
      } else
      {
         message = "Login failed: "+ msg;
      }
      confirm = Toast.makeText(actvCon, message, Toast.LENGTH_SHORT);
      confirm.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
      confirm.show();
      ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
            .vibrate(250);
   }

   public void setLoggedIn(boolean loggedIn)
   {
      if (loginDrawer == null)
      {
         loginDrawer = (SlidingDrawer) findViewById(R.id.lsifaceLoginDrawer);
      }

      // Replace login fields with loading animation
      findViewById(R.id.lsifaceLoginViews).setVisibility(View.VISIBLE);
      findViewById(R.id.lsifaceLoginLoading).setVisibility(View.GONE);

      if (loggedIn && loginDrawer.getVisibility() != View.GONE)
      {
         loginDrawer.setVisibility(View.GONE);
      } else if (!loggedIn && loginDrawer.getVisibility() != View.VISIBLE)
      {
         loginDrawer.setVisibility(View.VISIBLE);
      }
   }

   /**
    * Listens for keys and scrolls the login view accordingly. 
    * TODO: Determine cause of twin keyevents.
    */
   private class UnameKeyListener implements OnKeyListener
   {
      // Pressing ENTER will casue two identical events, like spawning twins.
      // This listener will only react to the second(twin) event
      private boolean twin = true;// TODO: Stop having to use this work-around

      public boolean onKey(View v, int keycode, KeyEvent event)
      {
         VideoInterface.this.hideInterface(false);// Keep interface up while
                                                  // typing
         switch (keycode)
         {
         case KeyEvent.KEYCODE_ENTER:
            twin = !twin;// Will flip to true on second ENTER keyevent
            if (twin)
            {// Ignored on first call to onKey with ENTER keyevent
               hideSoftKeyboard();
               passwordEntry.requestFocus(View.FOCUS_DOWN);
            }
            return true;

         default:
            return false;
         }
      }
   }

   public void hideNoVideosViews()
   {
      if (noVidsViews.getVisibility() != View.GONE)// Hide no videos view
      {
         noVidsViews.setVisibility(View.GONE);
      }
   }

   /**
    * This method is to be called once the video player view has been created.
    * This interface will hook up to the provided VideoPlayer view.
    */
   public void setVideoPlayer(VideoPlayer vp)
   {
      player = vp;
      actvCon = (ACTV_VideoPlayer) vp.getContext();
      // Get our views...
      // The view for when no videos are found
      noVidsViews = findViewById(R.id.playerNoVideosViews);
      configChannels();// Channels configged
//      configSearch();// Setup search box after channels
      addThumbnails();// Thumbnails configged
      setButtonListeners();// (up down prev next) buttons configged
   }

   @Override
   public boolean onInterceptTouchEvent(MotionEvent ev)
   {
      this.hideInterface(false);//Triggers a hide/show of the interface
      return false;
   }

   @Override
   public boolean onTouchEvent(MotionEvent ev)
   {
      super.onTouchEvent(ev);
      if (player != null)
      {//So that html video can recieve touch events
         player.onTouchEvent(ev);
      }
      return true;
   }

   private void configChannels()
   {
      chanAdapter = actvCon.getChannelsAdapter();
      channelList = (HorizontalListView) findViewById(R.id.lsifaceChannels);
      channelList.setAdapter(chanAdapter);
   }

   private class OnSearchClick implements OnClickListener
   {
      public void onClick(View v)
      {
         // Show text entry if not already visible
         if (subredditSearch.getText().length() == 0)// .getScrollX() > 0)
         {
            channelList.scrollTo(0);
            //subredditSearch.requestFocus();
//            channelList.requestChildRectangleOnScreen(subredditSearch, 
//                  new Rect(0,0,0,0), true);
         } else// Attempt to find a subreddit with the name entered
         {
            addSearchChannel();
         }
      }
   }

   private void configSearch()
   {
      subredditSearch = chanAdapter.getSearchBox();
      searchBtn = (ImageButton) findViewById(R.id.sboxSearchBtn);

      subredditSearch.setOnKeyListener(new OnKeyListener()
      {
         public boolean onKey(View v, int keyCode, KeyEvent event)
         {
            //Keep interface up while typing
            VideoInterface.this.hideInterface(false);
            if (keyCode == KeyEvent.KEYCODE_ENTER)
            {
               hideSoftKeyboard();
               searchBtn.performClick();
               return true;
            }
            return false;
         }
      });
      searchBtn.setOnClickListener(new OnSearchClick());
   }

   /* Read any input in search field and add it as a channel.*/
   private void addSearchChannel()
   {
      String search = subredditSearch.getText().toString();
      Animation spin = AnimationUtils.loadAnimation(actvCon,
            R.anim.clockwise_spin);
      // Show animation on search icon
      searchBtn.startAnimation(spin);

      // Load the new channel and add it to the list
      if (search.length() > 0)
      {
         scrollChannelsTop();// Scroll list to show new channel entry
         actvCon.pushChannel(
               subredditSearch.getText().toString()
                     .replaceAll("[^a-zA-Z0-9_]*", ""), true);
         subredditSearch.setText("");
      }
   }

   /**
    * Called when the BACK button is pressed.
    */
   public void backPressed()
   {
      // Close the login drawer and return
      if (loginDrawer.isOpened())
      {
         loginDrawer.close();
         return;
      }
      // Hide this interface and return;
      hideInterface(true);
   }

   private void scrollChannelsTop()
   {
      channelList.scrollTo(0, 0);
   }

   /**Notify this interface that thumbnail images have loaded*/
   public void thumbsLoaded()
   {
      //Make sure first thumbnail is now selected
      thumbSelected
            .onItemSelected(thumbnails,
                  tnsAdapter.getView(0, null, thumbnails), 0,
                  tnsAdapter.getItemId(0));
   }

   private class OnThumbSelected implements OnItemSelectedListener
   {
      private ImageView selectedImgView = null;
      android.widget.Gallery.LayoutParams bigLps, oldLps;

      public OnThumbSelected()
      {
         int tw = ThumbnailsAdapter.THUMB_WIDTH;
         int th = ThumbnailsAdapter.THUMB_HEIGHT;
         bigLps = new Gallery.LayoutParams(tw + (tw/4), th + (th/4));
      }

      public void onItemSelected(AdapterView<?> av, View v, int pos, long id)
      {
         // Update the title display
         ThumbnailContent tc = (ThumbnailContent) tnsAdapter.getItem(pos);

         if (titleDisplay != null)
         {
            titleDisplay.setText(tc.getTitle());
         }

         // Update score display
         if (scoreDisplay != null)
         {
            scoreDisplay.setText(Integer.toString(tc.getScore()));
         }
         // Update vote state
         setVoteState(tc.getVoteState());

         if (selectedImgView != null)// If a view has previously been
                                     // highlighted...
         {
            selectedImgView.setAlpha(127);// Return previous selection to
                                          // non-highlighted state
            selectedImgView.setLayoutParams(oldLps);
         }
         if (v != null)
         {
            // Update to latest selection
            selectedImgView = (ImageView) v;
            oldLps = (android.widget.Gallery.LayoutParams) selectedImgView
                  .getLayoutParams();
            // Highlight new selection
            selectedImgView.setAlpha(255);
            selectedImgView.setLayoutParams(bigLps);
         } else
         {
            selectedImgView = null;
         }
      }

      public void onNothingSelected(AdapterView<?> av)
      {// Set title to first video
         onItemSelected(av, null, 0, 0);
      }
   }

   /**
    * Add a scrolling list of video thumbnail images from the current channel.
    */
   public void addThumbnails()
   {
      // Get thumbnail views
      thumbnails = (Gallery) findViewById(R.id.lsifaceThumbnails);
      titleDisplay = (TextView) findViewById(R.id.lsifaceThumbTitle);
      scoreDisplay = (TextView) findViewById(R.id.lsifaceScore);
      tnsAdapter = new ThumbnailsAdapter(actvCon);
      thumbnails.setAdapter(tnsAdapter);
      player.setJSIThumbAdapter(tnsAdapter);
      
      // On thumbnail click...
      thumbnails.setOnItemClickListener(new OnItemClickListener()
      {
         public void onItemClick(AdapterView<?> av, View v, int pos, long id)
         {// Listener sends call to playVideo for the index of the thumbnail
          // clicked on
            actvCon.playVideo(pos);
         }
      });

      // On thumbnail highlighted...
      thumbSelected = new OnThumbSelected();
      thumbnails.setOnItemSelectedListener(thumbSelected);
   }

   /**
    * Set vote button states based on polarity of parameter: (state>0) = up
    * vote, (state==0) = no vote, (state<0) = down vote. This method strictly
    * acts on the UI, and does not send any request to reddit.
    */
   public void setVoteState(Integer state)
   {
      int score;

      try
      // to get the score
      {
         score = Integer.parseInt(scoreDisplay.getText().toString());
      } catch (NumberFormatException e)
      {
         score = 0;
      }

      // Animaitons for fading the arrows
      AlphaAnimation fadeIn = new AlphaAnimation((float) 0.3, (float) 1.0);
      AlphaAnimation fadeOut = new AlphaAnimation((float) 1.0, (float) 0.3);
      fadeIn.setFillAfter(true);
      fadeOut.setFillAfter(true);
      fadeIn.setDuration(ANIM_MS);
      fadeOut.setDuration(ANIM_MS);

      if (state == 0 && voteState == 0)// Special case for initial no vote
                                       // setting
      {
         downBtn.startAnimation(fadeOut);
         upBtn.startAnimation(fadeOut);
         voteState = CANCEL;
         return;
      }

      if (state == CANCEL)// Cancel vote
      {
         if (voteState < 0)// Undo down vote
         {
            downBtn.startAnimation(fadeOut);
            score++;
         }
         if (voteState > 0)// Undo up vote
         {
            upBtn.startAnimation(fadeOut);
            score--;
         }
      } else if (state < 0)
      {
         if (voteState >= 0)// Down vote
         {
            downBtn.startAnimation(fadeIn);
            score--;
         }
         if (voteState > 0)// Previously up voted
         {
            upBtn.startAnimation(fadeOut);
            score--;
         }
      } else if (state > 0)
      {
         if (voteState <= 0)// Up vote
         {
            upBtn.startAnimation(fadeIn);
            score++;
         }
         if (voteState < 0)// Previously down voted
         {
            downBtn.startAnimation(fadeOut);
            score++;
         }
      }

      // Update the score display (superficially, on the assumption that the
      // vote will eventually get counted)
      scoreDisplay.setText(Integer.toString(score));
      voteState = state;
   }

   /* Shows or hides the entire top panel of the interface */
   /*
   private void hideTopPanel(boolean hide)
   {
      if (topHidden == hide)// Already in desired state
      {
         return;// Don't run animation
      }

      // Flip the state and run a show/hide animation
      topHidden = !topHidden;
      float a1 = (float) (hide ? 1.0 : 0.0);
      float a2 = (float) (hide ? 0.0 : 1.0);
      AlphaAnimation hideAnim = new AlphaAnimation(a1, a2);
      hideAnim.setFillAfter(true);
      hideAnim.setDuration(ANIM_MS);

      if (topPanel == null)
      {
         topPanel = findViewById(R.id.lsifaceTopPanel);
      }
      topPanel.startAnimation(hideAnim);
   }
*/
   /* Shows or hides the entire side panel of the interface */
  /* private void hideSidePanel(boolean hide)
   {
      if (sideHidden == hide)// Already in desired state
      {
         return;// Don't run animation
      }

      // Flip the state and run a show/hide animation
      sideHidden = !sideHidden;
      float a1 = (float) (hide ? 1.0 : 0.0);
      float a2 = (float) (hide ? 0.0 : 1.0);
      AlphaAnimation hideAnim = new AlphaAnimation(a1, a2);
      hideAnim.setFillAfter(true);
      hideAnim.setDuration(ANIM_MS);

      if (sidePanel == null)
      {
         sidePanel = findViewById(R.id.lsifaceBtns);
      }
      sidePanel.startAnimation(hideAnim);
   }
*/
   
   /* Sliding drawer listeners */
   private class LoginClosed implements OnDrawerCloseListener
   {
      private Button loginHandle;

      public LoginClosed(Button handle)
      {
         loginHandle = handle;
      }

      public void onDrawerClosed()
      {
         loginHandle.setText(R.string.login);
      }
   }
   private class LoginOpened implements OnDrawerOpenListener
   {
      private Button loginHandle;

      public LoginOpened(Button handle)
      {
         loginHandle = handle;
      }

      public void onDrawerOpened()
      {
         loginHandle.setText(R.string.close_login);
      }
   }

   public void sendKeyToSearch(int keyCode, KeyEvent event)
   {
      subredditSearch.onKeyDown(keyCode, event);
   }

   public void filterEmptyChannels(boolean hideEmpties)
   {
      if (hideEmpties)
      {
         chanAdapter.hideEmptyChannels();
      } else
      {
         chanAdapter.showEmptyChannels();
      }
   }

   /**
    * Called when no videos for found in a channel. Alert user and ask if they
    * want to filter out all empty channels.
    */
   public void noVideosFound(String chan)
   {
      // Setup the view
      ((TextView) findViewById(R.id.playerEmptyPrompt)).setText(actvCon
            .getResources().getString(R.string.emptyVideosPrompt)
            + " "
            + chan
            + "!"); // = "No videos found in <subreddit>!"
      CheckBox filterCheck = (CheckBox) findViewById(R.id.playerFilterCheck);
      filterCheck.setChecked(false);
      filterCheck.setOnCheckedChangeListener(new OnCheckedChangeListener()
      {
         public void onCheckedChanged(CompoundButton cb, boolean isChecked)
         {
            filterEmptyChannels(isChecked);
         }
      });
      if (noVidsViews.getVisibility() != View.VISIBLE)// Show the view
      {
         noVidsViews.setVisibility(View.VISIBLE);
      }
   }

   /**
    * Hides or shows the entire interface. If interface is hidden, it becomes
    * visible and a timer is set to call this method is again. If the interface
    * is not hidden, this method examines the force parameter to determine if
    * it's being forced to hide now or should delay hiding by resetting the hide
    * timer.
    * 
    * @param forced
    *           True to hide immediately, false to hide after a duration.
    */
   public void hideInterface(boolean forced)
   {
      if (hidden)
      {
         showForTime();// Set timer to hide view after a duration
      } else if (!forced)//Don't hide now unless forced to
      {
         showForTime();
         return;
      }
      // Run a fade in/out animation on the interface
      float a1 = (float) (hidden ? 0.0 : 1.0);
      float a2 = (float) (hidden ? 1.0 : 0.0);
      hidden = !hidden; // Flip hidden state
      AlphaAnimation hideAnim = new AlphaAnimation(a1, a2);
      hideAnim.setFillAfter(true);
      hideAnim.setDuration(ANIM_MS);
      this.startAnimation(hideAnim);
   }

   /* Called when the hide interface timer expires */
   private class HideTask extends TimerTask
   {
      public void run()
      {
         actvCon.runOnUiThread(new Runnable()
         {
            public void run()
            {
               // Passing true forces the method to hide the interface now,
               // rather than later (after another timer duration).
               if (!hidden)
               {
                  hideInterface(true);
               }
            }
         });
      }
   }

   /* Schedule this interface to hide after a delay */
   private void showForTime()
   {
      // Remove any pending animations
      hideTimer.cancel();
      hideTimer = new Timer();
      // Schedule the new animation
      hideTimer.schedule(new HideTask(), HIDE_DELAY);// Always use a new Timer
                                                     // and TimerTask to avoid
                                                     // an illegal state
   }

   private void hideSoftKeyboard()
   {
      actvCon.hideSoftKeyboard(getWindowToken());
   }

   public void setLoadProgress(int p)
   {
      titleDisplay.setText("loading... %"+ p);
   }
}
