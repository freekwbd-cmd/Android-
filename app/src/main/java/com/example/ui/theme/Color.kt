package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cyberpunk Void & Dark Canvas
val VoidBlack = Color(0xFF05060A)
val DarkCanvas = Color(0xFF090B14)
val CyberSurface = Color(0xFF101322)
val CyberSurfaceElevated = Color(0xFF161B30)
val CyberSurfaceVariant = Color(0xFF1F2542)
val CyberBorder = Color(0xFF262E52)
val CyberBorderGlowing = Color(0x66FF2A55)

// Accent Neons & Glows
val NeonCrimson = Color(0xFFFF2A55)
val NeonCrimsonGlow = Color(0x33FF2A55)
val NeonViolet = Color(0xFF9D4EDD)
val NeonVioletLight = Color(0xFFC77DFF)
val NeonCyan = Color(0xFF00F0FF)
val NeonCyanGlow = Color(0x3300F0FF)
val NeonEmerald = Color(0xFF00E676)
val NeonEmeraldGlow = Color(0x3300E676)
val NeonAmber = Color(0xFFFFB300)
val NeonAmberGlow = Color(0x33FFB300)
val VibeNeonMagenta = Color(0xFFFF007F)
val VibeNeonElectricBlue = Color(0xFF00D4FF)

// Frosted Glass Screen & Translucent Cyber Acrylic
val GlassCanvas = Color(0xF2070912)
val GlassSurface = Color(0x9911162B)
val GlassSurfaceElevated = Color(0xB3171E3B)
val GlassSurfaceUltra = Color(0x660E1326)
val GlassSurfaceGlow = Color(0x1A00F0FF)
val GlassHighlight = Color(0x26FFFFFF)

// Glass Borders with Luminous Specular Tints
val GlassBorderCyan = Color(0x7700F0FF)
val GlassBorderMagenta = Color(0x77FF2A55)
val GlassBorderViolet = Color(0x779D4EDD)
val GlassBorderEmerald = Color(0x7700E676)
val GlassBorderAmber = Color(0x77FFB300)

// Text Colors
val TextPrimary = Color(0xFFF3F5FF)
val TextSecondary = Color(0xFFA5ACD4)
val TextMuted = Color(0xFF6B739B)
val TextCyan = Color(0xFF70F3FF)
val TextPink = Color(0xFFFF85A1)
val TextEmerald = Color(0xFF69F0AE)

// Cyber Vibe Dynamic Gradients
object VibeGradients {
    val CyanToMagenta = Brush.linearGradient(listOf(NeonCyan, VibeNeonMagenta))
    val VioletToCyan = Brush.linearGradient(listOf(NeonViolet, NeonCyan))
    val CrimsonToAmber = Brush.linearGradient(listOf(NeonCrimson, NeonAmber))
    val EmeraldToCyan = Brush.linearGradient(listOf(NeonEmerald, NeonCyan))
    val GlassCardGradient = Brush.verticalGradient(
        listOf(
            Color(0x331C2448),
            Color(0x1A0F152E)
        )
    )
    val GlassHeaderGradient = Brush.verticalGradient(
        listOf(
            Color(0xD90E1328),
            Color(0xBF080B17)
        )
    )
}
