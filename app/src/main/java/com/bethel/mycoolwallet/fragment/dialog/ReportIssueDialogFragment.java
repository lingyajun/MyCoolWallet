package com.bethel.mycoolwallet.fragment.dialog;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import android.view.View;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.helper.issue.IssueReporter;
import com.bethel.mycoolwallet.mvvm.view_model.ReportIssueViewModel;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.wallet.Wallet;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import butterknife.BindView;

/**
 * 问题反馈.
 */
public class ReportIssueDialogFragment extends BaseDialogFragment {
    private static final String TAG = "ReportIssueDialogFragment";
    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_CONTEXTUAL_DATA = "contextual_data";

    private boolean sent = false;

    public static void show(final FragmentManager fm, final int titleResId, final int messageResId,
                            final String subject, final String contextualData) {
        final DialogFragment newFragment = new ReportIssueDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(KEY_TITLE, titleResId);
        args.putInt(KEY_MESSAGE, messageResId);
        args.putString(KEY_SUBJECT, subject);
        args.putString(KEY_CONTEXTUAL_DATA, contextualData);
        newFragment.setArguments(args);
        newFragment.show(fm, TAG);
    }

    @BindView(R.id.report_issue_dialog_description)
    EditText viewDescription;
    @BindView(R.id.report_issue_dialog_collect_device_info)
    Checkable viewCollectDeviceInfo;
    @BindView(R.id.report_issue_dialog_collect_installed_packages)
    Checkable viewCollectInstalledPackages;
    @BindView(R.id.report_issue_dialog_collect_application_log)
    Checkable viewCollectApplicationLog;
    @BindView(R.id.report_issue_dialog_collect_wallet_dump)
    Checkable viewCollectWalletDump;
    @BindView(R.id.report_issue_dialog_message)
    TextView viewMessage;

    private TextView positiveButton;

    private ReportIssueViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(ReportIssueViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final int titleResId = args.getInt(KEY_TITLE);
        final int messageResId = args.getInt(KEY_MESSAGE);
        final String subject = args.getString(KEY_SUBJECT);
        final String contextualData = args.getString(KEY_CONTEXTUAL_DATA);

        View view = createAndBindDialogView(R.layout.fragment_report_issue_dialog);
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .title(titleResId)
                .customView(view, false)
                .positiveText(R.string.report_issue_dialog_report)
                .negativeText(R.string.button_cancel)
                .canceledOnTouchOutside(false)
                .cancelable(false)
                .show();
        viewMessage.setText(messageResId);
        dialog.setOnShowListener(dialogInterface -> {
            positiveButton = dialog.getActionButton(DialogAction.POSITIVE);
            positiveButton.setEnabled(false);

            viewModel.wallet.observe(ReportIssueDialogFragment.this,
                    wallet -> positiveButton.setEnabled(true));

            positiveButton.setOnClickListener(view1 -> {
                IssueReporter reporter = new IssueReporter(getActivity(), subject, viewDescription.getText(),
                        viewCollectDeviceInfo.isChecked(), viewCollectInstalledPackages.isChecked(),
                        viewCollectApplicationLog.isChecked(), viewCollectWalletDump.isChecked()) {
                    @Nullable
                    @Override
                    protected CharSequence collectApplicationInfo() throws IOException {
                        final CharSequence info = super.collectApplicationInfo();
                        final StringBuilder report = new StringBuilder( null!=info? info: "");

                        report.append("\n\n ========== wallet =========\n");
                        final Wallet wallet = viewModel.wallet.getValue();
                        report.append("Encrypted: " + wallet.isEncrypted() + "\n");
                        report.append("Keychain size: " + wallet.getKeyChainGroupSize() + "\n");

                        final Set<Transaction> transactions = wallet.getTransactions(true);
                        int numInputs = 0;
                        int numOutputs = 0;
                        int numSpentOutputs = 0;
                        for (final Transaction tx : transactions) {
                            numInputs += tx.getInputs().size();
                            final List<TransactionOutput> outputs = tx.getOutputs();
                            numOutputs += outputs.size();
                            for (final TransactionOutput txout : outputs) {
                                if (!txout.isAvailableForSpending())
                                    numSpentOutputs++;
                            }
                        }
                        report.append("Transactions: " + transactions.size() + "\n");
                        report.append("Inputs: " + numInputs + "\n");
                        report.append("Outputs: " + numOutputs + " (spent: " + numSpentOutputs + ")\n");
                        final int lastBlockSeenHeight = wallet.getLastBlockSeenHeight();
                        final Date lastBlockSeenTime = wallet.getLastBlockSeenTime();
                        report.append("Last block seen: " + lastBlockSeenHeight).append(" (")
                                .append(lastBlockSeenTime == null ? "time unknown" : Utils.dateTimeFormat(lastBlockSeenTime))
                                .append(")\n");

                        return report;
                    }

                    @Nullable
                    @Override
                    protected CharSequence collectContextualData() throws IOException {
                        return contextualData;
                    }

                    @Nullable
                    @Override
                    protected CharSequence collectWalletDump() throws IOException {
                        final Wallet wallet = viewModel.wallet.getValue();
                        return null!=wallet ? wallet.toString(false, false, null, true, true, null): null;
                    }
                };
                reporter.run();
                sent = true;
            }); // end  ClickListener
        }); // end  ShowListener
        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
//        if (sent)
        CrashReporter.deleteSaveCrashTrace();

        super.onDismiss(dialog);
    }
}
