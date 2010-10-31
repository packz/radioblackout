package org.radioblackout.android;

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

import java.io.IOException;


/**
 * This activity shows what to do when i want to read an audio
 * stream from remote resources.
 *
 * TODO: alert the user if is connected by mobile network.
 * TODO: use wake locks
 */
public class RadioActivity extends Activity implements AudioManager.OnAudioFocusChangeListener {
	private static final String TAG = "RadioActivity";
	private static String URL = "http://stream.radioblackout.org/blackout-low.mp3";

	static private final int STATE_STOPPED = 0;
	static private final int STATE_PLAYING = 1;
	static private int mState;

	static MediaPlayer MP = null;

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

		//RadioService.start(this);

		mState = STATE_STOPPED;

		checkNetworkStatus();
	}

	public void stopStream(View view) {
		switch(mState) {
			case STATE_PLAYING:
				RadioService.stop(this);
				break;
			case STATE_STOPPED:
				RadioService.start(this);
				break;
		}

		mState = mState == 0 ? 1 : 0;
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
