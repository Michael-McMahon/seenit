package com.mmcmahon.rtv2go.dialogs;

import java.util.Vector;

import com.mmcmahon.rtv2go.ACTV_VideoPlayer;
import com.mmcmahon.rtv2go.R;
import com.mmcmahon.rtv2go.R.id;
import com.mmcmahon.rtv2go.R.layout;
import com.mmcmahon.rtv2go.R.string;
import com.mmcmahon.rtv2go.channels.Channel;
import com.mmcmahon.rtv2go.channels.ChannelsAdapter;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

/**
 * Screen overlay for asking the user to remove subreddits saved to Shared
 * Preferences. May be configured to show a single subreddit or a list of
 * subreddits.
 * 
 * @author Michael McMahon
 */
public class RemoveDialog extends Dialog
{
   private View yes, no;
   private ChannelsAdapter chanAdapter;
   private Channel chan;// Single channel to be removed
   private Vector<Channel> list;// List to be removed
   private TextView prompt;
   private Resources res;// Has string resources
   private ACTV_VideoPlayer actvCon;
   private boolean isList = false; // True if prompting to remove a list of
                                   // channels

   public RemoveDialog(Context context, ChannelsAdapter ca)
   {
      super(context, android.R.style.Theme_Translucent_NoTitleBar);

      actvCon = (ACTV_VideoPlayer) context;
      chanAdapter = ca;
      setContentView(R.layout.remove_popup);
      yes = findViewById(R.id.removeYes);
      no = findViewById(R.id.removeNo);
      prompt = (TextView) findViewById(R.id.removeMsg);
      res = context.getResources();
      setListeners();
   }

   private void setPrompt(String chName)
   {
      String msg = String.format(res.getString(R.string.remove_prompt), chName);
      prompt.setText(msg);
   }

   private void setListPrompt()
   {
      String dlim = ", ";// Comma delimiter
      StringBuilder strBldr = new StringBuilder();
      int i;
      String msg;

      // Create a comma delimited list of names
      strBldr = strBldr.append(list.get(0).getName());
      for (i = 1; i < list.size(); i++)
      {
         strBldr = strBldr.append(dlim);
         strBldr = strBldr.append(list.get(i).getName());
      }
      // Now put list into removal prompt
      msg = String.format(res.getString(R.string.remove_prompt),
            strBldr.toString());
      prompt.setText(msg);
   }

   public RemoveDialog setChannel(final Channel c)
   {
      chan = c;
      isList = false;
      actvCon.runOnUiThread(new Runnable()
      {
         public void run()
         {
            setPrompt(chan.getName());
         }
      });
      return this;
   }

   public RemoveDialog setChannelList(Vector<Channel> cList)
   {
      if (cList.isEmpty())
      {
         list = new Vector<Channel>();
         list.add(new Channel("all empty channels"));
      } else
      {
         list = cList;
      }
      isList = true;

      actvCon.runOnUiThread(new Runnable()
      {
         public void run()
         {
            setListPrompt();
         }
      });

      return this;
   }

   public void addToList(final Channel c)
   {
      if (!isList)
      {
         return;
      }

      actvCon.runOnUiThread(new Runnable()
      {
         public void run()
         {
            setListPrompt();// Refresh visible list
         }
      });
   }

   private void setListeners()
   {
      // And set on-click listeners
      yes.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View arg0)
         {
            if (isList)// Remove list
            {
               for (Channel empty : list)
               {
                  chanAdapter.removeChannel(empty.getName(), false);
               }
            } else
            // Remove singular
            {
               chanAdapter.removeChannel(chan.getName(), true);
            }
            actvCon.showRemove(false, RemoveDialog.this);
         }
      });

      no.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View arg0)
         {
            RemoveDialog.this.hide();
            actvCon.showRemove(false, RemoveDialog.this);
         }
      });
   }

}
