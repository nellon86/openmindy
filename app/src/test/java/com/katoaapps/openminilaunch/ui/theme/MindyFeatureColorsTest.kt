package com.katoaapps.openminilaunch.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class MindyFeatureColorsTest {
    @Test
    fun magicActionIdentityColorsRemainStable() {
        assertEquals(0xFF2563EB.toInt(), MindyFeatureColors.MagicText.toArgb())
        assertEquals(0xFF198754.toInt(), MindyFeatureColors.MagicCall.toArgb())
        assertEquals(0xFFD6A300.toInt(), MindyFeatureColors.MagicTodo.toArgb())
        assertEquals(0xFF7C3AED.toInt(), MindyFeatureColors.MagicNote.toArgb())
        assertEquals(0xFF8B5A2B.toInt(), MindyFeatureColors.MagicEvent.toArgb())
        assertEquals(0xFFC62828.toInt(), MindyFeatureColors.MagicApp.toArgb())
        assertEquals(0xFFAD4A25.toInt(), MindyFeatureColors.ConversationAttention.toArgb())
        assertEquals(0xFFC62828.toInt(), MindyFeatureColors.ConversationCount.toArgb())
    }

    @Test
    fun blockedAndProviderIdentityColorsRemainStable() {
        assertEquals(0xFFFF0000.toInt(), MindyFeatureColors.AppBlocked.toArgb())
        assertEquals(0xFF4A154B.toInt(), MindyFeatureColors.Slack.toArgb())
        assertEquals(0xFFC5221F.toInt(), MindyFeatureColors.Gmail.toArgb())
        assertEquals(0xFF1A73E8.toInt(), MindyFeatureColors.GoogleMessages.toArgb())
    }
}
