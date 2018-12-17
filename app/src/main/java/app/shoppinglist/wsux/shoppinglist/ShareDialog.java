package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.app.Dialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.support.v7.app.AlertDialog;

import java.util.LinkedHashMap;

import app.shoppinglist.wsux.shoppinglist.firebase.Collaborator;
import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;

public class ShareDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = "SHARE_DIALOG";

    private ShopList shopList;
    private FireBaseManager fireBaseManager;

    public ShareDialog(Activity context, final ShopList shopList,
                       final FireBaseManager fireBaseManager) {
        super(context);
        this.fireBaseManager = fireBaseManager;
        this.shopList = shopList;

        setContentView(R.layout.share_layout);
        RecyclerView shareRecyclerView = findViewById(R.id.share_layout_recycle_view);
        LinearLayoutManager shareLayoutManager = new LinearLayoutManager(context);
        shareRecyclerView.setLayoutManager(shareLayoutManager);

        UserAdapter userAdapter = new UserAdapter(shopList);
        shareRecyclerView.setAdapter(userAdapter);

        defineSharedDialogButtons();
    }

    private void defineSharedDialogButtons() {
        findViewById(R.id.share_layout_share_button).setOnClickListener(this);
        findViewById(R.id.share_layout_dismiss).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.share_layout_dismiss:
                dismiss();
                break;
            case R.id.share_layout_share_button:
                fireBaseManager.getShareHandler().performShareList(shopList);
                break;
        }
    }
}


