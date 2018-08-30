package cn.gen.peer;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mac on 2018/3/23.
 */

public class BandwidthLimiter {

    private Handler handler;
    private long totalSend = 0;
    private long limitSpeed = 100 * 1024;
    private long currentSpeed = 0;
    private long startTime;
    private double during = 1.0;
    private boolean currentSecSent = false;

    private long displaySpeed = 0;

    private boolean disabled = false;

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public interface OnBandwidthFree {
        void onBandwidthFree();
    }

    public interface OnBandwidthEvent {
        void onCurrentSpeed(long speed);
    }

    private ArrayList<OnBandwidthFree> listeners = new ArrayList<>();

    OnBandwidthEvent onBandwidthEvent;

    public BandwidthLimiter() {
    }

    public void addFreeListener(OnBandwidthFree onFree) {
        listeners.add(onFree);
    }

    public void removeFreeListener(OnBandwidthFree onFree) {
        listeners.remove(onFree);
    }

    public void setOnBandwidthEvent(OnBandwidthEvent onBandwidthEvent) {
        this.onBandwidthEvent = onBandwidthEvent;
    }

    Runnable secClock = new Runnable() {
        @Override
        public void run() {
            long ds;
            if (currentSecSent || currentSpeed > limitSpeed) {
                during+=1;
                currentSecSent = false;
                currentSpeed = (long) (totalSend / during);
                int off = 0;
                while (currentSpeed < limitSpeed) {
                    if (listeners.size() <= off) break;
                    OnBandwidthFree lis = listeners.get(off++);
                    lis.onBandwidthFree();
                }
                ds = currentSpeed;
            }else {
                ds = 0;
            }
            if (ds != displaySpeed) {
                displaySpeed = ds;
                if (onBandwidthEvent != null) {
                    onBandwidthEvent.onCurrentSpeed(displaySpeed);
                }
            }
            handler.postDelayed(secClock, 1000);
        }
    };

    public void start(Handler handler) {
        this.handler = handler;
        currentSpeed = 0;
        totalSend = 0;
        startTime  = new Date().getTime();
        handler.postDelayed(secClock, 1000);
    }

    public void stop() {
        handler.removeCallbacks(secClock);
    }

    public void setLimitSpeed(long limitSpeed) {
        this.limitSpeed = limitSpeed;
    }

    public boolean canSend() {
        return !disabled && currentSpeed <= limitSpeed;
    }

    public void sendData(long size) {
        totalSend += size;
        currentSpeed = (long) (totalSend / during);
        currentSecSent = true;
    }

    public long getDisplaySpeed() {
        return displaySpeed;
    }
}
