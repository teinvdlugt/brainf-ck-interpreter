package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        recyclerView.setAdapter(adapter);

        // (Try to) load file list
        loadFileList();
        adapter.notifyDataSetChanged();
    }

    /**
     * Loads new data into Adapter but does NOT call notifyDataSetChanged();
     */
    private void loadFileList() {
        List<FileInfo> savedScripts = IOUtils.loadFileList(this);

        if (savedScripts == null)
            Snackbar.make(recyclerView, R.string.files_load_error, Snackbar.LENGTH_LONG).show();

        adapter.setData(savedScripts);
    }

    private ActionMode actionMode = null;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        int deleteMenuItemId = 1;
        int renameMenuItemId = 2;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Add delete action
            menu.add(Menu.NONE, deleteMenuItemId, 101, R.string.delete)
                    .setIcon(R.drawable.ic_delete_white_24dp)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            // Rename action is added in onPrepareActionMode
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (adapter.selectedItems.size() > 1) {
                // Remove rename option
                menu.removeItem(renameMenuItemId);
            } else {
                // Add rename option
                menu.add(Menu.NONE, renameMenuItemId, 100, R.string.rename)
                        .setIcon(R.drawable.ic_edit_white_24dp)
                        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == deleteMenuItemId) {
                // Remove file(s)
                IOUtils.removeFiles(FilesActivity.this, adapter.getFileNamesAtSelectedIndices());
                // Update adapter.data
                loadFileList();
                // NotifyItemRemoved changed in descending order
                Integer[] indices = adapter.selectedItems.toArray(new Integer[0]);
                Arrays.sort(indices);
                for (int i = indices.length - 1; i >= 0; i--) {
                    adapter.notifyItemRemoved(indices[i]);
                }
                adapter.selectedItems.clear();
                actionMode.finish();
            } else if (item.getItemId() == renameMenuItemId) {
                // Rename file
                final int selectedItem = adapter.selectedItems.iterator().next();
                final String oldFilename = adapter.data.get(selectedItem).filename;
                // Show filename dialog
                MainActivity.showEditTextDialog(FilesActivity.this, new MainActivity.EditTextDialogListener() {
                    @Override
                    public void onPositive(String newFileName) {
                        if (IOUtils.rename(FilesActivity.this, oldFilename, newFileName)) {
                            // Successfully renamed
                            loadFileList();
                            adapter.selectedItems.clear();
                            adapter.notifyDataSetChanged();
                        } else {
                            // Unsuccessful
                            Snackbar.make(recyclerView, R.string.file_rename_error, Snackbar.LENGTH_LONG).show();
                            adapter.selectedItems.clear();
                        }
                    }
                }, adapter.data.get(selectedItem).filename, recyclerView);
                actionMode.finish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.selectedItems.clear();
            adapter.notifyDataSetChanged();
            actionMode = null;
        }
    };

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        List<FileInfo> data;
        Set<Integer> selectedItems = new HashSet<>();
        private static final int VIEW_TYPE_NORMAL = 0;
        private static final int VIEW_TYPE_LAST = 1;

        void setData(List<FileInfo> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_NORMAL) {
                View view = LayoutInflater.from(FilesActivity.this).inflate(R.layout.list_item_saved, parent, false);
                return new ViewHolder(view);
            } else {
                View view = LayoutInflater.from(FilesActivity.this).inflate(R.layout.list_item_textview, parent, false);
                return new TextViewViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ViewHolder) {
                ViewHolder holder1 = (ViewHolder) holder;
                holder1.filename = data.get(position).filename;
                holder1.filenameTV.setText(data.get(position).filename);
                holder1.dateTV.setText(SimpleDateFormat.getDateInstance().format(data.get(position).date));
                holder1.position = position;
                holder1.root.setActivated(selectedItems.contains(position));
            } // other viewType does not need updating
        }

        @Override
        public int getItemCount() {
            return data == null ? 1 : data.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return (position == getItemCount() - 1) ? VIEW_TYPE_LAST : VIEW_TYPE_NORMAL;
        }

        List<String> getFileNamesAtSelectedIndices() {
            List<String> result = new ArrayList<>();
            for (int i : selectedItems)
                result.add(data.get(i).filename);
            return result;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView filenameTV, dateTV;
        String filename;
        View root;
        int position;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            filenameTV = itemView.findViewById(R.id.filename_textView);
            dateTV = itemView.findViewById(R.id.date_textView);
            root = itemView.findViewById(R.id.list_item_root);
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionMode != null) {
                        // In selection mode; add or remove this item from selection
                        handleClickInActionMode();
                    } else {
                        // Not in selection mode; load the script
                        String script = IOUtils.loadFile(FilesActivity.this, adapter.data.get(position).filename);
                        if (script != null) {
                            setResult(RESULT_OK, new Intent().putExtra(MainActivity.SCRIPT_EXTRA, script));
                            finish();
                        } else {
                            Snackbar.make(recyclerView, R.string.file_load_error, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    handleClickInActionMode();
                    return true;
                }
            });
        }

        /**
         * Called when root of list item is clicked or long clicked
         */
        private void handleClickInActionMode() {
            if (actionMode == null) {
                // Select this list item
                adapter.selectedItems.add(position);
                root.setActivated(true);
                // Initiate action mode
                actionMode = startSupportActionMode(actionModeCallback);
            } else {
                // Already in selection mode; add or remove this item from selection
                if (adapter.selectedItems.contains(position)) {
                    // Remove this item from selection
                    adapter.selectedItems.remove(position);
                    root.setActivated(false);
                    if (adapter.selectedItems.isEmpty())
                        actionMode.finish();
                    else if (adapter.selectedItems.size() == 1)
                        actionMode.invalidate(); // re-enable 'rename' option
                } else {
                    // Add this item to selection
                    adapter.selectedItems.add(position);
                    root.setActivated(true);
                    if (adapter.selectedItems.size() == 2)
                        actionMode.invalidate(); // Remove 'rename' option
                }
            }
        }
    }

    private class TextViewViewHolder extends RecyclerView.ViewHolder {
        TextViewViewHolder(@NonNull View itemView) {
            super(itemView);
            // Set text to TextView
            ((TextView) itemView.findViewById(R.id.textView))
                    .setText(FilesActivity.this.getString(R.string.no_saved_scripts,
                            IOUtils.theDirectory(FilesActivity.this)));
        }
    }

    static class FileInfo {
        private String filename;
        private Date date;

        FileInfo(String filename, Date date) {
            this.filename = filename;
            this.date = date;
        }

        String getFilename() {
            return filename;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return true;
    }
}
