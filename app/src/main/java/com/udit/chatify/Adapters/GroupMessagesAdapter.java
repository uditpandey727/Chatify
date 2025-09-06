package com.udit.chatify.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.Models.Message;
import com.udit.chatify.Models.User;
import com.udit.chatify.R;
import com.udit.chatify.activities.ImageViewerActivity;
import com.udit.chatify.activities.VideoViewerActivity;
import com.udit.chatify.databinding.DeleteDialogBinding;
import com.udit.chatify.databinding.ItemHeaderBinding;
import com.udit.chatify.databinding.ItemReceivedGroupBinding;
import com.udit.chatify.databinding.ItemSendGroupBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class GroupMessagesAdapter extends RecyclerView.Adapter{

    final int ITEM_HEADER = 0;
    final int ITEM_SENT = 1;
    final int ITEM_RECEIVE = 2;

    Context context;
    ArrayList<Message> messages;
    ArrayList<Message> selectedMessages;
    GroupMessageAdapterListener listener;


    public GroupMessagesAdapter(Context context, ArrayList<Message> messages, GroupMessageAdapterListener listener){
        this.context = context;
        this.messages = messages;
        this.listener = listener;
        selectedMessages = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == ITEM_HEADER) {
            // Inflate header layout
            View headerView = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(headerView);
        } else if (viewType == ITEM_SENT) {
            // Inflate sent message layout
            View sentView = inflater.inflate(R.layout.item_send_group, parent, false);
            return new SentViewHolder(sentView);
        } else{
            // Inflate received message layout
            View receivedView = inflater.inflate(R.layout.item_received_group, parent, false);
            return new ReceiveViewHolder(receivedView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message  message = messages.get(position);
        if (message.isHeader()){
            return ITEM_HEADER;
        }
        else if(Objects.equals(FirebaseAuth.getInstance().getUid(), message.getSenderId())){
            return ITEM_SENT;
        }else{
            return ITEM_RECEIVE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        int[] reactions = new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .withPopupColor(Color.BLACK)
                // Popup background alpha value between 0 (full transparent) and 255 (full opaque) (default: 230)
                .withPopupAlpha(190)
                .build();

        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {
            if(pos<0){
                return true;
            }
            if (holder.getClass() == SentViewHolder.class) {
                SentViewHolder viewHolder =(SentViewHolder) holder;
                viewHolder.binding.feeling.setImageResource(reactions[pos]);
                if (message.getFeeling()==pos) {
                    viewHolder.binding.feeling.setVisibility(View.GONE);
                }else{
                    viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                }
            }
            else{
                ReceiveViewHolder viewHolder =(ReceiveViewHolder) holder;
                viewHolder.binding.feeling.setImageResource(reactions[pos]);
                if (message.getFeeling()==pos) {
                    viewHolder.binding.feeling.setVisibility(View.GONE);
                }else{
                    viewHolder.binding.feeling.setVisibility(View.VISIBLE);
                }
            }

            if (message.getFeeling()==pos) {
                message.setFeeling(-1);
            }else{
                message.setFeeling(pos);
            }
//
            FirebaseDatabase.getInstance().getReference()
                    .child("public").child("messages")
                    .child(message.getMessageId()).setValue(message);
            listener.setEmoji();
            return true; // true is closing popup, false is requesting a new selection
        });
        int selectedColor = ContextCompat.getColor(context, R.color.selectedColor);

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.binding.date.setText(message.getMessageDate());
        } else if (holder instanceof SentViewHolder){
            SentViewHolder viewHolder =(SentViewHolder) holder;

            if(message.getMessage().equals("photo")){
                viewHolder.binding.imageinMsg.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl()).placeholder(R.drawable.avatar).into(viewHolder.binding.imageinMsg);
                viewHolder.binding.imageinMsg.setOnClickListener(v -> {
                    // Launch ImageViewerActivity and pass the image URL or path
                    Intent intent = new Intent(context, ImageViewerActivity.class);
                    intent.putExtra("image_url", message.getImageUrl());
                    intent.putExtra("image_title", "Image");
                    context.startActivity(intent);
                });
            }
            else if (message.getMessage().equals("video")) {
                viewHolder.binding.imageinMsg.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getVideoThumbnailUrl()).placeholder(R.drawable.avatar).into(viewHolder.binding.imageinMsg);
                viewHolder.binding.imageinMsg.setOnClickListener(v -> {
                    // Launch VideoViewerActivity and pass the video URL
                    Intent intent = new Intent(context, VideoViewerActivity.class);
                    intent.putExtra("video_url", message.getImageUrl());
                    context.startActivity(intent);
                });
            }
            else{
                viewHolder.binding.imageinMsg.setVisibility(View.GONE);
                viewHolder.binding.message.setVisibility(View.VISIBLE);
                SpannableString spannableString = new SpannableString(message.getMessage());
                Linkify.addLinks(spannableString, Linkify.ALL);

                URLSpan[] spans = spannableString.getSpans(0, spannableString.length(), URLSpan.class);
                for (URLSpan span : spans) {
                    int start = spannableString.getSpanStart(span);
                    int end = spannableString.getSpanEnd(span);
                    int flags = spannableString.getSpanFlags(span);

                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.GREEN); // Set your desired Link color here
                    spannableString.setSpan(colorSpan, start, end, flags);
                }
                viewHolder.binding.message.setMovementMethod(LinkMovementMethod.getInstance());
                viewHolder.binding.message.setText(spannableString);
            }

            FirebaseDatabase.getInstance()
                    .getReference().child("users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                User user = snapshot.getValue(User.class);
                                viewHolder.binding.name.setText("@"+user.getName());
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            long time = message.getTimestamp();
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            viewHolder.binding.time.setText(dateFormat.format(new Date(time)));

            if (message.isSeen()) {
                viewHolder.binding.tickMark.setImageResource(R.drawable.received);
            } else if (message.isSent()) {
                viewHolder.binding.tickMark.setImageResource(R.drawable.sent);
            }
            if(message.getFeeling() >= 0){
                viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.feeling.setVisibility(View.VISIBLE);
            }else{
                viewHolder.binding.feeling.setVisibility(View.GONE);
            }
            // Check if the message is selected
            boolean isSelected = selectedMessages.contains(message);
            // Update the background color based on the selection state
            if (isSelected) {
                holder.itemView.setBackgroundColor(selectedColor);
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            viewHolder.itemView.setOnLongClickListener(v -> {
                // Handle the long-press event
                boolean isMessageSelected = listener.onMessageSelected(message);
                if(isMessageSelected){
                    viewHolder.itemView.setBackgroundColor(selectedColor);
                    if(selectedMessages.size()==1) {
                        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                        popup.onTouch(viewHolder.itemView, motionEvent);
                    }
                }else{
                    viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                }
                return true;
            });
            viewHolder.binding.message.setOnLongClickListener(v -> {
                // Handle the long-press event
                boolean isMessageSelected = listener.onMessageSelected(message);
                if(isMessageSelected){
                    viewHolder.itemView.setBackgroundColor(selectedColor);
                    if(selectedMessages.size()==1) {
                        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                        popup.onTouch(viewHolder.itemView, motionEvent);
                    }
                }else{
                    viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                }
                return true;
            });
        }
        else {
            ReceiveViewHolder viewHolder = (ReceiveViewHolder) holder;

            if (message.getMessage().equals("photo")) {
                viewHolder.binding.imageinMsg.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUrl()).placeholder(R.drawable.avatar).into(viewHolder.binding.imageinMsg);
                viewHolder.binding.imageinMsg.setOnClickListener(v -> {
                    // Launch ImageViewerActivity and pass the image URL or path
                    Intent intent = new Intent(context, ImageViewerActivity.class);
                    intent.putExtra("image_url", message.getImageUrl());
                    context.startActivity(intent);
                });
            }else if (message.getMessage().equals("video")) {
                viewHolder.binding.imageinMsg.setVisibility(View.VISIBLE);
                viewHolder.binding.message.setVisibility(View.GONE);
                Glide.with(context).load(message.getVideoThumbnailUrl()).placeholder(R.drawable.avatar).into(viewHolder.binding.imageinMsg);
                viewHolder.binding.imageinMsg.setOnClickListener(v -> {
                    // Launch VideoViewerActivity and pass the video URL
                    Intent intent = new Intent(context, VideoViewerActivity.class);
                    intent.putExtra("video_url", message.getImageUrl());
                    context.startActivity(intent);
                });
            }  else {
                viewHolder.binding.imageinMsg.setVisibility(View.GONE);
                viewHolder.binding.message.setVisibility(View.VISIBLE);

                SpannableString spannableString = new SpannableString(message.getMessage());
                Linkify.addLinks(spannableString, Linkify.ALL);

                URLSpan[] spans = spannableString.getSpans(0, spannableString.length(), URLSpan.class);
                for (URLSpan span : spans) {
                    int start = spannableString.getSpanStart(span);
                    int end = spannableString.getSpanEnd(span);
                    int flags = spannableString.getSpanFlags(span);

                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.GREEN); // Set your desired Link color here
                    spannableString.setSpan(colorSpan, start, end, flags);
                }
                viewHolder.binding.message.setMovementMethod(LinkMovementMethod.getInstance());
                viewHolder.binding.message.setText(spannableString);
            }

            FirebaseDatabase.getInstance()
                    .getReference().child("users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                User user = snapshot.getValue(User.class);
                                viewHolder.binding.name.setText("@"+user.getName());
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            long time = message.getTimestamp();
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            viewHolder.binding.time.setText(dateFormat.format(new Date(time)));

            if(message.getFeeling() >= 0) {
                viewHolder.binding.feeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.feeling.setVisibility(View.VISIBLE);
            } else {
                viewHolder.binding.feeling.setVisibility(View.GONE);
            }

            // Check if the message is selected
            boolean isSelected = selectedMessages.contains(message);
            // Update the background color based on the selection state
            if (isSelected) {
                holder.itemView.setBackgroundColor(selectedColor);
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            viewHolder.binding.message.setOnLongClickListener(v -> {
                // Handle the long-press event
                boolean isMessageSelected = listener.onMessageSelected(message);
                if(isMessageSelected){
                    viewHolder.itemView.setBackgroundColor(selectedColor);
                    if(selectedMessages.size()==1) {
                        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                        popup.onTouch(viewHolder.itemView, motionEvent);
                    }
                }else{
                    viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                }
                return true;
            });
            viewHolder.itemView.setOnLongClickListener(v -> {
                // Handle the long-press event
                boolean isMessageSelected = listener.onMessageSelected(message);
                if(isMessageSelected){
                    viewHolder.itemView.setBackgroundColor(selectedColor);
                    if(selectedMessages.size()==1) {
                        MotionEvent motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);
                        popup.onTouch(viewHolder.itemView, motionEvent);
                    }
                }else{
                    viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        ItemHeaderBinding binding;
        public HeaderViewHolder(View itemView) {
            super(itemView);
            binding = ItemHeaderBinding.bind(itemView);
        }
    }

    public class SentViewHolder extends RecyclerView.ViewHolder{
        ItemSendGroupBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemSendGroupBinding.bind(itemView);
        }
    }

    public class ReceiveViewHolder extends RecyclerView.ViewHolder{
        ItemReceivedGroupBinding binding;
        public ReceiveViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemReceivedGroupBinding.bind(itemView);
        }
    }
    public void updateSelectedMessages(ArrayList<Message> selectedMessages, boolean isUpdateData) {
        this.selectedMessages = selectedMessages;
        if(isUpdateData){
            notifyDataSetChanged();
        }
    }
    public interface GroupMessageAdapterListener {
        boolean onMessageSelected(Message message);
        void setEmoji();
    }
}
