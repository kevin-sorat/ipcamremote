package com.ksorat.foscamremote;

import java.nio.ByteBuffer;

import com.decoder.util.DecH264;
import com.ipc.sdk.AVStreamData;
import com.ipc.sdk.FSApi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class VideoView extends View implements Runnable {
	private static final String TAG = "IPCamRemote:VideoView";
	private static final String MESSAGE = "Received an exception";
	
	//public static final int SIZE_STANDARD = 0;
	//public static final int SIZE_BEST_FIT = 1;
	//public static final int SIZE_FULLSCREEN = 2;
	public static final int SIZE_4_3 = 0;
	public static final int SIZE_16_9 = 1;

	public static final long TIME_OUT_PERIOD = 10000;	// 10 seconds
	public static final int NUM_RETRIES = 1000; 

	private DecH264 decoder = new DecH264();
	private AVStreamData videoStreamData = new AVStreamData();

	private int videoWidth = 1280; 	// 640;
	private int videoHeight = 960;	// 480;
	private int vvWidth = 0;
	private int vvHeight = 0;
	private boolean isEnableVideoStream = false;

	private boolean isThreadRun = true;
	private boolean restartDecoder = false;

	private byte[] mPixel = new byte[1280 * 960 * 2];
	private int[] gotPicture = new int[4];

	private ByteBuffer buffer = ByteBuffer.wrap(mPixel);
	private Bitmap videoBit = Bitmap.createBitmap(videoWidth, videoHeight, Config.RGB_565);
	private Bitmap bmpMJ = null;
	private int videoFormat = 0; // H264
	
	private int dispWidth;
	private int dispHeight;
	private int displayMode;
	private Rect destRect;
	private int channelId;
	
	// For pinch zoom support
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;
	private float focusX;
	private float focusY;
	private float lastFocusX = -1;
	private float lastFocusY = -1;
	private Context _context;
	private Paint overlayPaint;
	private int overlayTextColor;
	private int overlayBackgroundColor;
	private Bitmap ovl;
	private int centerX;
	private int centerY;
	private boolean isReadyToDisplay = false;

	public VideoView(Context context, int width, int height, int channelId, int screenMode) {
		super(context);
		setFocusable(true);
		
		_context = context;

		int i = 0;
		for (i = 0; i < mPixel.length; i++) {
			mPixel[i] = (byte) 0x00;
		}

		vvWidth = width;
		vvHeight = height;
		this.channelId = channelId;
		
		if (screenMode >= 0 && screenMode <=1) {
			displayMode = screenMode;
		} else {
			// Default screen ratio is 4:3
			displayMode = SIZE_4_3;
		}
		
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		
		overlayPaint = new Paint();
		overlayPaint.setTextAlign(Paint.Align.LEFT);
		overlayPaint.setTextSize(32);
		overlayPaint.setTypeface(Typeface.DEFAULT);
		overlayTextColor = Color.WHITE;
		overlayBackgroundColor = Color.BLACK;
	}

	public void setVVMetric(int width, int height) {
		vvWidth = width;
		vvHeight = height;
	}

	public void start() {
		isThreadRun = true;

		try {
			Thread thread = new Thread(this);
			//thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		} catch (Exception e) {
		}
	}

	public void stop() {
		isThreadRun = false;
	}
	
	public boolean isRunning() {
		return isThreadRun;
	}

	public void startVideoStream() {
		isEnableVideoStream = true;
		FSApi.startVideoStream(channelId);
	}

	public void stopVideoStream() {
		isEnableVideoStream = false;
		FSApi.stopVideoStream(channelId);
		restartDecoder = true;
	}

	public void clearScreen() {
		int i = 0;
		synchronized (this) {
			for (i = 0; i < mPixel.length; i++) {
				mPixel[i] = (byte) 0x00;
			}
		}
		postInvalidate();
	}

	protected Bitmap getScaleBmp(Bitmap src, float sx, float sy) {
		Matrix matrix = new Matrix();
		matrix.postScale(sx, sy);
		Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
		return resizeBmp;
	}

	protected Bitmap getHorizenBmp(Bitmap src) {
		Matrix matrix = new Matrix();
		matrix.postScale(vvHeight * 1.0f / videoHeight, vvHeight * 1.0f / videoHeight);
		Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
				src.getHeight(), matrix, true);
		return resizeBmp;
	}

	protected Bitmap getVerticalBmp(Bitmap src) {
		Matrix matrix = new Matrix();
		matrix.postScale(vvWidth * 1.0f / videoWidth, vvWidth * 1.0f / videoWidth);
		Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
				src.getHeight(), matrix, true);
		return resizeBmp;
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		dispWidth = w;
		dispHeight = h;
		destRect = destRect(videoBit.getWidth(), videoBit.getHeight());
		centerX = dispWidth / 2;
		centerY = dispHeight / 2;
	};

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// If videoFormat is H264
		if (videoFormat == 0) {
			try {
				buffer.rewind();
				videoBit.copyPixelsFromBuffer(buffer);
				canvas.scale(mScaleFactor, mScaleFactor, focusX, focusY);
				canvas.drawBitmap(videoBit, null, destRect, null);
				
				// Diplay "Please wait..." message until there is enough bitmap data to display
				// AND only for main activity (not multi view activity)
				if (!isReadyToDisplay && _context instanceof MainActivity) {
					ovl = createOverlayText(overlayPaint, getResources().getString(R.string.plese_wait));
					if (ovl != null) {
						canvas.drawBitmap(ovl, centerX-ovl.getWidth()/2, centerY-ovl.getHeight()/2 , null);
					}
				}
				
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		// Else if videoFormat is MJPEG (we don't use this now, since MjpegView is working fine)
		} else if (bmpMJ != null) {
			if (vvWidth > vvHeight) {
				if (vvHeight > videoHeight) {
					canvas.drawBitmap(getVerticalBmp(bmpMJ), 0,
							(vvHeight - vvWidth * 1.0f / videoWidth * videoHeight) / 2, null);
				} else {
					canvas.drawBitmap(getHorizenBmp(bmpMJ),
							(vvWidth - vvHeight * 1.0f / videoHeight * videoWidth) / 2, 0, null);
				}
			} else {
				canvas.drawBitmap(getVerticalBmp(bmpMJ), 0,
						(vvHeight - vvWidth * 1.0f / videoWidth * videoHeight) / 2, null);
			}
		}
	}

	public void run() {

		int failCounter = 0;
		int accumulatedDataLen = 0;
		decoder.InitDecoder();
		
		while (isThreadRun) {
			if (isEnableVideoStream) {
				try {
					FSApi.getVideoStreamData(videoStreamData, channelId);
				} catch (Exception e) {
					continue;
				}

				int dataLen = videoStreamData.dataLen;
				if (dataLen > 0) {
					accumulatedDataLen += dataLen;
					// Only start to display when there is enough data to display
					if (accumulatedDataLen > 10000 && !isReadyToDisplay) {
						isReadyToDisplay = true;
					}
					
					failCounter = 0;
					
					// H264
					if (videoStreamData.videoFormat == 0) { 
						videoFormat = 0;
						decoder.DecoderNal(videoStreamData.data,
								videoStreamData.dataLen, gotPicture, mPixel);
					// MJ
					} else if (videoStreamData.videoFormat == 1) { 
						videoFormat = 1;
						gotPicture[0] = 0;
						bmpMJ = BitmapFactory.decodeByteArray(
								videoStreamData.data, 0,
								videoStreamData.dataLen);
						videoWidth = bmpMJ.getWidth();
						videoHeight = bmpMJ.getHeight();
						postInvalidate();
					} else {
						gotPicture[0] = 0;
					}

					if (gotPicture[0] > 0) {
						if ((gotPicture[2] != videoWidth) || (gotPicture[3] != videoHeight)) {
							videoBit.recycle();
							videoWidth = gotPicture[2];
							videoHeight = gotPicture[3];
							videoBit = Bitmap.createBitmap(videoWidth,
									videoHeight, Config.RGB_565);
						}
						postInvalidate();
					}
					
					if (_context instanceof MainActivity) {
						final MainActivity mainAct = (MainActivity) _context;
						mainAct.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (!mainAct.isUIEnabled) {
									mainAct.enableUI();
								}
							}
						});
					}
				} else {
					try {
						Thread.sleep(TIME_OUT_PERIOD/NUM_RETRIES);
						
						failCounter++;
						
						// Try 1000 times (10 seconds), before displaying an error
						if (failCounter >= NUM_RETRIES) {
							// There is a real problem here
							this.stop();
							if (_context instanceof MainActivity) {
								final MainActivity mainAct = (MainActivity) _context;
								mainAct.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										mainAct.destroyVideoViewSource();
										mainAct.displayConnectionErrorMessage();
									}
								});
							} else if (_context instanceof MultiViewActivity) {
								// Just return from here
								return;
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (restartDecoder) {
					decoder.UninitDecoder();
					decoder.InitDecoder();
					restartDecoder = false;
				}
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		decoder.UninitDecoder();
	}
	
	private Rect destRect(int bmw, int bmh) {
		/*
		int tempx;
		int tempy;
		if (displayMode == VideoView.SIZE_STANDARD) {
			tempx = (dispWidth / 2) - (bmw / 2);
			tempy = (dispHeight / 2) - (bmh / 2);
			return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
		} else if (displayMode == VideoView.SIZE_BEST_FIT) {
			float bmasp = (float) bmw / (float) bmh;
			bmw = dispWidth;
			bmh = (int) (dispWidth / bmasp);
			if (bmh > dispHeight) {
				bmh = dispHeight;
				bmw = (int) (dispHeight * bmasp);
			}
			tempx = (dispWidth / 2) - (bmw / 2);
			tempy = (dispHeight / 2) - (bmh / 2);
			return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
		} else if (displayMode == VideoView.SIZE_FULLSCREEN) {
			return new Rect(0, 0, dispWidth, dispHeight);
		*/
		if (displayMode == VideoView.SIZE_4_3) {
			int height = (int) (dispWidth*0.75);
			if (height > dispHeight) {
				int top = 0;
				height = dispHeight;
				int bottom = height;
				int width = (int) (dispHeight*1.33f);
				int left = (int) (dispWidth-width)/2;
				int right = left+width;
				return new Rect(left, top, right, bottom);
			} else {
				int left = 0;
				int right = dispWidth;
				int top = (int) (dispHeight-height)/2;
				int bottom = top+height;
				return new Rect(left, top, right, bottom);
			}
		} else if (displayMode == VideoView.SIZE_16_9) {
			int height = (int) (dispWidth*0.5625);
			if (height > dispHeight) {
				int top = 0;
				height = dispHeight;
				int bottom = height;
				int width = (int) (dispHeight*1.78f);
				int left = (int) (dispWidth-width)/2;
				int right = left+width;
				return new Rect(left, top, right, bottom);
			} else {
				int left = 0;
				int right = dispWidth;
				int top = (int) (dispHeight-height)/2;
				int bottom = top+height;
				return new Rect(left, top, right, bottom);
			}
		}
		return null;
	}

	public void resetScaleFactor() {
		if (mScaleFactor != 1.f) {
			mScaleFactor = 1.f;
			invalidate();
		}
	}
	
	private Bitmap createOverlayText(Paint p, String text) {
		Rect b = new Rect();
		p.getTextBounds(text, 0, text.length(), b);
		int bwidth = b.width() + 2;
		int bheight = b.height() + 2;
		Bitmap bm = Bitmap.createBitmap(bwidth, bheight,
				Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bm);
		p.setColor(overlayBackgroundColor);
		c.drawRect(0, 0, bwidth, bheight, p);
		p.setColor(overlayTextColor);
		c.drawText(text, -b.left + 1,
				(bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
		return bm;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		super.onTouchEvent(ev);
		
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);
		
		final int action = ev.getAction();
		
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				mActivePointerId = ev.getPointerId(0);
				break;
			}
			
			case MotionEvent.ACTION_MOVE: {
				// Only move if the ScaleGestureDetector isn't processing a gesture.
				if (!mScaleDetector.isInProgress()) {
					invalidate();
				}
				break;
			}
			
			case MotionEvent.ACTION_UP: {
				mActivePointerId = INVALID_POINTER_ID;
				break;
			}

			case MotionEvent.ACTION_CANCEL: {
				mActivePointerId = INVALID_POINTER_ID;
				break;
			}
			
			 case MotionEvent.ACTION_POINTER_UP: {

				 final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				 final int pointerId = ev.getPointerId(pointerIndex);
				 if (pointerId == mActivePointerId) {
					 // This was our active pointer going up. Choose a new
					 // active pointer and adjust accordingly.
					 final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
					 mActivePointerId = ev.getPointerId(newPointerIndex);
				 }
				 break;
			 }
		}
		return true;
	}
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		 @Override
		 public boolean onScaleBegin(ScaleGestureDetector detector) {
			 lastFocusX = -1;
			 lastFocusY = -1;
		
			 return true;
		 }
		 
		 @Override
		 public boolean onScale(ScaleGestureDetector detector) {
			 mScaleFactor *= detector.getScaleFactor();
	
			 focusX = detector.getFocusX();
			 focusY = detector.getFocusY();
	
			 if (lastFocusX == -1)
				 lastFocusX = focusX;
			 if (lastFocusY == -1)
				 lastFocusY = focusY;
			 
			 // Don't let the object get too small or too large.
			 // Min = 50%, Max = 300%
			 mScaleFactor = Math.max(0.5f, Math.min(mScaleFactor, 3.0f));
	
			 lastFocusX = focusX;
			 lastFocusY = focusY;
	
			 invalidate();
			 return true;
		 }
	 }
}