package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                // Sort alphabetically
                Collections.sort(list, new Comparator<FilesActivity.FileInfo>() {
                    @Override
                    public int compare(FilesActivity.FileInfo o1, FilesActivity.FileInfo o2) {
                        return o1.getFilename().compareTo(o2.getFilename());
                    }
                });
                return list;
            } catch (SecurityException e) {
                e.printStackTrace();
                // TODO log Firebase error
                return null;
            }
        } else return null;
    }

    /**
     * Retrieves list of all files in theDirectory(),
     * returns empty list in case of error.
     */
    public static List<String> loadFilenameList(Context context) {
        if (isExternalStorageReadable()) {
            List<String> result = new ArrayList<>();
            try {
                for (File file : theDirectory(context).listFiles())
                    result.add(file.getName());
            } catch (SecurityException e) { /* ignored */ }
            return result;
        } else return new ArrayList<>();
    }

    /**
     * Remove files with filenames from theDirectory().
     */
    public static void removeFiles(Context context, List<String> filenames) {
        if (isExternalStorageWritable()) {
            for (String filename : filenames) {
                new File(theDirectory(context), filename).delete();
            }
        }
    }

    public static boolean rename(Context context, String oldName, String newName) {
        if (isExternalStorageWritable()) {
            return new File(theDirectory(context), oldName).renameTo(new File(theDirectory(context), newName));
        } else return false;
    }

    public static String loadFile(Context context, String filename) {
        if (isExternalStorageReadable()) {
            try {
                BufferedReader bR = new BufferedReader(new FileReader(new File(theDirectory(context), filename)));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bR.readLine()) != null) {
                    result.append(line).append('\n');
                }
                // Remove last newline char
                if (result.length() > 0) result.deleteCharAt(result.length() - 1);
                return result.toString();
            } catch (IOException e) {
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
