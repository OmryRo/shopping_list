package app.shoppinglist.wsux.shoppinglist;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
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
        fireBaseManager = new FireBaseManager(this, new FireBaseManager.FireBaseEventsInterface() {
            @Override
            public void onSignIn(UserInfo userInfo) {
                TestFirebaseActivity.this.userInfo = userInfo;
                changeAuthView();

                Toast.makeText(TestFirebaseActivity.this, "success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSignInFailed(int what) {

                Toast.makeText(TestFirebaseActivity.this, "failed " + what, Toast.LENGTH_SHORT).show();

                userInfo = null;
                changeAuthView();
            }

            @Override
            public void onSignOut() {
                userInfo = null;
                changeAuthView();

                Toast.makeText(TestFirebaseActivity.this, "out", Toast.LENGTH_SHORT).show();
            }
        });

        fireBaseManager.onCreate();

        db = FirebaseFirestore.getInstance();

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
        Map<String, Object> newList = new HashMap<>();
        newList.put("title", title);
        newList.put("author", userInfo.getUserId());

        db.collection("lists")
                .add(newList)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        fillListOfList();
                        Toast.makeText(getApplicationContext(), "New List Added", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "failed to add list", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fillListOfList() {
        final LinearLayout listOfLists = findViewById(R.id.list_of_listts);
        listOfLists.removeAllViews();

        db.collection("lists")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (final QueryDocumentSnapshot document : task.getResult()) {

                                final String itemId = document.getId();
                                Map<String, Object> data = document.getData();

                                TextView titleView = new TextView(getApplicationContext());
                                titleView.setText((String) data.get("title"));
                                titleView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        showList(itemId);
                                    }
                                });

                                listOfLists.addView(titleView);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void showList(final String listId) {

        final LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.addView(header);

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
                showAddTaskPopup(listId);
            }
        });
        header.addView(addMore);
        alertDialog.show();

        db.collection("lists").document(listId).collection("tasks")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (final QueryDocumentSnapshot document : task.getResult()) {

                                final String itemId = document.getId();
                                Map<String, Object> data = document.getData();

                                TextView titleView = new TextView(getApplicationContext());
                                titleView.setText((String) data.get("title"));

                                wrapper.addView(titleView);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void showAddTaskPopup(final String listId) {
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
                        createNewTask(listId, editText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showList(listId);
                    }
                }).create();
        alertDialog.show();
    }

    public void createNewTask(final String listId, String taskTitle) {
        Map<String, Object> newTask = new HashMap<>();
        newTask.put("title", taskTitle);

        db.collection("lists").document(listId).collection("tasks")
                .add(newTask)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        showList(listId);
                        Toast.makeText(getApplicationContext(), "New Task Added", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showList(listId);
                        Toast.makeText(getApplicationContext(), "failed to add list", Toast.LENGTH_SHORT).show();
                    }
                });
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
