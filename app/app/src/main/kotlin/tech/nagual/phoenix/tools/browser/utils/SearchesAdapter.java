package tech.nagual.phoenix.tools.browser.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import tech.nagual.phoenix.tools.browser.LogUtils;
import tech.nagual.phoenix.R;
import tech.nagual.phoenix.tools.browser.core.books.Bookmark;

public abstract class SearchesAdapter extends ArrayAdapter<Bookmark> {

    private static final String TAG = Bookmark.class.getSimpleName();


    public SearchesAdapter(@NonNull Context context, @NonNull ArrayList<Bookmark> data) {
        super(context, R.layout.browser_searches, data);
    }

    public abstract void onClick(@NonNull Bookmark bookmark);

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Bookmark bookmark = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag


        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.browser_searches, parent, false);
            viewHolder.title = convertView.findViewById(R.id.bookmark_title);
            viewHolder.uri = convertView.findViewById(R.id.bookmark_uri);
            viewHolder.image = convertView.findViewById(R.id.bookmark_image);
            viewHolder.view = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }


        String title = bookmark.getTitle();
        viewHolder.title.setText(title);
        viewHolder.uri.setText(bookmark.getUri());
        viewHolder.image.setTag(position);

        Bitmap image = bookmark.getBitmapIcon();
        if (image != null) {
            viewHolder.image.clearColorFilter();
            viewHolder.image.setImageBitmap(image);
        } else {
            viewHolder.image.setImageResource(R.drawable.browser_bookmark);
            if (!title.isEmpty()) {
                int color = ColorGenerator.MATERIAL.getColor(title);
                viewHolder.image.setColorFilter(color);
            }
        }

        viewHolder.view.setClickable(true);
        viewHolder.view.setFocusable(false);
        viewHolder.view.setOnClickListener((v) -> {
            try {
                onClick(bookmark);
            } catch (Throwable e) {
                LogUtils.error(TAG, e);
            }

        });
        return convertView;
    }

    private static class ViewHolder {
        TextView title;
        TextView uri;
        ImageView image;
        View view;
    }
}