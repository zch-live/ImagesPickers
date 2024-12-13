package cn.xz.imagespickers.utils;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    public static String createTmpFile(Context context, String fileName) {
        String baseFolder = Environment.getExternalStorageDirectory() + "/record/";
        File f = new File(baseFolder);
        if (!f.exists()) {
            boolean b = f.mkdirs();
            if (!b) {
                baseFolder = context.getExternalFilesDir(null).getAbsolutePath() + "/";
            }
        }
        return baseFolder  +  System.currentTimeMillis() + ".png";
    }

    public static File createTmpFile1(Context context, String filePath) {

        String externalStorageState = Environment.getExternalStorageState();

        File dir = new File(Environment.getExternalStorageDirectory() + filePath);

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            if (!dir.exists()) {
                dir.getParentFile().mkdirs();
            }
            return new File(dir, System.currentTimeMillis() + ".jpg");
        } else {
            File cacheDir = context.getCacheDir();
            return new File(cacheDir, System.currentTimeMillis() + ".jpg");
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

    public static void deleteFile(Context context,String filePath){
        File file = new File(filePath);
        //删除系统缩略图
        context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{filePath});
        //删除手机中图片
        file.delete();
    }


}