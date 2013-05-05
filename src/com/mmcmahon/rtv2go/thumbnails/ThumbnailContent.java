package com.mmcmahon.rtv2go.thumbnails;

/**
 * A set of data pertaining to a video thumbnail. This class provides
 * a light weight object for passing around during thumbnail interaction.
 *
 * Summer 2012
 * @author Michael McMahon
 */
public class ThumbnailContent
{
   private String url;
   private String title;
   private int score = 0;
   private String id = "0";
   private int voted = 0;

   public ThumbnailContent(String u, String t, int s, String i, int v)
   {
      url = u;
      title = t;
      score = s;
      id = i;
      voted = v;
   }

   public String getUrl()
   {
      return url;
   }

   public String getTitle()
   {
      return title;
   }

   public int getScore()
   {
      return score;
   }

   public String getId()
   {
      return id;
   }

   public int getVoteState()
   {
      return voted;
   }

   public void setVoteState(int v)
   {
      voted = v;
   }
}