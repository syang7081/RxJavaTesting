package com.syang7081.statemachine;


import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karen on 12/1/2017.
 */

public class EventProcessor {
    private static final String TAG = EventProcessor.class.getSimpleName();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Integer combineEvent(final EventA eventA, final EventB eventB) {
        // TODO: processing the events here
        Log.d(TAG, "eventA = " + eventA.value + ", event B = " + eventB.valueStr);
        return 1;
    }

    public void processEventBeforeCombined() {
        Observable<EventA> observableA = ObservableManager.getInstance().getObservableA();
        Observable<EventB> observableB = ObservableManager.getInstance().getObservableB();
        Observable<Integer> observable = Observable.combineLatest(observableA, observableB, this::combineEvent)
                .throttleLast(3, TimeUnit.SECONDS);
        observable.observeOn(Schedulers.io()).subscribe(value -> {Log.d(TAG, "processEventBeforeCombined: " + value);});
    }


    class EventContainer {
        public EventA eventA;
        public EventB eventB;
        public EventContainer(final EventA eventA, final EventB eventB) {
            this.eventA = eventA;
            this.eventB = eventB;
        }
    }

    public void processEventIndependently() {
        Observable<EventA> observableA = ObservableManager.getInstance().getObservableA();
        Observable<EventB> observableB = ObservableManager.getInstance().getObservableB();
        Disposable disposable = observableB.observeOn(Schedulers.io()).subscribe(eventB -> {
            Log.d(TAG, "Event B: " + eventB.valueStr);
        });
        compositeDisposable.add(disposable);

        disposable = observableA.observeOn(Schedulers.io()).subscribe(eventA -> {
            Log.d(TAG, "Event A: " + eventA.value);
        });
        compositeDisposable.add(disposable);

    }

    public void processEventAfterCombined() {
        Observable<EventA> observableA = ObservableManager.getInstance().getObservableA();
        Observable<EventB> observableB = ObservableManager.getInstance().getObservableB();
        Observable<EventContainer> observable = Observable.combineLatest(observableA, observableB, EventContainer::new)
                .throttleLast(3, TimeUnit.SECONDS);
        Disposable disposable = observable.observeOn(Schedulers.io()).subscribe(eventContainer -> {
            Log.d(TAG, "processEventAfterCombined: " + eventContainer.eventA.value
                               + ", " + eventContainer.eventB.valueStr);
        });
        compositeDisposable.add(disposable);
    }

    public void unsubscribe() {
        // Must unsubscribe to the original Observable or PublishSubject,
        // otherwise the app has memory leak and also leads to the original Observable being subscribed
        // multiple times as the original Observable just has a single instance.
        // This can be verified by opening/closing the app multiple times
        compositeDisposable.dispose();
    }
}
