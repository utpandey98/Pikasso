package com.example.pikasso.view;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class PikassoView extends View {

    public static final float TOUCH_TOLERANCE = 10;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen, paintLine;
    private HashMap<Integer, Path> pathHashMap;
    private HashMap<Integer, Point> prevPointHashMap;
    private ContextWrapper contextWrapper;

    public PikassoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    void init() {
        paintScreen = new Paint();

        paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);
        paintLine.setStrokeWidth(7);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeCap(Paint.Cap.ROUND);  //End of lines will be round

        pathHashMap = new HashMap<>();
        prevPointHashMap = new HashMap<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        for (Integer key : pathHashMap.keySet()) {
            canvas.drawPath(pathHashMap.get(key), paintLine);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            touchStarted(event.getX(actionIndex), event.getY(actionIndex), event.getPointerId(actionIndex));
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            touchEnded(event.getPointerId(actionIndex));
        } else {
            touchMoved(event);
        }

        invalidate(); //redraw the screen coz agar yeh nahi hoga ki jo bana woh gayab ho jaye

        return true;
    }

    private void touchMoved(MotionEvent event) {

        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerId);

            if (pathHashMap.containsKey(pointerId)) {
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathHashMap.get(pointerId);
                Point point = prevPointHashMap.get(pointerId);

                //calculate how far the user moved from the last update

                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                //if the distance is significant enough to considered a movement then
                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                    path.quadTo(point.x, point.y, (newX + point.x) / 2, (newY + point.y) / 2);

                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    public void clear() {
        pathHashMap.clear();
        prevPointHashMap.clear();
        bitmap.eraseColor(Color.WHITE);
    }

    private void touchEnded(int pointerId) {

        Path path = pathHashMap.get(pointerId); // get the corresponding path
        bitmapCanvas.drawPath(path, paintLine);  //draw to bitmap canvas

        path.reset();
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path;
        Point point; // store the last point in path

        if (pathHashMap.containsKey(pointerId)) {
            path = pathHashMap.get(pointerId);
            point = prevPointHashMap.get(pointerId);
        } else {
            path = new Path();
            pathHashMap.put(pointerId, path);
            point = new Point();
            prevPointHashMap.put(pointerId, point);
        }
        //move to coordinates of the touch
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    public void saveImage( ){
        String filename = "Pikasso" + System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,filename);
        values.put(MediaStore.Images.Media.DATE_ADDED,System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpg");

        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI,values);

        try  {
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri);

            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
            try {
                outputStream.flush();
                outputStream.close();

                Toast message = Toast.makeText(getContext(),"Image Saved",Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER,message.getXOffset()/2,
                        message.getYOffset()/2);
                message.show();
            }catch (IOException e){

                Toast message = Toast.makeText(getContext(),"Image not saved",Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER,message.getXOffset()/2,
                        message.getYOffset()/2);
                message.show();

            }
        }catch (FileNotFoundException e) {

            Toast message = Toast.makeText(getContext(),"Image not saved",Toast.LENGTH_LONG);
            message.setGravity(Gravity.CENTER,message.getXOffset()/2,
                    message.getYOffset()/2);
            message.show();
            //e.printStackTrace();
        }
    }

    public void saveImageToInternalStorage(){
        String filename = "Pikasso" + System.currentTimeMillis();
        contextWrapper = new ContextWrapper(getContext());

        File directory = contextWrapper.getDir("imageDir",Context.MODE_PRIVATE);

        File myPath = new File(directory,filename+".jpg");
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(myPath);
            bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                fos.flush();
                fos.close();
                Log.d("Image:",directory.getAbsolutePath());
                Toast message = Toast.makeText(getContext(),"Image Saved +"+directory.getAbsolutePath(),
                        Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER,message.getXOffset()/2,message.getYOffset()/2);
                message.show();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    public void setDrawingColor(int color) {
        paintLine.setColor(color);
    }

    public int getDrawingColor() {
        return paintLine.getColor();
    }

    public void setLineWidth(int width) {
        paintLine.setStrokeWidth(width);
    }

    public int getLineWidth() {
        return (int) paintLine.getStrokeWidth();
    }
}
