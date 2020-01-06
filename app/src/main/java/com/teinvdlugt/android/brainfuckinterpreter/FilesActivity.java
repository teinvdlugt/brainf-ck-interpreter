package com.teinvdlugt.android.brainfuckinterpreter;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FilesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Adapter adapter = new Adapter(IOUtils.loadFileList(this));
        recyclerView.setAdapter(adapter);
    }

    public static class FileInfo {
        private String filename;
        private Date date;

        public FileInfo(String filename, Date date) {
            this.filename = filename;
            this.date = date;
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        List<FileInfo> data;

        Adapter(List<FileInfo> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(FilesActivity.this).inflate(R.layout.list_item_saved, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.filename = data.get(position).filename;
            holder.filenameTV.setText(data.get(position).filename);
            holder.dateTV.setText(SimpleDateFormat.getDateInstance().format(data.get(position).date));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView filenameTV, dateTV;
        String filename;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            filenameTV = itemView.findViewById(R.id.filename_textView);
            dateTV = itemView.findViewById(R.id.date_textView);
            itemView.findViewById(R.id.list_item_root).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // load(filename);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return true;
    }
}
