package com.example.mycamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MyCameraActivity extends Activity {
  
  // Use constants above API 11 (MediaStore.Files.FileColumns)
  protected static final int MEDIA_TYPE_IMAGE = 0;
  protected static final int MEDIA_TYPE_VIDEO = 1;
  private static final int CAPTURE_IMAGE_ACTIVITY_REQ = 100;
  private static final String TAG = "MCAct";

  private Uri fileUri;
  private Camera camera;
  private CameraPreview preview;
  private MediaRecorder mr;
  private Button videoButton;
  protected boolean isRecording = false;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!checkCameraExists(this)) {
      Toast.makeText(this, "Sorry: you have no camera!", Toast.LENGTH_LONG);
      finish();
    }
    camera = getCameraInstance();
    setUpLayout();
  }

  // Method required if setting up an Intent button 
  // to call the built-in camera
  protected void onActivityResult(int requestCode, int resultCode, 
                                  Intent data) {
    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQ) {
      if (resultCode == RESULT_OK) {
        if (data == null) {
          // A known bug here! The image should have saved in fileUri
          Toast.makeText(this, "Image saved successfully", 
                         Toast.LENGTH_LONG).show();
        } else {
          Toast.makeText(this, "Image saved successfully in: " 
                         + data.getData(), Toast.LENGTH_LONG).show();
        }
      } else if (resultCode == RESULT_CANCELED) {
        // User cancelled the operation; do nothing
      } else {
        Toast.makeText(this, "Callout for image capture failed!", 
                       Toast.LENGTH_LONG).show();
      }
    }
  }

  protected void onPause() {
    releaseMediaRecorder();
    releaseCamera();
    super.onPause();
  }

  protected void onResume() {
    if (camera == null) {
      camera = getCameraInstance();
      setUpLayout();
    }
    super.onResume();
  }

  protected Uri getOutputMediaFileUri(int type) {
    return Uri.fromFile(getOutputMediaFile(type));
  }

  
  protected boolean prepareForVideoRecording() {
    camera.unlock();
    mr = new MediaRecorder();
    mr.setCamera(camera);
    mr.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    mr.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    mr.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
    mr.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
    mr.setPreviewDisplay(preview.getHolder().getSurface());
    try {
      mr.prepare();
    } catch (IllegalStateException e) {
      Log.e(TAG, "IllegalStateException when preparing MediaRecorder " 
            + e.getMessage());
      e.getStackTrace();
      releaseMediaRecorder();
      return false;
    } catch (IOException e) {
      Log.e(TAG, "IllegalStateException when preparing MediaRecorder " 
            + e.getMessage());
      e.getStackTrace();
      releaseMediaRecorder();
      return false;
    }
    return true;
  }

  private boolean checkCameraExists(Context c) {
    if (c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      return true;
    } else {
      return false;
    }
  }

  private Camera getCameraInstance() {
    Camera c = null;
    try {
      c = Camera.open();
    } catch (Exception e) {
      Log.e(TAG, "No camera: exception " + e.getMessage());
      e.getStackTrace();
      Toast.makeText(this, "Sorry: I can't get a camera!", Toast.LENGTH_LONG);
      finish();
    }
    return c;
  }

  private void getImage() {
    PictureCallback picture = new PictureCallback() {
      public void onPictureTaken(byte[] data, Camera cam) {
        new SaveImageTask().execute(data);
        camera.startPreview();
      }
    };
    camera.takePicture(null, null, picture);
    
  }

  private File getOutputMediaFile(int type) {
    // good location for shared pictures; will not be lost if app uninstalled
    File directory = new File(Environment.getExternalStoragePublicDirectory(
      Environment.DIRECTORY_PICTURES), getPackageName());
    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        Log.e(TAG, "Failed to create storage directory.");
        return null;
      }
    }
    String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss")
                       .format(new Date());
    File file;
    if (type == MEDIA_TYPE_IMAGE) {
      file = new File(directory.getPath() + File.separator + "IMG_" 
                    + timeStamp + ".jpg");
    } else if (type == MEDIA_TYPE_VIDEO) {
      file = new File(directory.getPath() + File.separator + "VID_" 
                      + timeStamp + ".mp4");
    } else {
      return null;
    }
    return file;
  }

  private void releaseCamera() {
    if (camera != null) {
      camera.stopPreview();
      camera.release();
      camera = null;
      preview = null;
    }
  }
  
  private void releaseMediaRecorder() {
    if (mr != null) {
      mr.reset();
      mr.release();
      mr = null;
      camera.lock();
    }
  }

  private void setUpLayout() {
    setContentView(R.layout.main);
    preview = new CameraPreview(this, camera);
    FrameLayout frame = (FrameLayout) findViewById(R.id.camera_preview);
    frame.addView(preview);

    Button captureButton = (Button) findViewById(R.id.button_capture);
    captureButton.setOnClickListener(
       new View.OnClickListener() {
       public void onClick(View v) {
        getImage();
      }
      }
    );
    setUpFlashButton();
    setUpIntentButton();
    setUpVideoButton();
  }

  private void setUpFlashButton() {
    final Camera.Parameters params = camera.getParameters();
    final List<String> flashList = params.getSupportedFlashModes();
    if (flashList == null) {
      // no flash!
      return;
    }
    final CharSequence[] flashCS = flashList.toArray(
                                   new CharSequence[flashList.size()]);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Choose flash type");
    builder.setSingleChoiceItems(flashCS, -1, 
                                 new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        params.setFlashMode(flashList.get(which));
        camera.setParameters(params);
        Toast.makeText(getApplicationContext(), params.getFlashMode(), 
                       Toast.LENGTH_SHORT).show();
        dialog.dismiss();
      }
    });
    final AlertDialog alert = builder.create();

    Button flashButton = new Button(this);
    setUpButton(flashButton, "flash");
    flashButton.setOnClickListener(
      new View.OnClickListener() {
         public void onClick(View v) {
          alert.show();
        }
      }
    );
  }

  private void setUpIntentButton() {
    Button intentButton = new Button(this);
    setUpButton(intentButton, "Open built-in camera app");
    intentButton.setOnClickListener(
      new View.OnClickListener() {
        public void onClick(View v) {
          Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
          fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
          Log.v(TAG, "fileUri: " + fileUri);
          i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
          startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQ);
      }
      }
    );  
  }

  private void setUpVideoButton() {
    videoButton = new Button(this);
    setUpButton(videoButton, "Start video");

    videoButton.setOnClickListener(
      new View.OnClickListener() {
        public void onClick(View v) {
          if (isRecording) {
            mr.stop();
            releaseMediaRecorder();
            camera.lock();
            videoButton.setText("Start video");
            isRecording = false;
          } else {
            if (prepareForVideoRecording()) {
              mr.start();
              videoButton.setText("Stop video");
              isRecording = true;
            } else {
              // Something has gone wrong! Release the camera
              releaseMediaRecorder();
              Toast.makeText(MyCameraActivity.this, 
                             "Sorry: couldn't start video", 
                             Toast.LENGTH_LONG).show();
            }
          }
        }
      }
    );  
  }
  
  private void setUpButton(Button button, String label) {
    LinearLayout lin = (LinearLayout) findViewById(R.id.buttonlayout);
    button.setText(label);
    button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, 
                 LayoutParams.WRAP_CONTENT));
    lin.addView(button);    
  }

  
  
  class SaveImageTask extends AsyncTask<byte[], String, String> {
    @Override
    protected String doInBackground(byte[]... data) {
    File picFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
    if (picFile == null) {
      Log.e(TAG, "Error creating media file; are storage permissions correct?");
      return null;
    }
    try {
      FileOutputStream fos = new FileOutputStream(picFile);
      fos.write(data[0]);
      fos.close();
    } catch (FileNotFoundException e) {
      Log.e(TAG, "File not found: " + e.getMessage());
      e.getStackTrace();
    } catch (IOException e) {
      Log.e(TAG, "I/O error with file: " + e.getMessage());
      e.getStackTrace();
    }
    
    return null;
     }
  }
}
