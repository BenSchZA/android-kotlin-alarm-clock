/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FileUtils Class
 *
 * Utility class for app file management.
 *
 * @author bscholtz
 * @version 1
 * @since 13/08/17
 */

public class FileUtils {

    /**
     * Copy file contents from one file to another.
     *
     * @param src source file, usually a temp file
     * @param dst destination file, usually initially an empty file
     * @throws IOException if unsuccessful
     */

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Copy file contents from one file stream to another file.
     *
     * @param in source file stream, e.g. from an intent for Rooster file sharing
     * @param dst destination file, usually initially an empty file
     * @throws IOException if unsuccessful
     * @see com.roostermornings.android.activity.SplashActivity
     */

    public static void copyFromStream(InputStream in, File dst) throws IOException {
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
