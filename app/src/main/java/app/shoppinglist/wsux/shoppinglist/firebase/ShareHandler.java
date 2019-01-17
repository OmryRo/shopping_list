package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import app.shoppinglist.wsux.shoppinglist.R;

public class ShareHandler {

    private FireBaseManager manager;
    private Intent handledIntent;

    ShareHandler(FireBaseManager manager) {
        this.manager = manager;
    }

    public void performShareList(ShopList shopList) {
        String token = generateShareToken(shopList);
        shopList.addToken(token);

        Intent sendIntent = generateShareIntent(shopList, token);
        manager.getAppContext().startActivity(sendIntent);
    }
    
    private Intent generateShareIntent(ShopList shopList, String token) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);

        String shareMessage = getShareMessage(shopList, token);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        sendIntent.setType("text/plain");
        return sendIntent;
    }
    
    private String generateShareToken(ShopList shopList) {
        String stringValue = String.format("%s_%s", shopList.getListId(), System.currentTimeMillis());
        int hashValue = stringValue.hashCode();
        return String.valueOf(Math.abs(hashValue));
    }
    
    private String getShareMessage(ShopList shopList, String token) {
        Context context = manager.getAppContext();
        String shareDomain = context.getString(R.string.share_domain);
        String url = context.getString(R.string.share_url, shareDomain, shopList.getListId(), token);
        String appName = context.getString(R.string.app_name);
        return context.getString(R.string.share_text, appName, url);
    }

    private String[] checkForIncomingIntent() {
        Intent startIntent = (manager.getAppContext()).getIntent();

        if (startIntent == null || startIntent == handledIntent) {
            return null;
        }

        handledIntent = startIntent;

        String action = startIntent.getAction();
        Uri data = startIntent.getData();

        if (data == null || isIntentDataHostValid(data)) {
            return null;
        }

        return extractTokenAndIdFromData(data);
    }

    @Nullable
    private String[] extractTokenAndIdFromData(Uri data) {
        String path = data.getPath();
        if (path == null) {
            return null;
        }

        String[] splittedPath = path.split("/");

        if (splittedPath.length != 3) {
            return null;
        }

        String listId = splittedPath[1];
        String token = splittedPath[2];

        return new String[] {listId, token};
    }

    private boolean isIntentDataHostValid(Uri data) {
        return (data.getHost() == null ||
                !data.getHost().equals(manager.getAppContext().getString(R.string.share_domain)));
    }

    public void checkIncomingShare(UserInfo userInfo) {
        String[] incomingIntent = checkForIncomingIntent();

        if (incomingIntent == null) {
            return;
        }

        userInfo.addToken(incomingIntent[0], incomingIntent[1]);
    }

    public void handleJoinList(UserInfo userInfo, ShopList shopList) {
        String token = userInfo.getToken(shopList.getListId());
        shopList.replaceTokenByCollaborators(token);
    }

    public void handleCancelJoinList(UserInfo userInfo, ShopList shopList) {
        userInfo.removeToken(shopList.getListId());
        userInfo.removeKnownList(shopList.getListId());
    }
}
