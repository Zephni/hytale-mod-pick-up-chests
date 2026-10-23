# CurseForge submission

## Project fields

- **Game:** Hytale
- **Name:** Pick Up Chests
- **Author:** MugHeadStudios
- **Summary:** Move single and double chests with their contents intact. Crouch + F to pick up; release crouch and right-click to place. Personal carry settings included.
- **License:** All Rights Reserved
- **Third-party distribution:** Allowed through CurseForge, for compatible launchers and clients.
- **Project type:** Mods (server plugin with included assets)
- **Category:** Quality of Life
- **Logo:** [icon.png](icon.png) (400 x 400 PNG; use the full square when cropping)
- **Gallery:** Screenshots are managed directly in [CurseForge Media](https://authors.curseforge.com/#/projects/1761154/media), not stored in Git.
- **Gallery title:** Carry a chest without emptying it
- **Gallery caption:** Carry a chest in your hands, then release crouch and use Secondary to place it with its contents intact. Supports single and double chests.
- **Gallery video:** [Pick Up Chests - Gameplay Demo](https://www.youtube.com/watch?v=d01ba2S56XA)
- **Source:** https://github.com/Zephni/hytale-mod-pick-up-chests
- **Issues:** https://github.com/Zephni/hytale-mod-pick-up-chests/issues

## Description

Move your storage without emptying every chest first.

**Pick Up Chests** lets you crouch and interact with a closed chest to lift it
with its contents intact. Carry it to its new home, release crouch, and use
Secondary to place it down.

### Gameplay video

<iframe src="https://www.youtube.com/embed/d01ba2S56XA" width="560" height="314" allowfullscreen="allowfullscreen"></iframe>

[Watch the Pick Up Chests gameplay demo on YouTube](https://www.youtube.com/watch?v=d01ba2S56XA).

You can also watch it in this project's Gallery tab.

### Features

- **Carry in your hands:** The default mode carries one chest using Hytale's
  native placement preview, even when your inventory is full.
- **Optional inventory mode:** Store picked-up chests in free inventory slots.
  Filled chests get a contents badge and a tooltip preview.
- **Personal settings:** Use `/pickupchests` to choose your carry mode and
  either Crouch + Interact or Crouch + Secondary. No operator permission needed.
- **Native controls:** Follows your rebound Hytale actions. Default pickup is
  **CTRL + F**; default placement is **right-click after releasing crouch**.
- **Single and double chests:** Supports vanilla small/single and large/double
  furniture chests, including stolen Goblin chests. Does not affect barrels or
  workbenches.

### Controls

With Hytale's default key bindings:

| Action | Controls |
| --- | --- |
| Pick up a closed chest | Hold **CTRL** (Crouch) and press **F** (Interact). |
| Place a chest carried in your hands | **Release CTRL**, aim at a valid location, then **right-click** (Secondary). |
| Open a chest normally | Press **F** without crouching. |
| Change your personal settings | Enter **`/pickupchests`** in chat. |

In `/pickupchests`, you can switch pickup to **Crouch + Secondary**
(**CTRL + right-click** by default). With that option selected, **F** opens
chests even while crouching.

You can also choose **Inventory** mode. Pick up the chest using your selected
gesture, then select its item in your hotbar and place it normally.

Rebound keys work too: the mod follows Hytale's **Crouch**, **Interact**, and
**Secondary** actions rather than hard-coding these keys.

### Installation

Requires **Hytale 0.7.4**. Dedicated servers also require **Java 25**.

For single-player, put the release JAR in your Hytale `UserData\Mods` folder
and enable PickUpChests in the world's mod list. On Windows, this is normally
`%APPDATA%\Hytale\UserData\Mods`.

For a dedicated server, stop the server, put the JAR in its `mods` folder,
and restart. Players do not need a separate client plugin; the included assets
are supplied by the server.

### Before moving valuable storage

- Back up your world and test the mod on a disposable save first.
- Close the chest before picking it up, and stay within five blocks.
- Put down a carried chest before changing settings or logging out. Death or
  disconnect drops it into the world using native handling; normal despawn
  rules apply.
- While carrying in your hands, collecting items and using tools are blocked
  until you place the chest.
- **Place all filled chests before uninstalling this mod.**
- Protection systems must implement Hytale's relevant use/break events;
  compatibility with custom protection mods needs testing.

By **MugHeadStudios**. Not affiliated with or endorsed by Hytale.

## Release file

- **Upload:** `build\PickUpChests-1.0.0.jar` (not the test JAR)
- **Display name:** Pick Up Chests 1.0.0 - Hytale 0.7.4
- **Release type:** Release
- **Supported game version:** 0.7.4 only. CurseForge currently offers a broad
  `0.7` tag, which was selected; the release name, description, and manifest
  retain the exact 0.7.4 requirement.
- **Dependencies:** No external mod dependencies.

### Changelog

Initial public release:

- Pick up closed vanilla single and double chests without losing their contents.
- Carry in hands by default, with an optional inventory mode.
- Per-player carry and control settings through `/pickupchests`.
- Placement guidance and filled-chest inventory contents previews.

## Preparing and submitting

1. Use `distribution\icon.png` as the master listing artwork. It includes the
   author's manual edits; do not overwrite it with the original icon generator.
2. Run `.\build.ps1 -Test` from the repository root.
3. Sign in at https://authors.curseforge.com/ and create a Hytale project.
4. Use the fields and description above, upload the logo and gallery image,
   and select All Rights Reserved. Keep the online description in WYSIWYG mode:
   its More toolbar > Insert/edit media control inserts the YouTube player
   directly beneath the Gameplay video heading. Do not replace it with a
   Markdown-only link.
5. Obtain explicit user approval before restoring or uploading a release for
   moderation. Version 1.0.0 received that approval on 2026-10-23 at 13:37 BST.
6. Once approved by the user, select the supported game version and submit for
   moderation. Publication is not complete until CurseForge approves the project
   and file.

## Submitted project

- **Project ID:** 1761154
- **Project URL:** https://www.curseforge.com/hytale/mods/pick-up-chests
- **Author preview:** https://www.curseforge.com/hytale/mods/pick-up-chests/preview
- **Dashboard:** https://authors.curseforge.com/#/projects/1761154/files
- **Release file ID:** 9297248
- **Release publication:** Version 1.0.0 restored for review with explicit user
  approval on 2026-10-23 at 13:37 BST. Automatically publishes once approved by
  CurseForge. Future submissions still require explicit user approval.
- **Gallery:** Screenshot uploaded and titled "Carry a chest without emptying it".
- **Video:** Playable YouTube gallery entry added, with a link in the description.
  Featured Media enabled and verified after reloading (`isFeatured: true`).
  Featured Media alone did not render a player in the description preview.
  Inserted the player directly through the WYSIWYG editor's native media
  control; verified the reloaded project preview contains exactly one YouTube
  iframe for `d01ba2S56XA` with a Play video button.
- **Account:** Renamed to [MugHeadStudios](https://www.curseforge.com/members/mugheadstudios).
- **Logo:** Replaced with the author's edited artwork on 2026-10-23. The local
  master matches the supplied image; the saved 400 x 400 CurseForge logo was
  downloaded and verified pixel-for-pixel, with the full square included.
- **Verification:** Native regression tests passed, including 142 persistence
  round trips. The downloaded CurseForge release matches the tested JAR.
- **Release SHA-256:** `156F6E8782C936AEC981F2C51F0C44D1FC7A8E9B2E994B7CF5923A550C86D99E`

**Status:** Version 1.0.0 restored and submitted after final user approval on
2026-10-23. Verified CurseForge shows **Under Review** for release file 9297248.
Awaiting moderation; not yet confirmed publicly available.
