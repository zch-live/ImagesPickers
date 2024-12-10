package cn.xz.imageselector2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import cn.xz.imagespickers.ImageConfig;
import cn.xz.imagespickers.ImageSelector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
    }
}