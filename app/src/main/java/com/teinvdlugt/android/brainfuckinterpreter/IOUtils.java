package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IOUtils {

    public static File theDirectory(Context context) {
        return context.getExternalFilesDir(null);
    }

    /**
     * Saves code to theDirectory(). Returns whether successful.
     * TODO public directory...
     */
    public static boolean save(Context context, String code, String filename) {
        if (isExternalStorageWritable()) {
            try {
                File file = new File(theDirectory(context), filename);
                OutputStream os = new FileOutputStream(file);
                os.write(code.getBytes());
                os.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Retrieves list of all files in theDirectory(),
     * or null in the case of an IO error/inaccessible storage.
     */
    public static List<FilesActivity.FileInfo> loadFileList(Context context) {
        if (isExternalStorageReadable()) {
            try {
                List<FilesActivity.FileInfo> list = new ArrayList<>();
                for (File file : theDirectory(context).listFiles()) {
                    String filename = file.getName();
                    Date fileDate = new Date(file.lastModified());
                    list.add(new FilesActivity.FileInfo(filename, fileDate));
                }
                return list;
            } catch (SecurityException e) {
                e.printStackTrace();
                // TODO log Firebase error
                return null;
            }
        } else return null;
    }


    private static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private static boolean isExternalStorageReadable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }
}
