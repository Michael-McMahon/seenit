package com.mmcmahon.rtv2go.channels;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.R.anim;
import com.mmcmahon.rtv2go.R.color;
import com.mmcmahon.rtv2go.R.id;
import com.mmcmahon.rtv2go.R.layout;
import com.mmcmahon.rtv2go.R.string;
import com.mmcmahon.rtv2go.dialogs.RemoveDialog;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.TransitionDrawable;

/**
 * Serves up views which display subreddit names. This adapter communicates 
 * with an instance of ChannelsJSInterface to accomplish two tasks:
 * 1. Display the subreddits currently listed in the ChannelsJSInterface
 * 2. Notify the ChannelsJSInterface when an item is added to or deleted from the
 *  subreddit list.
 * 
 * This adapter is configured to show a text entry view at position 0. The text
 * entry is for searching and adding new subreddits to the list.
 * If empty subreddit filtering is initiated, this adapter will manage a check
 * and callback system for finding and removing any subreddits with no video 
 * postings.    
 *  
 * Summer 2012
 * @author Michael McMahon
 *
 */
public class ChannelsAdapter extends BaseAdapter
{
   private static final String TAG = "#ChannelsAdapter#";

   private ACTV_VideoPlayer actvCon;
   private Vector<Channel> channels = new Vector<Channel>();
   private Vector<Channel> empties = new Vector<Channel>();
   private boolean emptiesHidden = false;
   private LayoutInflater inflater;
   private EditText searchBox;
   private RemoveDialog removePrompt;

   public ChannelsAdapter(Context c)
   {
      actvCon = (ACTV_VideoPlayer) c;
      inflater = (LayoutInflater) actvCon
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      // Create the search box view
      createSearchBox();
      // Create remove channel dialog
      removePrompt = new RemoveDialog(actvCon, this);
   }

   private void createSearchBox()
   {
      searchBox = new EditText(actvCon);
      searchBox.setHint(R.string.topicSearch);
      LayoutParams lps = new LayoutParams(LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT);
      searchBox.setPadding(15, 8, 15, 8);
      searchBox.setTextSize(25);
      searchBox.setGravity(Gravity.CENTER_VERTICAL);
      searchBox.setBackgroundResource(R.color.IFACE_BG);
      searchBox.setTextColor(actvCon.getResources().getColor(
            R.color.SEARCH_GREEN));// Search green color association!
      searchBox.setHintTextColor(actvCon.getResources().getColor(
            R.color.SEARCH_GREEN_OPQ));
      searchBox.setCursorVisible(true);
      searchBox.setLayoutParams(lps);
   }

   private class OnChannelClick implements OnClickListener, OnLongClickListener
   {
      private Channel channel = null;

      public OnChannelClick(Channel c)
      {
         setChannel(c);
      }

      public void onClick(View clicked)
      {
         actvCon.loadChannel(channel);// Channel gets loaded
      }

      public void setChannel(Channel c)
      {
         channel = c;
      }

      public boolean onLongClick(View v)
      {
         actvCon.showRemove(true, removePrompt.setChannel(channel));
         return true;
      }
   }

   public int getCount()
   {
      return channels != null ? channels.size() + 1 : 0;// Size plus one for one
                                                        // extra search view
   }

   public int getChanCount()
   {
      return channels != null ? channels.size() : 0;// Size of channels list,
                                                    // not a count of views
   }

   public Object getItem(int pos)
   {
      return channels != null ? channels.get(pos) : null;
   }

   public long getItemId(int id)
   {
      return id;
   }

   public View getView(final int pos, View v, ViewGroup list)
   {
      // If requesting the left most view, return the search box
      if (pos == 0)
      {
         searchBox.requestFocus();
         return searchBox;
      }
      // Else, return channel name view
      Channel ch = channels.get(pos - 1);// Offset by one; Search box is first
                                         // view
      String name = ch.getName();
      View row;
      TextView text;
      OnChannelClick occ;

      if (v == null || v == searchBox)// Create a new textview
      {
         row = inflater.inflate(R.layout.channel_view, null);

      } else
      // Recycled view
      {
         row = v;
      }

      // Set up text
      text = (TextView) row.findViewById(R.id.channel_text);
      text.setText(name);
      // Create an onclick listener
      occ = new OnChannelClick(ch);// Create a new on click
                                   // listener
      row.setOnClickListener(occ);
      row.setOnLongClickListener(occ);

      // Transition from green to black background on a newly added channel
      if (ch.isNew())
      {
         TransitionDrawable bgAnim = (TransitionDrawable) actvCon
               .getResources().getDrawable(R.anim.search_add);
         row.setBackgroundDrawable(bgAnim);
         bgAnim.setCrossFadeEnabled(true);
         bgAnim.startTransition(750);
      } else
      // Otherwise show flashy list animation
      {
         row.setBackgroundResource(R.color.IFACE_BG);
         text.startAnimation(ch.getAnimation());// Show animation
      }
      return row;
   }

   private void removeDefault(Channel channel, boolean showMsg)
   {
      actvCon.spDeleteChannel(channel.getName());
      if (showMsg)// Create a confirmation
      {
         String chName = channel.getName();
         String message = chName + " "
               + actvCon.getResources().getString(R.string.not_def_chan);
         Toast confirm = Toast.makeText(actvCon, message, 1500);
         confirm.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
         confirm.show();
         ChannelsAdapter.this.notifyDataSetChanged();
         ((Vibrator) actvCon.getSystemService(Context.VIBRATOR_SERVICE))
               .vibrate(250);
      }
   }

   private void addDefault(Channel channel)
   {
      actvCon.spSaveChannel(channel.getName());
   }

   public int getChannelIndex(String name)
   {
      Iterator<Channel> chanIter = channels.iterator();
      Channel next;
      int index = 0;

      while (chanIter.hasNext())
      {
         next = chanIter.next();
         if (next.getName().equals(name))
         {
            return index;
         }
         index++;
      }

      //Log.e(TAG, "ChanAdapter can not getChannelIndex(): " + name);
      return 0;
   }

   public void clearList()
   {
      actvCon.runOnUiThread(new Runnable()
      {
         public void run()
         {
            channels = new Vector<Channel>();
            ChannelsAdapter.this.notifyDataSetChanged();
         }
      });
   }

   public void removeChannel(final String ch, boolean showMsg)
   {
      int i = 0;
      for (Channel c : channels)
      {
         if (c.getName().equals(ch))
         {
            final int fi = i;
            ((Activity) actvCon).runOnUiThread(new Runnable()
            {
               public void run()
               {
                  actvCon.removeJSChannel(fi);
                  channels.remove(fi);
                  ChannelsAdapter.this.notifyDataSetChanged();
               }
            });
            removeDefault(c, showMsg);
            return;// Remove from saved preferences and return
         }
         i++;
      }
   }

   public void addChannel(String name)
   {
      addChannel(new Channel(name), channels.size());
   }

   public void addChannel(final Channel ch, final int i)
   {
      if (!channels.contains(ch))
      {
         addDefault(ch);// Store channel in saved preferences
         // Notify UI Thread of new entry
         ((Activity) actvCon).runOnUiThread(new Runnable()
         {
            public void run()
            {
               channels.insertElementAt(ch, i);
               ChannelsAdapter.this.notifyDataSetChanged();
            }
         });
      }
   }

   public void showEmptyChannels()
   {
      if (!emptiesHidden)
      {
         return;
      }
      emptiesHidden = false;
      actvCon.runOnUiThread(new Runnable()
      {
         public void run()
         {
            for (Channel ec : empties)
            {
               ChannelsAdapter.this.addChannel(ec.getName());
            }
            empties.removeAllElements();
            ChannelsAdapter.this.notifyDataSetChanged();
         }
      });
   }

   public void hideEmptyChannels()
   {
      int i, chanCount = channels.size();
      Channel chan;

      if (emptiesHidden)// Return if already hiding empty channels
      {
         return;
      }
      emptiesHidden = true;

      for (i = 0; i < chanCount; i++)
      {
         chan = channels.get(i);
         switch (chan.isEmpty())
         {
         case NOT_SURE:
         {
            // Have tv.js look for videos in each channel, calling back to
            // markAsEmpty if none are found
            actvCon.checkIfEmpty(i, chan.getName());
            break;
         }
         case EMPTY:
         {
            empties.add(chan);
            break;
         }
         }
      }

      // Ask the user to confirm the removal list
      actvCon.showRemove(true, removePrompt.setChannelList(empties));
   }

   /**
    * The callback method for tv.js to report an empty subreddit. tv.js will run
    * in a separate thread, as requested in hideEmptyChannels() when isEmpty ==
    * NOT_SURE for some subreddit.
    * 
    * @param ch
    *           The name of an empty subreddit.
    */
   public void markEmpty(final String ch)
   {
      ListIterator<Channel> li = channels.listIterator();
      Channel empChan = null;// The empty channel
      while (li.hasNext())// Look up channel with an iterator
      {
         empChan = li.next();
         if (empChan.getName().equals(ch))// Mark channel as empty
         {
            empChan.setEmpty(Channel.HasVideos.EMPTY);
            break;
         }
      }

      // If the app is asking the user to remove a list of empties, this
      // channel is now added to that list
      if (emptiesHidden && empChan != null)
      {
         empties.add(empChan);
         removePrompt.addToList(empChan);
      }
   }

   /*TODO: Perhaps a periodical rechecking of empty subreddits...
   public void markNotEmpty(String ch)
   {
      ListIterator<Channel> li = channels.listIterator();
      Channel empChan = null;// The empty channel
      while (li.hasNext())// Look up channel with an iterator
      {
         empChan = li.next();
         if (empChan.getName().equals(ch))// Mark channel as empty
         {
            empChan.setEmpty(Channel.HasVideos.NOT_EMPTY);
            break;
         }
      }
   }*/

   public void loadFirst()
   {
      if (channels.size() > 0)
      {
         actvCon.loadChannel(channels.get(0));
      }
   }

   /* Returns the view for subreddit searching */
   public EditText getSearchBox()
   {
      return searchBox;
   }
}
