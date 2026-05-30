package com.haoze.keynote.ui.theme

import org.junit.Test
import org.junit.Assert.*

class AppColorsTest {
    
    @Test
    fun `light theme should have correct primary color`() {
        val colors = AppColorPalette.Light
        assertEquals(0xFF0593FF.toInt(), colors.primary.hashCode())
    }
    
    @Test
    fun `dark theme should have correct primary color`() {
        val colors = AppColorPalette.Dark
        assertEquals(0xFF9ECAFF.toInt(), colors.primary.hashCode())
    }
    
    @Test
    fun `light and dark themes should have different colors`() {
        val light = AppColorPalette.Light
        val dark = AppColorPalette.Dark
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.surface, dark.surface)
    }
    
    @Test
    fun `chart colors should have 5 items`() {
        val light = AppColorPalette.Light
        assertEquals(5, light.chartColors.size)
    }
}