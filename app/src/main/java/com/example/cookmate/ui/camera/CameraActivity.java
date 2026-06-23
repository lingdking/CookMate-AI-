package com.example.cookmate.ui.camera;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.cookmate.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageButton btnCapture;
    private TextView tvResult;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.preview_view);
        btnCapture = findViewById(R.id.btn_capture);
        tvResult = findViewById(R.id.tv_result);

        startCamera();

        btnCapture.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // 创建临时文件
        File photoFile = new File(getCacheDir(),
                "food_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        tvResult.setText("拍照成功，正在识别...");

                        // 模拟识别结果（实际项目中这里调用百度AI的API）
                        simulateRecognition(photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this,
                                "拍照失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 模拟食材识别
     * 实际项目需要调用百度AI图像识别API，这里用模拟数据演示流程
     */
    private void simulateRecognition(String imagePath) {
        // 模拟识别结果
        String[] mockResults = {"番茄", "鸡蛋", "黄瓜", "胡萝卜", "土豆", "苹果", "香蕉"};
        String result = mockResults[(int) (Math.random() * mockResults.length)];

        tvResult.setText("识别结果：" + result);

        // 返回识别结果给前一个页面
        Intent resultIntent = new Intent();
        resultIntent.putExtra("food_name", result);
        setResult(RESULT_OK, resultIntent);

        // 延迟一下让用户看到结果
        tvResult.postDelayed(this::finish, 1500);
    }
}