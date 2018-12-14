package app.shoppinglist.wsux.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class TaskEditDialog extends Dialog {

    TaskEditDialog(Context context) {
        super(context);

        setContentView(R.layout.task_edit_dialog);

    }

}
