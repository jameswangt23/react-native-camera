package org.reactnative.camera.tasks;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class BarCodeScannerAsyncTask extends android.os.AsyncTask<Void, Void, Result> {
  private byte[] mImageData;
  private int mWidth;
  private int mHeight;
  private BarCodeScannerAsyncTaskDelegate mDelegate;
  private final MultiFormatReader mMultiFormatReader;
  private boolean mScanAreaLimit;
  private float mScanAreaX;
  private float mScanAreaY;
  private float mScanAreaWidth;
  private float mScanAreaHeight;
  private int mCameraWidth;
  private int mCameraHeight;

  //  note(sjchmiela): From my short research it's ok to ignore rotation of the image.
  public BarCodeScannerAsyncTask(
      BarCodeScannerAsyncTaskDelegate delegate,
      MultiFormatReader multiFormatReader,
      byte[] imageData,
      int width,
      int height,
      boolean scanAreaLimit,
      float scanAreaX,
      float scanAreaY,
      float scanAreaWidth,
      float scanAreaHeight,
      int cameraWidth,
      int cameraHeight
  ) {
    mImageData = imageData;
    mWidth = width;
    mHeight = height;
    mDelegate = delegate;
    mMultiFormatReader = multiFormatReader;
    mScanAreaLimit = scanAreaLimit;
    mScanAreaX = scanAreaX;
    mScanAreaY = scanAreaY;
    mScanAreaWidth = scanAreaWidth;
    mScanAreaHeight = scanAreaHeight;
    mCameraWidth = cameraWidth;
    mCameraHeight = cameraHeight;
  }

  @Override
  protected Result doInBackground(Void... ignored) {
    if (isCancelled() || mDelegate == null) {
      return null;
    }

    Result result = null;

    // int left = (int) (scanAreaX * mWidth);
    // int scanWidth = (int) (mScanAreaWidth * mWidth);

    // int top = (int) ((mHeight - scanAreaY * mHeight) / 2);
    // int scanHeight = scanWidth * mScanAreaHeight;

    float ratio = getAspectRatio().toFloat();

    int actualCamHeight = (int) (mCameraWidth / ratio);
    float actualMScanY = (((actualCamHeight - mCameraWidth) / 2) + (mScanAreaY * mCameraHeight)) / actualCamHeight;
    
    int left = (int) (mScanAreaX * mWidth);
    int top = (int) (actualMScanY * mHeight)
    int scanWidth = (int) (mScanAreaWidth * mWidth);
    int scanHeight = (int) (((mScanAreaHeight * mCameraWidth) / actualCamHeight) * mHeight);
    
    try {
      BinaryBitmap bitmap = generateBitmapFromImageData(
              mImageData,
              mWidth,
              mHeight,
              left,
              top,
              scanWidth,
              scanHeight
              false
      );
      result = mMultiFormatReader.decodeWithState(bitmap);
    } catch (NotFoundException e) {
      BinaryBitmap bitmap = generateBitmapFromImageData(
              rotateImage(mImageData,mWidth, mHeight),
              mHeight,
              mWidth,
              mWidth - scanWidth - left,
              mHeight - scanHeight - top,
              scanWidth,
              scanHeight
              false
      );
      try {
        result = mMultiFormatReader.decodeWithState(bitmap);
      } catch (NotFoundException e1) {
          BinaryBitmap invertedBitmap = generateBitmapFromImageData(
                  mImageData,
                  mWidth,
                  mHeight,
                  true,
                  mWidth - scanWidth - left,
                  mHeight - scanHeight - top,
                  scanWidth,
                  scanHeight
          );
        try {
          result = mMultiFormatReader.decodeWithState(invertedBitmap);
        } catch (NotFoundException e2) {
          BinaryBitmap invertedRotatedBitmap = generateBitmapFromImageData(
                  rotateImage(mImageData,mWidth, mHeight),
                  mHeight,
                  mWidth,
                  true,
                  top,
                  mWidth - scanWidth - left,
                  scanHeight,
                  scanWidth
          );
          try {
            result = mMultiFormatReader.decodeWithState(invertedRotatedBitmap);
          } catch (NotFoundException e3) {
            //no barcode Found
          }
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return result;
  }
  private byte[] rotateImage(byte[]imageData,int width, int height) {
    byte[] rotated = new byte[imageData.length];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        rotated[x * height + height - y - 1] = imageData[x + y * width];
      }
    }
    return rotated;
  }
  @Override
  protected void onPostExecute(Result result) {
    super.onPostExecute(result);
    if (result != null) {
      mDelegate.onBarCodeRead(result, mWidth, mHeight);
    }
    mDelegate.onBarCodeScanningTaskCompleted();
  }

  private BinaryBitmap generateBitmapFromImageData(byte[] imageData, int width, int height, boolean inverse, int left, int top, int sWidth, int sHeight) {
  // private BinaryBitmap generateBitmapFromImageData(byte[] imageData, int width, int height, boolean inverse) {
    PlanarYUVLuminanceSource source;
    if (mScanAreaLimit) {
      source = new PlanarYUVLuminanceSource(
        imageData, // byte[] yuvData
        width, // int dataWidth
        height, // int dataHeight
        left, // int left
        top, // int top
        sWidth, // int width
        sHeight, // int height
        false // boolean reverseHorizontal
      );
    } else {
        source = new PlanarYUVLuminanceSource(
        imageData, // byte[] yuvData
        width, // int dataWidth
        height, // int dataHeight
        0, // int left
        0, // int top
        width, // int width
        height, // int height
        false // boolean reverseHorizontal
      );
    }
    if (inverse) {
      return new BinaryBitmap(new HybridBinarizer(source.invert()));
    } else {
      return new BinaryBitmap(new HybridBinarizer(source));
    }
  }
}
