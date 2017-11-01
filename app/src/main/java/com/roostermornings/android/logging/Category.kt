package com.roostermornings.android.logging

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table

/**
 * Created by bscholtz on 2017/11/01.
 */

@Table(name="Categories")
class Category: Model() {
    @Column(name="Name")
    lateinit var name: String
}