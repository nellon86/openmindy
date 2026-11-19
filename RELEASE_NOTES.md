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
