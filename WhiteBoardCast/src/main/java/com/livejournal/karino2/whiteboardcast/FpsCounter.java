package com.livejournal.karino2.whiteboardcast;

import java.util.ArrayList;

/**
 * Created by karino on 7/20/13.
 */
public class FpsCounter {
    int fpsCashNum;

    // 12
    public FpsCounter(int cashNum) {
        fpsCashNum = cashNum;
    }

    public int cycleFps() {
        try {
            synchronized (deltaMillList) {
                if(deltaMillList.size() < fpsCashNum)
                    return -1;
                long sum = 0;
                for(long fps : deltaMillList) {
                    sum+=1000/fps;
                }
                return (int) sum/ deltaMillList.size();
            }

        }catch(ArithmeticException ae) {
            // fps is 0 for some reasons.
            return -1;
        }
    }

    long prevTick = -1;
    ArrayList<Long> deltaMillList = new ArrayList<Long>();

    public void push(long currentFrameMil) {
        if(prevTick == -1) {
            prevTick = currentFrameMil;
            return;
        }
        synchronized (deltaMillList) {
            deltaMillList.add(currentFrameMil-prevTick);
            if(deltaMillList.size() > fpsCashNum) {
                deltaMillList.remove(0);
            }
        }
        prevTick = currentFrameMil;
    }
}
