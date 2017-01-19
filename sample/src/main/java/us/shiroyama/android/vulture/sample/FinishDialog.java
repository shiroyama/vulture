package us.shiroyama.android.vulture.sample;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * @author Fumihiko Shiroyama
 */

public class FinishDialog extends DialogFragment {
    public static String TAG = FinishDialog.class.getSimpleName();

    public FinishDialog() {
    }

    public static FinishDialog newInstance(@NonNull String message) {
        Bundle args = new Bundle(1);
        args.putString(Args.MESSAGE.toString(), message);
        FinishDialog dialog = new FinishDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("SUCCESS!")
                .setMessage(getArguments().getString(Args.MESSAGE.toString()))
                .create();
    }

    private enum Args {
        MESSAGE
    }

}
