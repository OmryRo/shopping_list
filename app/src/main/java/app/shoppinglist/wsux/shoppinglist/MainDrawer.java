package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.support.annotation.NonNull;
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

import java.util.HashMap;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.UserInfo;

public class MainDrawer implements NavigationView.OnNavigationItemSelectedListener {

    private final static int HEADER_VIEW_INDEX = 0;


    private NavigationView navigationView;
    private UserInfo userInfo;

    private MenuItem addItemMenuRef;
    private Map<MenuItem, ShopList> shopListMenuRef;
    private ShopList choosenShopList;
    private MainDrawerInterface mainDrawerInterface;

    // layouts
    private DrawerLayout drawer;
    private TextView userNameTv;
    private TextView userEmailTv;
    private ImageView userPictureIv;

    MainDrawer(Activity context, Toolbar toolbar, MainDrawerInterface mainDrawerInterface)  {

        shopListMenuRef = new HashMap<>();
        this.mainDrawerInterface = mainDrawerInterface;

        drawer = context.findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                context, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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

        this.userInfo = userInfo;

        if (userInfo == null) {
            return;
        }

        userNameTv.setText(userInfo.getDisplayName());
        userEmailTv.setText(userInfo.getEmail());
        userPictureIv.setImageBitmap(userInfo.getPicture());

        updateMenuItems();
    }

    private void toggeLockDrawer(UserInfo userInfo) {
        drawer.setDrawerLockMode(userInfo == null ?
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED
        );
    }

    public void reportListChange() {
        updateMenuItems();
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
            MenuItem item = listSubMenu.add(shopList.getTitle());
            item.setIcon(R.drawable.ic_menu_assignment);
            shopListMenuRef.put(item, shopList);

            if (choosenShopList == shopList) {
                item.setChecked(true);
            } else {
                item.setChecked(false);
            }

        }


    }

    public void close() {
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        if (item == addItemMenuRef) {
            mainDrawerInterface.addNewListPressed();

        } else {
            ShopList choosen = shopListMenuRef.get(item);
            if (choosen != null) {
                item.setChecked(true);
                choosenShopList = choosen;
                mainDrawerInterface.selectedList(choosen);
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    interface MainDrawerInterface {
        void addNewListPressed();
        void selectedList(ShopList shopList);
    }

}
