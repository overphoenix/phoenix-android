package tech.nagual.phoenix.tools.browser.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

import tech.nagual.phoenix.tools.browser.core.books.Bookmark;


@SuppressWarnings("WeakerAccess")
public class BookmarkDiffCallback extends DiffUtil.Callback {
    private final List<Bookmark> mOldList;
    private final List<Bookmark> mNewList;

    public BookmarkDiffCallback(List<Bookmark> messages, List<Bookmark> messageThreads) {
        this.mOldList = messages;
        this.mNewList = messageThreads;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).areItemsTheSame(mNewList.get(
                newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return sameContent(mOldList.get(oldItemPosition), mNewList.get(newItemPosition));
    }


    private boolean sameContent(@NonNull Bookmark t, @NonNull Bookmark o) {

        if (t == o) return true;
        return t.getTimestamp() == o.getTimestamp() &&
                Objects.equals(t.getUri(), o.getUri()) &&
                Objects.equals(t.getTitle(), o.getTitle());
    }

}
