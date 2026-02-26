/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.eclipse.theia.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LabelsUtil}.
 */
class LabelsUtilTests {

    @Test
    void truncateLabelValue_shortValue_unchanged() {
        String value = "short-label";
        String result = LabelsUtil.truncateLabelValue(value);
        assertEquals("short-label", result);
    }

    @Test
    void truncateLabelValue_exactly63Chars_unchanged() {
        // 63 characters exactly
        String value = "a".repeat(63);
        String result = LabelsUtil.truncateLabelValue(value);
        assertEquals(63, result.length());
        assertEquals(value, result);
    }

    @Test
    void truncateLabelValue_over63Chars_truncatesCleanly() {
        // 70 alphanumeric characters - truncation at 63 ends with alphanumeric
        String value = "a".repeat(70);
        String result = LabelsUtil.truncateLabelValue(value);
        assertEquals(63, result.length());
        assertTrue(Character.isLetterOrDigit(result.charAt(result.length() - 1)));
    }

    @Test
    void truncateLabelValue_over63Chars_trailingDashStripped() {
        // Simulate a value that when truncated to 63 chars ends with '-'
        // "abcdefghij" (10) + "-" (1) + "a" * 52 = 63 chars, then add more chars
        // We need position 62 (0-indexed) to be a '-'
        // Build a string where char at index 62 is '-'
        String value = "a".repeat(62) + "-" + "bcdef";  // 67 chars total
        String result = LabelsUtil.truncateLabelValue(value);
        // Should strip the trailing '-', resulting in 62 chars
        assertEquals(62, result.length());
        assertTrue(Character.isLetterOrDigit(result.charAt(result.length() - 1)));
        assertEquals("a".repeat(62), result);
    }

    @Test
    void truncateLabelValue_over63Chars_multipleTrailingNonAlphanumericStripped() {
        // Value where truncation to 63 would end with multiple non-alphanumeric chars
        String value = "a".repeat(60) + "---" + "bcdef";  // 68 chars total
        String result = LabelsUtil.truncateLabelValue(value);
        // Should strip all trailing dashes, resulting in 60 chars
        assertEquals(60, result.length());
        assertTrue(Character.isLetterOrDigit(result.charAt(result.length() - 1)));
        assertEquals("a".repeat(60), result);
    }

    @Test
    void truncateLabelValue_realisticSessionName_trailingDashStripped() {
        // workspace name: "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo-theia-cloud-i"
        // session name: workspace + "-session" = 70 chars
        String sessionName = "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo-theia-cloud-i-session";
        assertEquals(70, sessionName.length());

        String result = LabelsUtil.truncateLabelValue(sessionName);

        // The naive substring(0, 63) would give "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo-theia-cloud-i-"
        // which ends with '-'. 
        assertTrue(result.length() <= 63);
        assertTrue(Character.isLetterOrDigit(result.charAt(result.length() - 1)));
        assertEquals("ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo-theia-cloud-i", result);
    }

    @Test
    void truncateLabelValue_emptyString_unchanged() {
        String result = LabelsUtil.truncateLabelValue("");
        assertEquals("", result);
    }

    @Test
    void truncateLabelValue_singleChar_unchanged() {
        String result = LabelsUtil.truncateLabelValue("a");
        assertEquals("a", result);
    }
}
