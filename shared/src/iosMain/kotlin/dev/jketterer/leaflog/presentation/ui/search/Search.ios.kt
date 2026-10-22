package dev.jketterer.leaflog.presentation.ui.search

import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSString
import platform.Foundation.stringByFoldingWithOptions

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun String.foldedForSearch(): String =
    (this as NSString).stringByFoldingWithOptions(
        NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch,
        locale = null,
    )
