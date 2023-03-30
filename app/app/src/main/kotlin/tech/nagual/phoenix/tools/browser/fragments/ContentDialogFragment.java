package tech.nagual.phoenix.tools.browser.fragments;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import tech.nagual.phoenix.tools.browser.LogUtils;
import tech.nagual.phoenix.R;
import tech.nagual.phoenix.tools.browser.core.Content;

public class ContentDialogFragment extends DialogFragment {

    public static final String TAG = ContentDialogFragment.class.getSimpleName();


    public static ContentDialogFragment newInstance(@NonNull Uri uri,
                                                    @NonNull String message,
                                                    @NonNull String url) {


        Bundle bundle = new Bundle();
        bundle.putString(Content.URI, uri.toString());
        bundle.putString(Content.TEXT, message);
        bundle.putString(Content.URL, url);
        ContentDialogFragment fragment = new ContentDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        View view = inflater.inflate(R.layout.browser_content_info, null);

        ImageView imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        Objects.requireNonNull(bundle);
        String title = getString(R.string.browser_information);
        String message = bundle.getString(Content.TEXT, "");
        Uri uri = Uri.parse(bundle.getString(Content.URI));
        Objects.requireNonNull(uri);
        String url = bundle.getString(Content.URL, "");


        TextView page = view.findViewById(R.id.page);

        if (url.isEmpty()) {
            page.setVisibility(View.GONE);
        } else {
            page.setText(url);
        }


        try {
            Glide.with(requireContext()).load(uri).into(imageView);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

        builder.setTitle(title)
                .setMessage(message)
                .setView(view)
                .create();

        return builder.create();
    }
}
