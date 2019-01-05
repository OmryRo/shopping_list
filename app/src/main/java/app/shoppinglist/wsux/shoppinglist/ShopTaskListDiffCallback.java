package app.shoppinglist.wsux.shoppinglist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.ArrayList;

import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;

public class ShopTaskListDiffCallback extends DiffUtil.Callback {
    private ArrayList<ShopTask> oldVersionList;
    private ArrayList<ShopTask> newVersionList;

    ShopTaskListDiffCallback(ArrayList<ShopTask> oldList,ArrayList<ShopTask> newList){
        this.oldVersionList=oldList;
        this.newVersionList=newList;
    }
    @Override
    public int getOldListSize() {
        return oldVersionList != null ? oldVersionList.size(): 0;
    }

    @Override
    public int getNewListSize() {
        return newVersionList != null ? newVersionList.size(): 0;
    }

    @Override
    public boolean areItemsTheSame(int oldShopTaskPosition, int newShopTaskPosition) {
        ShopTask oldShopTask = oldVersionList.get(oldShopTaskPosition);
        ShopTask newShopTask = newVersionList.get(newShopTaskPosition);
        return oldShopTask.getTaskId().equals(newShopTask.getTaskId());

    }

    @Override
    public boolean areContentsTheSame(int oldShopTaskPosition, int newShopTaskPosition) {
        ShopTask oldShopTask = oldVersionList.get(oldShopTaskPosition);
        ShopTask newShopTask = newVersionList.get(newShopTaskPosition);

        return oldShopTask.getTitle().equals(newShopTask.getTitle()) &&
                oldShopTask.getDescription().equals(newShopTask.getDescription());
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldShopTaskPosition, int newShopTaskPosition) {
        ShopTask oldShopTask = oldVersionList.get(oldShopTaskPosition);
        ShopTask newShopTask = newVersionList.get(newShopTaskPosition);
        Bundle diff = new Bundle();
        if(!newShopTask.getTitle().equals(oldShopTask.getTitle())){
            diff.putString("Title", newShopTask.getTitle());
        }
        if(!newShopTask.getDescription().equals(oldShopTask.getDescription())){
            diff.putString("Description", newShopTask.getDescription());
        }
        if(newShopTask.getState() != oldShopTask.getState()){
            diff.putLong("State", newShopTask.getState());
        }
        if(diff.size() == 0) {
            return null;
        }

        return diff;
    }
}
