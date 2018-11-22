package com.lmp.javacodesample;

import android.util.FloatMath;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Created by Ilya on 24.03.14.
 */
public abstract class MotionManagerAbstract
{
    public static final byte DRAG = 1;
    public static final byte ZOOM = 2;
    public static final byte NONE = 0;
    protected byte touchMode = NONE;

    public static final byte ORIENTATION_VERTICAL = 0;
    public static final byte ORIENTATION_HORIZONTAL = 1;
    protected byte currentDisplayRotation = ORIENTATION_VERTICAL;

    protected Camera3d camera = new Camera3d();
    protected Point2d visibilityDistance = new Point2d();

    protected Point3d cameraPosition = new Point3d();
    protected Point3d cameraTarget = new Point3d();

    protected float scale = 25f;
    protected float scaleMax = 100;
    protected float scaleMin = 5;
    protected float scaleSpeed = 0.8f;
    protected boolean scaleChanged = true;

    protected ArrayList<Float> touchList = new ArrayList<Float>();
    protected float x = 0;
    protected float y = 0;
    protected float startX = 0;
    protected float startY = 0;
    protected float oldDist = 10f;
    protected float trace = 0;
    protected long touchTime = 0;

    protected float aspectRatio;
    protected int orientationVersion = -1;

    public abstract boolean onTouchEvent(MotionEvent e);

    protected float spacing(MotionEvent e)
    {
        try
        {
            float x = e.getX(0) - e.getX(1);
            float y = e.getY(0) - e.getY(1);
            return FloatMath.sqrt(x * x + y * y);
        } catch (Exception ex)
        {
            touchMode = DRAG;
            return oldDist;
        }
    }

    public byte getDisplayOrientation()
    {
        RayPicking.updateWindowSize();
        if (RayPicking.getWindowWidth() < RayPicking.getWindowHeight()) return ORIENTATION_VERTICAL;
        else return ORIENTATION_HORIZONTAL;
    }

    public int getOrientationVersion()
    {
        return orientationVersion;
    }

    public Camera3d getCamera()
    {
        return camera;
    }

    public void setCameraPosition(Point3d cameraPosition)
    {
        camera.position.setAllFrom(cameraPosition);
        this.cameraPosition.setAllFrom(cameraPosition);
    }

    public void setCameraCoordinates(Camera3d sceneCamera)
    {
        camera.setAllFrom(camera);
    }

    public float getScale()
    {
        return scale;
    }

    public float getScaleMax()
    {
        return scaleMax;
    }

    public float getScaleMin()
    {
        return scaleMin;
    }

    public byte getTouchMode()
    {
        return touchMode;
    }

    public float getTrace()
    {
        return trace;
    }

    public float getTouchTime()
    {
        return touchTime;
    }

    public Point2d getVisibilityDistance()
    {
        return visibilityDistance;
    }

    public void setScaleMax(float scaleMax)
    {
        this.scaleMax = scaleMax;
    }

    public void setScaleMin(float scaleMin)
    {
        this.scaleMin = scaleMin;
    }

    public void setScaleSpeed(float scaleSpeed)
    {
        this.scaleSpeed = scaleSpeed;
    }
}