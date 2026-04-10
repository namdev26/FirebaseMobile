package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver để xử lý thông báo nhắc nhở phim sắp chiếu
 */
public class MovieReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String movieTitle = intent.getStringExtra("movieTitle");
        String showtime = intent.getStringExtra("showtime");

        String title = "Nhắc nhở: Phim sắp chiếu! 🎬";
        String body = "Phim \"" + movieTitle + "\" sẽ chiếu lúc " + showtime + ". Hãy chuẩn bị đi xem phim!";

        MyFirebaseMessagingService.showNotification(context, title, body);
    }
}