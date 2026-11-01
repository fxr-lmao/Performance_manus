# Angel Aura Amethyst (iOS / iPadOS)

This repo holds one file: the official IPA of [Angel Aura Amethyst](https://github.com/AngelAuraMC/Amethyst-iOS), a Minecraft: Java Edition launcher for iPhone and iPad.

| | |
|---|---|
| File | [`org.angelauramc.amethyst-1.0-ios.ipa`](https://github.com/fxr-lmao/Performance_manus/raw/claude/funny-newton-f07uqe/org.angelauramc.amethyst-1.0-ios.ipa) (191.5 MB, stored with Git LFS) |
| Version | `1.0.f98c413`, built from Amethyst-iOS commit [`f98c413`](https://github.com/AngelAuraMC/Amethyst-iOS/commit/f98c4133c72d39e06ceac42b57cc4477c52105fc) (2026-10-05) |
| Bundle ID | `org.angelauramc.amethyst` |
| Requires | iOS / iPadOS 14.0 or later, arm64 device |
| Source | Official Amethyst AltStore/SideStore source, `https://alt.crystall1ne.dev` (linked from the [Amethyst wiki](https://wiki.angelauramc.dev/wiki/getting_started/INSTALL.html#ios)) |
| SHA-256 | `c7a9ffc1655e0383f91d0889f4164b80d41dd79ef525c7b021a8e81ae177491e` |

## Installing

The IPA is fakesigned (signed ad hoc with `ldid`). Your sideloading tool re-signs it when it installs.

- **SideStore / AltStore**: download the IPA on your device, then open it in SideStore/AltStore (or use the **+** button on the Apps page). To get the memory-limit increase, follow the GetMoreRam steps in the [wiki](https://wiki.angelauramc.dev/wiki/getting_started/INSTALL.html#ios).
- **TrollStore**: open the IPA in TrollStore from the share menu. For automatic JIT under TrollStore, use the `-trollstore.tipa` build from the Amethyst project instead.

Minecraft needs **JIT** to run at a usable speed. See the [JIT guide](https://wiki.angelauramc.dev/wiki/faq/ios/JIT.html) for the right method for your iOS version.

Amethyst is made by AngelAuraMC and its contributors. See the [upstream license](https://github.com/AngelAuraMC/Amethyst-iOS/blob/main/LICENSE).

## Minecraft 26.3 (unofficial test patch)

Amethyst can't run Minecraft 26.3 out of the box (26.3 switched to SDL3). The
[`amethyst-sdl-shim`](amethyst-sdl-shim/README.md) folder has an unofficial compatibility layer, with install
steps. It's a first test build for keyboard and mouse that hasn't been tried on an iPad yet, so use a separate
test world.
