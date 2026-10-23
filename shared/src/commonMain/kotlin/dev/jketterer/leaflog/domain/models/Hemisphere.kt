package dev.jketterer.leaflog.domain.models

/** Which half of the world the seasons are named for. */
enum class Hemisphere {
    NORTHERN,
    SOUTHERN,
    ;

    companion object {
        /**
         * The hemisphere most of [region]'s people live in, from its ISO 3166 code. An unknown or
         * empty region is northern, where most of the world lives.
         */
        fun forRegion(region: String): Hemisphere =
            if (region.uppercase() in SOUTHERN_REGIONS) SOUTHERN else NORTHERN

        private val SOUTHERN_REGIONS = setOf(
            "AO", "AR", "AU", "BI", "BO", "BR", "BW", "CK", "CL", "FJ", "FK", "ID", "LS", "MG",
            "MU", "MW", "MZ", "NA", "NC", "NZ", "PE", "PF", "PG", "PY", "RE", "RW", "SB", "SZ",
            "TL", "TO", "TZ", "UY", "VU", "WS", "YT", "ZA", "ZM", "ZW",
        )
    }
}
