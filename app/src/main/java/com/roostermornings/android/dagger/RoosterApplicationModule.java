package com.roostermornings.android.dagger;

import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;

/**
 * Created by Abdul on 6/14/2016.
 * Dagger injection module, providing instances of the application classes and satisfying their dependencies
 */
@Module
public class RoosterApplicationModule {

    BaseApplication baseApplication;

    //pass the base application into the constructor for context
    public RoosterApplicationModule(BaseApplication baseApplication) {
        this.baseApplication = baseApplication;
    }

    //For cases where @Inject is insufficient or awkward,
    //an @Provides-annotated method satifies a dependency.
    //The method's return type defines which dependency it satisfies.

    //All @Provides methods must belong to a module.
    //These are just classes that have an @Module annotation.

    //By convention, @Provides methods are named with a 'provide' prefix
    //and module classes are named with a 'Module' suffix.

    @Provides
    @Singleton
    public BaseApplication providesBaseApplication() {
        return baseApplication;
    }

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences() {
        return baseApplication.getSharedPreferences(baseApplication.getString(R.string.preferences_key),
                baseApplication.MODE_PRIVATE);
    }
}


