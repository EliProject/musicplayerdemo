package com.example.eli.musicplayerdemo;

/**
 * Created by liyuanqin on 17-9-6.
 */


/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Sven Dubbeld
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Activity that shows media controls.
 */
public class MediaActivity extends Activity
        implements MediaSessionManager.OnActiveSessionsChangedListener, SeekBar.OnSeekBarChangeListener {

    /**
     * "pref_key_show_media"
     */
    private boolean mPrefShowMedia = true;

    private static final String TAG = "####";

    // Current track
    private TextView mMediaAlbum;
    private TextView mMediaArtist;
    private TextView mMediaTitle;

    // Next track
    private TextView mMediaUpNextArtist;
    private TextView mMediaUpNextTitle;

    // Controls
    private ImageView mMediaNext;
    private ImageView mMediaPlay;
    private ProgressBar mMediaPlayProgress;
    private ImageView mMediaPrev;
    //private ImageView mMediaVolDown;
    private ImageView mMediaVolUp;
    private ImageView mMusicPlayerIcon;

    // Seek bar
    private TextView mMediaTimeEnd;
    private TextView mMediaTimePosition;
    private SeekBar mMediaTimeSeekBar;

    private MediaController mMediaController = null;
    private MediaSessionManager mMediaSessionManager;

    private SharedPreferences mSharedPref;
    private Timer mTimer;
    private TimerTask mUpdatePositionTask;
    private UpdatePositionRunnable mUpdatePositionRunnable = new UpdatePositionRunnable();

    private Button mSelectMusicePlayerBtn;

    /**
     * Sets the layout and starts task for updating the seek bar.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied
     *                           in {@link #onSaveInstanceState(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate()......");

        // Set layout
        setContentView(R.layout.activity_main);

        // Get views
        mMediaTimePosition = ((TextView) findViewById(R.id.media_time_position));
        mMediaTimeSeekBar = ((SeekBar) findViewById(R.id.media_time_seek_bar));
        mMediaTimeEnd = ((TextView) findViewById(R.id.media_time_end));
        mMediaTitle = ((TextView) findViewById(R.id.media_title));
        mMediaArtist = ((TextView) findViewById(R.id.media_artist));
        mMediaAlbum = ((TextView) findViewById(R.id.media_album));
        //mMediaVolDown = ((ImageView) findViewById(R.id.media_vol_down));
        mMediaPrev = ((ImageView) findViewById(R.id.media_prev));
        mMediaPlay = ((ImageView) findViewById(R.id.media_play));
        mMediaPlayProgress = ((ProgressBar) findViewById(R.id.media_play_progress));
        mMediaNext = ((ImageView) findViewById(R.id.media_next));
        mMediaVolUp = ((ImageView) findViewById(R.id.media_vol_up));
        mMusicPlayerIcon = ((ImageView) findViewById(R.id.music_player_icon));
        mMediaUpNextTitle = ((TextView) findViewById(R.id.media_up_next_title));
        mMediaUpNextArtist = ((TextView) findViewById(R.id.media_up_next_artist));
        mSelectMusicePlayerBtn = ((Button) findViewById(R.id.select_music_player));

        mMediaTimeSeekBar.setOnSeekBarChangeListener(this);
        //mMediaVolDown.setOnClickListener(mMediaControlsListener);
        mMediaPrev.setOnClickListener(mMediaControlsListener);
        mMediaPlay.setOnClickListener(mMediaControlsListener);
        mMediaNext.setOnClickListener(mMediaControlsListener);
        mMediaVolUp.setOnClickListener(mMediaControlsListener);
        mMusicPlayerIcon.setOnClickListener(mMediaControlsListener);
        mSelectMusicePlayerBtn.setOnClickListener(mMediaControlsListener);

        mMediaTitle.setSelected(true);

        initButtonReceiver();

        mMediaUpNextTitle.setVisibility(View.GONE);
        mMediaUpNextArtist.setVisibility(View.GONE);
        mMediaTimePosition.setVisibility(View.GONE);
        mMediaTimeSeekBar.setVisibility(View.GONE);
        mMediaTimeEnd.setVisibility(View.GONE);
        mMediaAlbum.setVisibility(View.GONE);
        mMediaVolUp.setVisibility(View.GONE);

        // Timer for seek bar
        mTimer = new Timer();
        mUpdatePositionTask = new TimerTask() {
            public void run() {
                if (mMediaController != null) {
                    PlaybackState state = mMediaController.getPlaybackState();
                    MediaMetadata metadata = mMediaController.getMetadata();
                    if ((state != null) && (metadata != null) && (state.getState() == PlaybackState.STATE_PLAYING)) {
                        long rawPosition = state.getPosition();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("m:ss");
                        String position = dateFormat.format(new Date(rawPosition));

                        long rawEnd = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                        String end = "-" + dateFormat.format(new Date(rawEnd - rawPosition));
                        int progress = (int) (100.0f * ((float) rawPosition / (float) rawEnd));
                        mUpdatePositionRunnable.setParams(position, progress, end);
                        runOnUiThread(mUpdatePositionRunnable);
                    }
                }
            }
        };

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mTimer.scheduleAtFixedRate(mUpdatePositionTask, 100l, 1000l);


        initMusicPlayerInfos();
        addOnActiveSessionsChangedListener();
        mShowTimes = 0;
        mMusicPlayerIcon.setImageDrawable(mMusicPlayersList.get(mSelectedItem).icon);
    }

    /**
     * Detaches listeners.
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (mPrefShowMedia) {
            mMediaSessionManager.removeOnActiveSessionsChangedListener(this);
            if (mMediaController != null) {
                mMediaController.unregisterCallback(mMediaCallback);
                Log.d(TAG, "MediaController removed");
            }
        }
    }

    /**
     * Attaches listeners and gets preferences.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Get preferences
        //mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //mPrefShowMedia = true;
        Log.i(TAG, "onReume...mSelectedItem=" + mSelectedItem);

        // Check media access and connect to session
        /*if (mPrefShowMedia) {
        }*/
        //showCustomNotification();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mSharedPref.edit().putInt(SELECTED_ITEM,mSelectedItem).apply();
        Log.v(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        //mSharedPref.edit().putInt(SELECTED_ITEM,mSelectedItem).apply();
    }


    /**
     * Callback for the MediaController.
     */
    private MediaController.Callback mMediaCallback = new MediaController.Callback() {

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo playbackInfo) {
            super.onAudioInfoChanged(playbackInfo);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            Log.d(TAG, "onMetadataChanged, 切歌,");

            if (metadata != null) {

                // Update media container
                Log.d(TAG, "onMetadataChanged,新的歌曲信息: title=" + metadata.getText(MediaMetadata.METADATA_KEY_TITLE)+" artist="+metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
                mMediaTitle.setText(metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
                mMediaArtist.setText(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
                mMediaAlbum.setText(metadata.getText(MediaMetadata.METADATA_KEY_ALBUM));

                updateQueue(mMediaController.getQueue(), mMediaController.getPlaybackState());

                // Update position
                mUpdatePositionTask.run();
                showCustomNotification();
            } else {
                mMediaTitle.setText(getString(R.string.play_music));
                mMediaArtist.setText("");
                Log.d(TAG, "onMetadataChanged,这个播放器没有歌曲!...");
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);

            Log.v(TAG, "onPlaybackStateChanged state=" + state);

            if ((mMediaPlay != null) && (state != null)) {
                // Update play/pause button
                switch (state.getState()) {
                    case PlaybackState.STATE_BUFFERING:
                    case PlaybackState.STATE_CONNECTING:
                        mMediaPlay.setVisibility(View.GONE);
                        mMediaPlayProgress.setVisibility(View.VISIBLE);
                        mMediaPlay.setImageResource(R.drawable.ic_pause_white_24dp);
                        //isPlay = true;
                        break;
                    case PlaybackState.STATE_PLAYING:
                        mMediaPlay.setVisibility(View.VISIBLE);
                        mMediaPlayProgress.setVisibility(View.GONE);
                        mMediaPlay.setImageResource(R.drawable.ic_pause_white_24dp);
                        //isPlay = true;
                        break;
                    default:
                        mMediaPlay.setVisibility(View.VISIBLE);
                        mMediaPlayProgress.setVisibility(View.GONE);
                        mMediaPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                        //isPlay = false;
                        break;
                }

                updateQueue(mMediaController.getQueue(), state);
                showCustomNotification();

                // Update position
                mUpdatePositionTask.run();
            }
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            super.onQueueChanged(queue);

            updateQueue(queue, mMediaController.getPlaybackState());
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }
    };
    /**
     * OnClickListener for the media controls.
     */
    private View.OnClickListener mMediaControlsListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mMediaController != null) {

                // Handle media controls
                switch (v.getId()) {
                    case R.id.media_prev:
                        mMediaController.getTransportControls().skipToPrevious();
                        break;
                    case R.id.media_play:
                        mOldSelectedItem = mSelectedItem;
                        switch (mMediaController.getPlaybackState().getState()) {
                            case PlaybackState.STATE_BUFFERING:
                            case PlaybackState.STATE_CONNECTING:
                                mMediaPlay.setVisibility(View.GONE);
                                mMediaPlayProgress.setVisibility(View.VISIBLE);
                            case PlaybackState.STATE_PLAYING:
                                isPlay = false;
                                mMediaController.getTransportControls().pause();
                                Log.i(TAG,"onClick..点击了暂停音乐按钮");
                                break;
                            default:
                                isPlay = true;
                                mMediaController.getTransportControls().play();
                                Log.i(TAG,"onClick..点击了播放音乐按钮...当前因播放器包名="+mMediaController.getPackageName());
                                break;
                        }
                        break;
                    case R.id.media_next:
                        mMediaController.getTransportControls().skipToNext();
                        break;
                    case R.id.media_vol_up:
                        mMediaController.adjustVolume(AudioManager.ADJUST_RAISE, 0);
                        break;
                    case R.id.select_music_player:
                        showWindow(v);
                        break;
                    case R.id.music_player_icon:
                        Log.i(TAG, "点击了音乐播放器的icon,这个音乐播放器的componentName=" + mMusicPlayerPkgNameList.get(mSelectedItem));
                        Intent intent = getPackageManager().getLaunchIntentForPackage(mMusicPlayerPkgNameList.get(mSelectedItem));
                        startActivity(intent);
                        break;
                }
            } else {
                Intent mediaIntent = new Intent(Intent.ACTION_MAIN);
                mediaIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mediaIntent);

            }
        }
    };


    private PopupWindow mSelectMusicPlayerPopupWIndow;
    private View view;
    private ListView mMusicPlayersListView;
    private ArrayList<MusicPlayers> mMusicPlayersList;
    private ArrayList<String> mMusicPlayerPkgNameList = new ArrayList<>();
    private int mSelectedItem;
    private int mOldSelectedItem;
    private NotificationManager mNotificationManager;


    class MusicPlayers {
        Drawable icon;
        String title;
    }

    private void initMusicPlayerInfos() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://test.mp3");
        intent.setData(uri);
        intent.setDataAndType(uri, "audio/mpeg");
        ResolveInfo resolveInfo;
        mMusicPlayersList = new ArrayList<MusicPlayers>();
        List<ResolveInfo> currentResolveList = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        int currentResolveListSize = currentResolveList.size();
        for (int i = 0; i < currentResolveListSize; i++) {
            resolveInfo = currentResolveList.get(i);
            if (TextUtils.equals("com.android.browser", resolveInfo.activityInfo.packageName)) {
                continue;
            }
            MusicPlayers musicPlayers = new MusicPlayers();
            musicPlayers.title = resolveInfo.loadLabel(getPackageManager()).toString();
            musicPlayers.icon = resolveInfo.loadIcon(getPackageManager());
            mMusicPlayersList.add(musicPlayers);
            mMusicPlayerPkgNameList.add(resolveInfo.activityInfo.packageName);
            Log.i(TAG,"mMusicPlayerPkgNameList="+mMusicPlayerPkgNameList);
            if (TextUtils.equals("com.meizu.media.music", resolveInfo.activityInfo.packageName)) {
                mSelectedItem = i;
            }
        }
    }

    private void showWindow(View parent) {

        //if (mSelectMusicPlayerPopupWIndow == null) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = layoutInflater.inflate(R.layout.music_player_list, null);

        mMusicPlayersListView = view.findViewById(R.id.music_players);
        // 加载数据
        MusicPlayerAdapter groupAdapter = new MusicPlayerAdapter(this, mMusicPlayersList);
        mMusicPlayersListView.setAdapter(groupAdapter);
        // 创建一个PopuWidow对象
        mSelectMusicPlayerPopupWIndow = new PopupWindow(view, 600, 450);
        //}

        // 使其聚集
        mSelectMusicPlayerPopupWIndow.setFocusable(true);
        // 设置允许在外点击消失
        mSelectMusicPlayerPopupWIndow.setOutsideTouchable(true);

        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
        mSelectMusicPlayerPopupWIndow.setBackgroundDrawable(new BitmapDrawable());
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // 显示的位置为:屏幕的宽度的一半-PopupWindow的高度的一半
        int xPos = windowManager.getDefaultDisplay().getWidth() / 2 -
                mSelectMusicPlayerPopupWIndow.getWidth() / 2;
        Log.i(TAG, "xPos:" + xPos);

        mSelectMusicPlayerPopupWIndow.showAsDropDown(parent, xPos, 0);

        mMusicPlayersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {

                mMusicPlayerIcon.setImageDrawable(mMusicPlayersList.get(position).icon);
                mOldSelectedItem = mSelectedItem;
                mSelectedItem = position;
                addOnActiveSessionsChangedListener();

                Toast.makeText(MediaActivity.this,
                        "点击了第" + mSelectedItem + "个", Toast.LENGTH_SHORT).show();

                if (mSelectMusicPlayerPopupWIndow != null) {
                    mSelectMusicPlayerPopupWIndow.dismiss();
                }
            }
        });
    }

    /**
     * Updates the display of the queue.
     *
     * @param queue The queue.
     * @param state The playback state.
     */
    private void updateQueue(List<MediaSession.QueueItem> queue, PlaybackState state) {
        if (state != null) {
            long queueId = state.getActiveQueueItemId();

            if (queueId != MediaSession.QueueItem.UNKNOWN_ID) {

                int i;
                for (i = 0; queue.get(i).getQueueId() != queueId; i++) ;
                MediaSession.QueueItem nextItem = queue.get(i + 1);

                mMediaUpNextTitle.setText(nextItem.getDescription().getTitle());
                mMediaUpNextArtist.setText(nextItem.getDescription().getSubtitle());
            }
        }
    }

    /**
     * Called when the list of active {@link MediaController MediaControllers} changes.
     *
     * @param controllers List of active MediaControllers
     */
    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        Log.i(TAG, "MediaController controllers.size=" + controllers.size()+" mMediaController="+mMediaController);
        if (controllers.size() > 0) {

            if (mMediaController != null) {
                //if (!controllers.get(0).getSessionToken().equals(mMediaController.getSessionToken())) {
                // Detach current controller

                Log.i(TAG, "MediaController 要切换到音乐播放器 pkg=" + mMusicPlayerPkgNameList.get(mSelectedItem)
                        +" mSelectedItem="+mSelectedItem+" mOldSelectedItem="+mOldSelectedItem
                        +" playstat="+mMediaController.getPlaybackState().getState());
                MediaController mediaController = indexOfControllers(controllers, mMusicPlayerPkgNameList.get(mSelectedItem));
                if (mediaController== null) {
                    Log.w(TAG, "音乐播放器: "+mMusicPlayerPkgNameList.get(mSelectedItem)+"不在后台, 无法切换");
                    return;
                }

                if (mOldSelectedItem != mSelectedItem && mMediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) { //先把现在播放的暂停
                    Log.i(TAG, "切换音乐播放器，先把现在播放的音乐暂停．．．．．");
                    mMediaController.getTransportControls().pause();
                    isPlay = false;
                }

                mMediaController.unregisterCallback(mMediaCallback);
                Log.d(TAG, "MediaController removed");
                mMediaController = null;

                // Attach new controller
                mMediaController = mediaController;
                Log.d(TAG, "MediaController Attach new controller ..mSelectedItem="+mSelectedItem+" mOldSelectedItem="+mOldSelectedItem);
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                if (mOldSelectedItem != mSelectedItem) {
                    //mMediaController.getTransportControls().play();//还是焦点问题
                    Log.i(TAG, "播放歌曲");
                    mOldSelectedItem = mSelectedItem;
                }
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.d(TAG, "MediaController set: " + mMediaController.getPackageName());
            } else {//应用启动，时候会跑这里
                // Attach new controller
                mMediaController = controllers.get(0);
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                Log.d(TAG, "111..Attach new controller.MediaController set: " + mMediaController.getPackageName());
            }

        } else {

            mMediaArtist.setText("");
            mMediaAlbum.setText("");
            mMediaTitle.setText(R.string.media_idle);

            /*Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            PackageManager pm = getPackageManager();
            ResolveInfo info = pm.resolveActivity(i, 0);

            Drawable icon = info.loadIcon(pm);
            mMediaPlay.setPadding(20, 20, 20, 20);
            mMediaPlay.setImageDrawable(icon);

            mMediaPlay.setImageTintList(null);*/
        }
    }

    private void printLog(List<MediaController> controllers) {
        for (int i = 0;i < controllers.size();i++) {
            MediaController mediaController = controllers.get(i);
            Log.d(TAG, "第"+i+"个　pkgName="+mediaController.getPackageName()
                    +" PlaybackState="+mediaController.getPlaybackState()
                    +" getQueueTitle="+mediaController.getQueueTitle());
        }
    }

    private MediaController indexOfControllers(List<MediaController> controllers, String pkgName) {
        for (int i = 0;i < controllers.size();i++) {
            MediaController mediaController = controllers.get(i);
            Log.d(TAG, "第"+i+"个　pkgName="+mediaController.getPackageName()
                    +" PlaybackState="+mediaController.getPlaybackState()
                    +" getQueueTitle="+mediaController.getQueueTitle());
            if (TextUtils.equals(pkgName, mediaController.getPackageName())) {
                return mediaController;
            }
        }
        return null;
    }

    /**
     * Notification that the progress level has changed.
     *
     * @param seekBar  The SeekBar whose progress has changed
     * @param progress The current progress level. This will be in the range 0..max where max was
     *                 set by {@link SeekBar#setMax(int)}. (The default value for max is 100.)
     * @param fromUser True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.media_time_seek_bar:
                if (fromUser && mMediaController != null) {
                    // Seek track to position
                    long duration = mMediaController.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);
                    long position = (long) (progress / 100.0f * (float) duration);
                    mMediaController.getTransportControls().seekTo(position);
                }
                break;
        }
    }


    /**
     * 带按钮的通知栏点击广播接收
     */
    public void initButtonReceiver() {
        bReceiver = new ButtonBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CLICK);
        registerReceiver(bReceiver, intentFilter);
    }

    /**
     * 通知栏按钮广播
     */
    public ButtonBroadcastReceiver bReceiver;

    /**
     * 通知栏按钮点击事件对应的ACTION
     */
    public final static String ACTION_CLICK = "com.notifications.intent.action.customviewclick";
    public final static String INTENT_BUTTONID_TAG = "ButtonId";
    public final static String INDEX_OF = "IndexOf";
    /**
     * 上一首 按钮点击 ID
     */
    public final static int BUTTON_PREV_ID = 1;
    /**
     * 播放/暂停 按钮点击 ID
     */
    public final static int BUTTON_PALY_ID = 2;
    /**
     * 下一首 按钮点击 ID
     */
    public final static int BUTTON_NEXT_ID = 3;
    /**
     * 音乐播放器和播放按钮的显示切换
     */
    public final static int BUTTON_EXCHANGE_ID = 4;

    /**
     * 下一显示列表 按钮点击 ID
     */
    public final static int BUTTON_NEXT_SHOW_ID = 5;
    /**
     * 显示通知的列表中的第１个
     */
    public final static int BUTTON_FIRST_IN_LIST_ID = 6;
    /**
     * 显示通知的列表中的第2个
     */
    public final static int BUTTON_SECOND_IN_LIST_ID = 7;
    /**
     * 显示通知的列表中的第3个
     */
    public final static int BUTTON_THIRD_IN_LIST_ID = 8;
    /**
     * 删除通知
     */
    public final static int BUTTON_DELETE_NOFICY_ID = 9;
    /**
     * 是否在播放
     */
    public boolean isPlay = false;
    /**
     * 第几次显示播放器的列表
     */
    private int mShowTimes = 0;

    /**
     * 广播监听按钮点击时间
     */
    public class ButtonBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            Log.i(TAG,"onReceive......");
            String action = intent.getAction();
            if (action.equals(ACTION_CLICK)) {
                //通过传递过来的ID判断按钮点击属性或者通过getResultCode()获得相应点击事件
                int buttonId = intent.getIntExtra(INTENT_BUTTONID_TAG, 0);
                Log.i(TAG,"buttonId="+buttonId);
                switch (buttonId) {
                    case BUTTON_PREV_ID:
                        Log.d(TAG, "上一首");
                        mMediaController.getTransportControls().skipToPrevious();
                        Toast.makeText(getApplicationContext(), "上一首", Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_PALY_ID:
                        String play_status = "";
                        isPlay = !isPlay;
                        if (isPlay) {
                            mMediaController.getTransportControls().play();
                            play_status = "开始播放";
                        } else {
                            mMediaController.getTransportControls().pause();
                            play_status = "已暂停";
                        }
                        showCustomNotification();
                        Log.d(TAG, play_status);
                        Toast.makeText(getApplicationContext(), play_status, Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_NEXT_ID:
                        Log.d(TAG, "下一首");
                        mMediaController.getTransportControls().skipToNext();
                        Toast.makeText(getApplicationContext(), "下一首", Toast.LENGTH_SHORT).show();
                        break;
                    case BUTTON_EXCHANGE_ID:
                        mShowTimes = 0;
                        mShowMediaController = !mShowMediaController;
                        Log.i(TAG,"BUTTON_EXCHANGE_ID...");
                        showCustomNotification();
                        break;
                    case BUTTON_NEXT_SHOW_ID:
                        mShowTimes++;
                        Log.i(TAG,"BUTTON_NEXT_SHOW_ID...mShowTimes="+mShowTimes);
                        showCustomNotification();
                        break;
                    case BUTTON_FIRST_IN_LIST_ID:
                    case BUTTON_SECOND_IN_LIST_ID:
                    case BUTTON_THIRD_IN_LIST_ID:
                        mSelectedItem = intent.getIntExtra(INDEX_OF, 0);
                        mShowMediaController = !mShowMediaController;
                        Log.i(TAG,"BUTTON_THIRD_IN_LIST_ID...第"+mShowTimes+"列 第"+mSelectedItem+"个");
                        mMusicPlayerIcon.setImageDrawable(mMusicPlayersList.get(mShowTimes).icon);
                        showCustomNotification();
                        mShowTimes = 0;
                        break;
                    case BUTTON_DELETE_NOFICY_ID:
                        mNotificationManager.cancelAll();
                        Log.i(TAG,"mNotificationManager.cancelAll()");
                        break;
                    default:
                        Log.i(TAG,"default.....");
                        break;
                }
            }
        }
    }

    private boolean mShowMediaController = true;

    /**
     * 显示通知，有需要显示或者更新通知的状态有: 切歌(onMetadataChanged),播放状态切换(onPlaybackStateChanged),点击暂停/播放按钮(BUTTON_PALY_ID),
     *  通知栏中在显示播放器列表中点击更多按钮(BUTTON_NEXT_SHOW_ID)或者点击切换按钮(BUTTON_EXCHANGE_ID)或者
     *  点击音乐播放器(BUTTON_FIRST_IN_LIST_ID,BUTTON_SECOND_IN_LIST_ID,BUTTON_THIRD_IN_LIST_ID)
     */
    public void showCustomNotification() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.music_notification_custom_view);
        //API3.0 以上的时候显示按钮，否则消失
        Log.v(TAG, "showCustomNotification mSelectedItem="+mSelectedItem);

        //点击的事件处理
        Intent buttonIntent = new Intent(ACTION_CLICK);

        /* 切换播放器  */
        buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_EXCHANGE_ID);
        PendingIntent intent_exchange = PendingIntent.getBroadcast(this, BUTTON_EXCHANGE_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setImageViewBitmap(R.id.notification_music_player_custom_icon, drawableToBitmap(mMusicPlayersList.get(mSelectedItem).icon));
        remoteViews.setOnClickPendingIntent(R.id.notification_music_player_custom_icon, intent_exchange);

        /* 删除 按钮 */
        buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_DELETE_NOFICY_ID);
        PendingIntent intent_delete = PendingIntent.getBroadcast(this, BUTTON_DELETE_NOFICY_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.delete_notify, intent_delete);

        if (mShowMediaController) {
            remoteViews.setViewVisibility(R.id.notification_music_controller_icon, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.notification_music_player_icon_ll, View.GONE);

            remoteViews.setTextViewText(R.id.notification_music_title, mMediaTitle.getText().toString());
            /* 上一首按钮 */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PREV_ID);
            //这里加了广播，所及INTENT的必须用getBroadcast方法
            PendingIntent intent_prev = PendingIntent.getBroadcast(this, BUTTON_PREV_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_prev, intent_prev);
            /* 播放/暂停  按钮 */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_PALY_ID);
            PendingIntent intent_play = PendingIntent.getBroadcast(this, BUTTON_PALY_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_play, intent_play);
		    /* 下一首 按钮  */
            buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_ID);
            PendingIntent intent_next = PendingIntent.getBroadcast(this, BUTTON_NEXT_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.notification_media_next, intent_next);
            Log.d(TAG, "showCustomNotification isPlay="+isPlay+" title="+mMediaTitle.getText().toString());
            if (isPlay) {
                remoteViews.setImageViewResource(R.id.notification_media_play, R.drawable.ic_pause_white_24dp);
            } else {
                remoteViews.setImageViewResource(R.id.notification_media_play, R.drawable.ic_play_arrow_white_24dp);
            }
        } else {
            remoteViews.setViewVisibility(R.id.notification_music_controller_icon, View.GONE);
            remoteViews.setViewVisibility(R.id.notification_music_player_icon_ll, View.VISIBLE);
            remoteViews.setTextViewText(R.id.notification_music_title, getResources().getString(R.string.select_music_apps));

            int listSize = mMusicPlayersList.size() - mShowTimes * 3;
            Log.i("####","listSize="+listSize+" mShowTimes="+mShowTimes);
            if (listSize == 1) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_FIRST_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, BUTTON_FIRST_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next1);
            } else if (listSize == 2) {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon21, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon22, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_FIRST_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, BUTTON_FIRST_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon21, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_SECOND_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, BUTTON_SECOND_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon22, intent_next2);

            } else if (listSize == 3) {
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.GONE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_FIRST_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, BUTTON_FIRST_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_SECOND_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, BUTTON_SECOND_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_THIRD_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, BUTTON_THIRD_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);
            } else {
                remoteViews.setViewVisibility(R.id.notification_media_icon32, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon31, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.notification_media_icon33, View.VISIBLE);
                remoteViews.setImageViewBitmap(R.id.notification_media_icon31, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon32, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 1).icon));
                remoteViews.setImageViewBitmap(R.id.notification_media_icon33, drawableToBitmap(mMusicPlayersList.get(mShowTimes * 3 + 2).icon));
                remoteViews.setViewVisibility(R.id.notification_media_icon21, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_icon22, View.GONE);
                remoteViews.setViewVisibility(R.id.notification_media_more, View.VISIBLE);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_FIRST_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3);
                PendingIntent intent_next1 = PendingIntent.getBroadcast(this, BUTTON_FIRST_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon31, intent_next1);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_SECOND_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 1);
                PendingIntent intent_next2 = PendingIntent.getBroadcast(this, BUTTON_SECOND_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon32, intent_next2);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_THIRD_IN_LIST_ID);
                buttonIntent.putExtra(INDEX_OF, mShowTimes * 3 + 2);
                PendingIntent intent_next3 = PendingIntent.getBroadcast(this, BUTTON_THIRD_IN_LIST_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_icon33, intent_next3);

                buttonIntent.putExtra(INTENT_BUTTONID_TAG, BUTTON_NEXT_SHOW_ID);
                PendingIntent intent_next4 = PendingIntent.getBroadcast(this, BUTTON_NEXT_SHOW_ID, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.notification_media_more, intent_next4);
            }

        }
        mBuilder.setContent(remoteViews)
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setTicker("正在播放")
                .setPriority(Notification.PRIORITY_MAX)// 设置该通知优先级
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher);
        Notification notify = mBuilder.build();
        notify.flags = Notification.FLAG_AUTO_CANCEL; //通知常驻在锁屏加上一个flag: show_in_keyguard,是flyme自己加的
        //会报错，还在找解决思路
//		notify.contentView = remoteViews;
//		notify.contentIntent = PendingIntent.getActivity(this, 0, new Intent(), 0);
        mNotificationManager.notify(200, notify);
    }

    public PendingIntent getDefalutIntent(int flags){
        PendingIntent pendingIntent= PendingIntent.getActivity(this, 1, new Intent(), flags);
        return pendingIntent;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;

    }

    private List<MediaController> removeDuplicate(List<MediaController> controllers) {
        List<MediaController> newList = new ArrayList<>();
        List<String> newPkgNameList = new ArrayList<>();
        int size = controllers.size();
        for (int i = 0;i<size;i++) {
            if (!newPkgNameList.contains(controllers.get(i).getPackageName())) {
                newPkgNameList.add(controllers.get(i).getPackageName());
                newList.add(controllers.get(i));
            }
        }
        return newList;
    }

    private void addOnActiveSessionsChangedListener() {
        Log.i(TAG, "addOnActiveSessionsChangedListener");
        try {
            mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            ComponentName cn = new ComponentName(this, MyNotificationListenerService.class);
            List<MediaController> controllers = removeDuplicate(mMediaSessionManager.getActiveSessions(cn));
            Log.v(TAG, "controllers=" + controllers + " cn=" + cn);

            /*MediaController mediaController = indexOfControllers(controllers, mMusicPlayerPkgNameList.get(mSelectedItem));
            if (mediaController== null) {
                Log.w(TAG, "音乐播放器: "+mMusicPlayerPkgNameList.get(mSelectedItem)+"不在后台, 无法切换");
                Intent intent = new Intent();
                startActivity();
                return;
            }*/


            printLog(controllers);
            onActiveSessionsChanged(controllers);
            mMediaSessionManager.addOnActiveSessionsChangedListener(this, cn);
        } catch (SecurityException e) {
            Log.e(TAG, "No Notification Access e=" + e, new Exception());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_notification_access_title)
                    .setMessage(R.string.dialog_notification_access_message)
                    .setPositiveButton(R.string.dialog_notification_access_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.dialog_notification_access_negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mSharedPref.edit().putBoolean("pref_key_show_media", false).putBoolean("pref_key_speak_notifications", false).apply();
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Runnable that updates the position of the seek bar.
     */
    private class UpdatePositionRunnable implements Runnable {
        String mEnd = "-0:00";
        String mPosition = "0:00";
        int mProgress = 0;

        /**
         * Updates the position of the seek bar.
         */
        @Override
        public void run() {
            mMediaTimePosition.setText(mPosition);
            mMediaTimeSeekBar.setProgress(mProgress);
            mMediaTimeEnd.setText(mEnd);
        }

        /**
         * Sets the variables used to display the position.
         *
         * @param position The position of the seek bar.
         * @param progress The text to show to the left of the seek bar
         * @param end      The text to show to the right of the seek bar
         */
        public void setParams(String position, int progress, String end) {
            mPosition = position;
            mProgress = progress;
            mEnd = end;
        }
    }
}

