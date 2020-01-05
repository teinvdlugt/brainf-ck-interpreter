package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class IOUtils {

    /**
     * Saves code to context.getExternalFilesDir(null). Returns whether successful.
     * TODO public directory...
     */
    public static boolean save(Context context, String code, String filename) {
        if (isExternalStorageWritable()) {
            try {
                File file = new File(context.getExternalFilesDir(null), filename);
                OutputStream os = new FileOutputStream(file);
                os.write(code.getBytes());
                os.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("spaghetti", "fail 1");
                return false;
            }
        } else {
            Log.d("spaghetti", "fail 2: " + Environment.getExternalStorageState());
            return false;
        }
    }


    private static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private static boolean isExternalStorageReadable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }
}
