package com.udit.chatify.activities;

import static androidx.fragment.app.FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

import com.udit.chatify.databinding.ActivityInsightBinding;
import com.udit.chatify.fragments.DailyFragment;
import com.udit.chatify.fragments.MonthlyFragment;
import com.udit.chatify.fragments.TrendsFragment;
import com.udit.chatify.fragments.WeeklyFragment;

import java.util.ArrayList;
import java.util.List;

public class InsightActivity extends AppCompatActivity {
    ActivityInsightBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInsightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsightPagerAdapter pagerAdapter = new InsightPagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new DailyFragment(), "Daily");
        pagerAdapter.addFragment(new WeeklyFragment(), "Weekly");
        pagerAdapter.addFragment(new MonthlyFragment(), "Monthly");
        pagerAdapter.addFragment(new TrendsFragment(), "Trends");

        binding.viewPager.setAdapter(pagerAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }
}

class InsightPagerAdapter extends FragmentPagerAdapter {
    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<String> fragmentTitleList = new ArrayList<>();

    public InsightPagerAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    public void addFragment(Fragment fragment, String title) {
        fragmentList.add(fragment);
        fragmentTitleList.add(title);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }
}
