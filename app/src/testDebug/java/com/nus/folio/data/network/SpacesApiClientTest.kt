package com.nus.folio.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SpacesApiClientTest {

    @Test
    fun `formatUpdatedLabel uses relative buckets`() {
        val now = 1_700_000_000_000L

        assertEquals(
            "Updated just now",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:13:20.000Z", now),
        )
        assertEquals(
            "Updated 5m ago",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:08:20.000Z", now),
        )
        assertEquals(
            "Updated 3h ago",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T19:13:20.000Z", now),
        )
        assertEquals(
            "Updated 2d ago",
            SpacesApiClient.formatUpdatedLabel("2023-11-12T22:13:20.000Z", now),
        )
    }

    @Test
    fun `formatUpdatedLabel parses numeric plus00 offset`() {
        val now = 1_700_000_000_000L

        assertEquals(
            "Updated just now",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:13:20.000+00:00", now),
        )
        assertEquals(
            "Updated 5m ago",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:08:20+00:00", now),
        )
    }

    @Test
    fun `formatUpdatedLabel truncates microsecond timestamps`() {
        val now = 1_700_000_000_000L

        assertEquals(
            "Updated just now",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:13:20.000123Z", now),
        )
        assertEquals(
            "Updated 5m ago",
            SpacesApiClient.formatUpdatedLabel("2023-11-14T22:08:20.000999+00:00", now),
        )
    }

    @Test
    fun `normalizeIsoTimestamp truncates fraction to millis`() {
        assertEquals(
            "2023-11-14T22:13:20.000Z",
            SpacesApiClient.normalizeIsoTimestamp("2023-11-14T22:13:20.000123Z"),
        )
        assertEquals(
            "2023-11-14T22:13:20.123+00:00",
            SpacesApiClient.normalizeIsoTimestamp("2023-11-14T22:13:20.123456789+00:00"),
        )
        assertEquals(
            "2023-11-14T22:13:20.120Z",
            SpacesApiClient.normalizeIsoTimestamp("2023-11-14T22:13:20.12Z"),
        )
    }
}
