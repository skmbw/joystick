package com.xuershangda.joystick.nav;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.xuershangda.joystick.BuildConfig;
import com.xuershangda.joystick.R;
import com.xuershangda.joystick.view.FingerPaintImageView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * 跟随Activity，开始跟随、停止跟随、切换跟随
 *
 * @author yinlei
 * @since 2020-10-27
 */
public class FollowActivity extends AppCompatActivity {
    private static final String TAG = "FollowActivity";

    // 最多选择图片的个数
    private static int MAX_NUM = 3;
    private static final int TAKE_PICTURE = 520;
    private static final int OPEN_CAMERA = 20;

    public static final String INTENT_MAX_NUM = "intent_max_num";
    public static final String INTENT_SELECTED_PICTURE = "intent_selected_picture";
    //已选择的图片
    private ArrayList<String> selectedPicture = new ArrayList<>();
    private String cameraPath = null;

    private FingerPaintImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        setTitle(R.string.follow);

        findViewById(R.id.follow_target).setOnClickListener(v -> {
            goCamera();
        });

        imageView = findViewById(R.id.map);
    }

    /**
     * 使用相机拍照
     */
    protected void goCamera() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(FollowActivity.this, Manifest.permission.CAMERA);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(FollowActivity.this, new String[]{Manifest.permission.CAMERA}, OPEN_CAMERA);
            } else {
                showCamera();
            }
        } else {
            showCamera();
        }
    }

    private void showCamera() {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = getOutputMediaFileUri();
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(openCameraIntent, TAKE_PICTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        switch (requestCode) {
            case OPEN_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCamera();
                } else {
                    Toast.makeText(FollowActivity.this, "相机权限禁用了。请务必开启相机权", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 用于拍照时获取输出的Uri
     */
    protected Uri getOutputMediaFileUri() {
        // 这个参数的child，不能与其他app重复。否则保存图片会出错。
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "joystick");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        cameraPath = mediaFile.getAbsolutePath();

        Uri contentUri;
        // 判断是否是AndroidN以及更高的版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentUri = FileProvider.getUriForFile(FollowActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", mediaFile);
        } else {
            contentUri = Uri.fromFile(mediaFile);
        }

        return contentUri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == RESULT_OK && data != null) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    Bitmap bitmap = (Bitmap) bundle.get("data");
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}