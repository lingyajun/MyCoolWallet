package com.bethel.mycoolwallet.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;

public class AddressBean {
    public final Address address;
    public final String label;
//    public final String addressStr;

    public AddressBean(Address address,  @Nullable String label) {
        this.address = address;
        this.label = label;
//        this.addressStr = address.toString();
    }

    public AddressBean(final NetworkParameters addressParams, final String address, @Nullable final String label) {
//        this.addressStr = address;
        this.address = Address.fromString(addressParams, address);
        this.label = label;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("{%s} : {%s}", label, address.toString());
    }
}
