package com.udit.chatify.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.udit.chatify.databinding.ActivityOtpactivityBinding;

import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {
    ActivityOtpactivityBinding binding;
    FirebaseAuth auth;

    String verificationId;

    ProgressDialog dialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP.....");
        dialog.setCancelable(false);
        dialog.show();

        auth = FirebaseAuth.getInstance();

        getSupportActionBar().hide();
        
        String phoneNumber = getIntent().getStringExtra("phoneNumber");

        binding.phoneLabel.setText("Verify " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(OTPActivity.this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {}

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {}

                    @Override
                    public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verifyId, forceResendingToken);
                        dialog.dismiss();
                        verificationId = verifyId;

                        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        binding.otpView.requestFocus();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);

        binding.otpView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length()==6){
                    String otp = s.toString();
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
                    // ...
                    auth.signInWithCredential(credential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Check if the user is new or existing
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                            if (isNewUser) {
                                // User is new, navigate to SetupProfileActivity
                                Intent intent = new Intent(OTPActivity.this, SetupProfileActivity.class);
                                startActivity(intent);
                                finishAffinity();
                            } else {
                                // User is existing, navigate to MainActivity
                                Intent intent = new Intent(OTPActivity.this, MainActivity.class);
                                startActivity(intent);
                                finishAffinity();
                            }
                        } else {
                            Toast.makeText(OTPActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    });

// ...

                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}