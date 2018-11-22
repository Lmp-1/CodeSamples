package com.lmp.javacodesample;

import android.widget.Scroller;

import java.util.ArrayList;

public class KineticScrolling
{
    private Scroller scroller;
    private ArrayList<PointerParameters> pointers;
    private int smoothingPointersNumber = 4;
    private int maxVelocity = 0;

    public KineticScrolling()
    {
        scroller = new Scroller(Shared.context());
        pointers = new ArrayList<PointerParameters>();
    }

    public void fling(int startX, int startY)
    {
        if (pointers.size() == 0) return;

        if (maxVelocity == 0)
        {
            maxVelocity = Math.max(RayPicking.getWindowWidth(), RayPicking.getWindowHeight()) * 2;
        }

        int velocityX = 0;
        int velocityY = 0;

        for (int i = 0; i < pointers.size(); i++)
        {
            velocityX += pointers.get(i).x;
            velocityY += pointers.get(i).y;
        }

        int pointersSize = pointers.size();
        float time = (System.currentTimeMillis() - pointers.get(0).time) / pointersSize;
        if (time <= 15f)
        {
            time = 15f;
        }
        velocityX /= pointersSize;
        velocityY /= pointersSize;

        velocityX = (int) (velocityX * 1000f / time);
        velocityY = (int) (velocityY * 1000f / time);

        velocityX = Utils.clamp(-maxVelocity, maxVelocity, velocityX);
        velocityY = Utils.clamp(-maxVelocity, maxVelocity, velocityY);

        if (pointersSize < smoothingPointersNumber)
        {
            velocityX /= smoothingPointersNumber - pointersSize;
            velocityY /= smoothingPointersNumber - pointersSize;
        }

        pointers.clear();
        scroller.fling(startX, startY, velocityX, velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public synchronized void scroll(int x, int y)
    {
        pointers.add(new PointerParameters(x, y, System.currentTimeMillis()));
        if (pointers.size() > smoothingPointersNumber)
        {
            pointers.remove(0);
        }
    }

    public boolean computeScrollOffset()
    {
        return scroller.computeScrollOffset();
    }

    public int getX()
    {
        return scroller.getCurrX();
    }

    public int getY()
    {
        return scroller.getCurrY();
    }

    public void endMotion()
    {
        pointers.clear();
        scroller.forceFinished(true);
    }

    public void setSmoothingPointersNumber(int num)
    {
        smoothingPointersNumber = num;
    }

    class PointerParameters
    {
        int x;
        int y;
        long time;

        public PointerParameters(int x, int y, long time)
        {
            this.x = x;
            this.y = y;
            this.time = time;
        }
    }
}