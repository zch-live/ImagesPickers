package cn.xz.imagespickers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.xz.imagespickers.utils.FileUtils;
import cn.xz.imagespickers.utils.TimeUtils;


public class ImageSelectorFragment extends Fragment {

    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_CAMERA_7 = 1007;

    private ArrayList<String> resultList = new ArrayList<String>();
    private List<Folder> folderList = new ArrayList<Folder>();

    private List<Image> imageList = new ArrayList<Image>();

    private Callback callback;

    private ImageAdapter imageAdapter;
    private FolderAdapter folderAdapter;

    private ListPopupWindow folderPopupWindow;

    private TextView time_text;
    private TextView category_button;
    private View popupAnchorView;
    private GridView grid_image;


    private int gridWidth, gridHeight;

    private boolean hasFolderGened = false;

    private Context context;

    private ImageConfig imageConfig;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("The Activity must implement ImageSelectorFragment.Callback interface...");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.imageselector_main_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getActivity();

        time_text = (TextView) view.findViewById(R.id.time_text);
        category_button = (TextView) view.findViewById(R.id.category_button);
        grid_image = (GridView) view.findViewById(R.id.grid_image);
        popupAnchorView = view.findViewById(R.id.footer_layout);

        time_text.setVisibility(View.GONE);

        init();
    }

    private void init() {
        imageConfig = ImageSelector.getImageConfig();

        folderAdapter = new FolderAdapter(context, imageConfig);

        imageAdapter = new ImageAdapter(context, imageList, imageConfig);

        imageAdapter.setShowCamera(imageConfig.isShowCamera());
        imageAdapter.setShowSelectIndicator(imageConfig.isMutiSelect());
        grid_image.setAdapter(imageAdapter);

        resultList = imageConfig.getPathList();

        category_button.setText(R.string.all_folder);
        category_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (folderPopupWindow == null) {
                    createPopupFolderList(gridWidth, gridHeight);
                }

                if (folderPopupWindow.isShowing()) {
                    folderPopupWindow.dismiss();
                } 
                else {
                    folderPopupWindow.show();
                    int index = folderAdapter.getSelectIndex();
                    index = index == 0 ? index : index - 1;
                    folderPopupWindow.getListView().setSelection(index);
                }
            }
        });


        grid_image.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    time_text.setVisibility(View.GONE);
                } else if (scrollState == SCROLL_STATE_FLING) {
                    time_text.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (time_text.getVisibility() == View.VISIBLE) {
                    int index = firstVisibleItem + 1 == view.getAdapter().getCount() ? view.getAdapter().getCount() - 1 : firstVisibleItem + 1;
                    Image image = (Image) view.getAdapter().getItem(index);
                    if (image != null) {
                        time_text.setText(TimeUtils.formatPhotoDate(image.path));
                    }
                }
            }
        });

        grid_image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int width = grid_image.getWidth();
                final int height = grid_image.getHeight();

                gridWidth = width;
                gridHeight = height;

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                final int numCount = width / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (width - columnSpace * (numCount - 1)) / numCount;
                imageAdapter.setItemSize(columnWidth);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    grid_image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    grid_image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        grid_image.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (imageAdapter.isShowCamera()) {
                    if (i == 0) {
                        showCameraAction();
                    } else {
                        Image image = (Image) adapterView.getAdapter().getItem(i);
                        selectImageFromGrid(image, imageConfig.isMutiSelect());
                    }
                } else {
                    Image image = (Image) adapterView.getAdapter().getItem(i);
                    selectImageFromGrid(image, imageConfig.isMutiSelect());
                }
            }
        });


    }

    private void createPopupFolderList(int width, int height) {
    	
        folderPopupWindow = new ListPopupWindow(getActivity());
        folderPopupWindow.setBackgroundDrawable(null);
        folderPopupWindow.setAdapter(folderAdapter);
        folderPopupWindow.setContentWidth(width);
        folderPopupWindow.setWidth(width);
        folderPopupWindow.setHeight(height * 5 / 8);
        folderPopupWindow.setAnchorView(popupAnchorView);
        folderPopupWindow.setModal(true);
        folderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                folderAdapter.setSelectIndex(i);

                final int index = i;
                final AdapterView v = adapterView;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        folderPopupWindow.dismiss();

                        if (index == 0) {
                            getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
                            category_button.setText(R.string.all_folder);
                            callback.onChangeAlbum(context.getResources().getString(R.string.all_folder));
                            if (imageConfig.isShowCamera()) {
                                imageAdapter.setShowCamera(true);
                            } else {
                                imageAdapter.setShowCamera(false);
                            }
                        } 
                        else {
                            Folder folder = (Folder) v.getAdapter().getItem(index);
                            if (null != folder) {
                                imageList.clear();
                                imageList.addAll(folder.images);
                                imageAdapter.notifyDataSetChanged();

                                category_button.setText(folder.name);
                                callback.onChangeAlbum(folder.name);
                                if (resultList != null && resultList.size() > 0) {
                                    imageAdapter.setDefaultSelected(resultList);
                                }
                            }
                            imageAdapter.setShowCamera(false);
                        }

                        grid_image.smoothScrollToPosition(0);
                    }
                }, 100);
            }
        });
        
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (folderPopupWindow != null) {
            if (folderPopupWindow.isShowing()) {
                folderPopupWindow.dismiss();
            }
        }

        grid_image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int height = grid_image.getHeight();

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                final int numCount = grid_image.getWidth() / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (grid_image.getWidth() - columnSpace * (numCount - 1)) / numCount;
                imageAdapter.setItemSize(columnWidth);

                if (folderPopupWindow != null) {
                    folderPopupWindow.setHeight(height * 5 / 8);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    grid_image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    grid_image.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });


        super.onConfigurationChanged(newConfig);
    }

    private void showCameraAction() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            String tempFile = FileUtils.createTmpFile(getActivity(), imageConfig.getFilePath()) + System.currentTimeMillis() + ".png";
            openCamera(tempFile);
        } else {
            Toast.makeText(context, R.string.msg_no_camera, Toast.LENGTH_SHORT).show();
        }
    }

    private void selectImageFromGrid(Image image, boolean isMulti) {
        if (image != null) {
            if (isMulti) {
                if (resultList.contains(image.path)) {
                    resultList.remove(image.path);
                    if (callback != null) {
                        callback.onImageUnselected(image.path);
                    }
                } else {
                    if (imageConfig.getMaxSize() == resultList.size()) {
                        Toast.makeText(context, R.string.msg_amount_limit, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    resultList.add(image.path);
                    if (callback != null) {
                        callback.onImageSelected(image.path);
                    }
                }
                imageAdapter.select(image);
            } else {
                if (callback != null) {
                    callback.onSingleImageSelected(image.path);
                }
            }
        }
    }

    //调系统相机
    private Uri mCameraTempUri  = null;
    private void openCamera(String mPicUri){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            takePhotoBiggerThan7(new File(mPicUri).getAbsolutePath());
        } else {
            // 指定拍照意图
            Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 加载路径图片路径
            mCameraTempUri = Uri.fromFile(new File(mPicUri));
            // 指定存储路径，这样就可以保存原图了
            openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraTempUri);
            startActivityForResult(openCameraIntent, REQUEST_CAMERA);
        }
    }

    private Uri mCameraTempUri7 = null;
    private void takePhotoBiggerThan7(String absolutePath) {
        try {
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.DATA, absolutePath);
            mCameraTempUri7 = requireActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (mCameraTempUri7 != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraTempUri7);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            }
            startActivityForResult(intent, REQUEST_CAMERA_7);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA_7 && mCameraTempUri7 != null) {
            //data不为null说明没拍照直接返回了
            if (data == null){
                if (callback != null) {
                    callback.onCameraShot(getFileFromUri(requireContext(),mCameraTempUri7));
                }
            }
        }
        if (requestCode == REQUEST_CAMERA && mCameraTempUri != null){
            if (data == null){
                if (callback != null) {
                    callback.onCameraShot(getFileFromUri(requireContext(),mCameraTempUri));
                }
            }
        }
    }


    public File getFileFromUri(Context context, Uri uri) {
        File file = null;
        if (uri.getScheme().equals("content")) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    String filePath = cursor.getString(columnIndex);
                    file = new File(filePath);
                }
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            file = new File(uri.getPath());
        } else {
            try {
                file = saveUriToFile(context, uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public File saveUriToFile(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("temp_", ".tmp", context.getCacheDir());
        OutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        inputStream.close();
        outputStream.close();

        return tempFile;
    }



    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media._ID};

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ALL) {
                CursorLoader cursorLoader =
                        new CursorLoader(getActivity(),
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                                null, null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            } else if (id == LOADER_CATEGORY) {
                CursorLoader cursorLoader = new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[0] + " like '%" + args.getString("path") + "%'", null, IMAGE_PROJECTION[2] + " DESC");
                return cursorLoader;
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                int count = data.getCount();
                if (count > 0) {
                    List<Image> tempImageList = new ArrayList<Image>();
                    data.moveToFirst();
                    do {
                        String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        String name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        long dateTime = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                        Image image = new Image(path, name, dateTime);
                        tempImageList.add(image);
                        if (!hasFolderGened) {
                            File imageFile = new File(path);
                            File folderFile = imageFile.getParentFile();
                            Folder folder = new Folder();
                            folder.name = folderFile.getName();
                            folder.path = folderFile.getAbsolutePath();
                            folder.cover = image;
                            if (!folderList.contains(folder)) {
                                List<Image> imageList = new ArrayList<Image>();
                                imageList.add(image);
                                folder.images = imageList;
                                folderList.add(folder);
                            } else {
                                Folder f = folderList.get(folderList.indexOf(folder));
                                f.images.add(image);
                            }
                        }

                    } while (data.moveToNext());

                    imageList.clear();
                    imageList.addAll(tempImageList);
                    imageAdapter.notifyDataSetChanged();

                    if (resultList != null && resultList.size() > 0) {
                        imageAdapter.setDefaultSelected(resultList);
                    }

                    folderAdapter.setData(folderList);

                    hasFolderGened = true;

                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    public interface Callback {
        void onSingleImageSelected(String path);

        void onImageSelected(String path);

        void onImageUnselected(String path);

        void onCameraShot(File imageFile);
        
        void onChangeAlbum(String albumName);
    }

}
