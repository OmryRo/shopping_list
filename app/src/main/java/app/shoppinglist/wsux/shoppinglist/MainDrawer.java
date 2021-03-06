package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private Toolbar toolbar;
    private TextView userNameTv;
    private TextView userEmailTv;
    private ImageView userPictureIv;

    MainDrawer(Activity context, Toolbar toolbar, MainDrawerInterface mainDrawerInterface) {

        shopListMenuRef = new HashMap<>();
        this.mainDrawerInterface = mainDrawerInterface;
        this.defaultText = context.getString(R.string.not_available);

        initiateDrawer(context, toolbar);
        initiateNavigationAndHeaderViews(context);

    }

    private void initiateDrawer(Activity context, Toolbar toolbar) {
        drawer = context.findViewById(R.id.drawer_layout);
        this.toolbar = toolbar;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                context, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toggleLockDrawer(null);
    }

    private void initiateNavigationAndHeaderViews(Activity context) {
        navigationView = context.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(HEADER_VIEW_INDEX);
        userNameTv = headerView.findViewById(R.id.drawer_user_name);
        userEmailTv = headerView.findViewById(R.id.drawer_user_mail);
        userPictureIv = headerView.findViewById(R.id.drawer_user_picture);
    }

    public void setUserInfo(UserInfo userInfo) {

        toggleLockDrawer(userInfo);

        if (this.userInfo == userInfo) {
            return;
        }

        setUserInfoListeners(userInfo);
        this.userInfo = userInfo;
    }

    void toggleLockDrawer(UserInfo userInfo) {
        drawer.setDrawerLockMode(userInfo == null ?
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED
        );
    }

    private void updateMenuItems() {

        Menu navigationViewMenu = navigationView.getMenu();
        navigationViewMenu.clear();

        if (userInfo == null) {
            return;
        }

        addAddItemIntoMenu(navigationViewMenu);
        addListOfListsSubMenu(navigationViewMenu);
    }

    private void addAddItemIntoMenu(Menu navigationViewMenu) {
        addItemMenuRef = navigationViewMenu.add(R.string.create_new_list);
        addItemMenuRef.setIcon(R.drawable.ic_menu_add_box);
    }

    private void addListOfListsSubMenu(Menu navigationViewMenu) {
        SubMenu listSubMenu = navigationViewMenu.addSubMenu(R.string.menu_sublist_lists);
        MenuItem menuItem;

        for (ShopList shopList : getOrderedListOfLists()) {
            menuItem = listSubMenu.add(shopList.getTitle());
            setMenuItemDetails(menuItem, shopList);
        }
    }

    private void setMenuItemDetails(MenuItem menuItem, ShopList shopList){
        menuItem.setIcon(R.drawable.ic_menu_assignment);
        menuItem.setCheckable(true);

        shopListMenuRef.put(menuItem, shopList);

        if (selectedList == null && shopList.getListId().equals(userInfo.getLastList())) {
            selectedList = shopList;
        }

        menuItem.setChecked(selectedList == shopList);
    }

    private ArrayList<ShopList> getOrderedListOfLists() {
        ArrayList<ShopList> listOfLists = new ArrayList<>(userInfo.getLists().values());
        Collections.sort(listOfLists, new Comparator<ShopList>() {
            @Override
            public int compare(ShopList o1, ShopList o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
        return listOfLists;
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

    public boolean onBackPressed() {
        if (drawer.getDrawerLockMode(GravityCompat.START) != DrawerLayout.LOCK_MODE_LOCKED_OPEN) {
            return closeDrawer();
        }

        return false;
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
            defineCurrentShoplist(menuItem);
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openFirstAvailableShopList() {
        if (getOrderedListOfLists().size() <= 1) {
            openAndLockDrawer();
            mainDrawerInterface.selectList(null);
        } else {
            MenuItem menuItem = (MenuItem) shopListMenuRef.keySet().toArray()[0];
            defineCurrentShoplist(menuItem);
        }
    }

    private void defineCurrentShoplist(MenuItem menuItem) {
        ShopList selected = shopListMenuRef.get(menuItem);
        if (selected == null) {
            return;
        }
        menuItem.setChecked(true);
        selectedList = selected;
        navigationView.setCheckedItem(menuItem);
        userInfo.setLastList(selected.getListId());
        mainDrawerInterface.selectList(selected);
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

    public void openAndLockDrawer() {
        if(selectedList == null){
            drawer.openDrawer(GravityCompat.START);
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        }
    }

    interface MainDrawerInterface {
        void addNewListPressed();

        void renameListPressed();

        void selectList(ShopList shopList);
    }

}
