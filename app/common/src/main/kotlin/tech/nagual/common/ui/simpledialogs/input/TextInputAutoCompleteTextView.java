package tech.nagual.common.ui.simpledialogs.input;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.google.android.material.textfield.TextInputLayout;


/**
 * A special sub-class of {@link AppCompatAutoCompleteTextView} designed for use
 * as a child of {@link TextInputLayout}.
 * <p>
 * Using this class allows us to display a hint in the IME when in 'extract' mode.
 * <p>
 * Created by eltos on 16.02.17 as suggested here: http://stackoverflow.com/a/41864063
 */
public class TextInputAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    public boolean doNotFilter = false;

    public TextInputAutoCompleteTextView(Context context) {
        super(context);
    }

    public TextInputAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextInputAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        return doNotFilter || super.enoughToFilter();
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        super.performFiltering(doNotFilter ? "" : text, keyCode);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            ViewParent parent = getParent();
            while (parent instanceof View) {
                if (parent instanceof TextInputLayout) {
                    outAttrs.hintText = ((TextInputLayout) parent).getHint();
                    break;
                }
                parent = parent.getParent();
            }
        }
        return ic;
    }
}