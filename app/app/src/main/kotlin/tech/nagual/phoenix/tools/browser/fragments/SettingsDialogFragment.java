package tech.nagual.phoenix.tools.browser.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Objects;

import tech.nagual.phoenix.R;
import tech.nagual.phoenix.tools.browser.Settings;
import tech.nagual.phoenix.tools.browser.core.DOCS;

public class SettingsDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = SettingsDialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setPeekHeight(0);
        dialog.setContentView(R.layout.browser_settings_view);


        SwitchMaterial enableJavascript = dialog.findViewById(R.id.enable_javascript);
        Objects.requireNonNull(enableJavascript);
        enableJavascript.setChecked(Settings.isJavascriptEnabled(requireContext()));
        enableJavascript.setOnCheckedChangeListener((buttonView, isChecked) ->
                Settings.setJavascriptEnabled(requireContext(), isChecked)
        );


        SwitchMaterial enableRedirectUrl = dialog.findViewById(R.id.enable_redirect_url);
        Objects.requireNonNull(enableRedirectUrl);
        enableRedirectUrl.setChecked(Settings.isRedirectUrlEnabled(requireContext()));
        enableRedirectUrl.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Settings.setRedirectUrlEnabled(requireContext(), isChecked);
                    DOCS.getInstance(requireContext()).refreshRedirectOptions(requireContext());
                }
        );

        SwitchMaterial enableRedirectIndex = dialog.findViewById(R.id.enable_redirect_index);
        Objects.requireNonNull(enableRedirectIndex);
        enableRedirectIndex.setChecked(Settings.isRedirectIndexEnabled(requireContext()));
        enableRedirectIndex.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    Settings.setRedirectIndexEnabled(requireContext(), isChecked);
                    DOCS.getInstance(requireContext()).refreshRedirectOptions(requireContext());
                }
        );

        return dialog;
    }

}
