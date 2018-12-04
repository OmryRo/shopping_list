package app.shoppinglist.wsux.shoppinglist;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import app.shoppinglist.wsux.shoppinglist.firebase.BaseCollectionItem;
import app.shoppinglist.wsux.shoppinglist.firebase.Collaborator;
import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopList;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;
import app.shoppinglist.wsux.shoppinglist.firebase.UserInfo;

public class TestFirebaseActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "TEST_FIREBASE_ACTIVITY";

    private FireBaseManager fireBaseManager;
    private FirebaseFirestore db;
    private UserInfo userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_firebase);
        db = FirebaseFirestore.getInstance();

        fireBaseManager = new FireBaseManager(this, new FireBaseManager.FireBaseEventsInterface() {

            @Override
            public void onEventOccurred(int what, Object data, Exception e) {

                String message = String.format("%s - %s", what, e != null ? e.getMessage() :"");
                Log.d(TAG, "onEventOccurred: " + message);

                switch (what) {
                    case FireBaseManager.ON_SIGN_IN:
                        TestFirebaseActivity.this.userInfo = (UserInfo) data;
                        changeAuthView();

                        Toast.makeText(TestFirebaseActivity.this, "success", Toast.LENGTH_SHORT).show();
                        break;

                    case FireBaseManager.ON_SIGN_OUT:
                        userInfo = null;
                        changeAuthView();

                        Toast.makeText(TestFirebaseActivity.this, "out", Toast.LENGTH_SHORT).show();
                        break;

                    case FireBaseManager.ON_SIGN_ERR:
                        Toast.makeText(TestFirebaseActivity.this, "sign-err: " + message, Toast.LENGTH_SHORT).show();

                        userInfo = null;
                        changeAuthView();
                        break;

                    case FireBaseManager.ON_USER_LIST_UPDATED:
                        fillListOfList();
                        break;

                    case FireBaseManager.ON_LIST_CREATED:
                        Toast.makeText(TestFirebaseActivity.this, "list created", Toast.LENGTH_SHORT).show();
                        break;

                    case FireBaseManager.ON_LIST_FAILURE:
                        Toast.makeText(TestFirebaseActivity.this, "list fail: " + message, Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        Toast.makeText(TestFirebaseActivity.this, "default: " + message, Toast.LENGTH_SHORT).show();
                        break;
                }
            }

        });

        fireBaseManager.onCreate();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.create_a_list_button).setOnClickListener(this);
    }

    private void signIn() {
        fireBaseManager.getLoginManager().requestLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fireBaseManager.onStart();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fireBaseManager.onActivityResult(requestCode, resultCode, data);
    }



    private void signOut() {
        fireBaseManager.getLoginManager().requestLogout();
    }

    private void changeAuthView() {
        findViewById(R.id.sign_in_button).setVisibility(userInfo == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(userInfo  != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.create_a_list_button).setVisibility(userInfo  != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.list_of_listts).setVisibility(userInfo  != null ? View.VISIBLE : View.GONE);

        ((TextView) findViewById(R.id.user_name)).setText(userInfo  == null ? "Not logged in" : userInfo.getDisplayName());

        fillListOfList();
    }

    private void showCreateListPopUpDialog() {
        final TextView textView = new TextView(this);
        textView.setText("New list name:");

        final EditText editText = new EditText(this);
        final LinearLayout layout = new LinearLayout(this);
        layout.addView(textView);
        layout.addView(editText);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createNewList(editText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alertDialog.show();
    }

    private void createNewList(String title) {
        userInfo.createNewList(title);
    }

    private void fillListOfList() {
        final LinearLayout listOfLists = findViewById(R.id.list_of_listts);
        listOfLists.removeAllViews();

        if (userInfo == null) {
            return;
        }

        for (HashMap.Entry<String, ShopList> entry : userInfo.getLists().entrySet()) {
            final ShopList listInfo = entry.getValue();

            TextView titleView = new TextView(getApplicationContext());
            titleView.setText(listInfo.getTitle());
            titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showList(listInfo);
                }
            });

            listOfLists.addView(titleView);
        }
    }

    public void showList(final ShopList shopList) {

        final LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.addView(header);

        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(body);

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(wrapper)
                .create();

        Button dismiss = new Button(this);
        dismiss.setText("X");
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        header.addView(dismiss);

        final Button addMore = new Button(this);
        addMore.setText("+ Add");
        addMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                showAddTaskPopup(shopList);
            }
        });
        header.addView(addMore);
        alertDialog.show();

        shopList.setOnChildChangeListener(new BaseCollectionItem.OnChildChangeListener() {
            @Override
            public void onChange() {
                body.removeAllViews();

                for (HashMap.Entry<String, Collaborator> entry : shopList.getCollaborators().entrySet()) {
                    final Collaborator collaborator = entry.getValue();
                    final TextView titleView = new TextView(getApplicationContext());

                    collaborator.setOnChangeListener(new BaseCollectionItem.OnChangeListener() {
                        @Override
                        public void onChange() {
                            String text = String.format("%s - %s", collaborator.getName(), collaborator.getMessage());
                            titleView.setText(text);
                            titleView.setTextColor(collaborator.getColor());
                        }
                    });

                    body.addView(titleView);
                }

                for (HashMap.Entry<String, ShopTask> entry : shopList.getTasks().entrySet()) {
                    final ShopTask shopTask = entry.getValue();
                    final TextView titleView = new TextView(getApplicationContext());

                    shopTask.setOnChangeListener(new BaseCollectionItem.OnChangeListener() {
                        @Override
                        public void onChange() {
                            titleView.setText(shopTask.getTitle());

                            if (shopTask.getState() == ShopTask.SHOP_TASK_DONE) {
                                titleView.setPaintFlags(titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            } else {
                                titleView.setPaintFlags(titleView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                            }

                            titleView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    shopTask.setState(
                                            shopTask.getState() == ShopTask.SHOP_TASK_DONE ?
                                                ShopTask.SHOP_TASK_NOT_DONE :
                                                ShopTask.SHOP_TASK_DONE);
                                }
                            });
                        }
                    });

                    body.addView(titleView);
                }
            }
        });
    }

    private void showAddTaskPopup(final ShopList shopList) {
        final TextView textView = new TextView(this);
        textView.setText("New task:");

        final EditText editText = new EditText(this);
        final LinearLayout layout = new LinearLayout(this);
        layout.addView(textView);
        layout.addView(editText);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createNewTask(shopList, editText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showList(shopList);
                    }
                }).create();
        alertDialog.show();
    }

    public void createNewTask(final ShopList shopList, String taskTitle) {
        shopList.addNewTask(taskTitle, "");
        showList(shopList);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.create_a_list_button:
                showCreateListPopUpDialog();
                break;
        }
    }
}
