package app.shoppinglist.wsux.shoppinglist;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.UserInfo;

public class MainActivity extends AppCompatActivity
        implements FireBaseManager.FireBaseEventsInterface,
        View.OnClickListener, MainDrawer.MainDrawerInterface, View.OnLongClickListener {

    private static final String TAG = "MAIN_ACTIVITY";

    // objects
    private FireBaseManager fireBaseManager;
    private MainDrawer mainDrawer;
    private ShopListView shopListView;
    private UserInfo userInfo;
    private ShopList currentShopList;
    private LoginScreen loginScreen;
    private EventReporter eventReporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fireBaseManager = new FireBaseManager(this, this);
        fireBaseManager.onCreate();

        setContentView(R.layout.activity_main);
        setLoginScreen();

        eventReporter = new EventReporter(this);

        initiateTopToolBar();
    }

    private void initiateTopToolBar() {
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        shopListView = new ShopListView(this, topToolBar, fireBaseManager);
        mainDrawer = new MainDrawer(this, topToolBar, this);

        findViewById(R.id.drawer_sign_out).setOnClickListener(this);
        topToolBar.setOnLongClickListener(this);
    }

    private void setLoginScreen() {
        loginScreen = (LoginScreen) getSupportFragmentManager()
                .findFragmentById(R.id.login_screen_fragment);
        loginScreen.setFirebaseManager(fireBaseManager);
    }

    private void hideLoginScreen() {
        loginScreen.hide();
    }

    private void showLoginScreen() {
        loginScreen.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fireBaseManager.onStart(this);
    }

    @Override
    public void onBackPressed() {
        if (!mainDrawer.closeDrawer()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_change_list_name:
                renameListPressed();
                break;
            case R.id.action_quit_list:
                onQuitListClicked();
                break;
            case R.id.navigate_share:
                showShared();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEventOccurred(int what, Object data, Exception e) {
        eventReporter.onEventOccurred(what, data, e);
        switch (what) {
            case FireBaseManager.ON_SIGN_IN:
                onLogin((UserInfo) data);
                break;

            case FireBaseManager.ON_SIGN_OUT:
            case FireBaseManager.ON_SIGN_ERR:
                onLogout();
                break;
            case FireBaseManager.ON_LAST_LIST_DOWNLOADED:
                onLastListDownloaded((ShopList) data);
                break;
            case FireBaseManager.ON_SHARE_LIST_FOUND:
                onShareListFound((ShopList) data);
                break;
        }

        Log.d("FIREBASE_EVENTS",
                String.format(
                        "%s - %s - %s",
                        what,
                        data != null ? data.toString() : "null",
                        e != null ? e.getMessage() : "null"
                )
        );
    }

    private void onLogin(UserInfo userInfo) {
        this.userInfo = userInfo;
        mainDrawer.setUserInfo(userInfo);
        hideLoginScreen();
        mainDrawer.openAndLockDrawer();

    }

    private void onLogout() {
        userInfo = null;
        mainDrawer.setUserInfo(userInfo);
        mainDrawer.closeDrawer();
        showLoginScreen();
    }

    private void onLastListDownloaded(ShopList shopList) {
        mainDrawer.setSelectedList(shopList);
        selectList(shopList);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fireBaseManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        fireBaseManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.drawer_sign_out:
                fireBaseManager.getLoginManager().requestLogout();
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.toolbar:
                renameListPressed();
                return true;
        }
        return false;
    }

    @Override
    public void renameListPressed() {

        EditTitlePopup alert = new EditTitlePopup(
                this, R.string.chage_list_name, R.string.list_title, R.string.rename,
                new EditTitlePopup.ResultListener() {

                    @Override
                    public void onAcceptClick(String newTitle) {
                        currentShopList.setTitle(newTitle);
                    }

                    @Override
                    public void onCancelClick() {

                    }
                }
        );
        alert.setValue(currentShopList.getTitle());
        alert.show();
    }

    @Override
    public void addNewListPressed() {

        EditTitlePopup alert = new EditTitlePopup(
                this, R.string.create_new_list, R.string.list_title, R.string.add,
                new EditTitlePopup.ResultListener() {

                    @Override
                    public void onAcceptClick(String newTitle) {
                        userInfo.createNewList(newTitle);
                        mainDrawer.toggleLockDrawer(userInfo);
                    }

                    @Override
                    public void onCancelClick() {

                    }
                }
        );
        alert.show();

    }

    @Override
    public void selectList(final ShopList shopList) {
        currentShopList = shopList;
        shopListView.setShopList(shopList);
        mainDrawer.toggleLockDrawer(userInfo);
        mainDrawer.closeDrawer();
    }

    public void onShareListFound(final ShopList listFound) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.join_list_message, listFound.getTitle()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fireBaseManager.getShareHandler().handleJoinList(userInfo, listFound);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fireBaseManager.getShareHandler().handleCancelJoinList(userInfo, listFound);
                    }
                }).create().show();
    }

    private void onQuitListClicked() {
        if (currentShopList == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.verify_before_quit, currentShopList.getTitle()))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentShopList.remove();
                        mainDrawer.openFirstAvailableShopList();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing...
                    }
                }).create().show();
    }

    public void showShared() {

        if (currentShopList == null) {
            return;
        }

        ShareDialog shareDialog = new ShareDialog(this, currentShopList,
                fireBaseManager);
        shareDialog.show();
    }
}
