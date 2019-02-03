package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;

public class ShopListView implements View.OnClickListener, BaseCollectionItem.OnChildChangeListener,
        BaseCollectionItem.OnChangeListener {
    private Activity context;
    private TaskAdapter adapter;
    private ShopList currentShopList;
    private FireBaseManager fireBaseManager;

    // views
    private EditText addTextEt;
    private View addTaskContainer;
    private RecyclerView recyclerView;
    private Toolbar topToolbar;

    ShopListView(Activity context, Toolbar topToolbar, FireBaseManager fireBaseManager) {
        this.context = context;
        this.topToolbar = topToolbar;
        this.fireBaseManager = fireBaseManager;

        setMainView();
        setBottomToolbar();

    }

    private void setMainView() {
        recyclerView = context.findViewById(R.id.shopping_list_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new TaskAdapter(fireBaseManager);
        recyclerView.setAdapter(adapter);
    }

    private void setBottomToolbar() {
        addTaskContainer = context.findViewById(R.id.bar_add_task_container);
        toggleToolbar();
        setAddTextButtonView();
        setAddTextEditTextView();
    }

    private void setAddTextButtonView() {
        final View addTextBt = context.findViewById(R.id.bar_add_task_bt);
        addTextBt.setOnClickListener(this);
    }

    private void setAddTextEditTextView() {
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
        }

        if (currentShopList != null) {
            currentShopList.removeAllListeners();
        }

        currentShopList = shopList;
        toggleToolbar();
        shopList.setOnChangeListener(this);

        adapter.setList(currentShopList);
    }

    private void onAddTaskButtonClick() {
        if (currentShopList == null) {
            return;
        }
        setTaskTitle();
    }

    private void setTaskTitle() {
        String taskTitle = addTextEt.getText().toString();

        if (taskTitle.length() == 0) {
            showKeyboard();
            return;
        }

        addTextEt.setText("");
        currentShopList.addNewTask(taskTitle, "");
    }

    private void showKeyboard(){
        addTextEt.requestFocus();

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(addTextEt, InputMethodManager.SHOW_FORCED);
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

    @Override
    public void onChange() {
        topToolbar.setTitle(currentShopList.getTitle());
    }
}
