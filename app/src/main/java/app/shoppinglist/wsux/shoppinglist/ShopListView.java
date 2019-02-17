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
    private MainActivity context;
    private TaskAdapter adapter;
    private ShopList currentShopList;
    private FireBaseManager fireBaseManager;

    // views
    private EditText addTextEditText;
    private View addTaskContainer;
    private RecyclerView recyclerView;
    private View messageEmptyList;
    private View messageNoListToShow;
    private Toolbar topToolbar;

    ShopListView(MainActivity context, Toolbar topToolbar, FireBaseManager fireBaseManager) {
        this.context = context;
        this.topToolbar = topToolbar;
        this.fireBaseManager = fireBaseManager;

        setRecyclerView();
        setMessageViews();
        setBottomToolbar();
        toggleTaskViewing();
    }

    private void setRecyclerView() {
        recyclerView = context.findViewById(R.id.shopping_list_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);

        adapter = new TaskAdapter(fireBaseManager);
        recyclerView.setAdapter(adapter);
    }

    private void setMessageViews() {
        messageEmptyList = context.findViewById(R.id.message_empty_list);
        messageEmptyList.setOnClickListener(this);

        messageNoListToShow = context.findViewById(R.id.message_no_lists);
        messageNoListToShow.setOnClickListener(this);
    }

    private void setBottomToolbar() {
        addTaskContainer = context.findViewById(R.id.bar_add_task_container);
        addTaskContainer.setVisibility(View.GONE);
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

    private void toggleTaskViewing() {

        int visibleIfExists = (currentShopList != null ? View.VISIBLE : View.GONE);
        int visibleIfNotExists = (currentShopList == null ? View.VISIBLE : View.GONE);
        int visibleIfEmpty = (
                currentShopList != null && currentShopList.getTasks().isEmpty() ?
                View.VISIBLE : View.GONE);

        addTaskContainer.setVisibility(visibleIfExists);
        recyclerView.setVisibility(visibleIfEmpty);
        messageNoListToShow.setVisibility(visibleIfNotExists);
        messageEmptyList.setVisibility(visibleIfEmpty);

        if (currentShopList != null) {
            topToolbar.setTitle(currentShopList.getTitle());
        } else {
            topToolbar.setTitle(R.string.app_name);
        }
    }

    public void setShopList(ShopList shopList) {
        if (currentShopList == shopList) {
            return;
        }

        if (currentShopList != null) {
            currentShopList.removeAllListeners();
        }

        currentShopList = shopList;
        adapter.setList(currentShopList);

        if (shopList != null) {
            shopList.setOnChangeListener(this);
            shopList.setOnChildChangeListener(this);
        }

        toggleTaskViewing();
    }

    private void onAddTaskButtonClick() {
        if (currentShopList == null) {
            return;
        }
        setTaskTitle();
    }

    private void onMessageNoListClick() {
        context.selectList(null);
    }

    private void onMessageEmptyListClick() {
        showKeyboard();
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
        adapter.onChildChange();
        toggleTaskViewing();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bar_add_task_button:
                onAddTaskButtonClick();
                break;
            case R.id.message_no_lists:
                onMessageNoListClick();
                break;
            case R.id.message_empty_list:
                onMessageEmptyListClick();
                break;
        }
    }

    @Override
    public void onChange() {
        toggleTaskViewing();
    }
}
