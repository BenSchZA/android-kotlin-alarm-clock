/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

public class StrUtils {
    public static boolean notNullOrEmpty(String str) {
        return (str != null && !"".equals(str));
    }

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
