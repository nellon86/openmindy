package com.katoaapps.openminilaunch.features.conversations

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnMessageDetectionTest {
    @Test fun missingSenderIsTheUser() {
        assertTrue(isOwnMessage(sender = null, senderKey = null, selfName = null, selfKey = null))
    }

    @Test fun replyFromTheUserPersonIsOutgoing() {
        // WhatsApp: the reply is attached to the style's user person, named "Tú".
        assertTrue(isOwnMessage(sender = "Tú", senderKey = "me", selfName = "Tú", selfKey = "me"))
        assertTrue(isOwnMessage(sender = "Tú", senderKey = null, selfName = "Tú", selfKey = null))
    }

    @Test fun otherPeopleStayIncoming() {
        assertFalse(isOwnMessage(sender = "Ana", senderKey = "ana", selfName = "Tú", selfKey = "me"))
        assertFalse(isOwnMessage(sender = "Ana", senderKey = null, selfName = null, selfKey = null))
    }

    @Test fun sameNameDifferentPersonStaysIncoming() {
        assertFalse(isOwnMessage(sender = "Aniello", senderKey = "other", selfName = "Aniello", selfKey = "me"))
    }
}
