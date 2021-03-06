/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.onboarding.number_entry

interface NumberEntryListener {
    fun onMobileNumberValidated(mobileNumber: String)
    fun dismissFragment()
}
