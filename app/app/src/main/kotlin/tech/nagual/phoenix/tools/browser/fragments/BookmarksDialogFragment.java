package tech.nagual.phoenix.tools.browser.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Comparator;
import java.util.Objects;

import tech.nagual.phoenix.tools.browser.LogUtils;
import tech.nagual.phoenix.R;
import tech.nagual.phoenix.tools.browser.core.books.Bookmark;
import tech.nagual.phoenix.tools.browser.core.books.BookmarkViewModel;
import tech.nagual.phoenix.tools.browser.core.events.EVENTS;
import tech.nagual.phoenix.tools.browser.utils.BookmarksViewAdapter;
import tech.nagual.phoenix.tools.browser.utils.SwipeToDeleteCallback;

public class BookmarksDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = BookmarksDialogFragment.class.getSimpleName();

    private static final int CLICK_OFFSET = 500;

    private long mLastClickTime = 0;
    private BookmarksViewAdapter mBookmarksViewAdapter;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(0);

        dialog.setContentView(R.layout.browser_booksmark_view);

        RecyclerView bookmarks = dialog.findViewById(R.id.bookmarks);
        Objects.requireNonNull(bookmarks);


        bookmarks.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBookmarksViewAdapter = new BookmarksViewAdapter(requireContext(), bookmark -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            try {
                Thread.sleep(150);

                EVENTS.getInstance(requireContext()).uri(bookmark.getUri());
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            } finally {
                dismiss();
            }
        });
        bookmarks.setAdapter(mBookmarksViewAdapter);
        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SwipeToDeleteCallback(mBookmarksViewAdapter));
        itemTouchHelper.attachToRecyclerView(bookmarks);


        BookmarkViewModel bookmarkViewModel =
                new ViewModelProvider(this).get(BookmarkViewModel.class);

        bookmarkViewModel.getBookmarks().observe(this, (marks -> {
            try {
                if (marks != null) {
                    marks.sort(Comparator.comparing(Bookmark::getTimestamp).reversed());
                    mBookmarksViewAdapter.setBookmarks(marks);
                }
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }));

        return dialog;
    }

}
