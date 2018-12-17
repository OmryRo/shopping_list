package app.shoppinglist.wsux.shoppinglist;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import app.shoppinglist.wsux.shoppinglist.firebase.Collaborator;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private static final String TAG = "USER_ADAPTER";

    private ArrayList<Collaborator> usersDataset;

    class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView userName;
        private ImageView userImage;

        private UserViewHolder(LinearLayout userView) {
            super(userView);
            userName = userView.findViewById(R.id.user_layout_name);
            userImage = userView.findViewById(R.id.user_layout_image);
        }
    }

    UserAdapter(ShopList shopList) {
        usersDataset = new ArrayList<>(shopList.getCollaborators().values());
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {

        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_layout, parent, false);
        return new UserViewHolder(view);
    }

    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Collaborator collaborator = usersDataset.get(position);
        holder.userName.setText(collaborator.getName());
        holder.userName.setTextColor(collaborator.getColor());
        holder.userImage.setImageBitmap(collaborator.getPicture());
    }

    @Override
    public int getItemCount() {
        return usersDataset.size();
    }
}