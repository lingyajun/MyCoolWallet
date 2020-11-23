package com.bethel.mycoolwallet.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
import java.util.Set;

@Dao
public interface AddressBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(AddressBook book);

    @Query("DELETE FROM address_book WHERE address = :address")
    void delete(String address);

    @Query("SELECT label FROM address_book WHERE address = :address")
    String resolveLabel(String address);

    @Query("SELECT * FROM address_book WHERE address LIKE '%' || :content || '%' OR" +
            " label LIKE '%' || :content || '%' ORDER BY label COLLATE LOCALIZED ASC")
    List<AddressBook> get(String content);

    @Query("SELECT * FROM address_book  ORDER BY label COLLATE LOCALIZED ASC")
    LiveData<List<AddressBook>> getAll();

    @Query("SELECT * FROM address_book WHERE address NOT IN (:addresses)" +
            " ORDER BY label COLLATE LOCALIZED ASC")
    LiveData<List<AddressBook>> getAllExcept(Set<String> addresses);
}
