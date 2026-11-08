package com.udyneos.resteam;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.*;

/** Host Surface overlay (id 0 = stats, id 1 = sidebar). Render + logika di C++. */
public abstract class HostService extends Service implements SurfaceHolder.Callback {
    static { System.loadLibrary("resteam"); }
    static final HostService[] inst = new HostService[2];
    abstract int id();
    WindowManager wm; SurfaceView sv; WindowManager.LayoutParams lp; float d;
    static native void nativeSurface(int id, Surface s, float density);
    static native void nativeTouch(int id, int act, float rx, float ry, float x, float y);

    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onCreate() {
        super.onCreate(); inst[id()] = this;
        d = getResources().getDisplayMetrics().density;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sv = new SurfaceView(this);
        sv.setZOrderOnTop(true);
        sv.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        sv.getHolder().addCallback(this);
        sv.setOnTouchListener((v, e) -> { nativeTouch(id(), e.getActionMasked(), e.getRawX(), e.getRawY(), e.getX(), e.getY()); return true; });
        boolean stats = id() == 0;
        lp = new WindowManager.LayoutParams((int) ((stats ? 150 : 40) * d), (int) ((stats ? 86 : 40) * d),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = (int) ((stats ? 24 : 8) * d); lp.y = (int) ((stats ? 16 : 120) * d);
        wm.addView(sv, lp);
    }
    @Override public void onDestroy() { if (sv != null) wm.removeView(sv); inst[id()] = null; super.onDestroy(); }
    public void surfaceCreated(SurfaceHolder h) { nativeSurface(id(), h.getSurface(), d); }
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    public void surfaceDestroyed(SurfaceHolder h) { nativeSurface(id(), null, d); }

    static void resize(int id, final int w, final int h) {
        final HostService s = inst[id]; if (s == null) return;
        s.sv.post(() -> { s.lp.width = w; s.lp.height = h; s.wm.updateViewLayout(s.sv, s.lp); });
    }
    static void moveBy(int id, final int dx, final int dy) {
        final HostService s = inst[id]; if (s == null) return;
        s.sv.post(() -> { s.lp.x += dx; s.lp.y += dy; s.wm.updateViewLayout(s.sv, s.lp); });
    }
}
