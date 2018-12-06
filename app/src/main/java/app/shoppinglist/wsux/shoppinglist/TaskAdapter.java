
package app.shoppinglist.wsux.shoppinglist;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ItemViewHolder> {

    private static final String TAG = TaskAdapter.class.getSimpleName();

    private String[] mDataset;


    public TaskAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @Override
    public TaskAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        TextView view = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_list_item, parent, false);

        ItemViewHolder viewHolder = new ItemViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.listItemNumberView.setText(mDataset[position]);
    }


    @Override
    public int getItemCount() {
        Log.d(TAG, "#" );
        return mDataset.length;
    }


    class ItemViewHolder extends RecyclerView.ViewHolder {

        // Will display the position in the list, ie 0 through getItemCount() - 1
        public TextView listItemNumberView;

        public ItemViewHolder(TextView itemView) {
            super(itemView);

            listItemNumberView = itemView;
        }


        void bind(int listIndex) {
            listItemNumberView.setText(String.valueOf(listIndex));
        }
    }
}
