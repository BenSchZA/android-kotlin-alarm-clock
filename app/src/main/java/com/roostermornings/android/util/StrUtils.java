/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

/**
 * Useful methods not included in the String class.
 *
 * @author bscholtz
 * @version 1
 * @since 16/05/17
 */

public class StrUtils {

    /**
     * Check that a String object is neither null, nor empty, and hence safe to process
     * @param str The possibly null or empty String to be validated
     * @return Is the String not null and not empty?
     */

    public static boolean notNullOrEmpty(String str) {
        return (str != null && !"".equals(str));
    }

    /**
     * As the name says... create a pair on Strings that can be passed as an object to a function,
     * in an array, etc. Predominantly used in the MyContactsController class
     * @param <K> String pair first element
     * @param <V> String pair second element
     * @see MyContactsController
     */

    public static class StringPair<K extends String, V extends String> {
        //https://stackoverflow.com/questions/6271731/whats-the-best-way-to-return-a-pair-of-values-in-java

        private final K k;
        private final V v;

        public StringPair(K k, V v) {
            this.k = k;
            this.v = v;
        }

        public K getK() {
            return k;
        }

        public V getV() {
            return v;
        }

        public boolean notNullOrEmpty() {
            return StrUtils.notNullOrEmpty(k) && StrUtils.notNullOrEmpty(v);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringPair)) return false;

            StringPair<?, ?> that = (StringPair<?, ?>) o;

            return getV().equals(that.getV()) && getK().equals(that.getK());
        }

        @Override
        public int hashCode() {
            int result = getK().hashCode();
            result = 31 * result + getV().hashCode();
            return result;
        }
    }
}
