package app.shoppinglist.wsux.shoppinglist;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ProgressFragment extends Fragment {

    public static final int TYPE_LOADING = R.string.progress_loading;
    public static final int TYPE_UPDATING = R.string.progress_updating;
    public static final int TYPE_DELETING = R.string.progress_deleting;
    public static final int TYPE_CREATING = R.string.progress_creating;
    public static final int TYPE_DOWNLOADING = R.string.progress_downloading;
    public static final int TYPE_UPLOADING = R.string.progress_uploading;
    public static final int TYPE_QUITING = R.string.progress_quiting;
    public static final int TYPE_SIGN_IN = R.string.progress_sign_in;
    public static final int TYPE_SIGN_OUT = R.string.progress_sign_out;

    private View alertLayout;
    private TextView messageView;

    public ProgressFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        alertLayout = inflater.inflate(R.layout.progress_fragment, container, false);
        messageView = alertLayout.findViewById(R.id.alert_fragment_message);
        hide();

        return alertLayout;
    }

    public void show(int stringId) {
        messageView.setText(stringId);
        alertLayout.setVisibility(View.VISIBLE);
    }

    public void hide() {
        alertLayout.setVisibility(View.GONE);
    }

}
