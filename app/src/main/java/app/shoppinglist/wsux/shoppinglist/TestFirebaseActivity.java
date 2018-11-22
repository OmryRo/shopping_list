package app.shoppinglist.wsux.shoppinglist;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TestFirebaseActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "TEST_FIREBASE_ACTIVITY";
    private final static int RC_SIGN_IN = 999;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_firebase);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.create_a_list_button).setOnClickListener(this);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        //GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        currentUser = mAuth.getCurrentUser();
        changeAuthView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            currentUser = mAuth.getCurrentUser();
                            changeAuthView();

                        } else {
                            // If sign in fails, display a message to the user.
                            currentUser = null;
                            changeAuthView();
                        }
                    }
                });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void signOut() {
        currentUser = null;

        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        currentUser = null;
                        changeAuthView();
                    }
                });
    }

    private void changeAuthView() {
        findViewById(R.id.sign_in_button).setVisibility(currentUser == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(currentUser != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.create_a_list_button).setVisibility(currentUser != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.list_of_listts).setVisibility(currentUser != null ? View.VISIBLE : View.GONE);

        ((TextView) findViewById(R.id.user_name)).setText(currentUser == null ? "Not logged in" : currentUser.getDisplayName());

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
        newList.put("author", currentUser.getUid());

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
