package com.bethel.mycoolwallet.activity;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Crypto;
import com.bethel.mycoolwallet.utils.WalletUtils;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;
import com.xuexiang.xui.widget.toast.XToast;

import java.io.File;

public class WalletFilePickerActivity extends FilePickerActivity {
    @Override
    public void onFileClicked(File clickedFile) {
        // check if wallet file
        if (clickedFile.isFile() && !isWalletFile(clickedFile)){
            XToast.info(this, R.string.wrong_wallet_file_alarm).show();
            return;
        }

        super.onFileClicked(clickedFile);
    }

    private boolean isWalletFile(File file) {
        return file.isFile() && (WalletUtils.BACKUP_FILE_FILTER.accept(file)
                || Crypto.OPENSSL_FILE_FILTER.accept(file));
    }

}
