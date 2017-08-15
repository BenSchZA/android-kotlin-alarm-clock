/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MessageStatusFragmentActivity;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FileUtils;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;
import com.roostermornings.android.util.Toaster;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageStatusReceivedListAdapter extends RecyclerView.Adapter<MessageStatusReceivedListAdapter.ViewHolder> implements Filterable {
    private ArrayList<DeviceAudioQueueItem> mDataset = new ArrayList<>();
    private Activity mActivity;
    private Context context;

    private String fragmentType = "";

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private int currentMediaPlayerSourceID = -1;

    private ArrayList<SeekBar> seekBarArray = new ArrayList<>();

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.message_status_friend_profile_pic)
        ImageView imgProfilePic;

        @BindView(R.id.message_status_friend_profile_name)
        TextView txtName;

        @BindView(R.id.txtInitials)
        TextView txtInitials;

        @BindView(R.id.audio_listen)
        ImageButton listenImageButton;

        @BindView(R.id.audio_favourite)
        ImageButton favouriteImageButton;

        @BindView(R.id.audio_share)
        ImageButton shareImageButton;

        @BindView(R.id.message_status_friend_date)
        TextView date;

        @BindView(R.id.seekbar)
        SeekBar seekBar;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void resetMediaPlayer() {
        if(runnable != null) mHandler.removeCallbacks(runnable);
        mediaPlayer.reset();
        clearAudioArtifacts(null);
        notifyDataSetChanged();
    }

    public void add(int position, DeviceAudioQueueItem item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position, DeviceAudioQueueItem item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    public void refreshAll(ArrayList<DeviceAudioQueueItem> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MessageStatusReceivedListAdapter(ArrayList<DeviceAudioQueueItem> myDataset, Activity activity, String type) {
        mDataset = myDataset;
        mActivity = activity;
        fragmentType = type;

        //Update seekbar on UI thread
        runnable = new Runnable() {
            @Override
            public void run() {
                SeekBar seekBar = getSeekBarByID(currentMediaPlayerSourceID);

                if(seekBar != null && mediaPlayer.isPlaying()) {
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);

                    if (seekBar.getMax() >= mCurrentPosition) {
                        mHandler.removeCallbacks(runnable);
                        mHandler.postDelayed(runnable, 1000);
                    }
                }
            }
        };
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageStatusReceivedListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                      int viewType) {
        context = parent.getContext();
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_message_status_received, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new MessageStatusReceivedListAdapter.ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MessageStatusReceivedListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final DeviceAudioQueueItem audioItem = mDataset.get(position);

        holder.txtName.setText(audioItem.getName());

        //Check if image is null, else previous images reused
        if(StrUtils.notNullOrEmpty(audioItem.getPicture())) {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(1f);
            holder.txtInitials.setText("");
            setProfilePic(audioItem.getPicture(), holder, audioItem);
        } else {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(0.3f);
            holder.txtInitials.setText(RoosterUtils.getInitials(audioItem.getName()));
        }

        if(getSeekBarByID(audioItem.getId()) == null) {
            //Remove padding so that seekbar thumb aligns with text view
            holder.seekBar.setPadding(0, 0, 0, 0);
            //"Attach" the seekbar to a unique audio item
            holder.seekBar.setId(audioItem.getId());
            //Set the maximum value to the audio item length
            holder.seekBar.setMax(0);
            holder.seekBar.setMax(getAudioItemLength(audioItem.getId()) / 1000);
            //Listen for seekbar progress updates, and mediaPlayer.seekTo()
            holder.seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
            //Add the seekbar to a unique ArrayList
            addUniqueSeekBar(holder.seekBar);
        }

        //If audioitem is active, make seekbar visible
        if (audioItem.isPlaying() || audioItem.isPaused()) {
            holder.seekBar.setVisibility(View.VISIBLE);
        } else {
            holder.seekBar.setProgress(0);
            holder.seekBar.setVisibility(View.GONE);
        }

        if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) {
            holder.shareImageButton.setVisibility(View.VISIBLE);
        } else {
            holder.shareImageButton.setVisibility(View.INVISIBLE);
        }

        holder.shareImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fileName = audioItem.getFilename();
                File file = new File(mActivity.getFilesDir() + "/" + fileName);

                try {
                    //Create file in download directory to share
                    File shareFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RoosterShareFile.mp3");

                    //Check if directory exists, if not try create it
                    if(shareFile.getParentFile().exists() || shareFile.getParentFile().mkdirs()) {

                        //Copy file contents to new public file
                        FileUtils.copy(file, shareFile);

                        //If file is valid, open a share dialog
                        if (shareFile.isFile()) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.putExtra(Intent.EXTRA_STREAM,
                                    Uri.fromFile(shareFile));
                            intent.setType("audio/*");
                            mActivity.startActivity(Intent.createChooser(intent, "Share audio"));
                            shareFile.deleteOnExit();

                            if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) {
                                FA.Log(FA.Event.roosters_channel_share.class, FA.Event.roosters_channel_share.Param.channel_title, audioItem.getName());
                            } else {
                                FA.Log(FA.Event.roosters_social_share.class, null, null);
                            }
                        } else {
                            Toaster.makeToast(mActivity, "Failed to share audio", Toast.LENGTH_SHORT);
                        }

                    } else {
                        Toaster.makeToast(mActivity, "Failed to make directory", Toast.LENGTH_SHORT);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Toaster.makeToast(mActivity, "Failed to share audio", Toast.LENGTH_SHORT);
                }
            }
        });

        if(fragmentType.equals(Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_FAVOURITE)
                && holder.seekBar.getVisibility() != View.VISIBLE) {

            String dateString;

            if(DateUtils.isToday(audioItem.getDate_created())) {

                SimpleDateFormat parseFormat = new SimpleDateFormat(" - h a", Locale.getDefault());
                Date date = new Date(audioItem.getDate_created());
                dateString = "Today" + parseFormat.format(date);
            } else if (audioItem.getDate_created() >
                    (Calendar.getInstance().getTimeInMillis() - Constants.TIME__MILLIS_1_WEEK)) {

                SimpleDateFormat parseFormat = new SimpleDateFormat("EEEE - h a", Locale.getDefault());
                Date date = new Date(audioItem.getDate_created());
                dateString = parseFormat.format(date);
            } else {
                SimpleDateFormat parseFormat = new SimpleDateFormat("EE, MMMM dd, yyyy - h a", Locale.getDefault());
                Date date = new Date(audioItem.getDate_created());
                dateString = parseFormat.format(date);
            }

            holder.date.setText(dateString);
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.listenImageButton.setSelected(audioItem.isPlaying());
        holder.listenImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                holder.seekBar.setVisibility(View.VISIBLE);

                if(audioItem.isPlaying()) {
                    holder.listenImageButton.setSelected(false);
                    pauseRooster(audioItem);
                } else {
                    holder.listenImageButton.setSelected(true);
                    playRooster(audioItem);
                }
            }
        });

        holder.txtName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.listenImageButton.callOnClick();
            }
        });

        holder.date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.listenImageButton.callOnClick();
            }
        });

        holder.imgProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.favouriteImageButton.callOnClick();
            }
        });

        holder.favouriteImageButton.setSelected(audioItem.getFavourite() > 0);
        holder.favouriteImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(holder.favouriteImageButton.isSelected()) {
                    if(fragmentType.equals(Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_FAVOURITE)) {

                        new MaterialDialog.Builder(mActivity)
                                .theme(Theme.LIGHT)
                                .content(R.string.dialog_confirm_social_rooster_unfavourite)
                                .positiveText(R.string.confirm)
                                .negativeText(R.string.cancel)
                                .negativeColorRes(R.color.grey)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        holder.favouriteImageButton.setSelected(false);
                                        ((MessageStatusFragmentActivity)mActivity).favouriteRooster(audioItem, false);
                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                //Do something after 200ms
                                                remove(holder.getAdapterPosition(), audioItem);
                                                resetMediaPlayer();
                                            }
                                        }, 200);
                                    }
                                })
                                .show();

                    } else {

                        holder.favouriteImageButton.setSelected(false);
                        audioItem.setFavourite(Constants.AUDIO_TYPE_FAVOURITE_FALSE);
                        ((MessageStatusFragmentActivity)mActivity).favouriteRooster(audioItem, false);

                    }
                } else {

                    holder.favouriteImageButton.setSelected(true);
                    audioItem.setFavourite(Constants.AUDIO_TYPE_FAVOURITE_TRUE);
                    ((MessageStatusFragmentActivity)mActivity).favouriteRooster(audioItem, true);

                }
            }
        });
    }

    private void clearAudioArtifacts(DeviceAudioQueueItem disclude) {
        for (DeviceAudioQueueItem audioItem:
                mDataset) {
            if(!audioItem.equals(disclude)) {
                audioItem.setPlaying(false);
                audioItem.setPaused(false);
            }
        }
        notifyDataSetChanged();
    }

    private void setProfilePic(String url, final MessageStatusReceivedListAdapter.ViewHolder holder, final DeviceAudioQueueItem audioItem) {
        try{
            Picasso.with(context).load(url)
                    .resize(50, 50)
                    .centerCrop()
                    .into(holder.imgProfilePic, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) holder.imgProfilePic.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                            holder.txtInitials.setText("");
                            holder.imgProfilePic.setAlpha(1f);
                            holder.imgProfilePic.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError() {
                            holder.imgProfilePic.setAlpha(0.3f);
                            holder.txtInitials.setText(RoosterUtils.getInitials(audioItem.getName()));
                        }
                    });
        } catch(IllegalArgumentException e){
            e.printStackTrace();
            holder.txtInitials.setText(RoosterUtils.getInitials(audioItem.getName()));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public Filter getFilter() {

        final Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                mDataset = (ArrayList<DeviceAudioQueueItem>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<DeviceAudioQueueItem> filteredContacts = new ArrayList<>();

                //Perform your search here using the search constraint string
                constraint = constraint.toString().toLowerCase();
                for (int i = 0; i < mDataset.size(); i++) {
                    String contactData = mDataset.get(i).getName();
                    if (contactData.toLowerCase().contains(constraint.toString()))  {
                        filteredContacts.add(mDataset.get(i));
                    }
                }

                results.count = filteredContacts.size();
                results.values = filteredContacts;

                return results;
            }
        };

        return filter;
    }

    private int getAudioItemLength(int ID) {
        DeviceAudioQueueItem audioItem = getAudioItemByID(ID);
        int durationInt = 0;

        if(audioItem != null) {
            File file = new File(mActivity.getFilesDir() + "/" + audioItem.getFilename());

            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(file.getPath());
            String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            durationInt = Integer.parseInt(durationStr);
        }
        return durationInt;
    }

    private DeviceAudioQueueItem getAudioItemByID(int ID) {
        for (DeviceAudioQueueItem audioItem:
             mDataset) {
            if(audioItem.getId() == ID) return audioItem;
        }
        return null;
    }

    private void addUniqueSeekBar(SeekBar seekBar) {
        for (SeekBar sb:
             seekBarArray) {
            if(sb.getId() == seekBar.getId()) {
                seekBarArray.remove(sb);
                seekBarArray.add(seekBar);
                return;
            }
        }
        seekBarArray.add(seekBar);
    }

    private SeekBar getSeekBarByID(int ID) {
        for (SeekBar seekBar:
                seekBarArray) {
            if(seekBar.getId() == ID) return seekBar;
        }
        return null;
    }

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            DeviceAudioQueueItem audioItem = getAudioItemByID(currentMediaPlayerSourceID);

            if(fromUser && audioItem != null) {
                mediaPlayer.seekTo(progress * 1000);
                mediaPlayer.start();
                if(audioItem.isPaused()) {
                    mHandler.removeCallbacks(runnable);
                    mHandler.postDelayed(runnable, 1000);
                }
                audioItem.setPaused(false);
                audioItem.setPlaying(true);
                clearAudioArtifacts(audioItem);
            }
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void playRooster(final DeviceAudioQueueItem audioItem) {

        Boolean isPaused = !mediaPlayer.isPlaying() && mediaPlayer.getCurrentPosition() > 1;
        if(isPaused && currentMediaPlayerSourceID == audioItem.getId()) {
            mediaPlayer.start();
            audioItem.setPaused(false);
            audioItem.setPlaying(true);
            mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, 1000);
            clearAudioArtifacts(audioItem);
            return;
        } else {
            audioItem.setPaused(false);
            audioItem.setPlaying(true);
        }

        clearAudioArtifacts(audioItem);

        File file = new File(mActivity.getFilesDir() + "/" + audioItem.getFilename());
        mediaPlayer.reset();

        try {
            mediaPlayer.setDataSource(file.getPath());
            currentMediaPlayerSourceID = audioItem.getId();

            mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, 1000);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mediaPlayer) {

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            audioItem.setPaused(false);
                            audioItem.setPlaying(false);
                            clearAudioArtifacts(null);
                            if(runnable != null) mHandler.removeCallbacks(runnable);
                        }
                    });

                    mediaPlayer.start();

                    if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) {
                        FA.Log(FA.Event.roosters_channel_play.class, FA.Event.roosters_channel_play.Param.channel_title, audioItem.getName());
                    } else {
                        FA.Log(FA.Event.roosters_social_play.class, null, null);
                    }
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener(){
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    return true;
                }
            });

            //Prepare mediaplayer on new thread: onCompletion or onError listener called
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseRooster(DeviceAudioQueueItem audioItem) {
        mediaPlayer.pause();
        audioItem.setPaused(true);
        audioItem.setPlaying(false);
        if(runnable != null) mHandler.removeCallbacks(runnable);
    }
}
