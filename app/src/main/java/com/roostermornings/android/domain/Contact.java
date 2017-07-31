/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.roostermornings.android.util.StrUtils;

import java.util.HashMap;

public class Contact {
    private String name = "";
    private HashMap<String, String> numbers = new HashMap<>();
    private String primaryNumber = "";

    private Boolean selected = false; //this is important for list of friends that need to be selected eg for creating a new alarm

    public Contact(String name) {
        this.name = name;
    }

    public void addNumber(String nsnNumber, String isoNumber) {
        numbers.put(nsnNumber, isoNumber);
    }

    public String getPrimaryNumber() {
        for (String NSNNumber:
             numbers.keySet()) {
            if(StrUtils.notNullOrEmpty(NSNNumber)) {
                //Country code value + NSN number key = primary number
                primaryNumber = numbers.get(NSNNumber) + NSNNumber;
                break;
            }
        }
        return primaryNumber;
    }

    public void setPrimaryNumber(String primaryNumber) {
        this.primaryNumber = primaryNumber;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, String> getNumbers() {
        return numbers;
    }

    public void setNumbers(HashMap<String, String> numbers) {
        this.numbers = numbers;
    }
}
