package com.lmp.javacodesample;

public class Point3d
{
    public float x;
    public float y;
    public float z;

    public Point3d()
    {
        x = 0;
        y = 0;
        z = 0;
    }

    public Point3d(float _x, float _y, float _z)
    {
        x = _x;
        y = _y;
        z = _z;
    }

    public Point3d(String[] values)
    {
        this.x = Float.parseFloat(values[0]);
        this.y = Float.parseFloat(values[1]);
        this.z = Float.parseFloat(values[2]);
    }

    public void setAll(float[] array)
    {
        x = array[0];
        y = array[1];
        z = array[2];
    }

    public void setAll(float _x, float _y, float _z)
    {
        x = _x;
        y = _y;
        z = _z;
    }

    public void setAllFrom(Point3d another)
    {
        x = another.x;
        y = another.y;
        z = another.z;
    }

    public void normalize()
    {
        float mod = (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);

        if (mod != 0 && mod != 1)
        {
            mod = 1 / mod;
            this.x *= mod;
            this.y *= mod;
            this.z *= mod;
        }
    }

    public void add(Point3d another)
    {
        this.x += another.x;
        this.y += another.y;
        this.z += another.z;
    }

    public void subtract(Point3d another)
    {
        this.x -= another.x;
        this.y -= another.y;
        this.z -= another.z;
    }

    public void multiply(Float multiplier)
    {
        this.x *= multiplier;
        this.y *= multiplier;
        this.z *= multiplier;
    }

    public float length()
    {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Point3d clone()
    {
        return new Point3d(x, y, z);
    }

    public void rotateX(float angle)
    {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        Point3d temp = new Point3d(this.x, this.y, this.z);

        this.y = (temp.y * cosRY) - (temp.z * sinRY);
        this.z = (temp.y * sinRY) + (temp.z * cosRY);
    }

    public void rotateY(float angle)
    {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        Point3d temp = new Point3d(this.x, this.y, this.z);

        this.x = (temp.x * cosRY) + (temp.z * sinRY);
        this.z = (temp.x * -sinRY) + (temp.z * cosRY);
    }

    public void rotateZ(float angle)
    {
        float cosRY = (float) Math.cos(angle);
        float sinRY = (float) Math.sin(angle);

        Point3d temp = new Point3d(this.x, this.y, this.z);

        this.x = (temp.x * cosRY) - (temp.y * sinRY);
        this.y = (temp.x * sinRY) + (temp.y * cosRY);
    }

    @Override
    public String toString()
    {
        return x + "," + y + "," + z;
    }

    public static Point3d add(Point3d a, Point3d b)
    {
        return new Point3d(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Point3d subtract(Point3d a, Point3d b)
    {
        return new Point3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Point3d multiply(Point3d a, Point3d b)
    {
        return new Point3d(a.x * b.x, a.y * b.y, a.z * b.z);
    }

    public static Point3d cross(Point3d v, Point3d w)
    {
        return new Point3d((w.y * v.z) - (w.z * v.y), (w.z * v.x) - (w.x * v.z), (w.x * v.y) - (w.y * v.x));
    }

    public static float dot(Point3d v, Point3d w)
    {
        return (v.x * w.x + v.y * w.y + w.z * v.z);
    }

    public static float len(Point3d a, Point3d b)
    {
        return (float) Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z));
    }

    public static float lenXZ(Point3d a, Point3d b)
    {
        return (float) Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    public float[] toFloatArray()
    {
        return new float[]
                {
                        x, y, z
                };
    }

    public void multiplyMat4x4(float[] mat)
    {
        float xr = x * mat[0] + y * mat[4] + z * mat[8] + mat[12];
        float yr = x * mat[1] + y * mat[5] + z * mat[9] + mat[13];
        float zr = x * mat[2] + y * mat[6] + z * mat[10] + mat[14];
        x = xr;
        y = yr;
        z = zr;
    }
}
