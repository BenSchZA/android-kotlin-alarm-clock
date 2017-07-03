/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Channel implements Serializable {

    public Integer current_rooster_cycle_iteration;
    public Boolean new_alarms_start_at_first_iteration;
    public Integer priority;
    public String description;
    public String name;
    public String photo;
    public Integer rooster_count;
    public boolean active;
    public String uid;

    @Exclude
    private Boolean selected = false;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Channel() {
    }

    public Channel(int current_rooster_cycle_iteration, boolean new_alarms_start_at_first_iteration, String description, String name, String photo, int rooster_count, boolean active, boolean selected, String uid, Integer priority) {
        this.current_rooster_cycle_iteration = current_rooster_cycle_iteration;
        this.new_alarms_start_at_first_iteration = new_alarms_start_at_first_iteration;
        this.description = description;
        this.name = name;
        this.photo = photo;
        this.rooster_count = rooster_count;
        this.active = active;
        this.selected = selected;
        this.uid = uid;
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public int getCurrent_rooster_cycle_iteration() {
        return current_rooster_cycle_iteration;
    }

    public void setCurrent_rooster_cycle_iteration(int current_rooster_cycle_iteration) {
        this.current_rooster_cycle_iteration = current_rooster_cycle_iteration;
    }

    public boolean isNew_alarms_start_at_first_iteration() {
        return new_alarms_start_at_first_iteration;
    }

    public void setNew_alarms_start_at_first_iteration(boolean new_alarms_start_at_first_iteration) {
        this.new_alarms_start_at_first_iteration = new_alarms_start_at_first_iteration;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Exclude
    public boolean isSelected() {
        return selected;
    }

    @Exclude
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
