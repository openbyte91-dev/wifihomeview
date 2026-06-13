package com.openbyt91dev.wifihomeview.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.openbyt91dev.wifihomeview.data.repository.ScannerRepositoryImpl
import com.openbyt91dev.wifihomeview.domain.repository.ScannerRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindScannerRepository(
        impl: ScannerRepositoryImpl
    ): ScannerRepository
}
