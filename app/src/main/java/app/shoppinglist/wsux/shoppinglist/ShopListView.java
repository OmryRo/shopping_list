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
    private EditText addTextEditText;
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
        final View addTextButton = context.findViewById(R.id.bar_add_task_button);
        addTextButton.setOnClickListener(this);
    }

    private void setAddTextEditTextView() {
        addTextEditText = context.findViewById(R.id.bar_add_task_edit_text);
        addTextEditText.setOnKeyListener(new View.OnKeyListener() {
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
        String taskTitle = addTextEditText.getText().toString();

        if (taskTitle.length() == 0) {
            showKeyboard();
            return;
        }

        addTextEditText.setText("");
        currentShopList.addNewTask(taskTitle, "");
    }

    private void showKeyboard(){
        addTextEditText.requestFocus();

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(addTextEditText, InputMethodManager.SHOW_FORCED);
    }

    @Override
    public void onChildChange() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bar_add_task_button:
                onAddTaskButtonClick();
                break;
        }
    }

    @Override
    public void onChange() {
        topToolbar.setTitle(currentShopList.getTitle());
    }
}
