package com.morihacky.android.rxjava.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.morihacky.android.rxjava.MainActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.ConnectableFlowable;

public class RotationPersist1WorkerFragment extends Fragment {

    public static final String TAG = RotationPersist1WorkerFragment.class.toString();

    private IAmYourMaster mMasterFrag;
    private ConnectableFlowable<Integer> mStoredIntsFlowable;
    private Disposable mStoredIntsDisposable;

    /**
     * Hold a reference to the activity -> caller fragment this way when the worker frag kicks off we
     * can talk back to the master and send results
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mMasterFrag = (RotationPersist1Fragment) ((MainActivity) context)
                .getSupportFragmentManager()
                .findFragmentByTag(RotationPersist1Fragment.TAG);

        if (mMasterFrag == null) {
            throw new ClassCastException("We did not find a master who can understand us :(");
        }
    }

    /** This method will only be called once when the retained Fragment is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        if (mStoredIntsFlowable != null) {
            return;
        }

        Flowable<Integer> intsObservable = Flowable.interval(1, TimeUnit.SECONDS)
                .map(Long::intValue)
                .take(20);

        mStoredIntsFlowable = intsObservable.publish();
        mStoredIntsDisposable = mStoredIntsFlowable.connect();
    }

    /** The Worker fragment has started doing it's thing */
    @Override
    public void onResume() {
        super.onResume();
        mMasterFrag.observeResults(mStoredIntsFlowable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStoredIntsDisposable.dispose();
    }

    /** Set the callback to null so we don't accidentally leak the Activity instance. */
    @Override
    public void onDetach() {
        super.onDetach();
        mMasterFrag = null;
    }

    public interface IAmYourMaster {

        void observeResults(Flowable<Integer> intsObservable);
    }
}
