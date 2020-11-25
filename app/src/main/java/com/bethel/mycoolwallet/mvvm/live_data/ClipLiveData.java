package com.bethel.mycoolwallet.mvvm.live_data;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.bethel.mycoolwallet.utils.Constants;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

public class ClipLiveData extends LiveData<ClipData> {
    private final ClipboardManager clipboardManager;

    public ClipLiveData(Application application) {
        clipboardManager = (ClipboardManager) application.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    protected void onActive() {
        clipboardManager.addPrimaryClipChangedListener(listener);
        listener.onPrimaryClipChanged();
    }

    @Override
    protected void onInactive() {
        clipboardManager.removePrimaryClipChangedListener(listener);
    }
    ClipboardManager.OnPrimaryClipChangedListener listener = new ClipboardManager.OnPrimaryClipChangedListener() {
        @Override
        public void onPrimaryClipChanged() {
            setValue(clipboardManager.getPrimaryClip());
        }
    };


    public Address getAddressFromPrimaryClip() {
        final ClipData clip = getValue();
        if (clip == null)
            return null;
        final ClipDescription clipDescription = clip.getDescription();

        if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            final CharSequence clipText = clip.getItemAt(0).getText();
            if (clipText == null)
                return null;

            try {
                return Address.fromString(Constants.NETWORK_PARAMETERS, clipText.toString().trim());
            } catch (final AddressFormatException x) {
                return null;
            }
        } else if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)) {
            final Uri clipUri = clip.getItemAt(0).getUri();
            if (clipUri == null)
                return null;
            try {
                return new BitcoinURI(clipUri.toString()).getAddress();
            } catch (final BitcoinURIParseException x) {
                return null;
            }
        } else {
            return null;
        }
    }

}
