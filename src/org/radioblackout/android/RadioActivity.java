package org.radioblackout.android;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import android.view.View;
import android.app.Activity;
import android.os.Bundle;
import android.media.*;
import android.util.Log;
import android.app.AlertDialog;
import android.widget.*;
import android.os.AsyncTask;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.view.*;
import android.net.*;
import android.telephony.*;

import org.mcsoxford.rss.*;

import java.io.IOException;
import java.util.*;


/**
 * This activity shows what to do when i want to read an audio
 * stream from remote resources.
 *
 * TODO: alert the user if is connected by mobile network.
 * TODO: use wake locks
 */
public class RadioActivity extends SherlockActivity implements AudioManager.OnAudioFocusChangeListener {
	private static final String TAG = "RadioActivity";
	private static String URL = "http://stream.radioblackout.org/blackout-low.mp3";


	static MediaPlayer MP = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.main, menu);

	}
	/*
	 * Simply paste-copied from http://developer.android.com/guide/topics/media/mediaplayer.html
	 */
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				// resume playback
				if (MP == null)
					try {
					//initMediaPlayer();
					} catch (Exception e) {
						android.util.Log.e("TAG", "error: " + e.getMessage());
					}
				else
					if (!MP.isPlaying())
						MP.start();
				MP.setVolume(1.0f, 1.0f);
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				// Lost focus for an unbounded amount of time: stop playback and release media player
				if (MP.isPlaying()) MP.stop();
				MP.release();
				MP = null;
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				// Lost focus for a short time, but we have to stop
				// playback. We don't release the media player because playback
				// is likely to resume
				if (MP.isPlaying()) MP.pause();
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				if (MP.isPlaying()) MP.setVolume(0.1f, 0.1f);
				break;
		}
	}

	private final int NETWORK_UNAVAILABLE = 0;
	private final int NETWORK_MOBILE = 1;
	private final int NETWORK_WIFI = 2;

	private void checkNetworkStatus() {
		ConnectivityManager cm =
			(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		TelephonyManager tm =
			(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

		boolean isAvailable = activeNetwork == null ? false : activeNetwork.isAvailable();

		int type;

		if (!isAvailable)
			return NETWORK_UNAVAILABLE;

		switch (activeNetwork.getType()) {
			case (ConnectivityManager.TYPE_WIFI): 
				type = NETWORK_WIFI;
			case (ConnectivityManager.TYPE_MOBILE): 
				type = NETWORK_MOBILE;
			default: break;
		}

		Log.i(TAG, "Connectivity - availability: " + isAvailable + " type: " + type);

		return type;
	}

	/*
	 * Seems that direct URL streaming doesn't work so following this mail thread
	 *
	 * 	http://markmail.org/message/tkymyawq7gwfdvl3
	 *
	 * we download the file locally and then play it.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//showDialog(0);
		setContentView(R.layout.radio);

		//FIXME: use loaders <http://developer.android.com/guide/topics/fundamentals/loaders.html>
		Thread t = new Thread() {
			public void run() {
				RSSReader reader = new RSSReader();

				RSSFeed feed = null;
				try {
					feed = reader.load("http://radioblackout.org/feed/");
				} catch(Exception e) {
					//Log.e(TAG, e.getMessage());

					e.printStackTrace();
				}

				final RadioRSSAdapter rssAdapter =
					new RadioRSSAdapter(
							RadioActivity.this,
							(feed == null ? new ArrayList<RSSItem>() : feed.getItems())
						);

				final ListView lv = (ListView)findViewById(R.id.rss_list);
				lv.post(new Runnable(){
					public void run() {
						lv.setAdapter(rssAdapter);
						lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
								RSSItem item = (RSSItem)parent.getItemAtPosition(position);
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, item.getLink());
								startActivity(browserIntent);
							}
						});
					}
				});
			}
		};
		/*
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(
				this,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			// could not get audio focus.
			android.util.Log.i("TAG", "no focus baby");
		}*/

		t.start();

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_play:
				startStopStream(null);
				break;
		}
		return super.onOptionsItemSelected(item);

		checkNetworkStatus();
	}

	public void startStopStream(View view) {
		switch(RadioService.getStatus()) {
			case RadioService.RB_STREAM_STATUS_STARTED:
				RadioService.stop(this);
				break;
			case RadioService.RB_STREAM_STATUS_STOPPED:
				RadioService.start(this);
				break;
		}

	}

	private void setBannerMessage(String msg) {
		((TextView)findViewById(R.id.banner)).setText(msg);
	}

	public void displayBuffering() {
		((TextView)findViewById(R.id.banner)).setText("loading");
	}

	public void displayRemoveBuffering() {
		((TextView)findViewById(R.id.banner)).setText("go");
	}

	public void displayStoppedBuffering() {
		((TextView)findViewById(R.id.banner)).setText("stop");
	}
}
