package com.udit.chatify.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.databinding.FragmentInsightDailyBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

public class DailyFragment extends Fragment {
    private FragmentInsightDailyBinding binding;
    private String uid;
    private SimpleDateFormat dateFormat;

    PieChart pieChart;

    public DailyFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using data binding
        binding = FragmentInsightDailyBinding.inflate(inflater, container, false);
        uid = FirebaseAuth.getInstance().getUid();
        SimpleDateFormat dateFormat = new SimpleDateFormat("E, d MMM", Locale.ENGLISH);
        Calendar today = Calendar.getInstance();
        Date date = today.getTime();
        String selectedDateStr = dateFormat.format(date);
        pieChart = binding.pieChart;
        setupPieChart(pieChart);
        binding.dateView.setText(selectedDateStr);

        FirebaseDatabase.getInstance().getReference().child("studyHub")
                .child(uid).child(selectedDateStr)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        updateData(snapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        // Set up CalendarView listener
        binding.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                Date date = selectedDate.getTime();
                String selectedDateStr = dateFormat.format(date);
                binding.dateView.setText(selectedDateStr);
                FirebaseDatabase.getInstance().getReference().child("studyHub").child(uid).child(selectedDateStr)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                updateData(snapshot);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle the database error
                            }
                        });
            }
        });

        return binding.getRoot();
    }
    private void setupPieChart(PieChart pieChart) {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(true);
        pieChart.getDescription().setTextColor(Color.WHITE);
        pieChart.getDescription().setTextSize(18f);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setTextSize(18f);
    }

    private void loadPieChartData(PieChart pieChart, List<PieEntry> entries, String Description) {
        PieDataSet dataSet = new PieDataSet(entries, "Study Time");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void updateData(DataSnapshot snapshot){
        if (snapshot.exists()) {
            binding.pieChart.setVisibility(View.VISIBLE);
            binding.studyData.setVisibility(View.VISIBLE);
            binding.dataNotAvailable.setVisibility(View.GONE);
            float phyHr = snapshot.child("phyHr").getValue(Float.class);
            float chemHr = snapshot.child("chemHr").getValue(Float.class);
            float mathHr = snapshot.child("mathHr").getValue(Float.class);
            float classHr = snapshot.child("mathHr").getValue(Float.class);
            float totalTime = phyHr + chemHr + mathHr +classHr;
            float score =  totalTime -12;
            List<PieEntry> entries = new ArrayList<>();
            // Add your data entries here
            entries.add(new PieEntry(phyHr, "Physics"));
            entries.add(new PieEntry(chemHr, "Chemistry"));
            entries.add(new PieEntry(mathHr, "Math"));
            entries.add(new PieEntry(classHr, "Classes"));

            loadPieChartData(pieChart,entries,"");
            binding.score.setText(String.valueOf(score));
            binding.totalTime.setText(String.valueOf(totalTime) +"Hr");
        } else {
            binding.pieChart.setVisibility(View.GONE);
            binding.studyData.setVisibility(View.GONE);
            binding.dataNotAvailable.setVisibility(View.VISIBLE);
        }
    }

}
