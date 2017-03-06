package com.roostermornings.android;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;
import com.roostermornings.android.node_api.IHTTPClient;

import io.fabric.sdk.android.Fabric;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class BaseApplication extends android.app.Application {

    private static final String TAG = "BaseApplication";
    RoosterApplicationComponent roosterApplicationComponent;
    public Retrofit mRetrofit;
    public IHTTPClient mAPIService;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        if (BuildConfig.DEBUG) {
            //Remove in release version... don't want to leave stethoscopes lying around
            //Stetho: http://facebook.github.io/stetho/ - debug bridge for Android (view SQL etc.)
            //Go to chrome://inspect/ in Chrome to inspect
            Stetho.initializeWithDefaults(this);
        }

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


        mRetrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.node_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mAPIService = mRetrofit.create(IHTTPClient.class);
    }

    public RoosterApplicationComponent getRoosterApplicationComponent() {
        return roosterApplicationComponent;
    }

    public IHTTPClient getAPIService(){
        return mAPIService;
    }

}
