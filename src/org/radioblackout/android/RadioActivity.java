package org.radioblackout.android;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuInflater;
import android.view.View;
import android.widget.RelativeLayout;
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
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.mcsoxford.rss.*;

import de.neofonie.mobile.app.android.widget.crouton.Crouton;
import de.neofonie.mobile.app.android.widget.crouton.Style;

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

    /*
     * This class listen for change in the status of the RadioService and update
     * accordingly the interface.
     */
    public class RadioServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            adaptMenuButtonToState();
        }
    }

    private Menu mMenu = null;

	static MediaPlayer MP = null;

    private RSSFeed mFeed = null;

    private RadioServiceReceiver mRadioServiceReceiver;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.main, menu);

        mMenu = menu;

        adaptMenuButtonToState();
		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
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

	private int checkNetworkStatus() {
		ConnectivityManager cm =
			(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		TelephonyManager tm =
			(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

		boolean isAvailable = activeNetwork == null ? false : activeNetwork.isAvailable();

		int type = NETWORK_UNAVAILABLE;

		if (!isAvailable)
			return NETWORK_UNAVAILABLE;

		switch (activeNetwork.getType()) {
			case (ConnectivityManager.TYPE_WIFI): 
				type = NETWORK_WIFI;
			case (ConnectivityManager.TYPE_MOBILE): 
				type = NETWORK_MOBILE;
			default: break;
		}

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

        final ListView lv = (ListView)findViewById(R.id.rss_list);
        final RadioRSSAdapter rssAdapter =
            new RadioRSSAdapter(
                    RadioActivity.this,
                    (mFeed == null ? new ArrayList<RSSItem>() : mFeed.getItems())
                );
        lv.setAdapter(rssAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RSSItem item = (RSSItem)parent.getItemAtPosition(position);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, item.getLink());
                startActivity(browserIntent);
            }
        });

        /*
         * Here we inflate the empty view.
         *
         * We have to set the RelativeLayout parameters by hand since otherwise
         * the XML parameters are not used because it's inflated with null as parent.
         *
         * However, also not using null for no reason, when the list contains data
         * the empty view hide the real content.
         */
        LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View emptyView = li.inflate(R.layout.empty, null);
        // if not added to the parent it's not shown
        ((ViewGroup)lv.getParent()).addView(emptyView);

        // Set programmatically the layout parameters otherwise
        // they will be not calculated if inside XML
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)emptyView.getLayoutParams();
        rlp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        emptyView.setLayoutParams(rlp);

        // retrieve the image with the animation
        ImageView iv = (ImageView)emptyView.findViewById(R.id.empty_list_image);

        // start the animation
        AnimationDrawable frameAnimation = (AnimationDrawable)iv.getDrawable();
        frameAnimation.start();

        lv.setEmptyView(emptyView);

		if (checkNetworkStatus() == NETWORK_UNAVAILABLE) {
            Crouton.showText(
                    this, 
                    "No network, no party", 
                    Style.ALERT);

            return;
        }

		//FIXME: use loaders <http://developer.android.com/guide/topics/fundamentals/loaders.html>
		Thread t = new Thread() {
			public void run() {
                boolean isOk = true;
				RSSReader reader = new RSSReader();

				try {
					mFeed = reader.load("http://radioblackout.org/feed/");
				} catch(Exception e) {
					Crouton.showText(
                        RadioActivity.this,
                        "Problemi nel recupero dei feed",
                        Style.ALERT);

					e.printStackTrace();

                    isOk = false;
				}


                /*
                 * Remove and then add all the items.
                 */
                if (isOk) {
                    lv.post(new Runnable(){
                        public void run() {
                            rssAdapter.clear();
                            rssAdapter.addAll(mFeed.getItems());
                        }
                    });
                }
			}
		};

        // don't reload if there are feed
		if (mFeed == null)
            t.start();
	}

    @Override
    public void onResume() {
        IntentFilter radioServiceStatusIntentFilter =
            new IntentFilter(RadioService.STATUS_CHANGE);

        mRadioServiceReceiver = new RadioServiceReceiver();

        registerReceiver(mRadioServiceReceiver, radioServiceStatusIntentFilter);

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mRadioServiceReceiver);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_play:
				startStopStream(null);
				break;
		}
		return super.onOptionsItemSelected(item);
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
        adaptMenuButtonToState();
	}

	private void setBannerMessage(String msg) {
		((TextView)findViewById(R.id.banner)).setText(msg);
	}

	public void displayBuffering() {
	}

    private void adaptMenuButtonToState() {
        int status = RadioService.getStatus();
        if (status == RadioService.RB_STREAM_STATUS_STARTED) {
            Log.i(TAG, "STARTED");
            showButtonStop();
        } else if (status == RadioService.RB_STREAM_STATUS_STOPPED) {
            Log.i(TAG, "STOPPED");
            showButtonPlay();
        } else {
            showButtonLoading();
        }
    }

    private MenuItem showMenuButtonAs(int item_id, int resource) {
        MenuItem mi = mMenu.findItem(item_id);
        mi.setIcon(resource);

        return mi;
    }

    private void showButtonStop() {
        showMenuButtonAs(R.id.menu_play, R.drawable.ic_pause);
    }

    private void showButtonPlay() {
        showMenuButtonAs(R.id.menu_play, R.drawable.ic_play);
    }

    private void showButtonLoading() {
        MenuItem mi = showMenuButtonAs(R.id.menu_play, R.drawable.ic_refresh_animation);

        AnimationDrawable iconAnimation =
            (AnimationDrawable)mi.getIcon();

        iconAnimation.start();
        Log.i(TAG, "animation is running: " + iconAnimation.isRunning());
    }
}
