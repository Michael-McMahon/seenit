package com.mmcmahon.rtv2go.channels;


import android.util.Log;

/** 
 * This class is an interface accessible to tv.js. It provides access to the 
 * channel list saved in SharedPreferences.
 * 
 * Summer 2012
 * @author Michael McMahon
 */
public class ChannelsJSInterface
{
   private static final String TAG = "#ChannelsJSInterface#";
   private ChannelsAdapter chanAdapter;

   /* Create channel set from '/' separated names in a single string */
   public ChannelsJSInterface(ChannelsAdapter ca, String slashSepartedNames)
   {
      int len, i;
      Channel chan;

      // Received list of channels as forwardSlash('/') delimited string
      String[] list = slashSepartedNames.substring(1).split("/", 0);
      // substring to skip first '/'
      len = list.length;
      chanAdapter = ca;

      for (i = 0; i < len; i++)
      {
         chan = new Channel(list[i]);
         chanAdapter.addChannel(chan, chanAdapter.getChanCount());
      }
   }

   public void clearChannelList()
   {
      chanAdapter.clearList();
   }

   /**
    * Constructs a json formatted string to be passed back to tv.js.
    * All channels get "search" type.
    * @param chName Name of new channel
    * @return json string object with the channel data
    */
   public String getJSONChannel(String chName)
   {
      String jsonChan;
      Channel chan = new Channel(chName);

      // Construct a json formatted string object
      jsonChan = "{ " + "\"channel\" : \"" + chan.getName() + "\", "
            + "\"type\" : \"search\", " + // TODO: How do we determine type? It
                                          // seems that 'search' type works for
                                          // all subreddits so far...
            "\"feed\" : \"" + chan.getFeed() + "\" " + "}";

      return jsonChan;
   }

   public int getChannelIndex(String chName)
   {
      return chanAdapter.getChannelIndex(chName);
   }

   public String getChannelName(int i)
   {
      if (i < 0 || i >= chanAdapter.getChanCount())
      {
         return "";
      }
      return ((Channel) chanAdapter.getItem(i)).getName();
   }

   public void addChannel(String name)
   {
      chanAdapter.addChannel(name);
   }

   /**
    *  Called from tv.js when no videos are found in a channel 
    */
   public void markEmpty(String ch, boolean empty)
   {
      if (empty)
      {
         chanAdapter.markEmpty(ch);
      }
   }

   public void loadFirstChannel()
   {
      chanAdapter.loadFirst();
   }
}
