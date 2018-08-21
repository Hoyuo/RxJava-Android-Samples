package com.morihacky.android.rxjava;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.morihacky.android.rxjava.fragments.MainFragment;
import com.morihacky.android.rxjava.fragments.RotationPersist1WorkerFragment;
import com.morihacky.android.rxjava.fragments.RotationPersist2WorkerFragment;
import com.morihacky.android.rxjava.rxbus.RxBus;

public class MainActivity extends AppCompatActivity {

    private RxBus mRxBus = null;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        removeWorkerFragments();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new MainFragment(), this.toString())
                    .commit();
        }
    }

    // This is better done with a DI Library like Dagger
    public RxBus getRxBusSingleton() {
        if (mRxBus == null) {
            mRxBus = new RxBus();
        }

        return mRxBus;
    }

    private void removeWorkerFragments() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(RotationPersist1WorkerFragment.class.getName());

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }

        fragment = getSupportFragmentManager()
                .findFragmentByTag(RotationPersist2WorkerFragment.class.getName());

        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }
}
