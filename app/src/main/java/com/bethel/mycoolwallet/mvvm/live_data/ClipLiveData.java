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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClipLiveData extends LiveData<ClipData> {
    private final ClipboardManager clipboardManager;

    private static final Logger log = LoggerFactory.getLogger(ClipLiveData.class);

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

    @Override
    public void setValue(ClipData value) {
        super.setValue(value);
    }

    public Address getAddressFromPrimaryClip() {
        final ClipData clip = null!= getValue()? getValue() : clipboardManager.getPrimaryClip();
        log.info("ClipData:  {}", clip);
        if (clip == null)
            return null;
        final ClipDescription clipDescription = clip.getDescription();

        if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
            final CharSequence clipText = clip.getItemAt(0).getText();
            log.info("clipText:  {}", clipText);
            if (clipText == null)
                return null;

            try {
                return Address.fromString(Constants.NETWORK_PARAMETERS, clipText.toString().trim());
            } catch (final AddressFormatException x) {
                log.error("AddressFormatException", x);
                return null;
            }
        } else if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)) {
            final Uri clipUri = clip.getItemAt(0).getUri();
            log.info("clipUri:  {}", clipUri);
            if (clipUri == null)
                return null;
            try {
                return new BitcoinURI(clipUri.toString()).getAddress();
            } catch (final BitcoinURIParseException x) {
                log.info("BitcoinURIParseException", x);
                return null;
            }
        } else {
            //  MimeType: text/html
// clip Item: ClipData.Item { H:tb1qz6a8v2kh5fmusm0vk8hae2f00ew0zuyem0nyck<!--/data/user/0/com.samsung.android.app.notes/files/clipdata/clipdata_201125_152753_601.sdoc--> }

            if (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                String html = clip.getItemAt(0).getHtmlText();
                log.info("text/html:  html {},\n text {}", html, clip.getItemAt(0).getText());
            }
            final int count = clipDescription.getMimeTypeCount();
            if (0< count)
            for (int i = 0; i < count; i++) {
                log.info("MimeType: {}", clipDescription.getMimeType(i));
            }

           final int size = clip.getItemCount();
            if (size>0)
            for (int i = 0; i < size; i++) {
                log.info("clip Item: {}", clip.getItemAt(i));
            }
            log.info("ItemCount  {},  MimeTypeCount  {}",size, count);
            return null;
        }
    }

}
