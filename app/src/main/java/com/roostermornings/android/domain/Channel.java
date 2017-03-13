/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by steven on 2017/02/15.
 */

@IgnoreExtraProperties
public class Channel implements Serializable {

    private int current_rooster_iteration_cycle;
    private String description;
    private String name;
    private String photo;
    private int rooster_count;
    private HashMap<String, Boolean> subscribers;
    private boolean active;
    private boolean selected;
    private String uid;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Channel() {
    }

    public Channel(int current_rooster_iteration_cycle,
                   String description,
                   String name,
                   String photo,
                   int rooster_count,
                   HashMap<String, Boolean> subscribers,
                   boolean active,
                   boolean selected,
                   String uid) {

        this.current_rooster_iteration_cycle = current_rooster_iteration_cycle;
        this.description = description;
        this.name = name;
        this.photo = photo;
        this.rooster_count = rooster_count;
        this.subscribers = subscribers;
        this.active = active;
        this.selected = selected;
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setId(String id) {
        this.uid = id;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getCurrent_rooster_iteration_cycle() {
        return current_rooster_iteration_cycle;
    }

    public void setCurrent_rooster_iteration_cycle(int current_rooster_iteration_cycle) {
        this.current_rooster_iteration_cycle = current_rooster_iteration_cycle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getRooster_count() {
        return rooster_count;
    }

    public void setRooster_count(int rooster_count) {
        this.rooster_count = rooster_count;
    }

    public HashMap<String, Boolean> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(HashMap<String, Boolean> subscribers) {
        this.subscribers = subscribers;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
