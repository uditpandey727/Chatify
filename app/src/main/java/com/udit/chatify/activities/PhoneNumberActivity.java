package com.udit.chatify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.udit.chatify.databinding.ActivityPhoneNumberBinding;

public class PhoneNumberActivity extends AppCompatActivity {
    ActivityPhoneNumberBinding binding;
    FirebaseAuth auth;
    private ArrayAdapter<String> countryCodeAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        getSupportActionBar().hide();

        // Create an ArrayAdapter using a list of country codes
        String[] countryCodes = {"+91", "+1", "+44", "+61", "+81"}; // Replace with your own country codes
        countryCodeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countryCodes);

        // Set the dropdown layout style
        countryCodeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the adapter to the Spinner
        binding.spinnerCountryCode.setAdapter(countryCodeAdapter);

        binding.phoneBox.requestFocus();

        binding.continueBtn.setOnClickListener(v -> {
            String phoneNumber = binding.phoneBox.getText().toString();
            String selectedCountryCode = binding.spinnerCountryCode.getSelectedItem().toString();

            if (phoneNumber.length() == 10) {
                phoneNumber = selectedCountryCode + phoneNumber;
                Intent intent = new Intent(PhoneNumberActivity.this, OTPActivity.class);
                intent.putExtra("phoneNumber", phoneNumber);
                startActivity(intent);
            } else {
                binding.phoneBox.setError("Enter Correct Phone Number");
            }
        });
    }
}