/* Globals */
var Globals = {
	/* build uri for search type channels */
	search_str : (function() {
		var one_day = 86400, date = new Date(), unixtime_ms = date.getTime(), unixtime = parseInt(unixtime_ms / 1000);
		// 7-12-2012 Added "&syntax=cloudsearch", now this request will work;
		// Before it was making the Android AI non responsive!
		return "search/.json?q=%28and+%28or+site%3A%27youtube.com%27+site%3A%27vimeo.com%27+site%3A%27youtu.be%27%29+timestamp%3A"
				+ (unixtime - 5 * one_day)
				+ "..%29&restrict_sr=on&sort=top&syntax=cloudsearch";
	})(),

	/* Video Domains */
	domains : [ '5min.com', 'abcnews.go.com', 'animal.discovery.com',
			'animoto.com', 'atom.com', 'bambuser.com', 'bigthink.com',
			'blip.tv', 'break.com', 'cbsnews.com', 'cnbc.com', 'cnn.com',
			'colbertnation.com', 'collegehumor.com', 'comedycentral.com',
			'crackle.com', 'dailymotion.com', 'dsc.discovery.com',
			'discovery.com', 'dotsub.com', 'edition.cnn.com',
			'escapistmagazine.com', 'espn.go.com', 'fancast.com', 'flickr.com',
			'fora.tv', 'foxsports.com', 'funnyordie.com', 'gametrailers.com',
			'godtube.com', 'howcast.com', 'hulu.com', 'justin.tv',
			'kinomap.com', 'koldcast.tv', 'liveleak.com', 'livestream.com',
			'mediamatters.org', 'metacafe.com', 'money.cnn.com',
			'movies.yahoo.com', 'msnbc.com', 'nfb.ca', 'nzonscreen.com',
			'overstream.net', 'photobucket.com', 'qik.com', 'redux.com',
			'revision3.com', 'revver.com', 'schooltube.com', 'screencast.com',
			'screenr.com', 'sendables.jibjab.com', 'spike.com',
			'teachertube.com', 'techcrunch.tv', 'ted.com', 'thedailyshow.com',
			'theonion.com', 'traileraddict.com', 'trailerspy.com', 'trutv.com',
			'twitvid.com', 'ustream.com', 'viddler.com', 'video.google.com',
			'video.nationalgeographic.com', 'video.pbs.org', 'video.yahoo.com',
			'vids.myspace.com', 'vimeo.com', 'wordpress.tv',
			'worldstarhiphop.com', 'xtranormal.com', 'youtube.com', 'youtu.be',
			'zapiks.com' ],

	videos : [],
	cur_video : 0,
	cur_chan : 0,
	cur_chan_req : null,
	cur_vid_req : null,
	current_anchor : null,
	auto : true,
	sfw : true,
	shuffle : false,
	shuffled : [],
	theme : 'light',
};

/* MAIN (Document Ready) */
$().ready(function() {
	loadSettings();

	configAndroidRes();// Configure video size for an android device
	rtv2goChannels.loadFirstChannel();
});

/*
 * Android to Javascript communication Michael McMahon - Jun/2012
 */
// Ask android app how big the video should be
function configAndroidRes() {
	// Configure video size
	Globals.height = rtv2go.getHeight();
	Globals.width = rtv2go.getWidth();
}
// Send a list of thumbnail urls to the app
function sendListToAndroid(chan) {
	var this_chan = chan;
	for ( var i in Globals.videos[this_chan].video) {
		// Get video title
		var this_video = Globals.videos[this_chan].video[i];
		if (!this_video.title_unesc) {
			this_video.title_unesc = $.unescapifyHTML(this_video.title);
			this_video.title_quot = String(this_video.title_unesc).replace(
					/\"/g, '&quot;');
		}

		var voted = 0;
		if(this_video.likes != null)
		{//Get the previous vote history, if any
			if(this_video.likes)
			{
				voted = 1;
			}
			else
			{
				voted = -1;
			}
		}
		rtv2go.addThumbnail(getThumbnailUrl(this_chan, i),
				this_video.title_unesc, this_video.score, this_video.id, voted);
	}
}

/* Splice the video list, in case channel order is upset */
function addChannel(pos) {
	Globals.videos.splice(pos, 0, undefined);
}

function removeChannel(pos) {
	Globals.videos.splice(pos, 1);
}

/* Main Functions */
function loadSettings() {
	var channels_cookie = $.jStorage.get('user_channels'), auto_cookie = $.jStorage
			.get('auto'), sfw_cookie = $.jStorage.get('sfw'), theme_cookie = $.jStorage
			.get('theme'), shuffle_cookie = $.jStorage.get('shuffle');

	if (auto_cookie !== null && auto_cookie !== Globals.auto) {
		Globals.auto = (auto_cookie === 'true') ? true : false;
		// $('#auto').attr('checked', Globals.auto);
	}
	if (shuffle_cookie !== null && shuffle_cookie !== Globals.shuffle) {
		Globals.shuffle = (shuffle_cookie === 'true') ? true : false;
		// $('#shuffle').attr('checked', Globals.shuffle);
	}
	if (sfw_cookie !== null && sfw_cookie !== Globals.sfw) {
		Globals.sfw = (sfw_cookie === 'true') ? true : false;
		// $('#sfw').attr('checked', Globals.sfw);
	}
	if (theme_cookie !== null && theme_cookie !== Globals.theme) {
		Globals.theme = theme_cookie;
	}
}

function loadChannel(channel, video_id) {
	var last_req = Globals.cur_chan_req, this_chan = getChan(channel), $video_embed = $('#video-embed');
	
	if (last_req !== null) {
		last_req.abort();
	}

	Globals.shuffled = [];
	Globals.cur_chan = this_chan;

	$video_embed.addClass('loading');
	$video_embed.empty();

		if (Globals.videos[this_chan] === undefined) {/* PARSES VIDEO LIST */
		var feed = getFeedURI(channel);
		Globals.cur_chan_req = $
				.ajax({
					url : "http://www.reddit.com" + feed,
					dataType : "jsonp",
					jsonp : "jsonp",
					cache : false,
					success : function(data) {
						Globals.videos[this_chan] = {};
						Globals.videos[this_chan].video = []; // clear out
																// stored videos
						for ( var x in data.data.children) {
							if (isVideo(data.data.children[x].data.domain)
									&& (data.data.children[x].data.score > 1)) {
								if (isEmpty(data.data.children[x].data.media_embed)
										|| data.data.children[x].data.domain === 'youtube.com'
										|| data.data.children[x].data.domain === 'youtu.be') {
									/* CREATE EMBEDED YOUTUBE */var created = createEmbed(
											data.data.children[x].data.url,
											data.data.children[x].data.domain);
									if (created !== false) {
										data.data.children[x].data.media_embed.content = created.embed;
										data.data.children[x].data.media = {};
										data.data.children[x].data.media.oembed = {};
										data.data.children[x].data.media.oembed.thumbnail_url = created.thumbnail;
									}
								}
								/* PUSH EMBEDDED VIDEO */if (data.data.children[x].data.media_embed.content) {
									Globals.videos[this_chan].video
											.push(data.data.children[x].data);
								}
							}
						}

						Globals.videos[this_chan].video = filterVideoDupes(Globals.videos[this_chan].video);

						if (Globals.videos[this_chan].video.length > 0) {
							/* LOAD VIDEO */if (video_id !== null) {
								loadVideoById(video_id);
							} else {
								sendListToAndroid(this_chan);
								Globals.cur_video = 0;
								loadVideo('first');
							}
							$video_embed.removeClass('loading');
						} else {
							$video_embed.removeClass('loading');
							rtv2go.androidLog("No videos found in " + channel);
							rtv2goChannels.markEmpty(channel, true);
							rtv2go.promptNoVideos(channel);
						}
					},
					error : function(jXHR, textStatus, errorThrown) {
						rtv2go.androidLog("AJAX ERROR: " + textStatus);
						if (textStatus !== 'abort') {
							alert('No subreddit found at www.reddit.com/r/'+channel);
						}
					},
					complete : function(jqXHR, textStatus) {
						rtv2go.androidLog("After AJAX: " + textStatus);
					}
				});
	} else {/* VIDEOS ALREADY PARSED */
		if (Globals.videos[this_chan].video.length > 0) {
			if (video_id !== null) {
				loadVideoById(video_id);
			} else {
				sendListToAndroid(this_chan);
				Globals.cur_video = 0;
				loadVideo('first');
			}
		} else {
			alert('No videos loaded for ' + channel);
			rtv2goChannels.markEmpty(channel, true);
			rtv2go.promptNoVideos(channel);
		}
	}
}

/*
 * Will attempt to retrieve videos from a channel, and tell the app if none are
 * found
 */
function markIfEmpty(this_chan, channel)
{
	// Check if video list is already parsed
	if (Globals.videos[this_chan] !== undefined && Globals.videos[this_chan].length > 0)
	{
		rtv2goChannels.markEmpty(channel, false);
		return;
	}
	
	var feed = getFeedURI(channel);
	Globals.cur_chan_req = $
	.ajax({
		url : "http://www.reddit.com" + feed,
		dataType : "jsonp",
		jsonp : "jsonp",
		cache : false,
		success : function(data) {
			var count = 0;
			
			for ( var x in data.data.children) {
				if (isVideo(data.data.children[x].data.domain)
						&& (data.data.children[x].data.score > 1)) {
					rtv2goChannels.markEmpty(channel, false);
					return;
				}
			}
			// Execution will reach here if no videos were found
			rtv2goChannels.markEmpty(channel, true);
		},
		error : function(jXHR, textStatus, errorThrown) {
			rtv2go.androidLog("AJAX ERROR: " + textStatus);
			if (textStatus !== 'abort') {
				alert('Could not load feed. Is reddit down?');
			}
		},
		complete : function(jqXHR, textStatus) {
			rtv2go.androidLog("After AJAX: " + textStatus);
		}
	});
	
}

function loadVideo(video) {
	var this_chan = Globals.cur_chan, this_video = Globals.cur_video, selected_video = this_video, videos_size = Object
			.size(Globals.videos[this_chan].video) - 1;

	if (Globals.shuffle) {
		if (Globals.shuffled.length === 0) {
			shuffleChan(this_chan);
		}
		// get normal key if shuffled already
		selected_video = Globals.shuffled.indexOf(selected_video);
	}

	if (video === 'next' && selected_video <= videos_size) {
		selected_video++;
		if (selected_video > videos_size) {
			selected_video = 0;
		}
		while (sfwCheck(getVideoKey(selected_video), this_chan)
				&& selected_video < videos_size) {
			selected_video++;
		}
		if (sfwCheck(getVideoKey(selected_video), this_chan)) {
			selected_video = this_video;
		}
	} else if (selected_video >= 0 && video === 'prev') {
		selected_video--;
		if (selected_video < 0) {
			selected_video = videos_size;
		}
		while (sfwCheck(getVideoKey(selected_video), this_chan)
				&& selected_video > 0) {
			selected_video--;
		}
		if (sfwCheck(getVideoKey(selected_video), this_chan)) {
			selected_video = this_video;
		}
	} else if (video === 'first') {
		selected_video = 0;
		if (sfwCheck(getVideoKey(selected_video), this_chan)) {
			while (sfwCheck(getVideoKey(selected_video), this_chan)
					&& selected_video < videos_size) {
				selected_video++;
			}
		}
	}
	selected_video = getVideoKey(selected_video);

	if (typeof (video) === 'number') { // must be a number NOT A STRING -
										// allows direct load of video # in
										// video array
		selected_video = video;
	}

	// exit if trying to load over_18 content without confirmed over 18
	if (sfwCheck(selected_video, this_chan)) {
		return false;
	}

	if (selected_video !== this_video || video === 'first' || video === 0) {
		Globals.cur_video = selected_video;

		/* LOADS VIDEO */
		var $video_embed = $('#video-embed');

		$video_embed.empty();
		$video_embed.addClass('loading');

		var embed = $
				.unescapifyHTML(Globals.videos[this_chan].video[selected_video].media_embed.content);
		embed = prepEmbed(embed,
				Globals.videos[this_chan].video[selected_video].domain);
		embed = prepEmbed(embed, 'size');

		rtv2go.androidLog(embed);

		$video_embed.html(embed);
		$video_embed.removeClass('loading');

		addListeners(Globals.videos[this_chan].video[selected_video].domain);

		// Send video id to android
		//rtv2go.setVideoId(Globals.videos[this_chan].video[selected_video].id);
	}
}

function getVideoKey(key) {
	if (Globals.shuffle
			&& Globals.shuffled.length === Globals.videos[Globals.cur_chan].video.length) {
		return Globals.shuffled[key];
	} else {
		return key;
	}
}

function loadVideoById(video_id) {
	var this_chan = Globals.cur_chan, video = findVideoById(video_id, this_chan); // returns
																					// number
																					// //
																					// typed
	if (video !== false) {
		loadVideo(Number(video));
	} else {
		// ajax request
		var last_req = Globals.cur_vid_req;
		if (last_req !== null) {
			last_req.abort();
		}

		Globals.cur_vid_req = $.ajax({
			url : "http://www.reddit.com/by_id/t3_" + video_id + ".json",
			dataType : "jsonp",
			jsonp : "jsonp",
			cache : false,
			success : function(data) {
				if (!isEmpty(data.data.children[0].data.media_embed)
						&& isVideo(data.data.children[0].data.media.type)) {
					Globals.videos[this_chan].video.splice(0, 0,
							data.data.children[0].data);
				}
				loadVideo('first');
			},
			error : function(jXHR, textStatus, errorThrown) {
				if (textStatus !== 'abort') {
					alert('Could not load data. Is reddit down?');
				}
			}
		});
	}
}

function isVideo(video_domain) {
	return (Globals.domains.indexOf(video_domain) !== -1);
}

// http://dreaminginjavascript.wordpress.com/2008/08/22/eliminating-duplicates/
function filterVideoDupes(arr) {
	var i, out = [], obj = {}, original_length = arr.length;

	// work from last video to first video (so hottest dupe is left standing)
	// first pass on media embed
	for (i = arr.length - 1; i >= 0; i--) {
		if (typeof obj[arr[i].media_embed.content] !== 'undefined') {
			delete obj[arr[i].media_embed.content];
		}
		obj[arr[i].media_embed.content] = arr[i];
	}
	for (i in obj) {
		out.push(obj[i]);
	}

	arr = out.reverse();
	out = [];
	obj = {};

	// second pass on url
	for (i = arr.length - 1; i >= 0; i--) {
		if (typeof obj[arr[i].url] !== 'undefined') {
			delete obj[arr[i].url];
		}
		obj[arr[i].url] = arr[i];
	}
	for (i in obj) {
		out.push(obj[i]);
	}

	return out.reverse();
}

function findVideoById(id, chan) {
	for ( var x in Globals.videos[chan].video) {
		if (Globals.videos[chan].video[x].id === id) {
			return Number(x); // if found return array pos
		}
	}
	return false; // not found
}

function sfwCheck(video, chan) {
	return (Globals.sfw && Globals.videos[chan].video[video].over_18);
}

function showHideNsfwThumbs(sfw, this_chan) {
	$('.nsfw_thumb').each(
			function() {
				$(this)
						.attr(
								'src',
								getThumbnailUrl(this_chan, Number($(this).attr(
										'rel'))));
			});
}

function getThumbnailUrl(chan, video_id) {
	if (sfwCheck(video_id, chan)) {
		return 'img/nsfw.png';
	} else if (Globals.videos[chan].video[video_id].media.oembed) {
		return Globals.videos[chan].video[video_id].media.oembed.thumbnail_url !== undefined ? Globals.videos[chan].video[video_id].media.oembed.thumbnail_url
				: 'img/noimage.png';
	} else {
		return 'img/noimage.png';
	}
}

function getFeedURI(channel) {
	return formatFeedURI(JSON.parse(String(rtv2goChannels
			.getJSONChannel(channel)), null));
}

function formatFeedURI(channel_obj) {
	if (channel_obj.feed == undefined) {
		rtv2go.androidLog("formatFeedURI Cannot format undefined feed!");
		return "";
	}
	switch (channel_obj.type) {
	case 'search':
		return channel_obj.feed + Globals.search_str + '&limit=100';
	default:
		return channel_obj.feed + '.json?limit=100';
	}
}

function getChan(channel) {
	var cIndex = rtv2goChannels.getChannelIndex(channel);
	if (cIndex >= 0) {
		return cIndex;
	}

	return false;
}

function getUserChan(channel) {
	var cIndex = rtv2goChannels.getChannelIndex(channel);
	if (cIndex >= 0) {
		return cIndex;
	}

	return false;
}

function createEmbed(url, type) {
	switch (type) {
	case 'youtube.com':
	case 'youtu.be':
		return youtube.createEmbed(url);
	case 'vimeo.com':
		return vimeo.createEmbed(url);
	default:
		return false;
	}
}

function prepEmbed(embed, type) {
	switch (type) {
	case 'youtube.com':
	case 'youtu.be':
		return youtube.prepEmbed(embed);
	case 'vimeo.com':
		return vimeo.prepEmbed(embed);
	case 'size':
		embed = embed.replace(/height\="(\d\w+)"/gi, 'height="'
				+ Globals.height + '"');
		embed = embed.replace(/width\="(\d\w+)"/gi, 'width="' + Globals.width
				+ '"');

		return embed;
	default:
		return embed;
	}

}

function addListeners(type) {
	switch (type) {
	case 'vimeo.com':
		vimeo.addListeners();
	}
}

function togglePlay() {
	switch (Globals.videos[Globals.cur_chan].video[Globals.cur_video].domain) {
	case 'youtube.com':
	case 'youtu.be':
		youtube.togglePlay();
		break;
	case 'vimeo.com':
		vimeo.togglePlay();
		break;
	}
}

function removeAllChannels() {
	rtv2go.androidLog("tv.js Removing all channels!");

	for ( var i in Globals.videos) {
		Globals.videos[i] = {};
		Globals.videos[i].video = []; // clear out stored videos
	}
}

function shuffleChan(chan) { // by index (integer
	/*
	 * does not shuffle actual video array but rather creates a global array of
	 * shuffled keys
	 */
	Globals.shuffled = []; // reset
	for ( var x in Globals.videos[chan].video) {
		Globals.shuffled.push(x);
	}
	Globals.shuffled = shuffleArray(Globals.shuffled);
	consoleLog('shuffling channel ' + chan);
}

/* Utility Functions */
// safe console log
function consoleLog(string) {
	rtv2go.androidLog(string);
}

// http://stackoverflow.com/questions/962802/is-it-correct-to-use-javascript-array-sort-method-for-shuffling/962890#962890
function shuffleArray(array) {
	var tmp, current, top = array.length;

	if (top) {
		while (--top) {
			current = Math.floor(Math.random() * (top + 1));
			tmp = array[current];
			array[current] = array[top];
			array[top] = tmp;
		}
	}

	return array;
}

function isEmpty(obj) {
	for ( var prop in obj) {
		if (obj.hasOwnProperty(prop)) {
			return false;
		}
	}
	return true;
}

Object.size = function(obj) {
	var size = 0, key;
	for (key in obj) {
		if (obj.hasOwnProperty(key)) {
			size++;
		}
	}
	return size;
};

/* analytics */
function gaHashTrack() {
	if (_gaq) {
		_gaq.push([ '_trackPageview', location.pathname + location.hash ]);
	}
}