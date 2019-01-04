package app.shoppinglist.wsux.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditTitlePopup extends Dialog implements View.OnClickListener {

    private TextView popupTitle;
    private EditText titleText;
    private View acceptButton;
    private TextView acceptButtonText;
    private View cancelButton;
    private ResultListener listener;

    public EditTitlePopup(Context context, int popupTitleText, int titleTextHint,
                          int acceptButtonText, ResultListener listener) {
        super(context);
        setViewsFromLayout();
        setTexts(popupTitleText, titleTextHint, acceptButtonText);
        setListeners(listener);
    }

    private void setViewsFromLayout() {
        setContentView(R.layout.edit_title_popup_layout);
        popupTitle = findViewById(R.id.edit_title_popup_title);
        titleText = findViewById(R.id.edit_title_popup_text);
        acceptButton = findViewById(R.id.edit_title_popup_accept);
        acceptButtonText = findViewById(R.id.edit_title_popup_accept_text);
        cancelButton = findViewById(R.id.edit_title_popup_cancel);
    }

    private void setTexts(int popupTitleTextRes, int titleTextHintRes, int acceptButtonTextRes) {
        popupTitle.setText(popupTitleTextRes);
        titleText.setHint(titleTextHintRes);
        acceptButtonText.setText(acceptButtonTextRes);
    }

    public void setValue(String value) {
        titleText.setText(value);
    }

    private void setListeners(ResultListener listener) {
        this.listener = listener;
        acceptButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }

    private void onAcceptButtonClick() {
        String title = titleText.getText().toString();
        if (title.length() == 0) {
            Toast.makeText(getContext(), R.string.cant_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        listener.onAcceptClick(title);
        this.dismiss();
    }
    private void onCancleButtonClick() {
        listener.onCancelClick();
        this.dismiss();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_title_popup_accept:
                onAcceptButtonClick();
                break;
            case R.id.edit_title_popup_cancel:
                onCancleButtonClick();
                break;
        }
    }

    public interface ResultListener {
        void onAcceptClick(String newTitle);
        void onCancelClick();
    }
}
