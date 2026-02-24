package dev.jketterer.leaflog.presentation.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val navSerializationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NavRoute.HomeRoute::class, NavRoute.HomeRoute.serializer())
            subclass(NavRoute.CollectionRoute::class, NavRoute.CollectionRoute.serializer())
            subclass(NavRoute.HistoryRoute::class, NavRoute.HistoryRoute.serializer())
            subclass(NavRoute.MoreRoute::class, NavRoute.MoreRoute.serializer())
            subclass(NavRoute.AnalyticsRoute::class, NavRoute.AnalyticsRoute.serializer())
            subclass(NavRoute.SettingsRoute::class, NavRoute.SettingsRoute.serializer())

            subclass(NavRoute.LogTeaRoute::class, NavRoute.LogTeaRoute.serializer())
            subclass(NavRoute.TeaDetailsRoute::class, NavRoute.TeaDetailsRoute.serializer())
            subclass(NavRoute.EditTeaRoute::class, NavRoute.EditTeaRoute.serializer())
            subclass(NavRoute.SessionDetailsRoute::class, NavRoute.SessionDetailsRoute.serializer())
            subclass(NavRoute.TimerRoute::class, NavRoute.TimerRoute.serializer())
            subclass(NavRoute.QuickTimerRoute::class, NavRoute.QuickTimerRoute.serializer())
            subclass(NavRoute.EditSessionRoute::class, NavRoute.EditSessionRoute.serializer())
            subclass(NavRoute.VesselListRoute::class, NavRoute.VesselListRoute.serializer())
            subclass(NavRoute.VesselDetailRoute::class, NavRoute.VesselDetailRoute.serializer())
            subclass(NavRoute.EditVesselRoute::class, NavRoute.EditVesselRoute.serializer())
            subclass(NavRoute.TeaTypeListRoute::class, NavRoute.TeaTypeListRoute.serializer())
            subclass(NavRoute.EditTeaTypeRoute::class, NavRoute.EditTeaTypeRoute.serializer())
            subclass(NavRoute.CreateBrewingConfigurationRoute::class, NavRoute.CreateBrewingConfigurationRoute.serializer())
        }
    }
}
