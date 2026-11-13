package dev.jketterer.leaflog.presentation.ui.components.common

import dev.jketterer.leaflog.domain.models.WaterType
import leaf_log.shared.generated.resources.Res
import leaf_log.shared.generated.resources.water_type_alkaline
import leaf_log.shared.generated.resources.water_type_alkaline_name
import leaf_log.shared.generated.resources.water_type_distilled
import leaf_log.shared.generated.resources.water_type_distilled_name
import leaf_log.shared.generated.resources.water_type_filtered
import leaf_log.shared.generated.resources.water_type_filtered_name
import leaf_log.shared.generated.resources.water_type_mineral
import leaf_log.shared.generated.resources.water_type_mineral_name
import leaf_log.shared.generated.resources.water_type_other
import leaf_log.shared.generated.resources.water_type_other_name
import leaf_log.shared.generated.resources.water_type_spring
import leaf_log.shared.generated.resources.water_type_spring_name
import leaf_log.shared.generated.resources.water_type_tap
import leaf_log.shared.generated.resources.water_type_tap_name
import org.jetbrains.compose.resources.StringResource

/** "Spring", for where a label already says water type: a chip, a labeled row. */
val WaterType.shortLabel: StringResource
    get() = when (this) {
        WaterType.FILTERED -> Res.string.water_type_filtered
        WaterType.TAP -> Res.string.water_type_tap
        WaterType.SPRING -> Res.string.water_type_spring
        WaterType.DISTILLED -> Res.string.water_type_distilled
        WaterType.MINERAL -> Res.string.water_type_mineral
        WaterType.ALKALINE -> Res.string.water_type_alkaline
        WaterType.OTHER -> Res.string.water_type_other
    }

/** "Spring water", for where it stands alone beside an icon. */
val WaterType.label: StringResource
    get() = when (this) {
        WaterType.FILTERED -> Res.string.water_type_filtered_name
        WaterType.TAP -> Res.string.water_type_tap_name
        WaterType.SPRING -> Res.string.water_type_spring_name
        WaterType.DISTILLED -> Res.string.water_type_distilled_name
        WaterType.MINERAL -> Res.string.water_type_mineral_name
        WaterType.ALKALINE -> Res.string.water_type_alkaline_name
        WaterType.OTHER -> Res.string.water_type_other_name
    }
