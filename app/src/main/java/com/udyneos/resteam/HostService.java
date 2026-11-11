package com.udyneos.resteam;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.*;

/**
 * Host Surface tab/panel bubble (id 1). Layanan foreground agar statistik tetap hidup saat game di depan.
 * Tab menempel di kiri atas dan hanya dipasang ke layar ketika native meminta show(true) (game library berjalan).
 */
public abstract class HostService extends Service implements SurfaceHolder.Callback {
    static { System.loadLibrary("resteam"); }
    static final HostService[] inst = new HostService[2];
    static final Handler ui = new Handler(Looper.getMainLooper());
    abstract int id();
    WindowManager wm; SurfaceView sv; WindowManager.LayoutParams lp; float d; boolean attached;
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
        lp = new WindowManager.LayoutParams((int) (22 * d), (int) (52 * d),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START; lp.x = 0; lp.y = 0;
    }
    @Override public int onStartCommand(Intent i, int flags, int startId) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel("resteam_bubble", "Resteam Bubble", NotificationManager.IMPORTANCE_MIN));
            Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
            PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(this, "resteam_bubble").setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Resteam aktif").setContentText("Tab FPS muncul saat game dari library berjalan").setContentIntent(pi).setOngoing(true).build();
            if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(1, n);
        } catch (Throwable t) {}
        return START_STICKY;
    }
    @Override public void onDestroy() { setShown(false); inst[id()] = null; super.onDestroy(); }
    public void surfaceCreated(SurfaceHolder h) { nativeSurface(id(), h.getSurface(), d); }
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    public void surfaceDestroyed(SurfaceHolder h) { nativeSurface(id(), null, d); }

    void setShown(boolean on) {
        if (on == attached) return;
        try {
            if (on) { lp.width = (int) (22 * d); lp.height = (int) (52 * d); wm.addView(sv, lp); } else wm.removeView(sv);
            attached = on;
        } catch (Throwable t) {}
    }
    /** Dipanggil dari native; false bila layanan belum berjalan (native mencoba lagi). */
    static boolean show(int id, final boolean on) {
        final HostService s = inst[id]; if (s == null) return false;
        ui.post(() -> s.setShown(on)); return true;
    }
    /** w/h dalam px; h = -1 -> setinggi layar. */
    static void resize(int id, final int w, final int h) {
        final HostService s = inst[id]; if (s == null) return;
        ui.post(() -> { if (!s.attached) return; s.lp.width = w; s.lp.height = h; s.wm.updateViewLayout(s.sv, s.lp); });
    }
    static void moveBy(int id, final int dx, final int dy) {
        final HostService s = inst[id]; if (s == null) return;
        ui.post(() -> { if (!s.attached) return; s.lp.x += dx; s.lp.y += dy; s.wm.updateViewLayout(s.sv, s.lp); });
    }
}
