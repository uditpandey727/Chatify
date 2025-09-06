package com.udit.chatify.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityAiListBinding;

import java.util.ArrayList;
import java.util.List;

public class AiList extends AppCompatActivity {
    ActivityAiListBinding binding;
    private AiWebsiteAdapter aiWebsiteAdapter;
    private List<AiWebsite> aiWebsites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // AI Websites Data
        aiWebsites = new ArrayList<>();
        aiWebsites.add(new AiWebsite("AI Website 1", "Description 1", R.drawable.avatar));
        aiWebsites.add(new AiWebsite("AI Website 2", "Description 2", R.drawable.avatar));
        aiWebsites.add(new AiWebsite("AI Website 3", "Description 3", R.drawable.avatar));
        // Add more AI websites as needed

        // AI Websites Grid
        binding.aiWebsiteList.setHasFixedSize(true);
        binding.aiWebsiteList.setLayoutManager(new GridLayoutManager(this, 3));
        aiWebsiteAdapter = new AiWebsiteAdapter(aiWebsites);
        binding.aiWebsiteList.setAdapter(aiWebsiteAdapter);
    }

}
// Adapter for the AI Websites Grid
class AiWebsiteAdapter extends RecyclerView.Adapter<AiWebsiteAdapter.ViewHolder> {

    private List<AiWebsite> aiWebsites;

    public AiWebsiteAdapter(List<AiWebsite> aiWebsites) {
        this.aiWebsites = aiWebsites;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ai_website_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiWebsite aiWebsite = aiWebsites.get(position);

        holder.websiteImage.setImageResource(aiWebsite.getImageResId());
        holder.websiteTitle.setText(aiWebsite.getTitle());
        holder.websiteDescription.setText(aiWebsite.getDescription());

        holder.recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform action when the card view is clicked
                // Add your code here
            }
        });
    }

    @Override
    public int getItemCount() {
        return aiWebsites.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView websiteImage;
        TextView websiteTitle;
        TextView websiteDescription;
        RecyclerView recyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            websiteImage = itemView.findViewById(R.id.websiteImage);
            websiteTitle = itemView.findViewById(R.id.websiteTitle);
            websiteDescription = itemView.findViewById(R.id.websiteDescription);
            recyclerView = itemView.findViewById(R.id.aiWebsiteList);
        }
    }
}

// Model class for AI Website
class AiWebsite {
    private String title;
    private String description;
    private int imageResId;

    public AiWebsite(String title, String description, int imageResId) {
        this.title = title;
        this.description = description;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }
}