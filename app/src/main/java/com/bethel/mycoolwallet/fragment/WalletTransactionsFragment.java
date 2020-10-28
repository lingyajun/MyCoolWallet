package com.bethel.mycoolwallet.fragment;


import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.adapter.TestTransactionsAdapter;
import com.bethel.mycoolwallet.adapter.TransactionsAdapter;
import com.bethel.mycoolwallet.view.StickToTopLinearLayoutManager;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;

import static com.bethel.mycoolwallet.adapter.TestTransactionsAdapter.WarningType.CHAIN_FORKING;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletTransactionsFragment extends BaseFragment {
    @BindView(R.id.wallet_transactions_group)
    ViewAnimator viewAnimator;

    @BindView(R.id.wallet_transactions_empty)
    TextView emptyTv;

    @BindView(R.id.wallet_transactions_list)
    RecyclerView recyclerView;

    private ListAdapter mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // test ui
        final SpannableStringBuilder emptyText = new SpannableStringBuilder(
                getString( R.string.wallet_transactions_fragment_empty_text_received));
        emptyText.setSpan(new StyleSpan(Typeface.BOLD), 0, emptyText.length(),
                SpannableStringBuilder.SPAN_POINT_MARK);
        emptyText.append("\n\n")
                .append(getString(R.string.wallet_transactions_fragment_empty_text_howto));
        emptyTv.setText(emptyText);
        viewAnimator.setDisplayedChild(0);

        emptyTv.postDelayed(() -> {viewAnimator.setDisplayedChild(1);}, 1200);

        mAdapter = generateTestAdapter();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new StickToTopLinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new TransactionsAdapter.ItemAnimator());
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            private final int PADDING = 2
                    * getResources().getDimensionPixelOffset(R.dimen.card_padding_vertical);

            @Override
            public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent,
                                       final RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                final int position = parent.getChildAdapterPosition(view);
                if (position == 0)
                    outRect.top += PADDING;
                else if (position == parent.getAdapter().getItemCount() - 1)
                    outRect.bottom += PADDING;
            }
        });

        mAdapter.submitList(generateTestData());
    }

    private List generateTestData() {
        List<TestTransactionsAdapter.ListItem> list = new LinkedList<>();
        for(int i=0; i<10; i++){
            TestTransactionsAdapter.ListItem item = null;
            if ( i>7) {
              item = new  TestTransactionsAdapter.ListItem.WarningItem(CHAIN_FORKING);
            } else {
                item = new TestTransactionsAdapter.ListItem.TransactionItem();
            }
            list.add(item);
        }
        return list;
    }

    private ListAdapter generateTestAdapter() {
        return new TestTransactionsAdapter(getActivity());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_wallet_transactions;
    }

}
