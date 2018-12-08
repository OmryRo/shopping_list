package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;

public class ShopListView implements View.OnClickListener, BaseCollectionItem.OnChildChangeListener {

    private Activity context;
    private TaskAdapter adapter;
    private RecyclerView recyclerView;
    private Toolbar topToolbar;
    private View addTaskContainer;
    private ShopList currentShopList;

    // views
    private EditText addTextEt;

    ShopListView(Activity context, Toolbar topToolbar) {
        this.context = context;
        this.topToolbar = topToolbar;

        setMainView();
        setBottomToolbar();
    }

    private void setMainView() {
        recyclerView =  context.findViewById(R.id.shopping_list_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setBottomToolbar() {
        addTaskContainer = context.findViewById(R.id.bar_add_task_container);
        toggleToolbar();


        final View addTextBt = context.findViewById(R.id.bar_add_task_bt);
        addTextBt.setOnClickListener(this);
        addTextEt = context.findViewById(R.id.bar_add_task_et);
        addTextEt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    onAddTaskButtonClick();
                    return true;
                }
                return false;
            }
        });

    }

    private void toggleToolbar() {
        addTaskContainer.setVisibility(currentShopList != null ? View.VISIBLE : View.GONE);
    }

    public void setShopList(ShopList shopList) {
        if (currentShopList == shopList) {
            return;
        } else if (currentShopList != null) {
            currentShopList.removeAllListeners();
        }

        currentShopList = shopList;
        toggleToolbar();
        topToolbar.setTitle(shopList.getTitle());

        adapter.setList(currentShopList);

    }

    private void onAddTaskButtonClick() {
        if (currentShopList == null) {
            return;
        }

        String taskTitle = addTextEt.getText().toString();

        if (taskTitle.length() == 0) {
            return;
        }

        addTextEt.setText("");
        currentShopList.addNewTask(taskTitle, "");
    }

    public void updateTaskList() {

    }

    @Override
    public void onChildChange() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bar_add_task_bt:
                onAddTaskButtonClick();
                break;
        }
    }
}
