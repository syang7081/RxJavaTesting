package com.syang7081.statemachine;


import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by Karen on 12/1/2017.
 */

public class EventGenerator {
    private static final String TAG = EventGenerator.class.getSimpleName();
    private boolean stopped = false;

    Observable<EventA> replayObservableEvaentA = null;
    public void generateEventsTogather() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                PublishSubject<EventA> observableA = (PublishSubject<EventA>) ObservableManager.getInstance().getObservableA();
                BehaviorSubject<EventB> observableB = (BehaviorSubject<EventB>) ObservableManager.getInstance().getObservableB();

                replayObservableEvaentA = observableA.replay(2).autoConnect(0);

                for (int i = 0; i < 30 && !stopped; i++) {
                    EventA eventA = new EventA(i);
                    observableA.onNext(eventA);
                    if (i % 10 == 0) {
                        EventB eventB = new EventB(i);
                        observableB.onNext(eventB);
                    }
                    sleep(300);
                }
            }
        });
        t.start();
    }

    // With locks to ensure one stream finishes before another
    public void generateEventsSequentially() {
        LockManager.GlobalLock lock = LockManager.getInstance().acquireLock();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                PublishSubject<EventA> observableA = (PublishSubject<EventA>) ObservableManager.getInstance().getObservableA();

                for (int i = 0; i < 30; i++) {
                    EventA eventA = new EventA(i);
                    observableA.onNext(eventA);
                    sleep(300);
                }
                lock.release();
            }
        });
        t.start();

        t = new Thread(new Runnable() {
            @Override
            public void run() {
                LockManager.GlobalLock lock2 = LockManager.getInstance().acquireLock();
                BehaviorSubject<EventB> observableB = (BehaviorSubject<EventB>) ObservableManager.getInstance().getObservableB();

                for (int i = 0; i < 30; i++) {
                    EventB eventB = new EventB(i);
                    observableB.onNext(eventB);
                    sleep(300);
                }
                lock2.release();
            }
        });
        t.start();
    }


    public Observable<Long> generateSingleReplayEvent() {
        Observable<Long> source = Observable.intervalRange(1, 50, 0, 100,
                TimeUnit.MILLISECONDS);
        source.subscribe();
        Observable<Long> replay = source.replay(2).autoConnect(2);

        return replay;
    }

    public Observable<EventA> getReplayObservableEvaentA() {
        return replayObservableEvaentA;
    }

    public static void sleep(int timeInterval) {
        try {
            Thread.sleep(timeInterval);
        }
        catch (Exception e) {}
    }

    public void stop() {
        stopped = true;
    }


}
