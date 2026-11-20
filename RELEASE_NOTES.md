OpenMindy 1.0.5

- **Battery:** Mindy's breathing animation no longer runs on the hidden Mindy's Day page while Home is open; the conversation light blinks for 12 seconds and then stays lit without animating; MindySpace full screen keeps the display awake only while charging; conversation history is written less often and off the main thread.
- **Conversation badge** counts conversations with a posted notification, not every saved conversation.

---

OpenMindy 1.0.4

- **One thread per chat:** replies that WhatsApp and similar apps add to their notification as "You" are now recognised as your own messages, so they no longer create a second conversation named after you. A chat also keeps the same conversation when its contact match changes between notifications.

---

OpenMindy 1.0.3

- **Cleaner @ results:** with automatic sending, the contacts section names the app it sends with (for example "Contacts · WhatsApp"), people who already have a chat in that app are not repeated as contacts, and Enter opens the existing chat first.
- **No more BETA row for names:** "Write to …" only appears for phone numbers or when nothing else matches.
- **Direct send when possible:** with automatic sending on, an @ message to a chat that has a notification with quick reply is sent from OpenMindy without opening the app.

---

OpenMindy 1.0.2

- **Conversation history:** conversations stay after you reply or their notification goes away, until you swipe them away (or delete them inside the conversation). History is kept only on this phone, in a folder that is never backed up. Quick reply works while the app's notification is posted; otherwise the conversation opens the exact chat in its app.
- **Choose the apps in Conversations:** Settings → Messaging → Conversations. Turn on "Only apps I choose" and pick apps; picked apps count with all their notifications, even those not marked as messages.
- **Older messages show their date** in the conversation list.

---

OpenMindy 1.0.1

- **Smoother Home pages:** the pager between Mindy's Day, Home and Widgets snaps faster and no longer feels stuck.
- **Mindy's Day loads faster:** its summary is cached for up to 3 hours instead of being rebuilt every time the page opens.
- **First letter never lost:** a key typed on Home always reaches the Magic Box, and typing two letters quickly no longer replaces the first with the second.
- **Home button gestures:** tap, long press and swipe up on the round button of the search bar can each run an action (Magic Box modes, voice input, Quick Apps, All apps, conversations, notifications, quick settings, camera, flashlight, lock screen or an app). Double tap locks the screen. Set them in Magic Box settings → Home button.

---

OpenMindy 1.0.0 is the first release of this personal fork of MinkLauncher OpenSource 3.2.2.

- **App search first:** typing in the Magic Box without a hotkey searches installed apps and Enter opens the first match. If nothing matches, Enter sends the text to the browser.
- **`?` searches the web:** `?` hands the text to the default or chosen browser and opens web addresses directly.
- **No file search:** local file-name search is disabled in the Magic Box.
- **No MinkPad:** the Bluetooth keyboard, trackpad and remote, and their Bluetooth and foreground-service permissions, are removed.
- **Own identity:** OpenMindy installs as `io.github.nellon86.openmindy`, checks `nellon86/openmindy` for updates, and drops the upstream Google Play beta, F-Droid, support and community links.

MinkLauncher OpenSource backups can still be imported; their MinkPad settings are ignored.
