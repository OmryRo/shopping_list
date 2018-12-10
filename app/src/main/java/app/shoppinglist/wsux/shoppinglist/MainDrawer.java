package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.UserInfo;

public class MainDrawer implements NavigationView.OnNavigationItemSelectedListener,
        BaseCollectionItem.OnChildChangeListener, BaseCollectionItem.OnMediaDownload,
        BaseCollectionItem.OnChangeListener {

    private final static String TAG = "MAIN_DRAWER";

    private final static int HEADER_VIEW_INDEX = 0;

    private UserInfo userInfo;
    private MenuItem addItemMenuRef;
    private Map<MenuItem, ShopList> shopListMenuRef;
    private ShopList selectedList;
    private MainDrawerInterface mainDrawerInterface;

    // default values
    private String defaultText;

    // layouts
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private TextView userNameTv;
    private TextView userEmailTv;
    private ImageView userPictureIv;

    MainDrawer(Activity context, Toolbar toolbar, MainDrawerInterface mainDrawerInterface)  {

        shopListMenuRef = new HashMap<>();
        this.mainDrawerInterface = mainDrawerInterface;
        this.defaultText = context.getString(R.string.not_available);

        drawer = context.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                context, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toggeLockDrawer(null);

        navigationView = context.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(HEADER_VIEW_INDEX);

        userNameTv = headerView.findViewById(R.id.drawer_user_name);
        userEmailTv = headerView.findViewById(R.id.drawer_user_mail);
        userPictureIv = headerView.findViewById(R.id.drawer_user_picture);
    }

    public void setUserInfo(UserInfo userInfo) {

        toggeLockDrawer(userInfo);

        if (this.userInfo == userInfo) {
            return;
        }

        setUserInfoListeners(userInfo);
        this.userInfo = userInfo;
    }

    private void toggeLockDrawer(UserInfo userInfo) {
        drawer.setDrawerLockMode(userInfo == null ?
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED
        );
    }

    private void updateMenuItems() {

        navigationView.getMenu().clear();

        if (userInfo == null) {
            return;
        }

        Menu navigationViewMenu = navigationView.getMenu();
        addItemMenuRef = navigationViewMenu.add(R.string.create_new_list);

        addItemMenuRef.setIcon(R.drawable.ic_menu_add_box);

        SubMenu listSubMenu = navigationViewMenu.addSubMenu(R.string.menu_sublist_lists);

        HashMap<String, ShopList> lists = userInfo.getLists();
        for (HashMap.Entry<String, ShopList> entry : lists.entrySet()) {
            ShopList shopList = entry.getValue();
            MenuItem menuItem = listSubMenu.add(shopList.getTitle());
            menuItem.setIcon(R.drawable.ic_menu_assignment);
            menuItem.setCheckable(true);


            shopListMenuRef.put(menuItem, shopList);

            if (selectedList == null && shopList.getListId().equals(userInfo.getLastList())) {
                selectedList = shopList;
            }

            boolean isSelected = selectedList == shopList;
            menuItem.setChecked(isSelected);

        }


    }

    public void setSelectedList(ShopList shopList) {
        selectedList = shopList;
        updateMenuItems();
    }

    private void setUserInfoListeners(UserInfo newUserInfo) {
        if (userInfo != newUserInfo && userInfo != null) {
            userInfo.removeAllListeners();
        }

        if (newUserInfo == null) {
            return;
        }

        newUserInfo.setOnChangeListener(this);
        newUserInfo.setOnChildChangeListener(this);
        newUserInfo.setOnMediaDownload(this);
    }

    public boolean closeDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        if (menuItem == addItemMenuRef) {
            mainDrawerInterface.addNewListPressed();

        } else {
            ShopList selected = shopListMenuRef.get(menuItem);
            if (selected != null) {
                menuItem.setChecked(true);
                selectedList = selected;
                navigationView.setCheckedItem(menuItem);
                userInfo.setLastList(selected);
                mainDrawerInterface.selectedList(selected);
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onChildChange() {
        updateMenuItems();
    }

    @Override
    public void onMediaDownload() {
        if (userInfo != null) {
            userPictureIv.setImageBitmap(userInfo.getPicture());
        } else {
            userPictureIv.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public void onChange() {
        String userName = defaultText;
        String userEmail = defaultText;

        if (userInfo != null) {
            userName = userInfo.getDisplayName();
            userEmail = userInfo.getEmail();
        }

        userNameTv.setText(userName);
        userEmailTv.setText(userEmail);
    }

    interface MainDrawerInterface {
        void addNewListPressed();
        void selectedList(ShopList shopList);
    }

}
