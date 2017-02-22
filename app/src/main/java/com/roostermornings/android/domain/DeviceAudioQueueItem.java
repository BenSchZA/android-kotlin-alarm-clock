package com.roostermornings.android.domain;

/**
 * Created by bscholtz on 2/21/17.
 */



public class DeviceAudioQueueItem {
    private int id;
    private String queue_id;
    private String filename;
    private long alarm_id;
    private long date_created;
    private String sender_id;
    private String sender_name;
    private String sender_pic;

    public long getDate_created() {
        return date_created;
    }

    public void setDate_created(long date_created) {
        this.date_created = date_created;
    }

    public DeviceAudioQueueItem(){};

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQueue_id() {
        return queue_id;
    }

    public void setQueue_id(String queue_id) {
        this.queue_id = queue_id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(long alarm_id) {
        this.alarm_id = alarm_id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getSender_pic() {
        return sender_pic;
    }

    public void setSender_pic(String sender_pic) {
        this.sender_pic = sender_pic;
    }
}
