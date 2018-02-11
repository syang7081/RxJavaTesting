package com.syang7081.statemachine;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private EventGenerator eventGenerator = null;
    private EventProcessor eventProcessor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (eventGenerator == null) {
            Log.d(TAG, "Start event generating");
            eventGenerator = new EventGenerator();
            //eventGenerator.generateEventsTogather();
            eventGenerator.generateEventsSequentially();
        }

        if (eventProcessor == null) {
            eventProcessor = new EventProcessor();
            //eventProcessor.processEventBeforeCombined();
            //eventProcessor.processEventAfterCombined();
            eventProcessor.processEventIndependently();
        }

        //testReplayObservable();
    }

    Disposable disposable = null;
    void testReplayObservable() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start testReplayObservable");
                Observable<Long> replay = generateReplayObservable();

                EventGenerator.sleep(5000);

                disposable = replay.subscribe(data -> {
                            Log.d(TAG, "Replay 1 - Got data =  " + data);
                        });

                disposable = replay.subscribe(data -> {
                    Log.d(TAG, "Replay 2 - Got data =  " + data);
                });

                disposable = replay.subscribe(data -> {
                    Log.d(TAG, "Replay 3 - Got data =  " + data);
                });

                Log.d(TAG, "end of testReplayObservable");
            }
        });
        t.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "stop generator");

        if (eventGenerator != null) {
            eventGenerator.stop();
        }

        if (eventProcessor != null) {
            eventProcessor.unsubscribe();
        }
    }

    public Observable<Long> generateReplayObservable() {
        Observable<Long> source = Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> e) throws Exception {
                    for (long i = 0; i < 10; i++) {
                    e.onNext(i);
                    EventGenerator.sleep(300);
                }
            }
        });

        ConnectableObservable<Long> replay = source.replay(2); //.autoConnect(0);
        replay.connect();

        //source.replay(2).autoConnect(0) is equivalent to:
        // replay = source.replay(2);
        // replay.connect();
        // ...
        // replay.subscribe(...)

        // source.replay(2).autoConnect(1) means that the observabal will emit FROM THE FIRST item when
        // there is the first subscribe joined in. The first subscriber will get all items. The observable
        // keeps the replay size as 2 if the observable already emitted all
        // items. When a new subscriber joins, the new subscriber will get the last 2 items

        // Typical usage should be: source.replay(1).autoConnect(0) - just keep the last to be replayable

        return source.replay(1).autoConnect(1); //.throttleLast(1, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroyed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
