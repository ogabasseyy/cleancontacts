package com.ogabassey.contactscleaner.di

import android.content.Context
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.parser.ContactImportParser
import com.ogabassey.contactscleaner.data.repository.ContactRepositoryImpl
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.room.Room
import com.ogabassey.contactscleaner.data.db.ContactDatabase
import com.ogabassey.contactscleaner.data.db.dao.ContactDao

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideContactDatabase(
        @ApplicationContext context: Context
    ): ContactDatabase {
        return Room.databaseBuilder(
            context,
            ContactDatabase::class.java,
            "contacts_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideContactDao(database: ContactDatabase): ContactDao {
        return database.contactDao()
    }


    @Provides
    @Singleton
    fun provideContactsProviderSource(
        @ApplicationContext context: Context
    ): ContactsProviderSource {
        return ContactsProviderSource(context.contentResolver)
    }

    @Provides
    @Singleton
    fun provideJunkDetector(): JunkDetector {
        return JunkDetector()
    }

    @Provides
    @Singleton
    fun provideDuplicateDetector(@ApplicationContext context: Context): DuplicateDetector {
        return DuplicateDetector(context)
    }

    @Provides
    @Singleton
    fun provideContactImportParser(): ContactImportParser {
        return ContactImportParser()
    }

    @Provides
    @Singleton
    fun provideBillingRepository(): com.ogabassey.contactscleaner.domain.repository.BillingRepository {
        return com.ogabassey.contactscleaner.data.repository.RevenueCatBillingRepository()
    }

    @Provides
    @Singleton
    fun provideFileService(
        @ApplicationContext context: Context
    ): com.ogabassey.contactscleaner.domain.repository.FileService {
        return com.ogabassey.contactscleaner.data.repository.FileServiceImpl(context)
    }

    @Provides
    @Singleton
    fun provideContactRepository(
        contactDao: ContactDao,
        contactsProviderSource: ContactsProviderSource,
        junkDetector: JunkDetector,
        duplicateDetector: DuplicateDetector,
        formatDetector: com.ogabassey.contactscleaner.data.detector.FormatDetector,
        scanResultProvider: com.ogabassey.contactscleaner.data.util.ScanResultProvider
    ): ContactRepository {
        return ContactRepositoryImpl(
            contactDao = contactDao,
            contactsProviderSource = contactsProviderSource,
            junkDetector = junkDetector,
            duplicateDetector = duplicateDetector,
            formatDetector = formatDetector,
            scanResultProvider = scanResultProvider
        )
    }

    @Provides
    fun provideUndoDao(database: ContactDatabase): com.ogabassey.contactscleaner.data.db.dao.UndoDao {
        return database.undoDao()
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        undoDao: com.ogabassey.contactscleaner.data.db.dao.UndoDao
    ): com.ogabassey.contactscleaner.domain.repository.BackupRepository {
        return com.ogabassey.contactscleaner.data.repository.BackupRepositoryImpl(
            undoDao = undoDao
        )
    }

}
