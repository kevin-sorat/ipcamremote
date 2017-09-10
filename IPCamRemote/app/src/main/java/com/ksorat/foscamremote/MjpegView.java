package com.ksorat.foscamremote;

import java.io.IOException;

import com.ksorat.ipcamremote.httpCommand.IPCamType;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "IPCamRemote:MjpegView";
	private static final String MESSAGE = "Received an exception";
	
	public final static int POSITION_UPPER_LEFT = 9;
	public final static int POSITION_UPPER_RIGHT = 3;
	public final static int POSITION_LOWER_LEFT = 12;
	public final static int POSITION_LOWER_RIGHT = 6;

	public final static int SIZE_STANDARD = 1;
	public final static int SIZE_BEST_FIT = 4;
	public final static int SIZE_FULLSCREEN = 8;
	
	private static final long TIMEOUT = 100;

	private MjpegViewThread mjpegViewthread;
	private MjpegInputStream mIn = null;
	private boolean showFps = false;
	private boolean mRun = false;
	private boolean surfaceDone = false;
	private Paint overlayPaint;
	private int overlayTextColor;
	private int overlayBackgroundColor;
	private int ovlPos;
	private int dispWidth;
	private int dispHeight;
	private int displayMode;
	private Context _context;
	private int _camIndex = -1;
	
	// For pinch zoom support
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;
	private float focusX;
	private float focusY;
	private float lastFocusX = -1;
	private float lastFocusY = -1;

	public class MjpegViewThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private int frameCounter = 0;
		private long start;
		private Bitmap ovl;

		public MjpegViewThread(SurfaceHolder surfaceHolder, Context context) {
			mSurfaceHolder = surfaceHolder;
		}

		private Rect destRect(int bmw, int bmh) {
			int tempx;
			int tempy;
			if (displayMode == MjpegView.SIZE_STANDARD) {
				tempx = (dispWidth / 2) - (bmw / 2);
				tempy = (dispHeight / 2) - (bmh / 2);
				return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
			}
			if (displayMode == MjpegView.SIZE_BEST_FIT) {
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
			}
			if (displayMode == MjpegView.SIZE_FULLSCREEN)
				return new Rect(0, 0, dispWidth, dispHeight);
			return null;
		}

		public void setSurfaceSize(int width, int height) {
			synchronized (mSurfaceHolder) {
				dispWidth = width;
				dispHeight = height;
			}
		}

		private Bitmap makeFpsOverlay(Paint p, String text) {
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

		public void run() {
			start = System.currentTimeMillis();
			PorterDuffXfermode mode = new PorterDuffXfermode(
					PorterDuff.Mode.DST_OVER);
			Bitmap bm;
			int width;
			int height;
			Rect destRect;
			Canvas c = null;
			Paint p = new Paint();
			String fps = "";
			while (mRun) {
				if (surfaceDone) {
					try {
						c = mSurfaceHolder.lockCanvas();
						synchronized (mSurfaceHolder) {
							try {
								bm = mIn.readMjpegFrame();
								// If bitmap data is null
								if (bm == null) {
									if (_context instanceof MainActivity) {
										IPCamData data = IPCamData.getInstance();
										// Special case for the public IP camera
										// Reload the MjpegView
										if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
											MainActivity mainAct = (MainActivity) _context;
											mainAct.showBusy();
											mainAct.isReCreatingMjpeg = true;
											mainAct.generateURLStrings();
											// This will invoke reCreateMjpegView method at the end
											mainAct.getParametersFromIPCamAfterItemSelection();
										// Else bitmap is null and this is not public IP camera
										} else {
											// There is a real problem here
											mRun = false;
											final MainActivity mainAct = (MainActivity) _context;
											mainAct.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													mainAct.destroyVideoViewSource();
													mainAct.displayConnectionErrorMessage();
												}
											});
										}
									} else if (_context instanceof MultiViewActivity) {
										if (_camIndex >= 0) {
											final MultiViewActivity multiViewAct = (MultiViewActivity) _context;
											if (!multiViewAct.isFromOnPause) {
												final IPCamData data = IPCamData.getInstance();
												// If this is a public camera
												if (data.getIPCamType() == IPCamType.PUBLIC) {
													multiViewAct.runOnUiThread(new Runnable() {
														public void run() {
															// Reconnect the camera
															// Note: This is a special case to handle public ip camera becoming blank or frozen
															multiViewAct.createMjpegViewSource(data.getIPCamArray()[_camIndex].serverURL, _camIndex);
														}
													});
												// Else this is a non-public camera, e.g. Mjpeg or H.264
												} else {
													// Just return from here
													return;
												}
											}
										}
									}
									return;
								}
								
								destRect = destRect(bm.getWidth(),
										bm.getHeight());
								c.save();
								// For pinch zoom support
								c.scale(mScaleFactor, mScaleFactor, focusX, focusY);
								c.drawColor(Color.BLACK);
								c.drawBitmap(bm, null, destRect, p);
								c.restore();
								
								if (showFps) {
									p.setXfermode(mode);
									if (ovl != null) {
										height = ((ovlPos & 1) == 1) ? destRect.top
												: destRect.bottom
														- ovl.getHeight();
										width = ((ovlPos & 8) == 8) ? destRect.left
												: destRect.right
														- ovl.getWidth();
										c.drawBitmap(ovl, width, height, null);
									}
									p.setXfermode(null);
									frameCounter++;
									if ((System.currentTimeMillis() - start) >= 1000) {
										fps = String.valueOf(frameCounter)
												+ "fps";
										frameCounter = 0;
										start = System.currentTimeMillis();
										ovl = makeFpsOverlay(overlayPaint, fps);
									}
								}
							} catch (IOException e) {
								//Log.e(TAG, MESSAGE, e);
							} catch (NullPointerException e) {
								//Log.e(TAG, MESSAGE, e);
							} catch (Exception e) {
								//Log.e(TAG, MESSAGE, e);
							}
						}
					} finally {
						if (c != null) {
							try {
								mSurfaceHolder.unlockCanvasAndPost(c);
							} catch (Exception e) {
								Log.e(TAG, MESSAGE, e);
							}
						}
					}
				}
			}
		}
	}

	private void init(Context context) {
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		mjpegViewthread = new MjpegViewThread(holder, context);
		setFocusable(true);
		overlayPaint = new Paint();
		overlayPaint.setTextAlign(Paint.Align.LEFT);
		overlayPaint.setTextSize(12);
		overlayPaint.setTypeface(Typeface.DEFAULT);
		overlayTextColor = Color.WHITE;
		overlayBackgroundColor = Color.BLACK;
		ovlPos = MjpegView.POSITION_LOWER_RIGHT;
		displayMode = MjpegView.SIZE_STANDARD;
		dispWidth = getWidth();
		dispHeight = getHeight();
	}

	public void startPlayback() {
		try {
			if (mIn != null) {
				mRun = true;
				mjpegViewthread.start();
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	public synchronized void stopPlayback() {
		mRun = false;
		boolean retry = true;
		while (retry) {
			try {
				long startJoin = System.currentTimeMillis();
				mjpegViewthread.join();
				long stopJoin = System.currentTimeMillis();
				
				if ((stopJoin - startJoin) >= TIMEOUT) {
					try {
						throw new Exception("Timeout when stoping the playback.");
			        } catch (Exception e) {
			           e.printStackTrace();
			        }
				}
				
				retry = false;
			} catch (InterruptedException e) {
				Log.e(TAG, MESSAGE, e);
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}
	}
	
	public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
		mjpegViewthread.setSurfaceSize(w, h);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceDone = false;
		stopPlayback();
	}

	public MjpegView(Context context) {
		super(context);
		init(context);
		_context = context;
		
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}
	
	public MjpegView(Context context, int camIndex) {
		super(context);
		init(context);
		_context = context;
		_camIndex = camIndex;
		
		 mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}

	public void surfaceCreated(SurfaceHolder holder) {
		surfaceDone = true;
	}

	public void showFps(boolean b) {
		showFps = b;
	}

	public void setSource(MjpegInputStream source) {
		mIn = source;
		startPlayback();
	}

	public void setOverlayPaint(Paint p) {
		overlayPaint = p;
	}

	public void setOverlayTextColor(int c) {
		overlayTextColor = c;
	}

	public void setOverlayBackgroundColor(int c) {
		overlayBackgroundColor = c;
	}

	public void setOverlayPosition(int p) {
		ovlPos = p;
	}
	
	public int getDisplayMode() {
		return displayMode;
	}

	public void setDisplayMode(int s) {
		displayMode = s;
	}
	
	public MjpegInputStream getMjpegInputStream() {
		return mIn;
	}
	
	public void destroyMjpegInputStream() {
		mIn = null;
	}
	
	public void resetScaleFactor() {
		if (mScaleFactor != 1.f) {
			mScaleFactor = 1.f;
			invalidate();
		}
	}
	
	/*
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.save();
		canvas.scale(mScaleFactor, mScaleFactor, focusX, focusY);
		canvas.restore();
	}
	*/
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		super.onTouchEvent(ev);
		
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);
		
		final int action = ev.getAction();
		
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN: {
				//final float x = ev.getX() / mScaleFactor;
				//final float y = ev.getY() / mScaleFactor;
				mActivePointerId = ev.getPointerId(0);
				break;
			}
			
			case MotionEvent.ACTION_MOVE: {
				//final int pointerIndex = ev.findPointerIndex(mActivePointerId);
				//final float x = ev.getX(pointerIndex) / mScaleFactor;
				//final float y = ev.getY(pointerIndex) / mScaleFactor;

				// Only move if the ScaleGestureDetector isn't processing a gesture.
				if (!mScaleDetector.isInProgress()) {
					invalidate();
				}
				//mLastTouchX = x;
				//mLastTouchY = y;
				
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
					 //mLastTouchX = ev.getX(newPointerIndex) / mScaleFactor;
					 //mLastTouchY = ev.getY(newPointerIndex) / mScaleFactor;
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
			 // float x = detector.getFocusX();
			 // float y = detector.getFocusY();
		
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
	
			 //mPosX += (focusX - lastFocusX);
			 //mPosY += (focusY - lastFocusY);
			 
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
