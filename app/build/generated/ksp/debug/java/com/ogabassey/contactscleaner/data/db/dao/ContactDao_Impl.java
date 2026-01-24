package com.ogabassey.contactscleaner.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.paging.PagingSource;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.paging.LimitOffsetPagingSource;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.ogabassey.contactscleaner.data.db.entity.LocalContact;
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary;
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ContactDao_Impl implements ContactDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalContact> __insertionAdapterOfLocalContact;

  private final SharedSQLiteStatement __preparedStmtOfMarkDuplicateNumbers;

  private final SharedSQLiteStatement __preparedStmtOfMarkDuplicateEmails;

  private final SharedSQLiteStatement __preparedStmtOfMarkDuplicateNames;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ContactDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalContact = new EntityInsertionAdapter<LocalContact>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `contacts` (`id`,`display_name`,`normalized_number`,`raw_numbers`,`raw_emails`,`is_whatsapp`,`is_telegram`,`account_type`,`account_name`,`is_junk`,`junk_type`,`duplicate_type`,`is_format_issue`,`detected_region`,`is_sensitive`,`sensitive_description`,`last_synced`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalContact entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDisplayName());
        }
        if (entity.getNormalizedNumber() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getNormalizedNumber());
        }
        statement.bindString(4, entity.getRawNumbers());
        statement.bindString(5, entity.getRawEmails());
        final int _tmp = entity.isWhatsApp() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isTelegram() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getAccountType() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getAccountType());
        }
        if (entity.getAccountName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAccountName());
        }
        final int _tmp_2 = entity.isJunk() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
        if (entity.getJunkType() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getJunkType());
        }
        if (entity.getDuplicateType() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDuplicateType());
        }
        final int _tmp_3 = entity.isFormatIssue() ? 1 : 0;
        statement.bindLong(13, _tmp_3);
        if (entity.getDetectedRegion() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getDetectedRegion());
        }
        final int _tmp_4 = entity.isSensitive() ? 1 : 0;
        statement.bindLong(15, _tmp_4);
        if (entity.getSensitiveDescription() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getSensitiveDescription());
        }
        statement.bindLong(17, entity.getLastSynced());
      }
    };
    this.__preparedStmtOfMarkDuplicateNumbers = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET duplicate_type = 'NUMBER_MATCH' WHERE normalized_number IN (SELECT normalized_number FROM contacts WHERE normalized_number IS NOT NULL AND normalized_number != '' GROUP BY normalized_number HAVING COUNT(*) > 1)";
        return _query;
      }
    };
    this.__preparedStmtOfMarkDuplicateEmails = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET duplicate_type = 'EMAIL_MATCH' WHERE raw_emails IN (SELECT raw_emails FROM contacts WHERE raw_emails IS NOT NULL AND raw_emails != '' GROUP BY raw_emails HAVING COUNT(*) > 1) AND duplicate_type IS NULL";
        return _query;
      }
    };
    this.__preparedStmtOfMarkDuplicateNames = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET duplicate_type = 'NAME_MATCH' WHERE display_name IN (SELECT display_name FROM contacts WHERE display_name IS NOT NULL AND display_name != '' GROUP BY display_name HAVING COUNT(*) > 1) AND duplicate_type IS NULL";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts";
        return _query;
      }
    };
  }

  @Override
  public Object insertContacts(final List<LocalContact> contacts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalContact.insert(contacts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public void markDuplicateNumbers() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDuplicateNumbers.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfMarkDuplicateNumbers.release(_stmt);
    }
  }

  @Override
  public void markDuplicateEmails() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDuplicateEmails.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfMarkDuplicateEmails.release(_stmt);
    }
  }

  @Override
  public void markDuplicateNames() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDuplicateNames.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfMarkDuplicateNames.release(_stmt);
    }
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public PagingSource<Integer, LocalContact> getAllContactsPaged() {
    final String _sql = "SELECT * FROM contacts ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getWhatsAppContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_whatsapp = 1 ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getNonWhatsAppContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_whatsapp = 0 ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getJunkContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_junk = 1 ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getNoNameContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'NO_NAME' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getNoNumberContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'NO_NUMBER' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getDuplicateEmailContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getDuplicateNumberContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getDuplicateNameContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE duplicate_type = 'NAME_MATCH' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getSimilarNameContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE duplicate_type = 'SIMILAR_NAME_MATCH' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getSensitiveContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_sensitive = 1 ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public int countTotal() {
    final String _sql = "SELECT COUNT(*) FROM contacts";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countWhatsApp() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE is_whatsapp = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countTelegram() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE is_telegram = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public PagingSource<Integer, LocalContact> getTelegramContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_telegram = 1 ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public int countJunk() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE is_junk = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countDuplicates() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE duplicate_type IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countNoName() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NAME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countNoNumber() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countInvalidChar() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'INVALID_CHAR'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countLongNumber() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'LONG_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countShortNumber() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'SHORT_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countRepetitiveNumber() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countSymbolName() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE junk_type = 'SYMBOL_NAME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countAccounts() {
    final String _sql = "SELECT COUNT(DISTINCT account_type) FROM contacts WHERE account_type IS NOT NULL AND account_type != ''";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countDuplicateNumbers() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NUMBER_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countDuplicateEmails() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'EMAIL_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int countDuplicateNames() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NAME_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public PagingSource<Integer, LocalContact> getInvalidCharContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'INVALID_CHAR' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getLongNumberContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'LONG_NUMBER' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getShortNumberContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'SHORT_NUMBER' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getRepetitiveNumberContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getSymbolNameContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE junk_type = 'SYMBOL_NAME' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getAccountContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE account_type IS NOT NULL AND account_type != '' ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public PagingSource<Integer, LocalContact> getFormatIssueContactsPaged() {
    final String _sql = "SELECT * FROM contacts WHERE is_format_issue = 1 ORDER BY detected_region ASC, normalized_number ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return new LimitOffsetPagingSource<LocalContact>(_statement, __db, "contacts") {
      @Override
      @NonNull
      protected List<LocalContact> convertRows(@NonNull final Cursor cursor) {
        final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(cursor, "id");
        final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(cursor, "display_name");
        final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(cursor, "normalized_number");
        final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(cursor, "raw_numbers");
        final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(cursor, "raw_emails");
        final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(cursor, "is_whatsapp");
        final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(cursor, "is_telegram");
        final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(cursor, "account_type");
        final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(cursor, "account_name");
        final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(cursor, "is_junk");
        final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(cursor, "junk_type");
        final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(cursor, "duplicate_type");
        final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(cursor, "is_format_issue");
        final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(cursor, "detected_region");
        final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(cursor, "is_sensitive");
        final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(cursor, "sensitive_description");
        final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(cursor, "last_synced");
        final List<LocalContact> _result = new ArrayList<LocalContact>(cursor.getCount());
        while (cursor.moveToNext()) {
          final LocalContact _item;
          final long _tmpId;
          _tmpId = cursor.getLong(_cursorIndexOfId);
          final String _tmpDisplayName;
          if (cursor.isNull(_cursorIndexOfDisplayName)) {
            _tmpDisplayName = null;
          } else {
            _tmpDisplayName = cursor.getString(_cursorIndexOfDisplayName);
          }
          final String _tmpNormalizedNumber;
          if (cursor.isNull(_cursorIndexOfNormalizedNumber)) {
            _tmpNormalizedNumber = null;
          } else {
            _tmpNormalizedNumber = cursor.getString(_cursorIndexOfNormalizedNumber);
          }
          final String _tmpRawNumbers;
          _tmpRawNumbers = cursor.getString(_cursorIndexOfRawNumbers);
          final String _tmpRawEmails;
          _tmpRawEmails = cursor.getString(_cursorIndexOfRawEmails);
          final boolean _tmpIsWhatsApp;
          final int _tmp;
          _tmp = cursor.getInt(_cursorIndexOfIsWhatsApp);
          _tmpIsWhatsApp = _tmp != 0;
          final boolean _tmpIsTelegram;
          final int _tmp_1;
          _tmp_1 = cursor.getInt(_cursorIndexOfIsTelegram);
          _tmpIsTelegram = _tmp_1 != 0;
          final String _tmpAccountType;
          if (cursor.isNull(_cursorIndexOfAccountType)) {
            _tmpAccountType = null;
          } else {
            _tmpAccountType = cursor.getString(_cursorIndexOfAccountType);
          }
          final String _tmpAccountName;
          if (cursor.isNull(_cursorIndexOfAccountName)) {
            _tmpAccountName = null;
          } else {
            _tmpAccountName = cursor.getString(_cursorIndexOfAccountName);
          }
          final boolean _tmpIsJunk;
          final int _tmp_2;
          _tmp_2 = cursor.getInt(_cursorIndexOfIsJunk);
          _tmpIsJunk = _tmp_2 != 0;
          final String _tmpJunkType;
          if (cursor.isNull(_cursorIndexOfJunkType)) {
            _tmpJunkType = null;
          } else {
            _tmpJunkType = cursor.getString(_cursorIndexOfJunkType);
          }
          final String _tmpDuplicateType;
          if (cursor.isNull(_cursorIndexOfDuplicateType)) {
            _tmpDuplicateType = null;
          } else {
            _tmpDuplicateType = cursor.getString(_cursorIndexOfDuplicateType);
          }
          final boolean _tmpIsFormatIssue;
          final int _tmp_3;
          _tmp_3 = cursor.getInt(_cursorIndexOfIsFormatIssue);
          _tmpIsFormatIssue = _tmp_3 != 0;
          final String _tmpDetectedRegion;
          if (cursor.isNull(_cursorIndexOfDetectedRegion)) {
            _tmpDetectedRegion = null;
          } else {
            _tmpDetectedRegion = cursor.getString(_cursorIndexOfDetectedRegion);
          }
          final boolean _tmpIsSensitive;
          final int _tmp_4;
          _tmp_4 = cursor.getInt(_cursorIndexOfIsSensitive);
          _tmpIsSensitive = _tmp_4 != 0;
          final String _tmpSensitiveDescription;
          if (cursor.isNull(_cursorIndexOfSensitiveDescription)) {
            _tmpSensitiveDescription = null;
          } else {
            _tmpSensitiveDescription = cursor.getString(_cursorIndexOfSensitiveDescription);
          }
          final long _tmpLastSynced;
          _tmpLastSynced = cursor.getLong(_cursorIndexOfLastSynced);
          _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
          _result.add(_item);
        }
        return _result;
      }
    };
  }

  @Override
  public int countSensitive() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE is_sensitive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Object getDuplicateNumberGroups(
      final Continuation<? super List<DuplicateGroupSummary>> $completion) {
    final String _sql = "SELECT normalized_number as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' GROUP BY normalized_number HAVING COUNT(*) > 1 ORDER BY count DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DuplicateGroupSummary>>() {
      @Override
      @NonNull
      public List<DuplicateGroupSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupKey = 0;
          final int _cursorIndexOfCount = 1;
          final int _cursorIndexOfPreviewNames = 2;
          final List<DuplicateGroupSummary> _result = new ArrayList<DuplicateGroupSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DuplicateGroupSummary _item;
            final String _tmpGroupKey;
            _tmpGroupKey = _cursor.getString(_cursorIndexOfGroupKey);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final String _tmpPreviewNames;
            _tmpPreviewNames = _cursor.getString(_cursorIndexOfPreviewNames);
            _item = new DuplicateGroupSummary(_tmpGroupKey,_tmpCount,_tmpPreviewNames);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDuplicateEmailGroups(
      final Continuation<? super List<DuplicateGroupSummary>> $completion) {
    final String _sql = "SELECT raw_emails as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' GROUP BY raw_emails HAVING COUNT(*) > 1 ORDER BY count DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DuplicateGroupSummary>>() {
      @Override
      @NonNull
      public List<DuplicateGroupSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupKey = 0;
          final int _cursorIndexOfCount = 1;
          final int _cursorIndexOfPreviewNames = 2;
          final List<DuplicateGroupSummary> _result = new ArrayList<DuplicateGroupSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DuplicateGroupSummary _item;
            final String _tmpGroupKey;
            _tmpGroupKey = _cursor.getString(_cursorIndexOfGroupKey);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final String _tmpPreviewNames;
            _tmpPreviewNames = _cursor.getString(_cursorIndexOfPreviewNames);
            _item = new DuplicateGroupSummary(_tmpGroupKey,_tmpCount,_tmpPreviewNames);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDuplicateNameGroups(
      final Continuation<? super List<DuplicateGroupSummary>> $completion) {
    final String _sql = "SELECT display_name as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NAME_MATCH' GROUP BY display_name HAVING COUNT(*) > 1 ORDER BY count DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DuplicateGroupSummary>>() {
      @Override
      @NonNull
      public List<DuplicateGroupSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupKey = 0;
          final int _cursorIndexOfCount = 1;
          final int _cursorIndexOfPreviewNames = 2;
          final List<DuplicateGroupSummary> _result = new ArrayList<DuplicateGroupSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DuplicateGroupSummary _item;
            final String _tmpGroupKey;
            _tmpGroupKey = _cursor.getString(_cursorIndexOfGroupKey);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final String _tmpPreviewNames;
            _tmpPreviewNames = _cursor.getString(_cursorIndexOfPreviewNames);
            _item = new DuplicateGroupSummary(_tmpGroupKey,_tmpCount,_tmpPreviewNames);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAccountGroups(
      final Continuation<? super List<AccountGroupSummary>> $completion) {
    final String _sql = "SELECT account_type as accountType, account_name as accountName, COUNT(*) as count FROM contacts WHERE account_type IS NOT NULL AND account_type != '' GROUP BY account_type, account_name ORDER BY count DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AccountGroupSummary>>() {
      @Override
      @NonNull
      public List<AccountGroupSummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountType = 0;
          final int _cursorIndexOfAccountName = 1;
          final int _cursorIndexOfCount = 2;
          final List<AccountGroupSummary> _result = new ArrayList<AccountGroupSummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AccountGroupSummary _item;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new AccountGroupSummary(_tmpAccountType,_tmpAccountName,_tmpCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getContactsByNumberKey(final String key,
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE normalized_number = ? AND duplicate_type = 'NUMBER_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getContactsByEmailKey(final String key,
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE raw_emails = ? AND duplicate_type = 'EMAIL_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getContactsByNameKey(final String key,
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE display_name = ? AND duplicate_type = 'NAME_MATCH'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getNonWhatsAppContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE is_whatsapp = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getJunkContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE is_junk = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getNoNameContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'NO_NAME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getNoNumberContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'NO_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getInvalidCharContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'INVALID_CHAR'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLongNumberContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'LONG_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getShortNumberContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'SHORT_NUMBER'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRepetitiveNumberContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSymbolNameContactIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE junk_type = 'SYMBOL_NAME'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public int countFormatIssues() {
    final String _sql = "SELECT COUNT(*) FROM contacts WHERE is_format_issue = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Object getFormatIssueIds(final Continuation<? super List<Long>> $completion) {
    final String _sql = "SELECT id FROM contacts WHERE is_format_issue = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Long> _result = new ArrayList<Long>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Long _item;
            _item = _cursor.getLong(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getFormatIssueContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE is_format_issue = 1 ORDER BY detected_region ASC, normalized_number ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getContactsByIds(final List<Long> ids,
      final Continuation<? super List<LocalContact>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM contacts WHERE id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (long _item : ids) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item_1 = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getFormatIssueContactsByIds(final List<Long> ids,
      final Continuation<? super List<LocalContact>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM contacts WHERE id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") AND is_format_issue = 1");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (long _item : ids) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item_1 = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWhatsAppContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE is_whatsapp = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getNonWhatsAppContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE is_whatsapp = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getJunkContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE is_junk = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDuplicateContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE duplicate_type IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSensitiveContactsSnapshot(
      final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE is_sensitive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllContacts(final Continuation<? super List<LocalContact>> $completion) {
    final String _sql = "SELECT * FROM contacts ORDER BY display_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalContact>>() {
      @Override
      @NonNull
      public List<LocalContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "display_name");
          final int _cursorIndexOfNormalizedNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "normalized_number");
          final int _cursorIndexOfRawNumbers = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_numbers");
          final int _cursorIndexOfRawEmails = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_emails");
          final int _cursorIndexOfIsWhatsApp = CursorUtil.getColumnIndexOrThrow(_cursor, "is_whatsapp");
          final int _cursorIndexOfIsTelegram = CursorUtil.getColumnIndexOrThrow(_cursor, "is_telegram");
          final int _cursorIndexOfAccountType = CursorUtil.getColumnIndexOrThrow(_cursor, "account_type");
          final int _cursorIndexOfAccountName = CursorUtil.getColumnIndexOrThrow(_cursor, "account_name");
          final int _cursorIndexOfIsJunk = CursorUtil.getColumnIndexOrThrow(_cursor, "is_junk");
          final int _cursorIndexOfJunkType = CursorUtil.getColumnIndexOrThrow(_cursor, "junk_type");
          final int _cursorIndexOfDuplicateType = CursorUtil.getColumnIndexOrThrow(_cursor, "duplicate_type");
          final int _cursorIndexOfIsFormatIssue = CursorUtil.getColumnIndexOrThrow(_cursor, "is_format_issue");
          final int _cursorIndexOfDetectedRegion = CursorUtil.getColumnIndexOrThrow(_cursor, "detected_region");
          final int _cursorIndexOfIsSensitive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_sensitive");
          final int _cursorIndexOfSensitiveDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "sensitive_description");
          final int _cursorIndexOfLastSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "last_synced");
          final List<LocalContact> _result = new ArrayList<LocalContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpNormalizedNumber;
            if (_cursor.isNull(_cursorIndexOfNormalizedNumber)) {
              _tmpNormalizedNumber = null;
            } else {
              _tmpNormalizedNumber = _cursor.getString(_cursorIndexOfNormalizedNumber);
            }
            final String _tmpRawNumbers;
            _tmpRawNumbers = _cursor.getString(_cursorIndexOfRawNumbers);
            final String _tmpRawEmails;
            _tmpRawEmails = _cursor.getString(_cursorIndexOfRawEmails);
            final boolean _tmpIsWhatsApp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWhatsApp);
            _tmpIsWhatsApp = _tmp != 0;
            final boolean _tmpIsTelegram;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTelegram);
            _tmpIsTelegram = _tmp_1 != 0;
            final String _tmpAccountType;
            if (_cursor.isNull(_cursorIndexOfAccountType)) {
              _tmpAccountType = null;
            } else {
              _tmpAccountType = _cursor.getString(_cursorIndexOfAccountType);
            }
            final String _tmpAccountName;
            if (_cursor.isNull(_cursorIndexOfAccountName)) {
              _tmpAccountName = null;
            } else {
              _tmpAccountName = _cursor.getString(_cursorIndexOfAccountName);
            }
            final boolean _tmpIsJunk;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsJunk);
            _tmpIsJunk = _tmp_2 != 0;
            final String _tmpJunkType;
            if (_cursor.isNull(_cursorIndexOfJunkType)) {
              _tmpJunkType = null;
            } else {
              _tmpJunkType = _cursor.getString(_cursorIndexOfJunkType);
            }
            final String _tmpDuplicateType;
            if (_cursor.isNull(_cursorIndexOfDuplicateType)) {
              _tmpDuplicateType = null;
            } else {
              _tmpDuplicateType = _cursor.getString(_cursorIndexOfDuplicateType);
            }
            final boolean _tmpIsFormatIssue;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsFormatIssue);
            _tmpIsFormatIssue = _tmp_3 != 0;
            final String _tmpDetectedRegion;
            if (_cursor.isNull(_cursorIndexOfDetectedRegion)) {
              _tmpDetectedRegion = null;
            } else {
              _tmpDetectedRegion = _cursor.getString(_cursorIndexOfDetectedRegion);
            }
            final boolean _tmpIsSensitive;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsSensitive);
            _tmpIsSensitive = _tmp_4 != 0;
            final String _tmpSensitiveDescription;
            if (_cursor.isNull(_cursorIndexOfSensitiveDescription)) {
              _tmpSensitiveDescription = null;
            } else {
              _tmpSensitiveDescription = _cursor.getString(_cursorIndexOfSensitiveDescription);
            }
            final long _tmpLastSynced;
            _tmpLastSynced = _cursor.getLong(_cursorIndexOfLastSynced);
            _item = new LocalContact(_tmpId,_tmpDisplayName,_tmpNormalizedNumber,_tmpRawNumbers,_tmpRawEmails,_tmpIsWhatsApp,_tmpIsTelegram,_tmpAccountType,_tmpAccountName,_tmpIsJunk,_tmpJunkType,_tmpDuplicateType,_tmpIsFormatIssue,_tmpDetectedRegion,_tmpIsSensitive,_tmpSensitiveDescription,_tmpLastSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM contacts";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteContacts(final List<Long> contactIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM contacts WHERE id IN (");
        final int _inputSize = contactIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : contactIds) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
