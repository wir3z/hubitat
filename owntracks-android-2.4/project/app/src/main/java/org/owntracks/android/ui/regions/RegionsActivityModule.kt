package org.owntracks.android.ui.regions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class RegionsActivityModule {
    @Binds
    abstract fun bindViewModel(viewModel: RegionsViewModel): RegionsMvvm.ViewModel<RegionsMvvm.View>
}
