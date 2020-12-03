package com.bethel.mycoolwallet.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bethel.mycoolwallet.R;

public class TransactionsItemDecoration extends RecyclerView.ItemDecoration {
//    private Context context;
    private final int PADDING;

    public TransactionsItemDecoration(Context context) {
        //  10dp
        PADDING = 2 * context.getResources().getDimensionPixelOffset(R.dimen.card_padding_vertical);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        final int position = parent.getChildAdapterPosition(view);
        // 第一个
        if (0==position) {
            outRect.top += PADDING;
            return;
        }

        // 最后一个
        if (null!= parent.getAdapter() && position == (parent.getAdapter().getItemCount() -1)) {
            outRect.bottom += PADDING;
        }
    }
}
