package Framework.Gui;

import Framework.Utils;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static Framework.Utils.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Created by rafael on 5/28/17.
 */
public class Vis3DOpenGL{
    final boolean active;
    public final int xDim;
    public final int yDim;
    public final int zDim;
    final int maxDim;
    final float scaleDims;
    final float trans;
    final float[]circlPtsDefault= Utils.GenCirclePoints(1,20);
    //final float transZ;
    public final int xPix;
    public final int yPix;
    long lastFrameTime=-1;
    TickTimer tt=new TickTimer();
    Camera camera;
    private static int BLACK=RGB(0,0,0),WHITE=RGB(1,1,1);

    public Vis3DOpenGL(String title, int xPix, int yPix, int xDim, int yDim, int zDim, boolean active) {
        camera=new Camera();
        this.active = active;
        int maxDim = Math.max(xDim, yDim);
        this.maxDim = Math.max(maxDim, zDim);
        this.xDim = xDim;
        this.yDim = yDim;
        this.zDim = zDim;
        this.xPix = xPix;
        this.yPix = yPix;
        scaleDims = (float) (2.0 / this.maxDim);
        trans = (float) (-this.maxDim / 2.0);
        //transZ = (float) (-zDim * 0.6);

        if (active) {
            try {
                Display.setDisplayMode(new DisplayMode(xPix, yPix));
                Display.setTitle(title);
                Display.create();
            } catch (LWJGLException e) {
                e.printStackTrace();
                System.err.println("unable to create Vis3D display");
            }
            glEnable(GL_DEPTH_TEST);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glFrustum(-1, 1, -1, 1, 1, 1000);
            //glFrustum(0,maxDim,0,maxDim,maxDim,maxDim+zDim);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glScalef(scaleDims, scaleDims, scaleDims);
            //glTranslatef(transXY, transXY, transZ);
        }
    }
    public Vis3DOpenGL(String title, int xPix, int yPix, int xDim, int yDim, int zDim) {
        this.active = true;
        maxDim = Math.max(xDim, yDim);
        this.xDim = xDim;
        this.yDim = yDim;
        this.zDim = zDim;
        this.xPix = xPix;
        this.yPix = yPix;
        scaleDims = (float) (2.0 / this.maxDim);
        trans = (float) (-this.maxDim / 2.0);

        if (active) {
            try {
                Display.setDisplayMode(new DisplayMode(xPix, yPix));
                Display.setTitle(title);
                Display.create();
            } catch (LWJGLException e) {
                e.printStackTrace();
                System.err.println("unable to create Vis3D display");
            }
            glEnable(GL_DEPTH_TEST);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glFrustum(-1, 1, -1, 1, 1, 1000);
            //glFrustum(0,maxDim,0,maxDim,maxDim,maxDim+zDim);
            //glTranslatef(0,0,trans*2);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glScalef(scaleDims, scaleDims, scaleDims);
            //glTranslatef(transXY, transXY, transZ);
        }
    }
    int GetDelta() {
        long time = System.currentTimeMillis();
        int delta = (int) (time - lastFrameTime);
        if(lastFrameTime==-1){
            delta=0;
        }
        lastFrameTime = time;

        return delta;
    }
    public void TickPause(int millis){
        tt.TickPause(millis);
    }
    public void Clear(int color){
        if(active) {
            glClearColor((float)GetRed(color),(float)GetGreen(color),(float)GetBlue(color), 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
    }

    public void Show(){
        if(active) {
            camera.acceptInputRotate(1);
            camera.acceptInputGrab();
            camera.acceptInputMove(GetDelta()/10.0f);
            camera.apply();
            Display.update();
        }
    }
    public boolean CheckClosed(){
        if(active) {
            return Display.isCloseRequested();
        }
        return true;
    }
    public void Dispose(){
        if(active) {
            Display.destroy();
        }
    }
    public boolean IsActive(){
        return active;
    }
    public void FanShape(double centerX,double centerY,double centerZ,double scale,float[]points,double r,double g,double b) {
        if(active) {
            FanShape((float)centerX, (float)centerY, (float)centerZ, (float)scale, points,RGB((float)r, (float)g, (float)b));
        }
    }
    public void CelSphere(double x,double y,double z,double rad,int color){
        if(active) {
            float xf = (float) x, yf = (float) y, zf = (float) z, radf = (float) rad, rf = (float) GetRed(color), gf = (float) GetGreen(color), bf = (float) GetBlue(color);
            //draw outline
            FanShape(xf, yf, zf, radf, circlPtsDefault,RGB((float) 0, (float) 0, (float) 0), (float) 0, (float) 0, -0.1f);
            //draw circle
            FanShape(xf, yf, zf, radf*0.9f, circlPtsDefault,RGB(rf, gf, bf));
            //add cool specular lighting dot
            FanShape(xf, yf, zf, radf * 0.2f, circlPtsDefault,RGB((float) 1, (float) 1, (float) 1), 1.4f, 1.4f, 0.1f);
        }
    }
    public void Circle(double x,double y,double z,double rad,int color){
        FanShape((float)x,(float)y,(float)z,rad,circlPtsDefault,GetRed(color),GetGreen(color),GetBlue(color));
    }

    public void FanShape(float centerX,float centerY,float centerZ,float scale,float[]points,int color) {
        if(active) {
            glPushMatrix();
            glTranslatef(centerX+trans,centerY+trans,-centerZ+trans);
            //glRotatef((float)Math.PI,0,1,0);
            //glTranslatef(0,0,-trans);
            glScalef(scale,scale,scale);
            glRotatef(camera.rotation[2], 0, 0, -1);
            glRotatef(camera.rotation[1], 0, -1, 0);
            glRotatef(camera.rotation[0], -1, 0, 0);
            glColor4f((float)GetRed(color),(float)GetGreen(color) ,(float)GetBlue(color),(float)GetAlpha(color));
            glBegin(GL_TRIANGLE_FAN);
            glVertex3f(0, 0, 0);
            for (int i = 0; i < points.length / 2; i++) {
                float x = (points[i * 2]);
                float y = (points[i * 2 + 1]);
                float z = 0;
                glVertex3f(x, y, z);
            }
            glEnd();
            glPopMatrix();
        }
    }

    public void FanShape(float centerX,float centerY,float centerZ,float scale,float[]points,int color,float xdisp,float ydisp,float zdisp) {
        if(active) {
            float r=(float)GetRed(color);
            float g=(float)GetGreen(color);
            float b=(float)GetBlue(color);
            glPushMatrix();
            glTranslatef(centerX+trans,centerY+trans,-centerZ+trans);
            //glRotatef((float)Math.PI,0,1,0);
            //glTranslatef(0,0,-trans);
            glScalef(scale,scale,scale);
            glRotatef(camera.rotation[2], 0, 0, -1);
            glRotatef(camera.rotation[1], 0, -1, 0);
            glRotatef(camera.rotation[0], -1, 0, 0);
            glTranslatef(xdisp,ydisp,zdisp);
            glColor3f(r, g, b);
            glBegin(GL_TRIANGLE_FAN);
            glVertex3f(0, 0, 0);
            for (int i = 0; i < points.length / 2; i++) {
                float x = (points[i * 2]);
                float y = (points[i * 2 + 1]);
                float z = 0;
                glVertex3f(x, y, z);
            }
            glEnd();
            glPopMatrix();
        }
    }
    void SaveImg(String path,String mode){
        if(active){
            File out=new File(path);
            glReadBuffer(GL_FRONT);
            int width = Display.getDisplayMode().getWidth();
            int height= Display.getDisplayMode().getHeight();
            int bpp = 4; // Assuming a 32-bit display with a byte each for red, green, blue, and alpha.
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer );
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for(int x = 0; x < width; x++) {
                for(int y = 0; y < height; y++) {
                    int i = (x + (width * y)) * bpp;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
                }
            }
            try {
                ImageIO.write(image, mode, out);
            } catch (IOException e) { e.printStackTrace(); }

        }
    }
    public void ToPNG(String path){
        SaveImg(path,"png");
    }
    public void ToJPG(String path){
        SaveImg(path,"jpg");
    }
    public void ToGIF(String path){
        SaveImg(path,"gif");
    }
}



class Camera {
    public static float moveSpeed = 0.5f;

    private static float maxLook = 85;

    private static float mouseSensitivity = 0.05f;

    static float[]pos=new float[3];
    static float[]rotation=new float[3];

    public static void apply() {
        if(rotation[1] / 360 > 1) {
            rotation[1] -= 360;
        } else if(rotation[1] / 360 < -1) {
            rotation[1] += 360;
        }
        glLoadIdentity();
        glRotatef(rotation[0], 1, 0, 0);
        glRotatef(rotation[1], 0, 1, 0);
        glRotatef(rotation[2], 0, 0, 1);
        glTranslatef(-pos[0], -pos[1], -pos[2]);
    }

    public static void acceptInput(float delta) {
        acceptInputRotate(delta);
        acceptInputGrab();
        acceptInputMove(delta);
    }

    public static void acceptInputRotate(float delta) {
        if(Mouse.isGrabbed()) {
            float mouseDX = Mouse.getDX();
            float mouseDY = -Mouse.getDY();
            rotation[1] += mouseDX * mouseSensitivity * delta;
            rotation[0] += mouseDY * mouseSensitivity * delta;
            rotation[0] = Math.max(-maxLook, Math.min(maxLook, rotation[0]));
        }
    }

    public static void acceptInputGrab() {
        if(Mouse.isInsideWindow() && Mouse.isButtonDown(0)) {
            Mouse.setGrabbed(true);
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            Mouse.setGrabbed(false);
        }
    }

    public static void acceptInputMove(float delta) {
        boolean keyUp = Keyboard.isKeyDown(Keyboard.KEY_W);
        boolean keyDown = Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean keyRight = Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean keyLeft = Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean keyFast = Keyboard.isKeyDown(Keyboard.KEY_Q);
        boolean keySlow = Keyboard.isKeyDown(Keyboard.KEY_E);
        boolean keyFlyUp = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        boolean keyFlyDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        boolean keyReset= Keyboard.isKeyDown(Keyboard.KEY_R);


        float speed;

        if(keyReset){
            pos[0]=0;pos[1]=0;pos[2]=0;
            rotation[0]=0;rotation[1]=0;rotation[2]=0;
        }
        if(keyFast) {
            speed = moveSpeed * 5;
        }
        else if(keySlow) {
            speed = moveSpeed / 2;
        }
        else {
            speed = moveSpeed;
        }

        speed *= delta;

        if(keyFlyUp) {
            pos[1] += speed;
        }
        if(keyFlyDown) {
            pos[1] -= speed;
        }

        if(keyDown) {
            pos[0] -= Math.sin(Math.toRadians(rotation[1])) * speed;
            pos[2] += Math.cos(Math.toRadians(rotation[1])) * speed;
        }
        if(keyUp) {
            pos[0] += Math.sin(Math.toRadians(rotation[1])) * speed;
            pos[2] -= Math.cos(Math.toRadians(rotation[1])) * speed;
        }
        if(keyLeft) {
            pos[0] += Math.sin(Math.toRadians(rotation[1] - 90)) * speed;
            pos[2] -= Math.cos(Math.toRadians(rotation[1] - 90)) * speed;
        }
        if(keyRight) {
            pos[0] += Math.sin(Math.toRadians(rotation[1] + 90)) * speed;
            pos[2] -= Math.cos(Math.toRadians(rotation[1] + 90)) * speed;
        }
    }
}

