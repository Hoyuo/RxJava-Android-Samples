package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.widget.RxTextView;
import com.morihacky.android.rxjava.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subscribers.DisposableSubscriber;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static android.util.Patterns.EMAIL_ADDRESS;

public class FormValidationCombineLatestFragment extends BaseFragment {

    @BindView(R.id.btn_demo_form_valid)
    TextView mBtnValidIndicator;

    @BindView(R.id.demo_combl_email)
    EditText mEmail;

    @BindView(R.id.demo_combl_password)
    EditText mPassword;

    @BindView(R.id.demo_combl_num)
    EditText mNumber;

    private DisposableSubscriber<Boolean> mDisposableObserver = null;
    private Flowable<CharSequence> mEmailChangeObservable;
    private Flowable<CharSequence> mNumberChangeObservable;
    private Flowable<CharSequence> mPasswordChangeObservable;
    private Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_form_validation_comb_latest,
                container,
                false);
        mUnbinder = ButterKnife.bind(this, layout);

        mEmailChangeObservable =
                RxTextView.textChanges(mEmail).skip(1).toFlowable(BackpressureStrategy.LATEST);
        mPasswordChangeObservable =
                RxTextView.textChanges(mPassword).skip(1).toFlowable(BackpressureStrategy.LATEST);
        mNumberChangeObservable =
                RxTextView.textChanges(mNumber).skip(1).toFlowable(BackpressureStrategy.LATEST);

        combineLatestEvents();

        return layout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
        mDisposableObserver.dispose();
    }

    private void combineLatestEvents() {

        mDisposableObserver = new DisposableSubscriber<Boolean>() {
            @Override
            public void onNext(Boolean formValid) {
                if (formValid) {
                    mBtnValidIndicator.setBackgroundColor(
                            ContextCompat.getColor(getContext(), R.color.blue));
                } else {
                    mBtnValidIndicator.setBackgroundColor(
                            ContextCompat.getColor(getContext(), R.color.gray));
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "there was an error");
            }

            @Override
            public void onComplete() {
                Timber.d("completed");
            }
        };

        Flowable.combineLatest(
                mEmailChangeObservable,
                mPasswordChangeObservable,
                mNumberChangeObservable,
                (newEmail, newPassword, newNumber) -> {
                    boolean emailValid = !isEmpty(newEmail) && EMAIL_ADDRESS.matcher(newEmail)
                            .matches();
                    if (!emailValid) {
                        mEmail.setError("Invalid Email!");
                    }

                    boolean passValid = !isEmpty(newPassword) && newPassword.length() > 8;
                    if (!passValid) {
                        mPassword.setError("Invalid Password!");
                    }

                    boolean numValid = !isEmpty(newNumber);
                    if (numValid) {
                        int num = Integer.parseInt(newNumber.toString());
                        numValid = num > 0 && num <= 100;
                    }
                    if (!numValid) {
                        mNumber.setError("Invalid Number!");
                    }

                    return emailValid && passValid && numValid;
                })
                .subscribe(mDisposableObserver);
    }
}
