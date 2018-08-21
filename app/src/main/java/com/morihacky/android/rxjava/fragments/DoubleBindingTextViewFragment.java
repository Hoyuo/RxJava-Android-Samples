package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.morihacky.android.rxjava.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;

import static android.text.TextUtils.isEmpty;

public class DoubleBindingTextViewFragment extends BaseFragment {

    @BindView(R.id.double_binding_num1)
    EditText mEditTextNumber1;

    @BindView(R.id.double_binding_num2)
    EditText mEditTextNumber2;

    @BindView(R.id.double_binding_result)
    TextView mResultTextView;

    Disposable mDisposable;
    PublishProcessor<Float> mResultEmitterSubject;
    private Unbinder mUnbinder;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_double_binding_textview, container, false);
        mUnbinder = ButterKnife.bind(this, layout);

        mResultEmitterSubject = PublishProcessor.create();

        mDisposable = mResultEmitterSubject
                .subscribe(aFloat -> mResultTextView.setText(String.valueOf(aFloat)));

        onNumberChanged();
        mEditTextNumber2.requestFocus();

        return layout;
    }

    @OnTextChanged({R.id.double_binding_num1, R.id.double_binding_num2})
    public void onNumberChanged() {
        float num1 = 0;
        float num2 = 0;

        if (!isEmpty(mEditTextNumber1.getText().toString())) {
            num1 = Float.parseFloat(mEditTextNumber1.getText().toString());
        }

        if (!isEmpty(mEditTextNumber2.getText().toString())) {
            num2 = Float.parseFloat(mEditTextNumber2.getText().toString());
        }

        mResultEmitterSubject.onNext(num1 + num2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposable.dispose();
        mUnbinder.unbind();
    }
}
