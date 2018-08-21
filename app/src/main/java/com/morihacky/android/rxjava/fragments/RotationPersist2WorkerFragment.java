package com.morihacky.android.rxjava.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.morihacky.android.rxjava.MainActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;

public class RotationPersist2WorkerFragment extends Fragment {

    public static final String TAG = RotationPersist2WorkerFragment.class.toString();

    private PublishProcessor<Integer> mIntStream;
    private PublishProcessor<Boolean> mLifeCycleStream;

    private IAmYourMaster mMasterFrag;

    /**
     * Since we're holding a reference to the Master a.k.a Activity/Master Frag remember to explicitly
     * remove the worker fragment or you'll have a mem leak in your hands.
     *
     * <p>See {@link MainActivity#onBackPressed()}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mMasterFrag = (RotationPersist2Fragment) ((MainActivity) context)
                .getSupportFragmentManager()
                .findFragmentByTag(RotationPersist2Fragment.TAG);

        if (mMasterFrag == null) {
            throw new ClassCastException("We did not find a master who can understand us :(");
        }
    }

    /** This method will only be called once when the retained Fragment is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntStream = PublishProcessor.create();
        mLifeCycleStream = PublishProcessor.create();

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        mIntStream.takeUntil(mLifeCycleStream);

        Flowable.interval(1, TimeUnit.SECONDS).map(Long::intValue).take(20).subscribe(mIntStream);
    }

    /** The Worker fragment has started doing it's thing */
    @Override
    public void onResume() {
        super.onResume();
        mMasterFrag.setStream(mIntStream);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLifeCycleStream.onComplete();
    }

    /** Set the callback to null so we don't accidentally leak the Activity instance. */
    @Override
    public void onDetach() {
        super.onDetach();
        mMasterFrag = null;
    }

    public interface IAmYourMaster {

        void setStream(Flowable<Integer> intStream);
    }
}
