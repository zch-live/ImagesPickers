package cn.xz.imagespickers.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {


    private final static String PATTERN = "yyyyMMddHHmmss";


    public static String createTmpFile(Context context, String filePath) {
        String timeStamp = new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date());

        String baseFolder = Environment.getExternalStorageDirectory() + "/record/";
        File f = new File(baseFolder);
        if (!f.exists()) {
            boolean b = f.mkdirs();
            if (!b) {
                baseFolder = context.getExternalFilesDir(null).getAbsolutePath() + "/";
            }
        }
        return baseFolder;
    }

    public static File createTmpFile1(Context context, String filePath) {

        String timeStamp = new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date());

        String externalStorageState = Environment.getExternalStorageState();

        File dir = new File(Environment.getExternalStorageDirectory() + filePath);

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            if (!dir.exists()) {
                dir.getParentFile().mkdirs();
            }
            return new File(dir, timeStamp + ".jpg");
        } else {
            File cacheDir = context.getCacheDir();
            return new File(cacheDir, timeStamp + ".jpg");
        }

    }


    public static void createFile(String filePath) {
        String externalStorageState = Environment.getExternalStorageState();

        File dir = new File(Environment.getExternalStorageDirectory().getPath() + filePath);
        File cropFile = new File(Environment.getExternalStorageDirectory().getPath() + filePath + "/crop");

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            if (!cropFile.exists()) {
                cropFile.mkdirs();
            }

            File file = new File(dir, ".nomedia");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


}