package cn.xz.imageselector2;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import cn.xz.imagespickers.ImageLoader;

public class GlideLoader implements ImageLoader {

	private static final long serialVersionUID = 1L;

	@Override
    public void displayImage(Context context, String path, ImageView imageView) {
        Log.e("图片地址", "displayImage: " + path);
        Glide.with(context)
                .load(path)
                .centerCrop()
                .into(imageView);
    }

}
