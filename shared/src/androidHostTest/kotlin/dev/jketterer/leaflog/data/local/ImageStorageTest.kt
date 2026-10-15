package dev.jketterer.leaflog.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageStorageTest {

    @Test
    fun `a photo within the limit keeps its size`() {
        assertEquals(1600 to 1200, photoTargetSize(1600, 1200))
        assertEquals(2048 to 1536, photoTargetSize(2048, 1536))
    }

    @Test
    fun `a landscape photo shrinks to the limit along its width`() {
        assertEquals(2048 to 1536, photoTargetSize(4032, 3024))
    }

    @Test
    fun `a portrait photo shrinks to the limit along its height`() {
        assertEquals(1536 to 2048, photoTargetSize(3024, 4032))
    }

    @Test
    fun `a sliver never shrinks to nothing`() {
        assertEquals(2048 to 1, photoTargetSize(10000, 2))
    }

    @Test
    fun `a photo near the limit decodes at full size`() {
        assertEquals(1, photoSampleSize(4095, 3000))
    }

    @Test
    fun `a large photo decodes at the coarsest size still above the limit`() {
        assertEquals(2, photoSampleSize(4096, 3072))
        assertEquals(2, photoSampleSize(8160, 6120))
        assertEquals(4, photoSampleSize(8192, 6144))
    }
}
