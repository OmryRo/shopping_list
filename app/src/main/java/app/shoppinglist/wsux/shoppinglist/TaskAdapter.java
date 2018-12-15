
package app.shoppinglist.wsux.shoppinglist;


import android.app.Activity;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder>
        implements BaseCollectionItem.OnChildChangeListener {

    private static final String TAG = "TASK_ADAPTER";

    private ShopList currentShopList;
    private ArrayList<ShopTask> shopTasks;
    private FireBaseManager fireBaseManager;

    public TaskAdapter(FireBaseManager fireBaseManager) {
        shopTasks = new ArrayList<>();
        this.fireBaseManager = fireBaseManager;
    }

    public void setList(ShopList shopList) {

        if (shopList == currentShopList) {
            return;
        }
       
        if (currentShopList != null) {
            currentShopList.removeAllListeners();
        }

        currentShopList = shopList;
        currentShopList.setOnChildChangeListener(this);
    }

    public void resetDataset() {
        shopTasks.clear();
        if (currentShopList != null) {
            shopTasks.addAll(currentShopList.getTasks().values());
            notifyDataSetChanged();
        }
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

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
    public void onChildChange() {
        resetDataset();
    }

    @Override
    public int getItemCount() {
        return shopTasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder
            implements BaseCollectionItem.OnChangeListener,
            CompoundButton.OnCheckedChangeListener, BaseCollectionItem.OnMediaDownload,
                View.OnLongClickListener {

        private TextView taskNameTv;
        private TextView taskNoteTv;
        private CheckBox statusCb;
        private ImageView thumbnailIv;
        private ShopTask task;

        private TaskViewHolder(LinearLayout itemView) {
            super(itemView);
            taskNameTv = itemView.findViewById(R.id.task_name_tv);
            taskNoteTv = itemView.findViewById(R.id.task_note_tv);
            statusCb = itemView.findViewById(R.id.task_status);
            thumbnailIv = itemView.findViewById(R.id.task_thumbnail);
            itemView.setOnLongClickListener(this);
        }

        private void updateView(int position) {

            task = shopTasks.get(position);

            if (task == null) {
                return;
            }

            task.setOnChangeListener(this);
            task.setOnMediaDownload(this);
        }

        @Override
        public void onChange() {

            if (task == null) {
                return;
            }

            if (taskNameTv != null) {
                taskNameTv.setText(task.getTitle());
                Log.d(TAG, "taskNameTv: " + task.getTitle());
            }

            if (taskNoteTv != null) {
                taskNoteTv.setText(task.getDescription());
                Log.d(TAG, "taskNoteTv: " + task.getDescription());
            }

            if (statusCb != null) {
                statusCb.setOnCheckedChangeListener(null);
                statusCb.setChecked(task.getState() == ShopTask.SHOP_TASK_DONE);
                statusCb.setOnCheckedChangeListener(this);
            }

            onMediaDownload();
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if (task != null) {
                task.setState(checked ? ShopTask.SHOP_TASK_DONE : ShopTask.SHOP_TASK_NOT_DONE);
            }
        }

        @Override

        public void onMediaDownload() {
            if (thumbnailIv != null && task.hasPicture()) {
                Bitmap thumbnail = task.getPicture();

                if (thumbnail == null) {
                    thumbnailIv.setImageResource(R.mipmap.ic_launcher);
                } else {
                    thumbnailIv.setImageBitmap(thumbnail);
                }
            }
        }

        public boolean onLongClick(View view) {
            new TaskEditDialog(view.getContext(), task, fireBaseManager).show();
            return false;
        }
    }
}
