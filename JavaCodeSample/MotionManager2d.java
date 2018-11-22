package com.lmp.javacodesample;

import android.view.MotionEvent;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Ilya on 24.03.14.
 */
public class MotionManager2d extends MotionManagerAbstract
{
    private static final ReentrantLock lock = new ReentrantLock();

    private Point2d visibilityDistanceLimit;
    private float visibilityFactor;

    private boolean isMovementByXAllowed = true;
    private boolean isMovementByYAllowed = true;
    private boolean isZoomingEnabled = true;

    private float centerCorrectionX = 0f;
    private float centerCorrectionZ = 0f;
    private KineticScrolling scroller;
    private boolean flingEnded = true;

    private Point2d moveTo = null;
    private Point2dInt movementDirection;
    private float moveSpeed = 0.001f;
    private long lastMoveTime;

    private long maxClickTime = 500;
    private float maxClickTrace;
    private float minZoomTrace;

    public MotionManager2d(float minScale, float maxScale, float currentScale)
    {
        scaleMin = minScale;
        scaleMax = maxScale;
        scale = currentScale;

        scroller = new KineticScrolling();
        maxClickTrace = RayPicking.getWindowHeight() * 0.05f;
        minZoomTrace = RayPicking.getWindowHeight() * 0.005f;
    }

    public void preDraw(Camera3d sceneCamera)
    {
        move();
        fling();

        lock.lock();
        try
        {
            checkVisibilityDistance();
            checkVisibilityLimit();
            camera.setViewMatrix();
            setCameraCoordinates(sceneCamera);
        }finally
        {
            lock.unlock();
        }
    }

    private void fling()
    {
        if (scroller.computeScrollOffset())
        {
            float passedX;
            float passedY;

            if (isMovementByXAllowed) passedX = startX - scroller.getX();
            else passedX = 0;

            if (isMovementByYAllowed) passedY = startY - scroller.getY();
            else passedY = 0;

            drag(passedX, passedY);

            startX = scroller.getX();
            startY = scroller.getY();
            flingEnded = false;
        } else if (!flingEnded)
        {
            flingEnded = true;
        }
    }

    public void moveCameraTo(float x, float y, float speed)
    {
        moveTo = new Point2d(x, y);
        movementDirection = new Point2dInt();

        if (moveTo.x == camera.target.x) movementDirection.x = 0;
        else if (moveTo.x > camera.target.x) movementDirection.x = 1;
        else movementDirection.x = -1;

        if (moveTo.y == camera.target.z) movementDirection.y = 0;
        else if (moveTo.y > camera.target.z) movementDirection.y = 1;
        else movementDirection.y = -1;

        moveSpeed = speed;
        lastMoveTime = Utils.currentTimeMillis();
    }

    public boolean isMoving()
    {
        return moveTo != null;
    }

    private void move()
    {
        if (moveTo == null) return;

        long now = Utils.currentTimeMillis();
        float stepX = (now - lastMoveTime) * moveSpeed * movementDirection.x;
        float stepZ = (now - lastMoveTime) * moveSpeed * movementDirection.y;

        drag(stepX, stepZ);

        checkMovementPositionX();
        checkMovementPositionZ();

        if (movementDirection.x == 0 && movementDirection.y == 0)
        {
            moveTo = null;
            movementDirection = null;
        }

        lastMoveTime = now;
    }

    private void checkMovementPositionX()
    {
        if (((movementDirection.x > 0 && moveTo.x < camera.target.x) || (movementDirection.x < 0 && moveTo.x > camera.target.x)))
        {
            camera.position.x = moveTo.x;
            camera.target.x = moveTo.x;
            movementDirection.x = 0;
        }
    }

    private void checkMovementPositionZ()
    {
        if (((movementDirection.y > 0 && moveTo.y < camera.target.z) || (movementDirection.y < 0 && moveTo.y > camera.target.z)))
        {
            camera.position.z = moveTo.y;
            camera.target.z = moveTo.y;
            movementDirection.y = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        if (isMoving())
        {
            return false;
        }

        onStartAction(e);

        switch (e.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
                actionDown();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                actionPointerDown(e);
                break;

            case MotionEvent.ACTION_MOVE:
                actionMove(e);
                break;

            case MotionEvent.ACTION_UP:
                return actionUp();

            case MotionEvent.ACTION_POINTER_UP:
                actionPointerUp();
                break;
        }

        return false;
    }

    public void actionDown()
    {
        try
        {
            startX = x;
            startY = y;
            trace = 0.0f;
            touchTime = System.currentTimeMillis();
            scroller.endMotion();
            touchMode = DRAG;
        } catch (Exception ex)
        {
            touchMode = NONE;
            Finals.LOG.writeLog(ex);
            ex.printStackTrace();
        }
    }

    public void actionPointerDown(MotionEvent e)
    {
        try
        {
            touchMode = ZOOM;
            oldDist = spacing(e);
            scroller.endMotion();
        } catch (Exception ex)
        {
            touchMode = NONE;
            Finals.LOG.writeLog(ex);
            ex.printStackTrace();
        }
    }

    public void actionMove(MotionEvent e)
    {
        trace = trace + Math.abs(startX - x) + Math.abs(startY - y);

        if (touchMode == DRAG)
        {
            actionDrag();
        } else if (touchMode == ZOOM)
        {
            actionZoom(e);
        }
    }

    public void actionDrag()
    {
        float passedX;
        float passedY;

        if (isMovementByXAllowed) passedX = startX - x;
        else passedX = 0;

        if (isMovementByYAllowed) passedY = startY - y;
        else passedY = 0;

        drag(passedX, passedY);

        scroller.scroll((int) -passedX, (int) -passedY);
        startX = x;
        startY = y;
    }

    public void actionZoom(MotionEvent e)
    {
        if (!isZoomingEnabled) return;

        touchTime -= 2000;
        float newDist = spacing(e);

        try
        {
            if (Math.abs(oldDist - newDist) > minZoomTrace)
            {
                float averageScale = findAvarageScale(newDist);

                float scaleTmp = scale + averageScale * scaleSpeed * 0.1f;

                if (scaleTmp > scaleMax) scaleTmp = scaleMax;
                if (scaleTmp < scaleMin) scaleTmp = scaleMin;

                lock.lock();
                try
                {
                    scale = scaleTmp;
                    camera.position.y = cameraPosition.y * scale;
                } finally
                {
                    lock.unlock();
                }

                scaleChanged = true;
                oldDist = spacing(e);
            }
        } catch (Exception ex)
        {
            touchMode = NONE;
            Finals.LOG.writeLog(ex);
            ex.printStackTrace();
        }
    }

    private float findAvarageScale(float newDist)
    {
        float averageScale = oldDist - newDist;
        if (touchList.size() >= 3)
        {
            touchList.remove(0);
            touchList.add(oldDist - newDist);
            averageScale = (touchList.get(0) + touchList.get(1) + touchList.get(2)) / 3f;
        } else touchList.add(averageScale);

        return averageScale;
    }

    public boolean actionUp()
    {
        boolean clicked = false;
        if (System.currentTimeMillis() - touchTime < maxClickTime && trace < maxClickTrace)
        {
            clicked = true;
        } else
        {
            if (touchMode == DRAG)
            {
                scroller.fling((int) x, (int) y);
            }
        }

        touchTime = 0;
        touchMode = NONE;

        return clicked;
    }

    public void actionPointerUp()
    {
        touchMode = NONE;
        scroller.endMotion();
    }

    public void onStartAction(MotionEvent e)
    {
        x = e.getX();
        y = e.getY();
    }

    private void drag(float x, float y)
    {
        float passedX = x * visibilityDistance.x * 2f / RayPicking.getWindowWidth();
        float passedY = y * visibilityDistance.y * 2f / RayPicking.getWindowHeight();

        camera.target.x += passedX;
        camera.target.z += passedY;
    }

    private boolean checkVisibilityLimit()
    {
        boolean changedPosition = false;

        if (visibilityDistanceLimit != null)
        {
            if (camera.target.x - visibilityDistance.x < -visibilityDistanceLimit.x + centerCorrectionX)
            {
                camera.target.x = -visibilityDistanceLimit.x + visibilityDistance.x + centerCorrectionX;
                changedPosition = true;
                if (moveTo != null)
                {
                    checkMovementPositionX();
                    movementDirection.x = 0;
                }
            } else if (camera.target.x + visibilityDistance.x > visibilityDistanceLimit.x + centerCorrectionX)
            {
                camera.target.x = visibilityDistanceLimit.x - visibilityDistance.x + centerCorrectionX;
                changedPosition = true;
                if (moveTo != null)
                {
                    checkMovementPositionX();
                    movementDirection.x = 0;
                }
            }

            if (camera.target.z - visibilityDistance.y < -visibilityDistanceLimit.y + centerCorrectionZ)
            {
                camera.target.z = -visibilityDistanceLimit.y + visibilityDistance.y + centerCorrectionZ;
                changedPosition = true;
                if (moveTo != null)
                {
                    checkMovementPositionZ();
                    movementDirection.y = 0;
                }
            } else if (camera.target.z + visibilityDistance.y > visibilityDistanceLimit.y + centerCorrectionZ)
            {
                camera.target.z = visibilityDistanceLimit.y - visibilityDistance.y + centerCorrectionZ;
                changedPosition = true;
                if (moveTo != null)
                {
                    checkMovementPositionZ();
                    movementDirection.y = 0;
                }
            }
        }

        camera.position.x = camera.target.x;
        camera.position.z = camera.target.z;

        return changedPosition;
    }

    public float getVisibilityFactor()
    {
        return visibilityFactor;
    }

    public void init()
    {
        try
        {
            currentDisplayRotation = getDisplayOrientation();
            aspectRatio = (float) Math.max(RayPicking.getWindowWidth(), RayPicking.getWindowHeight())
                    / (float) Math.min(RayPicking.getWindowWidth(), RayPicking.getWindowHeight());
        } catch (Exception e)
        {
            e.printStackTrace();
            Finals.LOG.writeLog(e);
        }

        if (currentDisplayRotation == ORIENTATION_HORIZONTAL)
        {
            scale /= aspectRatio;
            scaleMax /= aspectRatio;
            scaleMin /= aspectRatio;
            camera.position.y = cameraPosition.y * scale;
        }

        camera.setViewMatrix();
    }

    private void checkIfDisplayRotated()
    {
        byte newDisplayRotation = getDisplayOrientation();
        if (newDisplayRotation != currentDisplayRotation)
        {
            if (newDisplayRotation == ORIENTATION_VERTICAL)
            {
                scale *= aspectRatio;
                scaleMax *= aspectRatio;
                scaleMin *= aspectRatio;
            } else
            {
                scale /= aspectRatio;
                scaleMax /= aspectRatio;
                scaleMin /= aspectRatio;
            }

            camera.position.y = cameraPosition.y * scale;
            camera.setViewMatrix();

            currentDisplayRotation = newDisplayRotation;
        }
    }

    private void checkVisibilityDistance()
    {
        if (!scaleChanged && orientationVersion == FenixRenderer.orientationVersion()) return;

        checkIfDisplayRotated();

        camera.setViewMatrix();
        visibilityDistance = RayPicking.intersectionWithXOZ(0f, 0f, camera.getViewMatrix(), AbstractCamera.getProjectionMatrix());
        visibilityDistance.x = Math.abs(visibilityDistance.x - camera.target.x);
        visibilityDistance.y = Math.abs(visibilityDistance.y - camera.target.z);

        visibilityFactor = Math.abs(RayPicking.getWindowWidth() / (visibilityDistance.x - camera.target.x) / 2f);

        checkNewVisibilityDistance();

        scaleChanged = false;
        orientationVersion = FenixRenderer.orientationVersion();
    }

    private void checkNewVisibilityDistance()
    {
        if (visibilityDistanceLimit != null && (visibilityDistance.x > visibilityDistanceLimit.x || visibilityDistance.y > visibilityDistanceLimit.y))
        {
            float scaleMultiplierX = 1f;
            float scaleMultiplierY = 1f;
            boolean resetX = false;
            boolean resetY = false;
            if (visibilityDistance.x > visibilityDistanceLimit.x)
            {
                scaleMultiplierX = visibilityDistanceLimit.x / visibilityDistance.x;
                resetX = true;
            }

            if (visibilityDistance.y > visibilityDistanceLimit.y)
            {
                scaleMultiplierY = visibilityDistanceLimit.y / visibilityDistance.y;
                resetY = true;
            }

            lock.lock();
            try
            {
                if (resetX)
                {
                    camera.target.x = centerCorrectionX;
                    camera.position.x = centerCorrectionX;
                }

                if (resetY)
                {
                    camera.target.z = centerCorrectionZ;
                    camera.position.z = centerCorrectionZ;
                }

                scale *= Math.min(scaleMultiplierX, scaleMultiplierY);
                camera.position.y = cameraPosition.y * scale;
            } finally
            {
                lock.unlock();
            }
            checkVisibilityDistance();
        }
    }

    public void disableYMovement()
    {
        isMovementByYAllowed = false;
    }

    public void disableXMovement()
    {
        isMovementByXAllowed = false;
    }

    public void disableZooming()
    {
        isZoomingEnabled = false;
    }

    public void setVisibilityDistanceLimit(float distX, float distY)
    {
        visibilityDistanceLimit = new Point2d(distX, distY);
    }

    public void setCenterCorrectionPoint(float x, float z)
    {
        centerCorrectionX = x;
        centerCorrectionZ = z;
    }

    public void setCameraPosition(float x, float y, float z)
    {
        camera.position.setAll(x, y, z);
        camera.target.setAll(x, 0f, z);
        cameraPosition.setAll(x, y, z);
    }
}