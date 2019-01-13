// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package pt.ipleiria.pp.facedetection;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;

import pt.ipleiria.pp.OpatioActivity;
import pt.ipleiria.pp.common.VisionProcessorBase;
import pt.ipleiria.pp.common.CameraImageGraphic;
import pt.ipleiria.pp.common.FrameMetadata;
import pt.ipleiria.pp.common.GraphicOverlay;


/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

  private static final String TAG = "FaceDetectionProcessor";

  private final FirebaseVisionFaceDetector detector;

  public FaceDetectionProcessor() {
    FirebaseVisionFaceDetectorOptions options =
        new FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .enableTracking()
            .build();

    detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
  }

  @Override
  public void stop() {
    try {
      detector.close();
    } catch (IOException e) {
      Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
    }
  }

  @Override
  protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
    return detector.detectInImage(image);
  }

  @Override
  protected void onSuccess(
      @Nullable Bitmap originalCameraImage,
      @NonNull List<FirebaseVisionFace> faces,
      @NonNull FrameMetadata frameMetadata,
      @NonNull GraphicOverlay graphicOverlay) {
    graphicOverlay.clear();
    if (originalCameraImage != null) {
      CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
      graphicOverlay.add(imageGraphic);
    }



    for (int i = 0; i < faces.size(); ++i) {
      FirebaseVisionFace face = faces.get(i);

      // TODO: Communicate with the UI thread
//      if (face.getSmilingProbability() < 0.5) {
//        Message message = OpatioActivity.mHandler.obtainMessage();
//        Bundle bundle = new Bundle();
//        bundle.putString("FEEDBACK", "Why so serious? :-|");
//        message.setData(bundle);
//        OpatioActivity.mHandler.sendMessage(message);
//      }

//      if (face.getRightEyeOpenProbability()>0.5  || face.getLeftEyeOpenProbability() >0.5 ) {
//        Message message = OpatioActivity.mHandler.obtainMessage();
//        Bundle bundle = new Bundle();
//        //bundle.putString("FEEDBACK", "olho aberto");
//        bundle.putBoolean("olhoAberto",true);
//        message.setData(bundle);
//        OpatioActivity.mHandler.sendMessage(message);
//      }
      Message message = OpatioActivity.mHandler.obtainMessage();
      Bundle bundle = new Bundle();
     if (face.getRightEyeOpenProbability()<0.2  || face.getLeftEyeOpenProbability()<0.2){
       bundle.putBoolean("blinkEye",true);
       message.setData(bundle);
      }

      if (face.getSmilingProbability()>0.5){
        bundle.putBoolean("Smiling",true);
        message.setData(bundle);

      }
      OpatioActivity.mHandler.sendMessage(message);




      int cameraFacing =
          frameMetadata != null ? frameMetadata.getCameraFacing() :
              Camera.CameraInfo.CAMERA_FACING_FRONT;
      FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, cameraFacing);
      graphicOverlay.add(faceGraphic);
    }
    graphicOverlay.postInvalidate();

  }


  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Face detection failed " + e);
  }
}
