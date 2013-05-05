package com.mmcmahon.rtv2go;

import java.util.TimerTask;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Semaphore;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.Uri;
import android.util.Log;

/**
 * An API for doing reddit stuff; voting, login, etc...
 * This code obtained from and mostly authored at this repo: 
 *    http://code.google.com/p/radioreddit-android/
 * TODO: Radio Reddit has updated their API. Figure out why, and if it's 
 *    important for this app.
 * Minor changes made for reddit video watching app: 
 * Added a semaphore which restricts this API to 1 request every 2 seconds. 
 * User agent is obtained on app initiation and set dynamically with 
 *    setUserAgent().
 */
public class RedditAPI
{
   // Reddit API Urls
   private final static String REDDIT_LOGIN = "http://www.reddit.com/api/login";
   private final static String REDDIT_ME = "http://www.reddit.com/api/me.json";
   private final static String REDDIT_VOTE = "http://www.reddit.com/api/vote";
   private final static String REDDIT_MINE = "http://www.reddit.com/reddits/mine";
   
   private static final String TAG = "#RedditAPI#";
   private static final long REQ_DELAY = (long)2000;//Amount of milliseconds between requests
   
   //TODO:10/22 User agent should now be set dynamically in VideoPlayer init. Consider not allowing this default string at all; Reddit is strict about user agents!
   private static String RTV2GO_USER_AGENT = "Mozilla/5.0 (Linux; Android; en-us;) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1; seenit";
   private static RedditAccount account = null;
   //Semaphore enforces the one request per 2 seconds rule
   private static Semaphore requestLock = new Semaphore(1, true);
   
   public static class RedditAccount
   {
      public String ErrorMessage;
      public String Username;
      public String Cookie;
      public String Modhash;
   }
   
   private static class ClearRequests extends TimerTask
   {
      public void run()
      {
       //Two seconds have expired, clear the lock
        requestLock.release();
      }
   }
   
   public static void checkReqTimer()
   {
      try
      {
         requestLock.acquire();
         //Release lock after two seconds
         new Timer().schedule(new ClearRequests(), REQ_DELAY);
      } catch (InterruptedException e)
      {//TODO: Recover from this
         Log.e(TAG, "Request was interrupted while waiting for requests-per-second limit to expire.");
      }
   }
   
   public static void setUserAgent(final String userAgent)
   {
      RTV2GO_USER_AGENT = userAgent;
   }

   public static void logout()
   {
      account = null;
   }

   public static boolean login(String username, String password)
   {
      account = new RedditAccount();
      
      try
      {
         String url = REDDIT_LOGIN + "/" + username;

         // post values
         ArrayList<NameValuePair> post_values = new ArrayList<NameValuePair>();

         BasicNameValuePair user = new BasicNameValuePair("user", username);
         post_values.add(user);

         BasicNameValuePair passwd = new BasicNameValuePair("passwd", password);
         post_values.add(passwd);

         BasicNameValuePair api_type = new BasicNameValuePair("api_type",
               "json");
         post_values.add(api_type);

         String outputLogin = post(url, post_values);

         JSONTokener reddit_login_tokener = new JSONTokener(outputLogin);
         JSONObject reddit_login_json = new JSONObject(reddit_login_tokener);

         JSONObject json = reddit_login_json.getJSONObject("json");

         if (json.getJSONArray("errors").length() > 0)
         {
            String error = json.getJSONArray("errors").getJSONArray(0)
                  .getString(1);

            account.ErrorMessage = error;
         } else
         {
            JSONObject data = json.getJSONObject("data");

            String cookie = data.getString("cookie");
            String modhash = data.getString("modhash");
            // success!
            account.Username = username;
            account.Cookie = cookie;
            account.Modhash = modhash;
            account.ErrorMessage = "";

            return true;
         }
      } catch (Exception ex)
      {
         Log.e(TAG, "EXCEPTION DURING LOGIN: "+ ex.toString());

         account.ErrorMessage = ex.toString();
         return false;
      }

      return false;
   }

   // voteDirection = -1 vote down, 0 remove vote, 1 vote up
   public static String vote(int voteDirection, String videoId)
   {
      String errorMessage = "";

      if (account == null)
      {
         return "account is null";
      }

      try
      {
         try
         {
            account.Modhash = updateModHash();

            if (account.Modhash == null)
            {
               errorMessage = "Error voting";
               return errorMessage;
            }
         } catch (Exception ex)
         {
            errorMessage = ex.getMessage();
            return errorMessage;
         }

         String url = REDDIT_VOTE;

         // post values
         ArrayList<NameValuePair> post_values = new ArrayList<NameValuePair>();

         BasicNameValuePair id = new BasicNameValuePair("id", videoId);
         post_values.add(id);

         BasicNameValuePair dir = new BasicNameValuePair("dir",
               Integer.toString(voteDirection));
         post_values.add(dir);

         BasicNameValuePair uh = new BasicNameValuePair("uh", account.Modhash);
         post_values.add(uh);

         BasicNameValuePair api_type = new BasicNameValuePair("api_type",
               "json");
         post_values.add(api_type);

         String outputVote = post(url, post_values);

         JSONTokener reddit_vote_tokener = new JSONTokener(outputVote);
         JSONObject reddit_vote_json = new JSONObject(reddit_vote_tokener);

         if (reddit_vote_json.has("json"))
         {
            JSONObject json = reddit_vote_json.getJSONObject("json");

            if (json.has("errors") && json.getJSONArray("errors").length() > 0)
            {
               String error = json.getJSONArray("errors").getJSONArray(0)
                     .getString(1);

               errorMessage = error;
            }
         }
         // success!
      } catch (Exception ex)
      {
         Log.e(TAG, ex.toString());
      }

      return errorMessage;
   }

   /*
    * public static String getChannelLogo(String chan) { String imgUrl = "null";
    * 
    * try { String url = "http://www.reddit.com" + chan +"/about.json";
    * 
    * String output = get(url);
    * 
    * JSONTokener reddit_tokener = new JSONTokener(output); JSONObject jsob =
    * new JSONObject(reddit_tokener); imgUrl =
    * jsob.getJSONObject("data").getString("header_img"); } catch (Exception ex)
    * { Log.e(TAG, ex.toString()); }
    * 
    * //Catch a null header image if(imgUrl.equals("null")) { imgUrl = null; }
    * 
    * return imgUrl; }
    */

   public static void getUserChannels(ACTV_VideoPlayer actvCon)
   {
      try
      {
         final String url = REDDIT_MINE + ".json";

         final String outputMine = get(url);

         final JSONTokener reddit_mine_tokener = new JSONTokener(outputMine);
         final JSONObject jsob = new JSONObject(reddit_mine_tokener);

         // Reddit returns an json object with an array of objects containing
         // subreddit info.
         final JSONArray children = jsob.getJSONObject("data").getJSONArray(
               "children");
         final int len = children.length();
         JSONObject data;
         String name;
         // Extract the data for each subreddit entry
         for (int i = 0; i < len; i++)
         {
            data = children.getJSONObject(i).getJSONObject("data");
            name = data.getString("display_name");
            actvCon.pushChannel(name, false);
         }
      } catch (Exception ex)
      {
         Log.e(TAG, ex.toString());
      }
   }

   public static String updateModHash()
   {
      // Calls me.json to get the current modhash for the user
      String output = "";
      boolean errorGettingModHash = false;

      try
      {
         try
         {
            output = get(REDDIT_ME);
         } catch (Exception ex)
         {
            Log.e(TAG, ex.toString());
            errorGettingModHash = true;
         }

         if (!errorGettingModHash && output.length() > 0)
         {
            JSONTokener reddit_me_tokener = new JSONTokener(output);
            JSONObject reddit_me_json = new JSONObject(reddit_me_tokener);

            JSONObject data = reddit_me_json.getJSONObject("data");

            String modhash = data.getString("modhash");

            return modhash;
         } else
         {
            return null;
         }
      } catch (Exception ex)
      {
         ex.printStackTrace();
         return null;
      }
   }

   /* InputStream to String */
   public static String slurp(InputStream in) throws IOException
   {
      StringBuffer out = new StringBuffer();
      byte[] b = new byte[4096];
      for (int n; (n = in.read(b)) != -1;)
      {
         out.append(new String(b, 0, n));
      }
      return out.toString();
   }

   // gets http output from URL
   public static String get(String url) throws ClientProtocolException,
         IOException
   {
      checkReqTimer();
      
      HttpURLConnection conn = (HttpURLConnection) new URL(url)
            .openConnection();
      conn.setRequestProperty("User-Agent", RTV2GO_USER_AGENT);

      if (account != null)
      {
         conn.setRequestProperty("Cookie", "reddit_session=" + account.Cookie);
      }

      String output = slurp(conn.getInputStream());
      conn.getInputStream().close();

      return output;
   }

   // posts data to http, gets output from URL
   private static String post(String url, List<NameValuePair> params)
         throws ClientProtocolException, IOException
   {
      checkReqTimer();
      
      StringBuilder post = new StringBuilder(); // Using the built in method to
                                                // post a List<NameValuePair>
                                                // fails on Android 2.1 /
                                                // 2.2.... manually post the
                                                // data
      for (NameValuePair p : params)
      {
         post.append(Uri.encode(p.getName()) + "=" + Uri.encode(p.getValue())
               + "&");
      }
      post.deleteCharAt(post.length() - 1); // Remove trailing &

      HttpURLConnection conn = (HttpURLConnection) new URL(url)
            .openConnection();
      conn.setDoOutput(true);
      conn.setRequestProperty("User-Agent", RTV2GO_USER_AGENT);
      conn.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded");
      conn.setRequestProperty("Content-Length", Integer.toString(post.length()));

      if (account != null)
      {
         conn.setRequestProperty("Cookie", "reddit_session=" + account.Cookie);
      }

      OutputStream os = conn.getOutputStream();
      os.write(post.toString().getBytes());

      String output = slurp(conn.getInputStream());
      conn.getInputStream().close();

      return output;
   }

   public static String getError()
   {
      return account.ErrorMessage;
   }
   /*
    * Set id of current video public static void setVideoId(String newId) {
    * videoId = newId; }
    */
}
