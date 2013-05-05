package com.mmcmahon.rtv2go.video;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.RedditAPI;
import com.mmcmahon.rtv2go.channels.Channel;
import com.mmcmahon.rtv2go.thumbnails.ThumbnailsAdapter;

import android.content.Context;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebSettings;

/**
 * A WebView which loads html+javasript derived from reddit.tv. Provides an API
 * for java classes to interact with the tv.js script.
 * 
 * Summer 2012
 * 
 * @author Michael McMahon
 */
public class VideoPlayer extends HTML5WebView
{
   // private final String TAG = "#VideoPlayer#";
   private final String RTV2GO_HTML_LOC = "file:///android_asset/rtv-video/index.html";// Modified
                                                                                       // reddit.tv
                                                                                       // source
                                                                                       // for
                                                                                       // this
                                                                                       // app
   private ACTV_VideoPlayer actvCon;
   private JsInterface jsi;

   public VideoPlayer(Context context)
   {
      super(context);
      onCreate(context);
   }

   public VideoPlayer(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      onCreate(context);
   }

   public VideoPlayer(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);
      onCreate(context);
   }

   @Override
   /**
    * Fire initial loading of html here!(Should happen once[every time screen changes size])
    * TODO: Seems hackish; find a better place to call vpInit?
    */
   protected void onSizeChanged(int nW, int nH, int oW, int oH)
   {
      super.onSizeChanged(nW, nH, oW, oH);
      vpInit();
   }

   @Override
   public boolean onKeyDown(int keycode, KeyEvent event)
   {
      return false;
   }

   /* Not an override! */
   private void onCreate(Context c)
   {
      actvCon = (ACTV_VideoPlayer) c;
      jsi = new JsInterface(actvCon);
   }

   public void vpInit()
   {
      RedditAPI.setUserAgent(this.getSettings().getUserAgentString());

      // Javascript will size embeded videos to fit device's screen
      jsi.setHeight(this.getHeight());
      jsi.setWidth(this.getWidth());

      // Allow communcation with js in webview through two interface classes:
      //rtv2go handles general, non-channel related tasks
      addJavascriptInterface(jsi, "rtv2go");
      //rtv2goChannels handles channel related tasks
      addJavascriptInterface(actvCon.getChannelInterface(), "rtv2goChannels");
      //Load html which runs tv.js
      loadUrl(RTV2GO_HTML_LOC);
      requestFocus();
   }

   @Override
   /* Override to prevent scrolling */
   protected void onScrollChanged(int ch, int cv, int oh, int ov)
   {
      scrollTo(0, 0);// Don't scroll
      super.onScrollChanged(0, 0, 0, 0);
   }

   @Override
   public boolean onInterceptTouchEvent(MotionEvent ev)
   {
      return true;// Touch event still sent to content
   }

   /******************************
    * app ---> tv.js API *
    ****************************** 
    * The methods below are the apps medium for manipulating the webview's
    * content. This API lets the app call javascript functions in tv.js. Results
    * are sent back through the two interface classes set in vpInit().
    */
   public void playVideo(int i)
   {
      this.clearHistory();
      loadUrl("javascript:loadVideo(" + i + ")");
   }

   public void playNext()
   {
      loadUrl("javascript:loadVideo('next')");
   }

   public void playPrev()
   {
      loadUrl("javascript:loadVideo('prev')");
   }

   /**
    * Set the thumbnails adapter which will receive urls from tv.js
    */
   public void setJSIThumbAdapter(ThumbnailsAdapter tnsAdapter)
   {
      jsi.setTnsAdapter(tnsAdapter);
   }

   public void loadChannel(Channel channel)
   {
      this.clearHistory();
      loadUrl("javascript:loadChannel(\"" + channel.getName() + "\", null)");
   }

   /**
    * Called when a new channel is added. Inserts a new entry into tv.js's
    * videos list to reflect the new channel list.
    */
   public void addChannel(int i)
   {
      loadUrl("javascript:addChannel(" + i + ")");
   }

   public void removeChannel(int i)
   {
      loadUrl("javascript:removeChannel(" + i + ")");
   }

   public void removeAllChannels()
   {
      loadUrl("javascript:removeAllChannels()");
   }

   /**
    * Will have tv.js look for videos at the given feed, marking index i as
    * empty if none are found
    */
   public void checkIfEmpty(int i, String name)
   {
      loadUrl("javascript:markIfEmpty(" + i + ", \"" + name + "\")");
   }

   public void setLoadProgress(int np)
   {
      actvCon.setLoadProgress(np);
   }
}
