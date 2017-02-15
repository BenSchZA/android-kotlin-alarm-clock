package com.roostermornings.android;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.dagger.RoosterApplicationModule;

import io.fabric.sdk.android.Fabric;

import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;

public class BaseApplication extends android.app.Application {

    private static final String TAG = "BaseApplication";


    RoosterApplicationComponent roosterApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        /*Component implementations are primarily instantiated via a generated builder.
        An instance of the builder is obtained using the builder() method on the component implementation.
        If a nested @Component.Builder type exists in the component, the builder() method will
        return a generated implementation of that type. If no nested @Component.Builder exists,
        the returned builder has a method to set each of the modules() and component dependencies()
        named with the lower camel case version of the module or dependency type.
         */
        roosterApplicationComponent = DaggerRoosterApplicationComponent
                .builder()
                .roosterApplicationModule(new RoosterApplicationModule(this))
                .build();
    }

    public RoosterApplicationComponent getRoosterApplicationComponent() {
        return roosterApplicationComponent;
    }

}
