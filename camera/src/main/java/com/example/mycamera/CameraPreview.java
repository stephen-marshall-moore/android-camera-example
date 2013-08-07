package com.example.mycamera;

import java.io.IOException;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView 
                           implements SurfaceHolder.Callback {
  private static final String TAG = "CameraPreview";
  private SurfaceHolder sh;
  private Camera camera;
  
  public CameraPreview(Context context, Camera cm) {
    super(context);
    camera = cm;
    sh = getHolder();
    sh.addCallback(this);
    // deprecated but required pre-3.0
    sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }
  
  public void surfaceCreated(SurfaceHolder holder) {
    try {
      camera.setPreviewDisplay(holder);
      camera.startPreview();
    } catch (IOException e) {
      Log.e(TAG, "Error setting up preview: " + e.getMessage());
      e.getStackTrace();
    }
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width,
      int height) {
    if (sh.getSurface() == null) {
      // no preview surface!
      return;
    }
    
    // Stop preview before changing.
    try {
      camera.stopPreview();
    } catch (Exception e) {
      // Tried to stop non-existent preview
    }
    
    if (getResources().getConfiguration().orientation == 
        Configuration.ORIENTATION_PORTRAIT) {
      camera.setDisplayOrientation(90);
    } 
    
    try {
      camera.setPreviewDisplay(sh);
      camera.startPreview();
    } catch (Exception e) {
      Log.e(TAG, "Error restarting preview: " + e.getMessage());
      e.getStackTrace();
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    // Activity looks after releasing camera preview
  }

}
