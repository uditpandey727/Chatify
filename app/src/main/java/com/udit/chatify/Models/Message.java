package com.udit.chatify.Models;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class Message {
    private String messageId, message, senderId, imageUrl, messageDate, videoThumbnailUrl;
    private long timestamp;
    private long feeling = -1;
    private boolean isSeen, isSent, isHeader, isChatGptMessage, isNewMessage;

    public Message() {}

    public Message(String message, String senderId, long timestamp, boolean isNewMessage) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.isNewMessage = isNewMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFeeling() {
        return (int) feeling;
    }

    public void setFeeling(long feeling) {
        this.feeling = feeling;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    public String getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(String messageDate) {
        this.messageDate = messageDate;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public void setHeader(boolean header) {
        isHeader = header;
    }

    public boolean isChatGptMessage() {
        return isChatGptMessage;
    }

    public void setChatGptMessage(boolean chatGptMessage) {
        isChatGptMessage = chatGptMessage;
    }

    public void setVideoThumbnailUrl(Context context,String videoUrl) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(videoUrl, new HashMap<>());
        Bitmap thumbnail = retriever.getFrameAtTime();

        // Convert the Bitmap thumbnail to a file and get its URL
        File thumbnailFile = new File(context.getCacheDir(), "thumbnail.jpg");
        try (OutputStream out = new FileOutputStream(thumbnailFile)) {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.videoThumbnailUrl =  thumbnailFile.getAbsolutePath();
    }

    public String getVideoThumbnailUrl() {
        return videoThumbnailUrl;
    }

    public boolean isNewMessage() {
        return isNewMessage;
    }

    public void setNewMessage(boolean newMessage) {
        isNewMessage = newMessage;
    }
}

