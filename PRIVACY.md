# Privacy

Effective November 18, 2026 (OpenMindy fork; based on the MinkLauncher OpenSource privacy notice)

OpenMindy is designed to work locally. It does not operate an application server,
include analytics or advertising SDKs, create an account, or upload launcher content or personal
data. Its normal Internet permission is used only for the optional GitHub release-metadata check
described below.

## Information stored on this device

The app stores launcher settings, App Grid folders and choices, Quick Drawer choices, to-dos, the
five most recent successful plain-text queries, approved document-folder references, optional
cropped Home wallpaper, widget configuration, Mink’s Day preferences, app-bubble preferences,
MinkSpace mini-app preferences, launcher-keyboard preferences, and onboarding state in app-private
storage. Android cloud backup is disabled for OpenMindy.

MinkSpace stores calculator history locally, up to its visible history limit. Each of the three
Quick Widget slots stores only the Android widget ID it hosts.

The Music mini-app reads Android’s currently active media-session metadata, artwork, playback state,
position, provider identity, and advertised control capabilities while the feature is being used.
This state is kept in memory rather than recorded as listening history. OpenMindy does not record
audio and does not request microphone permission.

## Favorites

Favorites can show Android Contacts that the user has starred and Mink-created integrated contacts.
Android Contact names, phone numbers, photos, and contact records remain in Android's Contacts
provider and are not copied into OpenMindy storage or portable backups. Per-person route choices
are stored using an Android-generated contact lookup reference rather than copied contact data.

A Mink-created integrated Favorite stores the display name and the messaging or calling endpoints
the user selects, such as an app shortcut, provider address, username, or phone number. These records
and their ordering are kept in AES-GCM encrypted app-private storage backed by Android Keystore.
They are not uploaded or used to create an account.

## Portable backup

An explicit portable backup is readable JSON. It includes portable launcher layout and settings,
App Grid folders, Quick Drawer choices, launcher-keyboard preferences, MinkSpace order and
enablement, to-dos, Mink-created integrated Favorites and
their ordering, and widget restore recipes so a user can move between distributions. The export
screen warns that readable contact details may be present. For Mink-created Favorites, that can
include display names, phone numbers, usernames, provider routes, and app shortcut identifiers.

Widget restore recipes contain the provider package and component, app and widget labels, saved
layout position, and requested size. They do not contain Android widget IDs, live widget contents, or
provider-specific widget configuration. During import, recipes can reserve a place and ask Android to
bind or configure a replacement; they do not clear widgets already configured on the receiving
device.

Portable backup excludes Android permissions and roles, accessibility and notification access,
Android widget IDs and live widget state, Android Contacts, contact photos, document-folder grants,
wallpaper images, recent-query history, calculator history,
active media sessions, Work and Privacy Space state, device-specific private-area gateways, active
bubble notifications, demo data, and update caches or reminders.

## Apps, shortcuts, work profiles, and private areas

As a launcher, OpenMindy reads installed launchable app labels, icons, package identities, and
developer-published shortcuts that Android makes available. Recent messaging conversations are
derived from compatible shortcuts published locally by installed providers. This discovery data is
used to show and launch choices and is not sent to the OpenMindy developer.

Accessible Work Apps are shown in a separate drawer with Android’s work badge. OpenMindy can ask
Android to pause or resume the profile, but Android and the managing organization control the profile,
its policy, and its contents. OpenMindy does not read work-app account or document contents.

Android Private Space apps are kept outside regular search, the App Grid, Quick Drawer, and See All. On devices
using a manufacturer-specific private area, the user may save a device-owned app or shortcut as a
gateway. OpenMindy stores and opens only that target; the manufacturer controls authentication and
the apps inside it.

Mink’s Day pausing changes only which selected personal apps OpenMindy displays or launches. It
does not disable, suspend, modify, or control those apps at the Android system level. The always-on
launcher barrier does not require Usage Access; pausing after a daily limit does.

## Handoffs to other apps

When the user launches another app, opens selected media, composes a message, creates a note or event,
opens a file, or delegates a Web or AI query, the chosen content leaves
OpenMindy through Android or the recipient. The receiving app, service, person, or
device then controls that copy under its own privacy terms. OpenMindy does not receive the
provider’s response.

## Deletion

Users can delete to-dos, query history, calculator history, app-bubble choices, approved folders, widgets, and other supported items from
their relevant screens. Permissions and special access can be revoked in Android Settings. Clearing
OpenMindy storage or uninstalling it removes its local app data, subject to files or exports the
user deliberately saved elsewhere.

Privacy questions can be opened as an issue at https://github.com/nellon86/openmindy/issues.
