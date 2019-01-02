package app.shoppinglist.wsux.shoppinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import app.shoppinglist.wsux.shoppinglist.firebase.Collaborator;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private static final String TAG = "USER_ADAPTER";

    private ArrayList<Collaborator> usersDataset;
    private ShopList shopList;
    private Context context;

    class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView userName;
        private TextView userStatus;
        private ImageView userImage;
        private View owner;
        private View remove;

        private UserViewHolder(LinearLayout userView) {
            super(userView);
            userName = userView.findViewById(R.id.user_layout_name);
            userStatus = userView.findViewById(R.id.user_layout_status);
            userImage = userView.findViewById(R.id.user_layout_image);
            owner = userView.findViewById(R.id.user_layout_owner);
            remove = userView.findViewById(R.id.user_layout_remove);
        }
    }

    UserAdapter(Context context, ShopList shopList) {
        this.shopList = shopList;
        usersDataset = new ArrayList<>(getOrderedCollaborators(shopList));
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_layout, parent, false);

        return new UserViewHolder(view);
    }

    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Collaborator collaborator = usersDataset.get(position);
        holder.userName.setText(collaborator.getName());
        holder.userName.setTextColor(collaborator.getColor());
        holder.userImage.setImageBitmap(collaborator.getPicture());
        holder.userStatus.setText(collaborator.getMessage());
        setOwnerRemoveIcon(holder, collaborator);
    }

    private void setOwnerRemoveIcon(UserViewHolder holder, final Collaborator collaborator) {
        boolean isAuthor = shopList.getAuthor().equals(collaborator.getUserId());
        holder.owner.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        holder.remove.setVisibility(!isAuthor && shopList.isAuthor() ? View.VISIBLE : View.GONE);
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCollaborator(collaborator);
            }
        });
    }

    private void removeCollaborator(final Collaborator collaborator) {

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.verify_before_remove_collaborator, collaborator.getName()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        shopList.removeCollaborators(collaborator.getUserId());
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing...
                    }
                }).create().show();
    }

    @Override
    public int getItemCount() {
        return usersDataset.size();
    }

    private ArrayList<Collaborator> getOrderedCollaborators(final ShopList shopList) {
        ArrayList<Collaborator> collaborators = new ArrayList<>(shopList.getCollaborators().values());
        Collections.sort(collaborators, new Comparator<Collaborator>() {
            @Override
            public int compare(Collaborator o1, Collaborator o2) {
                if (o1.getUserId().equals(shopList.getAuthor())) {
                    return -1;
                } else if (o2.getUserId().equals(shopList.getAuthor())) {
                    return 1;
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        });
        return collaborators;
    }
}