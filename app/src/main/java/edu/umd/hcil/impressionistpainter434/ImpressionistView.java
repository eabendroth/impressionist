package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Paint;
    private float _minBrushRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //MediaStore.Images.Media.insertImage(context.getContentResolver(), _offScreenBitmap, "a", "b");
        Bitmap.Config conf = Bitmap.Config.ARGB_4444; // see other conf types
        _offScreenBitmap = Bitmap.createBitmap(_offScreenBitmap.getWidth(), _offScreenBitmap.getHeight(), conf); // this creates a MUTABLE bitmap
        _offScreenCanvas = new Canvas(_offScreenBitmap);
        invalidate();
    }

    public void savePainting (Context context) {
        MediaStore.Images.Media.insertImage(context.getContentResolver(), _offScreenBitmap, "a", "b");
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        int rad = (int)_minBrushRadius;
        if (_lastPoint == null) {
            _lastPoint = new Point((int)touchX - 1, (int)touchY - 1);
            _lastPointTime = System.currentTimeMillis() - 2000;
        }
        Rect r = getBitmapPositionInsideImageView(_imageView);
        if (!r.contains((int)touchX,(int)touchY)) {
            return true;
        }

        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE || motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (_offScreenCanvas == null) {
                _offScreenCanvas = new Canvas();
            }
//            if (_lastPoint == null) {
//                _lastPoint = new Point((int)touchX, (int)touchY);
//            }
//            _offScreenCanvas.drawLine(_lastPoint.x, _lastPoint.y, touchX, touchY, _paint);
//            _lastPoint.set((int)touchX, (int)touchY);
//            Rect r = getBitmapPositionInsideImageView(_imageView);
            if (_brushType == BrushType.Paint) {
                Bitmap b = (_imageView.getDrawingCache());
                int pixel = b.getPixel((int)touchX,(int)touchY);
                _paint.setColor(pixel);
                _paint.setAlpha(_alpha);
                long cTime = System.currentTimeMillis();
                double speed = (Math.sqrt(Math.pow(touchX - _lastPoint.x, 2) + Math.pow(touchY - _lastPoint.y, 2)))/(cTime - _lastPointTime);
                _lastPoint.set((int)touchX,(int)touchY);
                _lastPointTime = cTime;
                rad = (int)(speed * 20);
                if (rad < 5) {
                    rad = 5;
                }
                _offScreenCanvas.drawRect(touchX - rad, touchY - rad, touchX + rad, touchY + rad, _paint);
                invalidate(new Rect((int) touchX - rad, (int) touchY - rad, (int) touchX + rad, (int) touchY + 20));
            } else if (_brushType == BrushType.Charcoal) {
                Bitmap b = (_imageView.getDrawingCache());
                int pixel = b.getPixel((int) touchX, (int) touchY);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int absColor = (int)(red*0.2126) + (int)(green*0.7152) + (int)(blue*0.0722);
                _paint.setARGB(_alpha,absColor,absColor,absColor);
                _offScreenCanvas.drawCircle(touchX, touchY, 20, _paint);
                invalidate(new Rect((int) touchX - 11, (int) touchY - 11, (int) touchX + 11, (int) touchY + 11));
            } else if (_brushType == BrushType.Scribble) {
                Bitmap b = (_imageView.getDrawingCache());
                int pixel = b.getPixel((int) touchX, (int) touchY);
                _paint.setColor(pixel);
                _paint.setAlpha(_alpha);
                float scribbleX = (float)touchX + (float)(Math.random() * 60 - 30);
                float scribbleY = (float)touchY + (float)(Math.random() * 60 - 30);
                _offScreenCanvas.drawLine(_lastPoint.x,_lastPoint.y,scribbleX,scribbleY,_paint);
                _offScreenCanvas.drawLine(_lastPoint.x + 2,_lastPoint.y,scribbleX + 2,scribbleY,_paint);
                _offScreenCanvas.drawLine(_lastPoint.x + 2,_lastPoint.y + 2,scribbleX + 2,scribbleY + 2,_paint);
                _lastPoint.set((int) scribbleX, (int) scribbleY);
                invalidate((int) touchX - 31, (int) touchY - 31, (int) touchX + 31, (int) touchY + 31);
            } else {
                Bitmap b = (_imageView.getDrawingCache());
                int pixel = b.getPixel((int) touchX, (int) touchY);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int absColor = (int)(red*0.2126) + (int)(green*0.7152) + (int)(blue*0.0722);
                if (_brushType == BrushType.Red) {
                    _paint.setARGB(_alpha,absColor,0,0);
                } else if (_brushType == BrushType.Green) {
                    _paint.setARGB(_alpha,0,absColor,0);
                } else if (_brushType == BrushType.Blue) {
                    _paint.setARGB(_alpha,0,0,absColor);
                }

                _offScreenCanvas.drawCircle(touchX, touchY, 20, _paint);
                invalidate(new Rect((int) touchX - 11, (int) touchY - 11, (int) touchX + 11, (int) touchY + 11));
            }


        }
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            _lastPoint = null;
        }

        //AT some point soon, I need to localize this.

        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

