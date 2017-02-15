package com.roostermornings.android.dagger;

import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Component;

import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.fragment.base.BaseFragment;

/**
 * Annotates an interface or abstract class for which a fully-formed, dependency-injected implementation
 * is to be generated from a set of modules(). The generated class will have the name of the type
 * annotated with @Component prepended with Dagger. For example, @Component interface MyComponent {...}
 * will produce an implementation named DaggerMyComponent.
 */
@Component(modules = RoosterApplicationModule.class)
@Singleton
public interface RoosterApplicationComponent {

    //Members-injection methods have a single parameter and inject dependencies into each of the
    //Inject-annotated fields and methods of the passed instance.
    //A members-injection method may be void or return its single parameter as a convenience for chaining.

    void inject(RoosterApplicationModule roosterApplicationModule);

    void inject(BaseActivity baseActivity);

    void inject(BaseFragment baseFragment);

    //Provision methods have no parameters and return an injected or provided type.
    //Each method may have a Qualifier annotation as well.

    SharedPreferences sharedPreferences();

}
