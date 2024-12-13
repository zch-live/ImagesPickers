package cn.xz.imageselector2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.List;

import cn.xz.imagespickers.ImageConfig;
import cn.xz.imagespickers.ImageSelector;
import cn.xz.imagespickers.ImageSelectorActivity;
import cn.xz.imagespickers.utils.FileUtils;

public class MainActivity extends AppCompatActivity {

    ImageView ivPic;
    String mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivPic = findViewById(R.id.iv_pic);
        findViewById(R.id.tv_pic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageConfig imageConfig = new ImageConfig.Builder(
                        new GlideLoader())
                        .steepToolBarColor(getResources().getColor(cn.xz.imagespickers.R.color.black))
                        .titleBgColor(getResources().getColor(cn.xz.imagespickers.R.color.black))
                        .titleSubmitTextColor(getResources().getColor(R.color.white))
                        .titleTextColor(getResources().getColor(R.color.white))
                        // 开启单选   （默认为多选）
                        .singleSelect()
                        // 裁剪 (只有单选可裁剪)
                        //.crop()
                        // 开启拍照功能 （默认关闭）
                        .showCamera()
                        //设置显示容器
                        .build();
                ImageSelector.open(MainActivity.this, imageConfig);
            }
        });
        findViewById(R.id.tv_del).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.deleteFile(MainActivity.this,mUrl);
            }
        });


        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {//联系人的权限 Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};//读写SD卡权限
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ImageSelector.IMAGE_REQUEST_CODE) && resultCode == RESULT_OK && data != null) {
            List<String> pathList = data.getStringArrayListExtra(ImageSelectorActivity.EXTRA_RESULT);
            if (pathList.size() > 0){
                ivPic.post(new Runnable() {
                    @Override
                    public void run() {
                        mUrl = pathList.get(0);
                        Log.e("返回链接", "run: " + pathList.get(0) );
                        ivPic.setImageBitmap(BitmapFactory.decodeFile(pathList.get(0)));
                    }
                });
            }
        }
    }
}