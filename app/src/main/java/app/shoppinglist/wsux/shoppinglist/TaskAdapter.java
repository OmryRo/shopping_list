
package app.shoppinglist.wsux.shoppinglist;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final String TAG = TaskAdapter.class.getSimpleName();

    private String[] mDataset;


    public TaskAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @Override
    public TaskAdapter.TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_list_item, parent, false);

        TaskViewHolder viewHolder = new TaskViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        holder.updateView(position);
    }


    @Override
    public int getItemCount() {
        return 100;
    }


    class TaskViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout listItemNumberView;
        private TextView taskNameTv;
        private TextView taskNoteTv;
        private CheckBox statusCb;
        private ImageView thumbnailIv;

        private TaskViewHolder(LinearLayout itemView) {
            super(itemView);
            listItemNumberView = itemView;
            taskNameTv = itemView.findViewById(R.id.task_name_tv);
            taskNoteTv = itemView.findViewById(R.id.task_note_tv);
            statusCb = itemView.findViewById(R.id.task_status);
            thumbnailIv = itemView.findViewById(R.id.task_thumbnail);
        }

        private void updateView(int position) {
            taskNameTv.setText(String.valueOf(position));
        }

    }
}
