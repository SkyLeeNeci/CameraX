package karpenko.test.camerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ImageView preview;
    public static final int PERMISSION_REQUEST_CAMERA = 123;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    YUVtoRGB yuVtoRGB = new YUVtoRGB();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preview = findViewById(R.id.imagepreview);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }else {
            initializeCamera();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            initializeCamera();
        };
    }

    private void initializeCamera(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    /*Preview preview = new Preview.Builder().build();
                    ImageCapture imageCapture = new ImageCapture.Builder().build();*/

                    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1920,1080))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(MainActivity.this), new ImageAnalysis.Analyzer() {
                        @Override
                        public void analyze(@NonNull ImageProxy image) {
                            @SuppressLint("UnsafeOptInUsageError") Image image1 = image.getImage();
                            Bitmap bitmap = yuVtoRGB.translateYUV(image1,MainActivity.this);
                            int size = bitmap.getWidth() * bitmap.getHeight();
                            int[] pixels = new int[size];

                            bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0, bitmap.getWidth(),bitmap.getHeight());

                            for (int i = 0; i < size; i++) {
                                int color = pixels[i];
                                int r = color >> 16 & 0xff;
                                int g = color >> 8 & 0xff;
                                int b = color & 0xff;
                                int gray = (r+g+b)/3;
                                pixels[i] = 0xff000000 | gray << 16 | gray <<8 | gray;

                            }
                            bitmap.setPixels(pixels,0,bitmap.getWidth(),0,0, bitmap.getWidth(),bitmap.getHeight());

                            preview.setRotation(image.getImageInfo().getRotationDegrees());
                            preview.setImageBitmap(bitmap);
                            image.close();
                        }
                    });

                    cameraProvider.bindToLifecycle(MainActivity.this,cameraSelector,imageAnalysis);

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

}