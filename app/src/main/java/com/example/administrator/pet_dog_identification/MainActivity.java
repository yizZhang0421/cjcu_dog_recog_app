package com.example.administrator.pet_dog_identification;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private ImageButton mImageButton;
    private MediaPlayer mp;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback;
    private RelativeLayout mRelativeLayout;
    private TextureView mTextureView;
    private Surface mTextureViewSurface;
    private ImageReader mImageReader;
    private Surface mImageReaderSurface;
    private String mCameraID;
    private CameraManager mCameraManager;
    private static int CAMERA_PERMISSION_REQUEST_CODE=0;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private boolean isPreviewed;

    private String url="http://microcraft.nctu.me:9487/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraDevice=null;
        mCameraDeviceStateCallback=new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                MainActivity.this.mCameraDevice=cameraDevice;
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                if(mCameraDevice!=null){
                    mCameraDevice=null;
                }
                stopHandler();
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {

            }
        };
        mRelativeLayout = findViewById(R.id.mRelativeLayout);
        mRelativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRelativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                try {
                    Size size = mCameraManager.getCameraCharacteristics(mCameraID).get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    ViewGroup.LayoutParams layout = mTextureView.getLayoutParams();
                    mTextureView.setLayoutParams(layout);
                    layout.height = size.getHeight();
                    layout.width = (int) ((mRelativeLayout.getWidth() * 1.0) * (size.getHeight() * 1.0 / mRelativeLayout.getHeight() * 1.0));
                }catch (Exception e){}
            }
        });
        mRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "processing", Toast.LENGTH_SHORT).show();
                try {
                    mCameraCaptureSession.stopRepeating();
                }catch (Exception e){}
                CaptureRequest.Builder captureRequest = null;
                try {
                    captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    int deviceRotation=MainActivity.this.getWindowManager().getDefaultDisplay().getRotation();
                    captureRequest.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation(mCameraManager.getCameraCharacteristics(mCameraID),deviceRotation));
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                captureRequest.addTarget(mImageReaderSurface);
                captureRequest.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                try {
                    mCameraCaptureSession.capture(captureRequest.build(), null, mHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        mTextureView=findViewById(R.id.mTextureView);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mTextureViewSurface = new Surface(mTextureView.getSurfaceTexture());
                startPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                if(isPreviewed==false && ResponseActivity.isReponseActivity==false) {
                    startPreview();
                }
            }
        });
        mCameraManager= (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String i : mCameraManager.getCameraIdList()){
                if(mCameraManager.getCameraCharacteristics(i).get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_BACK){
                    mCameraID=i;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},MainActivity.CAMERA_PERMISSION_REQUEST_CODE);
        }
        else{
            openCamera();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mCameraDevice.close();
        isPreviewed=false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(
                requestCode==MainActivity.CAMERA_PERMISSION_REQUEST_CODE &&
                        grantResults[0]==PackageManager.PERMISSION_GRANTED
                ){
            openCamera();
        }
    }

    private void openCamera(){
        startHandler();
        try {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
                mCameraManager.openCamera(mCameraID,mCameraDeviceStateCallback,mHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startHandler(){
        mHandlerThread=new HandlerThread("backgroundThread");
        mHandlerThread.start();
        mHandler=new Handler(mHandlerThread.getLooper());
    }
    private void stopHandler(){
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mHandlerThread=null;
        if(mHandler!=null){
            mHandler=null;
        }
    }

    private CaptureRequest.Builder mPreviewRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private HttpURLConnection httpcon;
    public static String return_string;
    public static byte[] bytes;
    private void startPreview(){
        isPreviewed=true;
        mImageReader = ImageReader.newInstance(mTextureView.getWidth(),mTextureView.getHeight(), ImageFormat.JPEG,1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = mImageReader.acquireLatestImage();
                ByteBuffer ByteBuffer  = image.getPlanes()[0].getBuffer();
                bytes  = new byte[ByteBuffer.remaining()];
                ByteBuffer.get(bytes);
                image.close();
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String imageStr =  Base64.encodeToString(bytes, Base64.DEFAULT);
                        String json=imageStr;
                        try {
                            httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
                            httpcon.setDoOutput(true);
                            httpcon.setRequestProperty("Content-Type", "text/plain");
                            httpcon.setRequestMethod("POST");
                            DataOutputStream out = new DataOutputStream(httpcon.getOutputStream());
                            out.write(json.getBytes("UTF-8"));
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            InputStreamReader in = new InputStreamReader(httpcon.getInputStream(), "UTF-8");
                            int a=0;
                            return_string="";
                            while ((a = in.read()) > 0)
                                return_string += (char) a;
                            in.close();
                            httpcon.disconnect();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ResponseActivity.isReponseActivity=true;
                                    MainActivity.this.startActivity(new Intent(MainActivity.this, ResponseActivity.class));
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
                t.start();
            }
        },mHandler);
        mImageReaderSurface=mImageReader.getSurface();
        try {
            mPreviewRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewRequest.addTarget(mTextureViewSurface);
        mPreviewRequest.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mTextureViewSurface,mImageReaderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.setRepeatingRequest(mPreviewRequest.build(),null,mHandler);
                        mCameraCaptureSession=cameraCaptureSession;
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        return jpegOrientation;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mUrlSetAction:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("URL");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        url = input.getText().toString();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
