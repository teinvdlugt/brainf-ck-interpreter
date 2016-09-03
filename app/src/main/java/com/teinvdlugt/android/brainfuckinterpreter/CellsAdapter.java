package com.teinvdlugt.android.brainfuckinterpreter;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class CellsAdapter extends RecyclerView.Adapter<CellsAdapter.ViewHolder> {
    private Activity context;
    private byte[] data;
    private int pointedCellPosition = 0;
    private Cell pointedCell;
    private LinearLayoutManager layoutManager;
    private Runnable notifyItemChangedRunnable = new Runnable() {
        @Override
        public void run() {
            notifyItemChanged(pointedCellPosition);
        }
    };

    public CellsAdapter(Activity context, LinearLayoutManager layoutManager) {
        this.context = context;
        this.layoutManager = layoutManager;
        this.data = new byte[CellsLayout.MAX_CELL_AMOUNT];
    }

    public void incrementPointedCellValue() {
        data[pointedCellPosition]++;
        final int pointedCellPositionFinal = pointedCellPosition;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(pointedCellPositionFinal);
            }
        });
    }

    public void decrementPointedCellValue() {
        data[pointedCellPosition]--;
        final int pointedCellPositionFinal = pointedCellPosition;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(pointedCellPositionFinal);
            }
        });
    }

    public void setPointedCellValue(byte value) {
        data[pointedCellPosition] = value;
        final int pointedCellPositionFinal = pointedCellPosition;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyItemChanged(pointedCellPositionFinal);
            }
        });
    }

    public byte getPointedCellValue() {
        return data[pointedCellPosition];
    }

    public void clearMemory() {
        this.data = new byte[CellsLayout.MAX_CELL_AMOUNT];
        pointedCellPosition = 0;
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layoutManager.scrollToPosition(0);
                notifyDataSetChanged();
            }
        });
    }

    public void movePointer(int position) {
        this.pointedCellPosition = position;

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pointedCell != null) pointedCell.setPointed(false);
                // TODO Try: if (layoutManager.findFirstVisibleItemPosition() <= position && layoutManager.findLastVisibleItemPosition() >= position) {
                notifyItemChanged(pointedCellPosition);
                // }
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(new Cell(context));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.cell.setText(Byte.toString(data[position]));
        if (position == pointedCellPosition) {
            holder.cell.setPointed(true);
            pointedCell = holder.cell;
        } else {
            holder.cell.setPointed(false);
        }
    }

    @Override
    public int getItemCount() {
        return data.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        Cell cell;

        public ViewHolder(View itemView) {
            super(itemView);
            cell = (Cell) itemView;
        }
    }

}
