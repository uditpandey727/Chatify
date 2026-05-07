package com.udit.chatify.other;

import android.view.View;
import android.widget.ImageButton;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessageFunctions {

    // Setting Message Date & LastSeen , etc
    public static String getMessageDate(Date date) {
        if (isToday2(date)) {
            return "Today";
        } else if (isYesterday2(date)) {
            return "Yesterday";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            return dateFormat.format(date);
        }
    }
    public static String getLastSeenString(long timestamp) {
        Calendar currentCalendar = Calendar.getInstance();
        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTimeInMillis(timestamp);

        if(currentCalendar.getTimeInMillis() - timestamp <=60000){
            return "Online";
        }
        else if (isToday(currentCalendar, messageCalendar)) {
            // Today
            SimpleDateFormat dateFormat = new SimpleDateFormat("'Last seen today at' h:mm a", Locale.getDefault());
            return dateFormat.format(new Date(timestamp));
        } else if (isYesterday(currentCalendar, messageCalendar)) {
            // Yesterday
            SimpleDateFormat dateFormat = new SimpleDateFormat("'Last seen yesterday at' h:mm a", Locale.getDefault());
            return dateFormat.format(new Date(timestamp));
        } else {
            // Other days
            SimpleDateFormat dateFormat = new SimpleDateFormat("'Last seen' d MMM 'at' h:mm a", Locale.getDefault());
            return dateFormat.format(new Date(timestamp));
        }
    }
    private static boolean isToday(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
                && calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }
    private static boolean isYesterday(Calendar currentCalendar, Calendar messageCalendar) {
        currentCalendar.add(Calendar.DAY_OF_YEAR, -1);
        return isToday(currentCalendar, messageCalendar);
    }
    private static boolean isToday2(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH);
    }
    private static boolean isYesterday2(Date date) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return yesterday.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                && yesterday.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                && yesterday.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static void scrollToLastMessage(RecyclerView msgList, RecyclerView.Adapter adapter) {
        msgList.postDelayed(() -> {
            int itemCount = adapter.getItemCount();
            if (itemCount > 0) {
                msgList.scrollToPosition(itemCount - 1);
            }
        }, 200); // Add a slight delay to allow the keyboard to fully open/close
    }
    public static void scrollListener(RecyclerView msgList, RecyclerView.Adapter adapter, ImageButton scrollToLastButton){
        LinearLayoutManager layoutManager = (LinearLayoutManager) msgList.getLayoutManager();
        int lastVisibleItemPosition = 0;
        if (layoutManager != null) {lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();}
        // Check if the last visible item position is the last item in the adapter
        if (lastVisibleItemPosition == adapter.getItemCount() - 1) {
            scrollToLastButton.setVisibility(View.GONE);
        }else {
            scrollToLastButton.setVisibility(View.VISIBLE);
        }
    }
}
