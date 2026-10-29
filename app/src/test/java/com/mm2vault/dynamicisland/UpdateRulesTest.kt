package com.mm2vault.dynamicisland

import com.mm2vault.dynamicisland.update.UpdateRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRulesTest {
    @Test fun parsesBuildTags() {
        assertEquals(19, UpdateRules.buildOf("build-19"))
        assertNull(UpdateRules.buildOf("v1.2"))
        assertNull(UpdateRules.buildOf("build-x"))
        assertNull(UpdateRules.buildOf(null))
    }

    @Test fun comparesVersions() {
        assertTrue(UpdateRules.isNewer(20, 19))
        assertFalse(UpdateRules.isNewer(19, 19))
        assertFalse(UpdateRules.isNewer(18, 19))
    }

    @Test fun readsChecksumLine() {
        val h = "21d4e1fd6b27c31679d4b248536526d874a0c5a6eef113eb937fe6ae71e36d9f"
        val other = "a".repeat(64)
        val sums = "$other  out/Dynamic-Island.apk\n$h  out/Dynamic-Island-Full.apk\n"
        assertEquals(h, UpdateRules.expectedHash(sums, "Dynamic-Island-Full.apk"))
        assertEquals(other, UpdateRules.expectedHash(sums, "Dynamic-Island.apk"))
        assertNull(UpdateRules.expectedHash("not a hash  Dynamic-Island-Full.apk", "Dynamic-Island-Full.apk"))
        assertNull(UpdateRules.expectedHash("", "Dynamic-Island-Full.apk"))
    }
}
