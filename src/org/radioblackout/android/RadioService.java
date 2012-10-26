package org.radioblackout.android;

import android.os.*;
import android.app.*;
import android.content.*;
import android.media.*;
import android.util.Log;
import android.widget.*;


/**
 * This class implements a foreground service with a status bar notification
 * with radio controls. Clicking on this will open the RadioActivity.
 *
 * The code is inspired from
 *  https://developer.android.com/guide/topics/media/mediaplayer.html
 *  https://github.com/commonsguy/cw-android/blob/master/Notifications/FakePlayer/src/com/commonsware/android/fakeplayerfg/PlayerService.java
 *
 *  In order to test how work the app with different network condition
 *
 *   network speed 14.4 80
 *   network delay gprs
 *
 * TODO: use a secondary thread to control playback status like https://gist.github.com/916157
 * but using getCurrentPosition()
 *
 */
public class RadioService  extends Service implements MediaPlayer.OnPreparedListener {
	private static final String TAG = "RadioService";
	private static String URL = "http://stream.radioblackout.org/blackout-low.mp3";
	private static final String ACTION_PLAY = "com.example.action.PLAY";

	private static final String ACTION_STOP = "com.example.action.STOP";
	private static final String ACTION_STATUS = "com.example.action.STATUS";

    public static final String STATUS_CHANGE = "org.radioblackout.android.STATUS_CHANGE";

    public static final int RB_STREAM_STATUS_STOPPED = 0;
    public static final int RB_STREAM_STATUS_LOADING = 1;
    public static final int RB_STREAM_STATUS_STARTED = 2;
    private static int RB_STREAM_STATUS = 0;

	MediaPlayer mMediaPlayer = null;

	Notification mNote = null;

	@Override
	public void onCreate() {
		super.onCreate();

		Log.i(TAG, "onCreate()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

    private void announceStatusChange() {
        Intent i = new Intent(STATUS_CHANGE);

        sendBroadcast(i);
    }

	private void initMediaPlayer() {
		if (mMediaPlayer == null)
			mMediaPlayer = new MediaPlayer();

		if (mMediaPlayer == null)
			Log.i("new MediaPlayer()", "null");

		try {
			mMediaPlayer.setDataSource(URL);
		} catch (java.io.IOException e) {
			Log.e(TAG, e.getMessage());
		}
		mMediaPlayer.setOnBufferingUpdateListener(
			new MediaPlayer.OnBufferingUpdateListener() {
				@Override
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					Log.i("RadioActivity", "buffering: " + percent);
				}
			}
		);
		mMediaPlayer.setOnErrorListener(
			new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.e("RadioActivity FTW", what + " " + extra);
					/* non gestisco l'errore */
					return false;
				}
			}
		);

		mMediaPlayer.setOnPreparedListener(this);
		mMediaPlayer.prepareAsync(); // prepare async to not block main thread
	}

	/**
	 * https://developer.android.com/guide/topics/ui/notifiers/notifications.html#CustomExpandedView
	 */
	private void createNotificationStatus() {
		// TODO: remove useless reinstanciation
		Notification note =
			new Notification(R.drawable.icon,
				"Can you hear the music?",
				System.currentTimeMillis());

		Intent i = new Intent(this, RadioActivity.class);

		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
				Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setTextViewText(R.id.title, "Radio Blackout 105.25 FM");
        contentView.setImageViewResource(R.id.image, R.drawable.radio);
        note.contentView = contentView;
        note.contentIntent = pi;
		note.flags |= Notification.FLAG_NO_CLEAR;

		Log.i(TAG, "start foreground service");
		startForeground(1337, note);

		mNote = note;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		Log.i(TAG, "action: " + action);
		// if asked for PLAY and the media player doesn't exist or not playing, play it!!!
		// FIXME: if is asked during buffering?
		if (action.equals(ACTION_PLAY) && (mMediaPlayer == null || !mMediaPlayer.isPlaying())) {
            RB_STREAM_STATUS = RB_STREAM_STATUS_LOADING;
            announceStatusChange();
			initMediaPlayer();
		} else if(action.equals(ACTION_STOP)) {
            RB_STREAM_STATUS = RB_STREAM_STATUS_STOPPED;
			mMediaPlayer.stop();
            mMediaPlayer.reset();
			mMediaPlayer = null;

			// remove the notification
			stopForeground(true);
            announceStatusChange();
        }

		return START_NOT_STICKY;
	}

	/** Called when MediaPlayer is ready */
	public void onPrepared(MediaPlayer player) {
		player.start();
        RB_STREAM_STATUS = RB_STREAM_STATUS_STARTED;
        announceStatusChange();
		createNotificationStatus();
	}

	@Override
	public void onDestroy() {
		if (mMediaPlayer != null)
			mMediaPlayer.release();
	}

	/**
	 * This method is called when we want to start this service
	 */
	static public void start(Context caller) {
		Intent i = new Intent(caller, RadioService.class);

		i.setAction(ACTION_PLAY);
		caller.startService(i);
	}

	static public void stop(Context caller) {
		Intent i = new Intent(caller, RadioService.class);

		i.setAction(ACTION_STOP);
		caller.startService(i);
	}

    static public int getStatus() {
        return RB_STREAM_STATUS;
    }

}
