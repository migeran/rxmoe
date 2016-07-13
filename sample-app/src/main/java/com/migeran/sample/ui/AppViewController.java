/**
 * Copyright 2016 MattaKis Consulting Kft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.migeran.sample.ui;

import com.intel.moe.natj.general.Pointer;
import com.intel.moe.natj.general.ann.Owned;
import com.intel.moe.natj.general.ann.RegisterOnStartup;
import com.intel.moe.natj.objc.ObjCRuntime;
import com.intel.moe.natj.objc.ann.ObjCClassName;
import com.intel.moe.natj.objc.ann.Property;
import com.intel.moe.natj.objc.ann.Selector;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import ios.NSObject;
import ios.foundation.NSOperationQueue;
import ios.foundation.enums.NSQualityOfService;
import ios.uikit.UIButton;
import ios.uikit.UILabel;
import ios.uikit.UIViewController;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.ios.schedulers.IOSSchedulers;

/**
 * Demonstrates how to use RxJava on iOS with MOE.
 */
@com.intel.moe.natj.general.ann.Runtime(ObjCRuntime.class)
@ObjCClassName("AppViewController")
@RegisterOnStartup
public class AppViewController extends UIViewController {

    private static final String TAG = "RxMOESample";

    private final NSOperationQueue operationQueue;

    @Owned
    @Selector("alloc")
    public static native AppViewController alloc();

    @Selector("init")
    public native AppViewController init();

    protected AppViewController(Pointer peer) {
        super(peer);
        operationQueue = NSOperationQueue.alloc().init();
        operationQueue.setQualityOfService(NSQualityOfService.Background);
    }

    public UILabel statusText = null;
    public UIButton helloButton = null;

    @Override
    public void viewDidLoad() {
        statusText = getLabel();
        helloButton = getHelloButton();
    }

    @Selector("statusText")
    @Property
    public native UILabel getLabel();

    @Selector("helloButton")
    @Property
    public native UIButton getHelloButton();

    @Selector("BtnPressedCancel_helloButton:")
    public void BtnPressedCancel_button(NSObject sender) {
        statusText.setText("Hello!");
        Log.d(TAG, "Button pressed on thread " + Thread.currentThread().getId());
        sampleObservable()
                // Run on a background thread
                .subscribeOn(IOSSchedulers.handlerThread(operationQueue))
                // Be notified on the main thread
                .observeOn(IOSSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Completed on thread " + Thread.currentThread().getId());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error on thread " + Thread.currentThread().getId(), e);
                    }

                    @Override
                    public void onNext(String string) {
                        Log.d(TAG, "Next " + string + " on thread " +
                                Thread.currentThread().getId());
                    }
                });
    }

    private static Observable<String> sampleObservable() {
        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                Log.d(TAG, "Call on thread " + Thread.currentThread().getId());
                try {
                    // Do some long running operation
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } catch (InterruptedException e) {
                    throw OnErrorThrowable.from(e);
                }
                return Observable.just("one", "two", "three", "four", "five");
            }
        });
    }

}
