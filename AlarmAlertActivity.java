/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;


public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    private long mNoteId;
    private String mSnippet;
    private static final int SNIPPET_PREW_MAX_LEN = 60;
    MediaPlayer mPlayer;

    @Override
    // 当Activity创建时调用此方法  
protected void onCreate(Bundle savedInstanceState) {  
    super.onCreate(savedInstanceState); // 调用父类的onCreate方法  
    // 请求无标题窗口特性  
    requestWindowFeature(Window.FEATURE_NO_TITLE);  
  
    final Window win = getWindow(); // 获取当前窗口  
    // 添加标志，允许在锁屏时显示窗口  
    win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);  
  
    // 检查屏幕是否已点亮  
    if (!isScreenOn()) {  
        // 如果屏幕未点亮，添加多个标志来保持屏幕常亮、点亮屏幕、允许在屏幕点亮时锁屏、调整布局装饰  
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON  
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);  
    }  
  
    // 获取启动此Activity的Intent  
    Intent intent = getIntent();  
  
    try {  
        // 从Intent中提取笔记ID  
        mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));  
        // 根据笔记ID获取笔记摘要  
        mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);  
        // 如果摘要长度超过最大预览长度，则截断并添加省略号  
        mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,  
                SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)  
                : mSnippet;  
    } catch (IllegalArgumentException e) {  
        // 如果捕获到异常，打印堆栈跟踪并返回  
        e.printStackTrace();  
        return;  
    }  
  
    // 初始化MediaPlayer对象  
    mPlayer = new MediaPlayer();  
    // 检查笔记是否在数据库中可见  
    if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {  
        // 如果可见，显示操作对话框并播放警报声音  
        showActionDialog();  
        playAlarmSound();  
    } else {  
        // 如果不可见，结束Activity  
        finish();  
    }  
}  
  
// 检查屏幕是否已点亮  
private boolean isScreenOn() {  
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);  
    return pm.isScreenOn();  
}  
  
// 播放警报声音  
private void playAlarmSound() {  
    // 获取默认的警报铃声URI  
    Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);  
  
    // 获取静音模式下受影响的音频流  
    int silentModeStreams = Settings.System.getInt(getContentResolver(),  
            Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);  
  
    // 根据静音模式设置音频流类型  
    if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {  
        mPlayer.setAudioStreamType(silentModeStreams);  
    } else {  
        mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);  
    }  
    try {  
        // 设置数据源并准备播放  
        mPlayer.setDataSource(this, url);  
        mPlayer.prepare();  
        mPlayer.setLooping(true); // 设置循环播放  
        mPlayer.start(); // 开始播放  
    } catch (Exception e) {  
        // 捕获并打印异常  
        e.printStackTrace();  
    }  
}  
  
// 显示操作对话框  
private void showActionDialog() {  
    // 创建AlertDialog.Builder对象  
    AlertDialog.Builder dialog = new AlertDialog.Builder(this);  
    dialog.setTitle(R.string.app_name); // 设置对话框标题  
    dialog.setMessage(mSnippet); // 设置对话框消息  
    dialog.setPositiveButton(R.string.notealert_ok, this); // 设置确定按钮及其点击事件监听器  
    if (isScreenOn()) {  
        // 如果屏幕已点亮，设置取消按钮及其点击事件监听器  
        dialog.setNegativeButton(R.string.notealert_enter, this);  
    }  
    dialog.show().setOnDismissListener(this); // 显示对话框并设置对话框消失时的监听器  
}  
  
// 当对话框按钮被点击时调用此方法  
public void onClick(DialogInterface dialog, int which) {  
    switch (which) {  
        case DialogInterface.BUTTON_NEGATIVE:  
            // 如果点击的是取消按钮，启动NoteEditActivity以编辑笔记  
            Intent intent = new Intent(this, NoteEditActivity.class);  
            intent.setAction(Intent.ACTION_VIEW);  
            intent.putExtra(Intent.EXTRA_UID, mNoteId);  
            startActivity(intent);  
            break;  
        default:  
            break;  
    }  
}  
  
// 当对话框消失时调用此方法  
public void onDismiss(DialogInterface dialog) {  
    // 停止播放警报声音并结束Activity  
    stopAlarmSound();  
    finish();  
}  
  
// 停止播放警报声音  
private void stopAlarmSound() {  
    if (mPlayer != null) {  
        mPlayer.stop(); // 停止播放  
        mPlayer.release(); // 释放资源  
        mPlayer = null; // 将MediaPlayer对象置为null  
    }  
}
