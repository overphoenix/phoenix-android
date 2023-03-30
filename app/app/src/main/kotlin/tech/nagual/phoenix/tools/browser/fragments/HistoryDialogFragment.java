package tech.nagual.phoenix.tools.browser.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;

import tech.nagual.phoenix.tools.browser.LogUtils;
import tech.nagual.phoenix.R;
import tech.nagual.phoenix.tools.browser.core.events.EVENTS;
import tech.nagual.phoenix.tools.browser.utils.HistoryViewAdapter;

public class HistoryDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = HistoryDialogFragment.class.getSimpleName();

    private static final int CLICK_OFFSET = 500;
    private long mLastClickTime = 0;
    private ActionListener mListener;

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (ActionListener) getActivity();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(0);

        dialog.setContentView(R.layout.browser_history_view);

        RecyclerView history = dialog.findViewById(R.id.history);
        Objects.requireNonNull(history);


        history.setLayoutManager(new LinearLayoutManager(requireContext()));
        HistoryViewAdapter mHistoryViewAdapter = new HistoryViewAdapter(url -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            try {
                Thread.sleep(150);

                EVENTS.getInstance(requireContext()).uri(url);
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            } finally {
                dismiss();
            }
        }, mListener.getWebView().copyBackForwardList());
        history.setAdapter(mHistoryViewAdapter);


        return dialog;
    }

}
