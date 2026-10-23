# Pick Up Chests - Hytale 0.7.4

A mod by **MugHeadStudios** (`MugHeadStudios:PickUpChests`).

A server-side Java plugin with a small asset pack: **crouch and interact with a
closed chest** to pick it up, keeping its contents. The default controls are
**CTRL + F**. By default you carry the intact chest in your hands, with its
original appearance and contents. Release crouch, aim at a valid placement
location and use **Secondary / right-click** to put it down before lifting another.
Supported chests explicitly enable Hytale's placement preview rather than using
its conditional default visibility. This applies to both carried chests and
chest items; other blocks and the normal placement restrictions are unchanged.
Pickup does not play block-shattering particles or breaking sounds.
**F / Interact while not carrying a chest opens
chests**. Crouching + Interact now picks them up instead.
Pickup accounts for queued crouch updates and uses the chest selected by the
interaction, so remote players do not depend on the server's last processed
crouch state or aim. The five-block pickup limit still applies.

While aiming at a chest within pickup range, a second prompt appears beneath
the normal open hint: **Crouch + Interact to carry chest** by default, or
**Crouch + Interact to pick up chest** in Inventory mode.
This is a single centered text line, without keycap boxes or icons. Selecting
Secondary changes the action name in the hint to **Secondary**.
The hint names actions rather than physical keys, so it does not display incorrect
default keys when controls are rebound. The default bindings are CTRL for Crouch,
F for Interact, and right-click for Secondary.
The hint remains visible while standing or crouching;
pickup still requires crouching. Looking away or moving out of range hides it;
the normal open hint is unchanged. While carrying, the prompt instead shows
**Release crouch; use Secondary to place chest**. The placement hint stays visible
until you put the chest down.

Supports vanilla furniture chests, including large and stolen Goblin chests.
Barrels, workbenches and unrelated containers are not affected. No new recipes,
client patching or external libraries are needed.

## Personal settings and carry mode

Run **`/pickupchests`** to open the game-styled settings panel. Every player can
use it without operator permissions. Changes apply immediately and are saved
with that player's character data on this world/server, including across restarts.

- **Inventory** (optional): puts the chest in an empty hotbar/storage slot.
  You can continue collecting items and carry multiple chest items as before.
- **Carry in hands** (default for players without saved settings): uses Hytale's native carried-block system, not an item
  selected in the hotbar. Carry one chest at a time, including with a full
  inventory, and place it using the game's native carried-block preview and placement.
  Release crouch and use Secondary (right-click by default) to place it.
  The crouched pickup gesture never places or replaces the chest you are holding.
  Other chest pickups, automatic ground loot, interactive item pickups,
  throwing, tool use and manual item dropping are blocked until you put it down.
  Placement restores its contents and immediately unlocks normal item collection.
- **Crouch + Interact** (default): CTRL + F with default controls.
- **Crouch + Secondary**: CTRL + right-click with default controls. In this mode
  F/Interact opens chests even while crouching.

The hint shows the selected action and says **"to carry chest"** in carry mode.
Existing saved mode choices are preserved; choose **Carry in hands** in
`/pickupchests` if you previously selected Inventory.
Change the physical keys through Hytale's **Controls** settings; the mod follows
those bindings rather than hard-coding CTRL, F or a mouse button. It does not add
a separate arbitrary client keybind or change other mods' controls.

Put down a carried chest before changing these settings. Carrying uses native
death and disconnect handling: the chest is dropped into the world with its
contents, rather than silently converted to inventory mode. Recover it where
it was dropped. Normal world item-despawn rules apply; place valuable chests
before logging out. Stored inventories and their filters are not changed to
implement the ground-loot restriction.

## Filled chest indicator

- Filled chests use an original chest icon with a bright green contents/list
  badge in the hotbar and inventory. All filled chest styles share this indicator;
  their name and placed appearance still identify the original chest style.
- Hovering shows the original chest name followed by **(Contains items)**,
  total item quantity, occupied/capacity slots, and the first **three occupied
  slots** with quantities and localized item names. Additional occupied slots are
  summarized instead of overflowing the tooltip.
- Empty chests keep their vanilla icon and name. Filled chests do not stack.
- The icon, tooltip and contents survive saving/reloading and inventory moves.
- Single chests carried over from earlier development builds in your hotbar, storage or backpack
  are upgraded when you enter the world. Legacy items stored elsewhere can be
  placed and picked up again to get the indicator.

The server supplies the icon through Hytale's normal asset-pack download; players
do not need to install a separate client mod.

## Install in single-player (Windows)

1. Close your world and back up the save before trying a new mod.
2. Copy `build\PickUpChests-1.0.0.jar` into
   `%APPDATA%\Hytale\UserData\Mods`.
   Replace the existing JAR when updating. If you installed the temporarily named
   `PickUpChests-1.1.0.jar`, remove that file; do not keep both installed.
3. Start Hytale **0.7.4**, enable **PickUpChests** in the world's mod list, and
   load the world. Restart the game if the newly copied mod is not listed.
4. Crouch and press Interact (default
   CTRL + F) at a closed chest within five blocks. An empty hand works, as does
   holding an item. The plugin confirms pickup in chat. Use `/pickupchests`
   to choose carrying in hands or crouch + Secondary instead.
5. Release crouch and use Secondary (right-click by default) to place the carried
   chest, then open it to retrieve its contents. If you chose Inventory mode,
   select the resulting chest item in your hotbar and place it normally.

From PowerShell in this project, after building:

```powershell
$mods = Join-Path $env:APPDATA "Hytale\UserData\Mods"
New-Item -ItemType Directory -Force -Path $mods | Out-Null
Copy-Item ".\build\PickUpChests-1.0.0.jar" $mods -Force
```

Local builds automatically replace this JAR in your Hytale Mods directory;
the manual copy above is only needed when installing a separately supplied JAR.
You still need to enable the mod in your world's mod list.

## Install on a dedicated server

Stop the server, back up its universe, and put the production JAR in the
server's `mods` directory (relative to the server's working directory). Restart
the server. Its log should report `Enabled plugin MugHeadStudios:PickUpChests`.

This replaces the development plugin identity `Local:PickUpChests`. When
upgrading a world from a development build, check its mod list and enable
the MugHeadStudios version if needed.

The server must run Hytale 0.7.4 with Java 25. Players do not need to install a
separate client plugin. See the official
[Hytale Server Manual](https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual)
for server setup and general mod installation.

## Build

Requires **JDK 25**, including `java`, `javac` and `jar` on `PATH`, and a legally
installed Hytale 0.7.4 server JAR. No Maven/Gradle installation or downloads are
required. The server and game assets are not bundled.

```powershell
.\build.ps1
```

By default, this uses the release installation under
`%APPDATA%\Hytale\install\release\package\game\latest`. For another installation:

```powershell
.\build.ps1 -ServerJar "D:\Hytale\Server\HytaleServer.jar"
```

Development builds stay at **1.0.0** and overwrite the same
`build\PickUpChests-1.0.0.jar`; do not bump the version during 1.0 development.
After a successful build (and successful tests when using `-Test`), the script
also copies the production JAR to `%APPDATA%\Hytale\UserData\Mods`, creating
the directory if needed and replacing the existing `PickUpChests-1.0.0.jar`.
Close your world before building and reopen it afterward to load the update.
Copy failures fail the build rather than reporting a successful installation.
Both the build and plugin manifest
require exactly 0.7.4; newer versions need revalidation before changing that
constraint.

## Safety and limitations

- Inventory-mode pickup requires an empty hotbar/storage slot. It does not merge a filled
  chest into an existing stack. A full inventory leaves the chest untouched.
  Chat displays **"Make room in your hotbar or inventory first."** The same
  click cannot fall through to block placement or tool use after rejection.
- Close the chest first; pickup is refused while its inventory is open.
- The plugin respects cancelled use events, world block-breaking restrictions,
  and a cancellable `BreakBlockEvent` before removing anything. Protection mods
  must actually implement these events; custom protection systems need their
  own compatibility testing.
- Contents use Hytale's native `BlockHolder` item metadata, not a separate file
  or a server-memory lookup. Normal native block placement restores the data.
- Contents are attached to the chest item. Destroying that item destroys its
  contents. Creative duplication copies them too; this is not an anti-cheat mod.
- Filled items reference this mod's item assets. **Place all filled chests before
  uninstalling the mod**, so their contents are safely back in world blocks.
- Pickup follows Hytale's Crouch and selected Interact/Secondary bindings. If you
  rebind those actions, use your new bindings. Other actions are unchanged unless
  crouching and aiming at a chest within five blocks, or already carrying a chest.
- No special placement override is installed. Chest joining, rotation, support,
  death drops and item movement follow Hytale's own behavior.
- Test on a disposable world before moving valuable items. Keep backups and
  avoid third-party inventory tools that discard item metadata.

## Validation

```powershell
.\build.ps1 -Test
# If Assets.zip is not beside the Server directory:
.\build.ps1 -Test -ServerJar "D:\Hytale\Server\HytaleServer.jar" -AssetsZip "D:\Hytale\Assets.zip"
```

This compiles the production mod, then starts an isolated offline Hytale server
under `build\test-server`, bound only to loopback on an automatically allocated
port. It stops automatically after startup and does not load your existing
saves. After the tests pass, the production JAR is installed into your local
Hytale Mods directory, just like a normal build; the test JAR is not installed there.

The test plugin checks ECS/command registration, both selectable crouch gestures
(rejecting standing and other actions), the Inventory + Interact defaults,
native per-player settings serialization for all four combinations, preserved
existing unarmed bindings, empty-slot/full inventory behavior, icon and settings/
pickup-hint asset registration, supported UI string escapes (Hytale UI does not
accept `\n` or `\t`; use wrapping or separate labels), all hint bindings, native hint layout anchors,
chest hint eligibility, tooltip limits, legacy inventory upgrades,
unchanged native placement block keys, and native serialization/deserialization
of every eligible installed chest variant. Persistence checks include empty and
filled chests, capacity, items in the first/last slots, item metadata and display
metadata, plus carried-block contents. The transient pickup guard is tested for
rejection without stack merging, unlocking after carrying, and preservation of
existing inventory filters. Results and server output are in `build\test-server`.
The test JAR is **not** the mod to install.

Regression tests also break empty and filled small/large crude chests in a
disposable world and check vanilla chest drops and content quantities. Inventory
guard tests verify that collecting and merging loot after putting a chest down
notifies the underlying hotbar/storage containers, so client inventory updates
are not lost behind a combined-inventory wrapper.

These are headless API/persistence tests, not an automated player/client test.
Before using a valuable save, manually verify:

1. With the default action, standing F/Interact opens a chest and CTRL + F picks
   it up. Switch to Crouch + Secondary: F opens it in both stances and CTRL +
   right-click picks it up. Neither action picks up chests without crouching.
2. Test pickup empty-handed and while holding a block/tool: move a
   filled small chest into inventory; placement restores every item and quantity.
3. Repeat for a large chest, interacting with both its main and filler blocks.
4. A full inventory prevents Inventory-mode pickup; an open chest or denied
   break permission prevents pickup in either mode.
5. Save/reload while holding a filled chest, then place and open it.
6. Check placement next to another chest and your server's protection mods.
7. Confirm the green-badged icon and hover contents preview for filled chests,
   and the normal icon/name for empty ones. Try more than three occupied slots.
8. Aim at a chest within five blocks while standing and crouching: check the
   pickup hint sits beneath the normal open hint at your resolution/UI scale,
   with native hotkeys and no extra background panel. It must show both Crouch
   and the selected action in either stance, but only pick up while crouching.
   Look away, move out of range and pick up the chest to check the hint hides.
   Rebind Crouch, Interact and Secondary, including a keyboard Secondary binding,
   and confirm labels/icons follow the selected bindings.
9. Open `/pickupchests` without operator permissions, select each combination,
   close/reopen the panel and reconnect/restart the world: your settings should
   persist independently from another player's choices.
10. Select Carry in hands, including with a full inventory. Pick up a filled
   small/large chest, try collecting ordinary ground loot and carryable dropped
   blocks, using tools, throwing/dropping items and picking up another chest.
   These must be blocked without losing ground loot or altering your inventory.
   Place it in valid space: contents must be restored and loot collection
   must resume. Invalid/protected placement must leave the chest in your hands.
11. On a disposable save, die or disconnect while carrying: recover the native
   dropped chest and verify every item, with no duplicate chest left behind.
   Also check normal world shutdown/restart and world travel while carrying.
