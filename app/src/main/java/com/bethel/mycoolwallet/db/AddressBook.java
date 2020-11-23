package com.bethel.mycoolwallet.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "address_book")
public class AddressBook {
    @PrimaryKey
    @ColumnInfo(name = "address")
    private String address;
    @ColumnInfo(name = "label")
    private String label;

    public AddressBook(String address, String label) {
        this.address = address;
        this.label = label;
    }

    public String getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public static Map<String , AddressBook> asMap(List<AddressBook> list) {
        if (null==list) return null;

        final  Map<String , AddressBook> map = new HashMap<>();
        for (AddressBook book: list) {
            map.put(book.getAddress(), book);
        }

        return map;
    }
}
