<p align="center">
  <img src="docs/brand/banner.png" width="100%" alt="LAIBACH LIFE™, the state's home screen for every Android citizen">
</p>

<p align="center">
  <a href="https://github.com/mitramix/laibach-launcher/releases/latest/download/laibach-launcher.apk"><img src="https://img.shields.io/badge/%E2%96%BC%20REPORT%20FOR%20DUTY-DOWNLOAD%20THE%20LATEST%20APK-C1121F?style=for-the-badge&labelColor=0A0A0A" alt="Download the latest APK"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/VERSION-2.8%20THE%20EXCHANGE-ECE6D6?style=flat-square&labelColor=0A0A0A" alt="Version 2.8, The Exchange">
  <img src="https://img.shields.io/badge/ANDROID-12%2B-ECE6D6?style=flat-square&labelColor=0A0A0A" alt="Android 12 or later">
  <img src="https://img.shields.io/badge/SIZE-~247%20KB-ECE6D6?style=flat-square&labelColor=0A0A0A" alt="About 247 KB">
  <img src="https://img.shields.io/badge/LIBRARIES-NONE-ECE6D6?style=flat-square&labelColor=0A0A0A" alt="No libraries">
  <img src="https://img.shields.io/badge/LANGUAGES-EN%20%C2%B7%20DE%20%C2%B7%20SL-C1121F?style=flat-square&labelColor=0A0A0A" alt="English, German, Slovenian">
</p>

<p align="center">
  <b><a href="#proclamation">PROCLAMATION</a></b> ■
  <b><a href="#enlistment">ENLISTMENT</a></b> ■
  <b><a href="#visual-evidence">EVIDENCE</a></b> ■
  <b><a href="#the-terminal">TERMINAL</a></b> ■
  <b><a href="#uniforms">UNIFORMS</a></b> ■
  <b><a href="#the-collective">COLLECTIVE</a></b> ■
  <b><a href="#total-harmony">HARMONY</a></b><br>
  <b><a href="#synthetic-state">SYNTHETIC STATE</a></b> ■
  <b><a href="#mobilisation">MOBILISATION</a></b> ■
  <b><a href="#laibach-tv">LAIBACH TV</a></b> ■
  <b><a href="#volkskunst">VOLKSKUNST</a></b> ■
  <b><a href="#language-of-instruction">LANGUAGE</a></b> ■
  <b><a href="#what-the-state-does-not-see">PRIVACY</a></b> ■
  <b><a href="#chronicle">CHRONICLE</a></b>
</p>

> [!CAUTION]
> **OFFICIAL NOTICE.** This document is issued by the Laibach Collective. Read
> it in full. Reading it is not optional. Not reading it will be noted.

<a id="proclamation"></a>

## 📜 I. PROCLAMATION

**LAIBACH LIFE™** is a home screen for Android. It replaces the citizen's
previous home screen. The previous home screen has been informed.

The terminal speaks the visual language of Laibach and NSK and no other:
black, blood red and bone white; monumental condensed type; a cogwheel
emblem; cogwheel badges on every programme. It serves any phone on Android 12
or later, of any make.

From this point the citizen's whole life runs in LAIBACH LIFE™. The interface
uses the plain language of the bureaucracy:

| The citizen says | The state says |
|---|---|
| user | **citizen** |
| app | **permitted programme** |
| hide | **redact** |
| show | **declassify** |
| uninstall | **banish** |
| app info | **dossier** |
| theme | **uniform** |
| change home screen | **defect** |
| day of the year | **plan day** |

Instruction is given in English, German or Slovenian. English is assigned by
default.

| | |
|---|---|
| **Package** | `dev.laibach.launcher` |
| **Version** | 2.9, *Interference* |
| **Source** | 107 Java files, no libraries, no Gradle |
| **Size** | about 275 KB |
| **Requires** | Android 12 (API 31) or later |

**Statement of origin.** All artwork and text of the launcher are original
and were produced for the state. There are no band logos, no album art and no
lyrics. The emblem is a plain 12-tooth cogwheel around a red disc with a
Suprematist cross. The slogans were written for this launcher. The wallpaper
is fan art fetched live from the Volkskunst gallery on laibach.org and is
always shown with its title and its artist. Nothing from the site is bundled
in the app.

<a id="enlistment"></a>

## ⚙️ II. ENLISTMENT

### ■ Report for duty

1. Download **[`laibach-launcher.apk`](https://github.com/mitramix/laibach-launcher/releases/latest/download/laibach-launcher.apk)**,
   the current issue. Earlier issues have been withdrawn from circulation.
2. Open it on the phone. Android will ask permission to install from this
   source. Grant it.
3. Make it the home screen: Settings → Apps → Default apps → Home app →
   Laibach. The path varies by phone.

Every release is signed with the same key, so a newer APK installs over the
old one and the citizen's papers are kept.

### ■ Construction from source

Requires the Android SDK (build-tools 35.0.0, platform android-36) and JDK
17+ (Homebrew's `openjdk@21` by default). Gradle is not permitted.

```bash
./build.sh
```

The product is `build/laibach-launcher.apk`. On the first build, `build.sh`
creates a signing key in `keystore/` (`laibach.jks` and `password.txt`).

> [!WARNING]
> **Guard the `keystore/` folder.** Every update must carry the same key. It
> is never committed.

### ■ Organs of the state (source layout)

Each part of the terminal is a file of its own in `src/dev/laibach/launcher/`.
`HomeActivity` only assembles the screen and answers the system; every part
of the home screen extends `HomePart` and draws with the shared tools in
`StateActivity`, which every other screen also extends.

| Organ | Files |
|---|---|
| **Screens** | `StateActivity` (shared uniform and type), `HomeActivity`, `PapersActivity`, `LockActivity`, `CensorActivity`, `DarkroomActivity`, `VolkskunstActivity`, `BroadcastActivity`, `ExchangeActivity`, `AlarmActivity` |
| **Home screen parts** | `HomePart` (base), `Masthead` (emblem, network, uniform, device line), `Monument` (clock, date, proclamation), `Register` (the programme list, its filter and footer), `Command` (ticker and command line), `Ledger` (number, standing, ration, life force), `Decrees`, `OnAir`, `ArtCredit`, `News` (the ticker's text), `Weather` (the sun, from the clock alone), `Curfew`, `RollCall`, `DeadDrop`, `Regulations`, `Slogans` |
| **The papers** | `PapersPart` (base), `PapersCard` (the identity card), `PapersFigures`, `PapersAlbum`, `PapersDesk`, `PapersExports`, `PapersRecord`, `Passport` |
| **Programmes** | `App`, `ProgrammeAdapter` (rows, grids, ministry headers), `Counter` (opening a programme past the quotas, the ration, the queue and the coupons), `WaitingRoom`, `ProgrammeMenu`, `Ministry`, `Quotas`, `Formation` |
| **Menus and orders** | `GearMenu`, `Orders`, `CallUps`, `PowerRation`, `StateMenu`, `Tiles` (quick-settings switches) |
| **The lock** | `Terminal` (the seal's policy), `Seal` (the hold-to-unlock seal), `Dots`, `LockWallpaper` |
| **Broadcast and news** | `Feed` (the channel's Atom feeds, resume and watch later), `Player` (the embedded player, kept to its own page), `Fullscreen`, `Bulletins` (concerts and news), `WallArt` (the approved art of the day), `DecreeListener` (counts notifications, never reads them) |
| **Darkroom and art** | `Censor` (develops and redacts), `Prints` (reading, drawing, filing and sharing prints), `Plate` (the photograph on the table), `Specimen` (the drawn test subject), `DarkroomProvider`, `TextCensor`, `PosterPress`, `Wall` (the Volkskunst gallery wall) |
| **Sound** | `Tones` (every sound, synthesised), `Anthems` (the sounds handed to the system), `Hourly` (the anthem of the hour), `Morse`, `Alarm` (reveille) |
| **Effects** | `Fx` (the shared clock), `Cyber` (shared drawing), `Variant` (the uniforms), `Backdrop` (the album scenes) on `Stage` (their shared props), `Crt`, `GlitchText`, `Motion`, `Eye`, `Pulse`, `Ticker`, `Boot`, `EmblemView`, `ArtView`, `RationBar` |
| **The citizen** | `Citizen`, `Attention` (usage events, read on the phone only), `Norm`, `NumberCheck`, `Record`, `Report`, `Constitution`, `Drills`, `Comrades`, `Qr`, `ExitVisa` |
| **Copies that leave** | `Exports` (cards, pictures, sharing), `CaseFile` (the PDF case file), `Backup` (settings saved and restored) |
| **Honours** | `Stamps` (the album), `StampArt` (glyphs, stamps and medals), `Ceremony`, `Promotion`, `Foundation` |
| **Language** | `Lang`, `Slovene` (reads `slovene.txt`, every English string beside its Slovenian) |

#### How the organs are bound

- **One screen, one activity.** An activity builds its screen and answers the
  system. Anything that can be drawn on its own is a view of its own (`Wall`,
  `Plate`, `Seal`, `Player`, `Backdrop`), told only what it needs through a
  small `Host` interface the activity fills in.
- **Parts report to their screen.** The home screen's parts and the papers'
  sections each get the screen they stand in and nothing else; one part asks
  another through that screen, never directly.
- **The state's records live outside the screens.** What more than one screen
  reads (the feed, the seal's policy, the comrades, the record, the citizen)
  is a plain class with static methods taking a `Context`, so no screen reaches
  into another. Screens refer to one another only to open them, by
  `X.class` and the extras the opened screen names.
- **Most-used**: `Variant` (the uniform) and `Lang`, `Record` and `Citizen`,
  which nearly every organ consults.
- **Size.** No source file is longer than 500 lines. The Slovenian texts are
  data, not code, and are kept as a table.

### ■ Installation over adb

```bash
adb install -r build/laibach-launcher.apk
```

Some phones ask for confirmation on the first install. Updates pass without
question.

```bash
adb shell cmd role add-role-holder --user 0 android.app.role.HOME dev.laibach.launcher 0
```

Or from the launcher's own menu → **Defect to another home screen**.

### ■ Defection

Defection is permitted. It will be noted.

Choose another home app in Settings → Apps → Default apps → Home app, or from
the launcher's menu → **Defect to another home screen**. Then, if the citizen
insists, banish the app. Android returns to its own home screen.

```bash
adb uninstall dev.laibach.launcher
```

<a id="visual-evidence"></a>

## 📷 III. VISUAL EVIDENCE

Recorded in English on an Android 17 phone. The picture behind the terminal
is the day's approved art from the Volkskunst gallery on laibach.org,
credited on screen. Stills from Laibach TV are pixelated here. They are the
property of the channel.

### ■ The terminal and its menus

<table>
<tr><td align="center"><img src="docs/screenshots/home-tv.jpg" width="250" alt="The terminal, with the Laibach TV strip"><br><sub>The terminal, with the Laibach TV strip</sub></td><td align="center"><img src="docs/screenshots/menu-gear.jpg" width="250" alt="The menu (tap the gear)"><br><sub>The menu (tap the gear)</sub></td><td align="center"><img src="docs/screenshots/regulations.jpg" width="250" alt="Settings → Regulations"><br><sub>Settings → Regulations</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/doctrine.jpg" width="250" alt="Uniforms ▸"><br><sub>Uniforms ▸</sub></td><td align="center"><img src="docs/screenshots/app-menu.jpg" width="250" alt="Long-press on a programme"><br><sub>Long-press on a programme</sub></td><td align="center"><img src="docs/screenshots/ministries.jpg" width="250" alt="Transfer to ministry"><br><sub>Transfer to ministry</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/programmes.jpg" width="250" alt="Permitted programmes by ministry"><br><sub>Permitted programmes by ministry</sub></td></tr>
</table>

### ■ Papers, record and the Constitution

<table>
<tr><td align="center"><img src="docs/screenshots/papers.jpg" width="250" alt="Papers: identity card and statistics"><br><sub>Papers: identity card and statistics</sub></td><td align="center"><img src="docs/screenshots/papers-2.jpg" width="250" alt="Self-criticism and the permanent record"><br><sub>Self-criticism and the permanent record</sub></td><td align="center"><img src="docs/screenshots/constitution.jpg" width="250" alt="The Constitution"><br><sub>The Constitution</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/stamp-album.jpg" width="250" alt="The stamp album"><br><sub>The stamp album</sub></td><td align="center"><img src="docs/screenshots/ceremony.jpg" width="250" alt="A ceremony"><br><sub>A ceremony</sub></td></tr>
</table>

### ■ Routines and Laibach TV

<table>
<tr><td align="center"><img src="docs/screenshots/curfew.jpg" width="250" alt="Curfew (here as a drill)"><br><sub>Curfew (here as a drill)</sub></td><td align="center"><img src="docs/screenshots/rollcall.jpg" width="250" alt="Morning roll call"><br><sub>Morning roll call</sub></td><td align="center"><img src="docs/screenshots/tv.jpg" width="250" alt="Laibach TV: one channel"><br><sub>Laibach TV: one channel</sub></td></tr>
</table>

### ■ Mobilisation

<table>
<tr><td align="center"><img src="docs/screenshots/mobilisation-orders.jpg" width="250" alt="Mobilisation orders"><br><sub>Mobilisation orders</sub></td><td align="center"><img src="docs/screenshots/reveille.jpg" width="250" alt="Reveille ▸ (the state's alarm)"><br><sub>Reveille ▸ (the state's alarm)</sub></td><td align="center"><img src="docs/screenshots/weekly-report.jpg" width="250" alt="Weekly report"><br><sub>Weekly report</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/papers-export.jpg" width="250" alt="Papers for export"><br><sub>Papers for export</sub></td></tr>
</table>

### ■ The nine uniforms

<table>
<tr><td align="center"><img src="docs/screenshots/uniform-opus-dei.jpg" width="250" alt="OPUS DEI"><br><sub>OPUS DEI</sub></td><td align="center"><img src="docs/screenshots/uniform-nova-akropola.jpg" width="250" alt="NOVA AKROPOLA"><br><sub>NOVA AKROPOLA</sub></td><td align="center"><img src="docs/screenshots/uniform-kapital.jpg" width="250" alt="KAPITAL"><br><sub>KAPITAL</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/uniform-wat.jpg" width="250" alt="WAT"><br><sub>WAT</sub></td><td align="center"><img src="docs/screenshots/uniform-volk.jpg" width="250" alt="VOLK"><br><sub>VOLK</sub></td><td align="center"><img src="docs/screenshots/uniform-iron-sky.jpg" width="250" alt="IRON SKY"><br><sub>IRON SKY</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/uniform-spectre.jpg" width="250" alt="SPECTRE"><br><sub>SPECTRE</sub></td><td align="center"><img src="docs/screenshots/uniform-zarathustra.jpg" width="250" alt="ZARATHUSTRA"><br><sub>ZARATHUSTRA</sub></td><td align="center"><img src="docs/screenshots/uniform-sound-of-music.jpg" width="250" alt="SOUND OF MUSIC"><br><sub>SOUND OF MUSIC</sub></td></tr>
</table>

### ■ The ten formations

All ten in the SPECTRE uniform. Any uniform can be worn in any formation.

<table>
<tr><td align="center"><img src="docs/screenshots/formation-monument.jpg" width="250" alt="MONUMENT (standard)"><br><sub>MONUMENT (standard)</sub></td><td align="center"><img src="docs/screenshots/formation-parade.jpg" width="250" alt="PARADE"><br><sub>PARADE</sub></td><td align="center"><img src="docs/screenshots/formation-housing-block.jpg" width="250" alt="HOUSING BLOCK"><br><sub>HOUSING BLOCK</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/formation-front-page.jpg" width="250" alt="FRONT PAGE"><br><sub>FRONT PAGE</sub></td><td align="center"><img src="docs/screenshots/formation-switchboard.jpg" width="250" alt="SWITCHBOARD"><br><sub>SWITCHBOARD</sub></td><td align="center"><img src="docs/screenshots/formation-dossier.jpg" width="250" alt="DOSSIER"><br><sub>DOSSIER</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/formation-obelisk.jpg" width="250" alt="OBELISK"><br><sub>OBELISK</sub></td><td align="center"><img src="docs/screenshots/formation-silence.jpg" width="250" alt="SILENCE"><br><sub>SILENCE</sub></td><td align="center"><img src="docs/screenshots/formation-terminal.jpg" width="250" alt="TERMINAL"><br><sub>TERMINAL</sub></td></tr>
<tr><td align="center"><img src="docs/screenshots/formation-iconostasis.jpg" width="250" alt="ICONOSTASIS"><br><sub>ICONOSTASIS</sub></td><td align="center"><img src="docs/screenshots/menu-formations.jpg" width="250" alt="Formations ▸ (in the gear menu)"><br><sub>Formations ▸ (in the gear menu)</sub></td></tr>
</table>

Withheld from this record: the phone's own lock and PIN screens in uniform,
the seal, the boot sequence, the reveille ringing and the charging screen.
The phone used blanks the lock screen in every screenshot. Only a camera
could document them.

<a id="the-terminal"></a>

## 🏛️ IV. THE TERMINAL

### ■ Layout

- **Masthead.** The emblem, whose gear turns once every 24 s around the fixed
  red disc and cross. **Tap the gear for the menu.** Beside it, **LAIBACH**
  in Roboto Flex at its narrowest and heaviest, and the network status with
  the uniform in force (`WI-FI`, `VPN (NOTED)`, or `OFF GRID (REPORTED)`).
  Below: a heavy accent rule and the device line: the version of the launcher
  and of Android. Android's status bar is withdrawn. Swipe down
  from the top edge to recall it.
- **The monument.** It scrolls away with the list of programmes.
  - A huge clock with the colon in the accent colour. Tap it for alarms.
    Long-press it for the menu.
  - The date and the day of the plan (`SATURDAY, 24 OCTOBER · PLAN DAY 297`).
    The plan day is the day of the year.
  - A proclamation in serif capitals. One is issued per day. Tapping it
    issues the next.
  - The citizen's papers: `CITIZEN № 0438-2177-6512-0941-3378-7`, with
    `STANDING B · OBEDIENT` on the line below.
  - SERVICE (uptime), RATION LEFT (attention left today, or OVERDRAWN) and
    LIFE FORCE (battery, ▲ while charging, red at 15% or below).
  - The ration bar, with a tick at each quarter, and `N% OF 2H 0M ATTENTION
    CONSUMED (ESTIMATED)`.
  - The decree in force: `■ DECREE 297/3 · MINISTRY OF …`, then its text.
  - The credit for the wallpaper: `■ APPROVED ART OF THE DAY`, then
    `TITLE — FROM: ARTIST`.
- **PERMITTED PROGRAMMES.** The list of apps: numbered rows, cogwheel badges,
  condensed capitals, grouped under their ministries. The count stands on the
  right.
- **COMMAND ▶ REQUEST A PROGRAMME.** The search line. While the citizen
  types, the monument steps aside, the first match receives a ▶, and the Go
  key opens it.

### ■ Formations

The uniform decides what the terminal wears. The formation decides where
everything stands. Choose one under **gear ▸ Formations**. The
change is stamped into the permanent record, and the terminal redraws at once.

| Formation | Order of the day |
|---|---|
| **MONUMENT** | The standard: the monument over the full register. |
| **PARADE** | The names march past, centred, in large narrow capitals. No badges. |
| **HOUSING BLOCK** | Badges in a grid, four flats to a floor, each ministry its own block. |
| **FRONT PAGE** | The state newspaper: the proclamation as a serif headline, programmes in two columns. |
| **SWITCHBOARD** | Numbered switches in frames, three to a row. The gauges stay. |
| **DOSSIER** | Small print. Every programme fits on one screen. |
| **OBELISK** | The hour stacked over the minute, everything aligned right. |
| **SILENCE** | Only the hour and the names. Silence is a duty. |
| **TERMINAL** | Fixed-width type, the command line on top, `$` for the prompt and `>` before each programme. |
| **ICONOSTASIS** | Badges alone, five to a row, one unbroken wall. |

Each formation stands down what it has no room for (the gauges, the TV strip,
the approved art, the ministry headings) and leaves the rest in place. In a
grid, a tap still opens a programme and a long-press still opens its menu.
The first match of a search is marked in the accent colour.

### ■ Standard procedures

| Action | Result |
|---|---|
| **Tap the gear** (or long-press the clock) | The menu |
| **Long-press a programme** | Redact / Declassify, Rationed, Essential, Transfer to ministry, Dossier, Banish (redacting and banishing need an exit visa) |
| **Tap the footer** `N REDACTED — DECLASSIFY` | Everything listed, redacted and suspended programmes dimmed |
| **Home or Back** | Search cleared, back to the top, redacted programmes hidden again |

- **Suspended programmes are removed automatically.** They return without a
  restart once unsuspended. They never appear in search.
- **Redacted programmes can still be found by search.**
- While everything is declassified, the footer reads `ALL PROGRAMMES
  DECLASSIFIED — HIDE REDACTED`. Tap it again or choose Hide redacted in the
  menu to redact them again.
- **Banish** is offered only for programmes that are not part of the system.
- The clock obeys the phone's 12/24-hour setting.

**The menu** holds Uniforms (with the five-year plan), Formations, Papers &
permanent record, Weekly report, Mobilisation orders, the Constitution, Art &
camera (Volkskunst, the next approved artwork, Laibach Camera), Duties
(mobilise, roll call, curfew drill), Show redacted (Hide redacted while they
are shown) and Settings.

**Settings** opens the **Regulations**: the launcher's own settings on one
page in the uniform. Each line states how things stand (`ON`, `OFF`, a time,
`GRANTED`); a tap changes it.

| Section | Lines |
|:--|:--|
| Sound | Sounds · Morse vibration · Issue sounds to the phone |
| Display | Animation |
| Lock screen | Seal · Uniform on the lock screen |
| Routines | Curfew · Reveille (time) · Reveille tone · Cancel reveille |
| Camera | Laibach Camera (issued or withdrawn) |
| Censor | The censor's desk (opened or closed) |
| Permissions | Decrees (notification access) · Attention (usage access) |
| Language | English · Deutsch · Slovenščina |
| System | Defect to another home screen · Android settings |

Back, home or `✕` closes it.

The redacted list, the uniform and the language are filed in the app's own
preferences (`laibach`).

### ■ Approved art of the day

The background is one work from the Volkskunst → Volkart gallery on
laibach.org (`https://www.laibach.org/volkskunst/volkart/`, about 19 pages of
fan art). The citizen does not choose it. It is approved.

- Once a day, when the terminal comes up, the state selects a random page and
  a random work on it. The picture is downloaded at the width of the screen
  and one copy is kept (`files/wallart.jpg`) with its title and artist. The
  copy serves until the next day, offline included.
- It is shown centre-cropped, in greyscale with a little extra contrast,
  under a veil of the uniform's background colour. The veil is thinner behind
  the clock and thicker behind the list, so that orders remain legible. The
  colours obey the uniform, including the light KAPITAL one.
- **Tap the credit** (or menu → Next approved artwork) for another approved
  work. **Long-press the credit** to open the Volkskunst page on laibach.org
  in the browser.
- The site captions its works as `Title - From: Artist`, `Title - Author:
  Artist`, `Title - Artist` or a title alone. All four are read. An unknown
  artist is recorded as `UNKNOWN`.
- With no connection and nothing kept, the credit reads `■ ART WITHHELD: NO
  CONNECTION` and the background stays plain.

### ■ Proclamations

Twenty-one, each in English and German. Among them:

> YOUR HAPPINESS HAS BEEN PRE-INSTALLED ■ THANK YOU FOR YOUR DATA. IT IS SAFE
> WITH US ■ THE FUTURE IS MANDATORY ■ REST IS SCHEDULED FOR 23:00 ■ FREEDOM
> IS A PREMIUM FEATURE ■ ALL LIFE IS AN APP. PLEASE RATE IT ■ WORK. OBEY.
> SCROLL ■ UPDATES ARE NOT OPTIONAL. NEITHER ARE YOU ■ ATTENTION ON LOAN FROM
> THE STATE ■ SLEEP IS A CIVIC DUTY ■ A QUIET PHONE IS A LOYAL PHONE ■
> EVERYTHING IS PERMITTED. EVERYTHING IS NOTED.

They are kept in `HOME` in `Slogans.java`, each as an English–German
pair. Amendments are made there.

<a id="uniforms"></a>

## 🎖️ V. UNIFORMS

Every citizen wears a uniform. Each uniform is named after a Laibach album
and cut after it: its own palette, emblem, animated scene behind the terminal
and the lock screen, and a motto that runs in the broadcast and the boot. A
change of uniform recolours the terminal and, if the lock screen is dressed,
the lock screen too. All of it is drawn in code. There is no album art, no
logo and no lyric in the app; only original shapes and slogans.

| Uniform | Colours | Emblem | Scene | Motto |
|---|---|---|---|---|
| **OPUS DEI** (default) | black, bone, red | cross with flared arms | banners hang in a nave, ash falls, a bell tolls | OBEY THE BELL. THE BELL OBEYS NO ONE. |
| **NOVA AKROPOLA** | charcoal, stone, oxblood, bronze | octagon | a temple's columns before a mountain, the sun crossing | BUILD THE NEW ACROPOLIS ON THE OLD MOUNTAIN. |
| **KAPITAL** | paper (light), black, red | chip with pins | a price line, volume bars, falling currency, a barcode | EVERYTHING HAS A PRICE. YOU ARE ON SALE. |
| **WAT** | black, white, red | square | a tunnel of squares, an equaliser at 128 BPM, a clock hand | WE ARE TIME. YOU ARE ON THE CLOCK. |
| **VOLK** | red, bone, black | rosette | a rippling banner, a turning ring of twelve crosses, a loudspeaker | ONE ANTHEM FOR EVERY NATION. ONE NATION FOR EVERY ANTHEM. |
| **IRON SKY** | night blue, bone, gold | riveted plate | stars, a cratered moon with its dark side, an orbit, an airship | WE CAME BACK FROM THE DARK SIDE. WE BROUGHT IT WITH US. |
| **SPECTRE** | near-black, ice white, neon red, cyan | cogwheel | the wheel, data rain and a neon grid | A SPECTRE IS HAUNTING YOUR SCREEN. |
| **ZARATHUSTRA** | dusk violet, cream, orange | sun of 24 rays | the sun rising behind a ridge, turning rays, a ring of eternal return | THE SUN RISES AGAIN. AND AGAIN. AND AGAIN. |
| **SOUND OF MUSIC** | alpine paper (light), pine, red | six-petal flower | the Alps in outline, a stave with a scrolling tune, flowers | THE MOUNTAINS LISTEN. SING ALONG. |
| **HIGH CONTRAST** | pure black, pure white, yellow | — | — | SEE EVERYTHING. EVERYTHING SEES YOU. |

Every motto exists in German and Slovenian as well.

- **HIGH CONTRAST** (since 2.3) follows no album. It serves citizens with weak
  eyes. The five-year plan never issues it. It must be requested.
- **Issue.** A uniform is drawn from the menu (tap the turning gear) →
  Uniforms. The issue is filed and also colours the Volkskunst gallery.
- **The five-year plan.** Menu → Uniforms → Five-year plan: one uniform per
  week, or one per day of the week, issued by the calendar. Choosing a
  uniform by hand suspends the plan. A dressed lock screen follows each new
  issue.
- **Amendment.** To add a uniform, add a line to `ALL` in `Variant.java`. Each
  uniform also carries a second neon, `glow`, which the 2.0 effects use beside
  the accent.
- On its first start, 2.0 issued every citizen the SPECTRE uniform, once, and
  noted it in the record. The citizen may change back at any time via
  Uniforms. It will not happen again.

<a id="the-collective"></a>

## ⚖️ VI. THE COLLECTIVE

Established in 1.4. The citizen is placed under the Laibach Collective, whose
doctrine is Total Harmony: *restraint is loyalty*. Every provision is issued
in English and German.

### ■ Papers and the attention ration

- **Citizen number.** The phone's app-scoped Android ID in full: 64 bits
  written as 20 digits, then a check digit, `0438-2177-6512-0941-3378-7`. Two
  phones share a number only if Android gave them the same ID. The check
  digit follows Damm's algorithm, so a mistyped digit or two swapped
  neighbours never pass for a real number. A phone with no Android ID is
  issued a random 64-bit number once and keeps it. The number survives
  reinstallation. It changes only after a factory reset or under a different
  signing key.
- **The ration.** Two hours of attention a day, counted from midnight. Time
  spent in other programmes is charged. Time spent on this terminal is time
  with the Collective and is free of charge.
- **Measurement.** With usage access (menu → Settings → Attention), the state reads Android's own usage events and adds up the
  foreground time of every other programme since midnight; the note then
  drops `(ESTIMATED)`. Without usage access, the state estimates attention
  from the time the terminal spends out of sight while the screen is on, and
  the note says `(ESTIMATED)`. Past the ration, the ration reads `OVERDRAWN`.
  With usage access the note then adds `(REPORTED)`.
- **The norm.** Every day has a norm, and the norm is always fulfilled: 101%
  and up, a little more for a better grade. The weekly report states the
  week's fulfilment and announces that the next norm has been raised
  accordingly. The figures are invented. They are also binding.

### ■ Standing

| Grade | Standing | Ration used |
|---|---|---|
| **A** | EXEMPLARY | up to 25% |
| **B** | OBEDIENT | up to 50% |
| **C** | ADEQUATE | up to 75% |
| **D** | UNDER REVIEW | up to 100% |
| **E** | UNDER OBSERVATION | up to 150% |
| **F** | RE-EDUCATION SCHEDULED | beyond |

Each curfew exemption taken today costs one further grade. From D downwards,
the standing is displayed in the accent colour.

### ■ Decrees

- With notification access (menu → Settings → Decrees), waiting
  notifications become decrees, one per ministry, for example `3
  COMMUNIQUÉ(S) AWAIT YOUR REPLY. SILENCE WILL BE NOTED.`
- **The state reads only which programme posted a notification and how many
  are waiting. Never the title. Never the text.** Ongoing, non-clearable and
  group-summary notifications are disregarded. The listener is
  `DecreeListener.java`.
- **Tap** a decree to obey it: the programme with the most notifications in
  that ministry opens. **Long-press** for the next decree.
- Without notification access, or when nothing is waiting, standing decrees
  are issued. They change daily.

### ■ Ministries

Every programme answers to one ministry: Communication, Enlightenment,
Labour, Joy (marked RATIONED), Movement, Prosperity, or Administration for
the remainder. The ministry is assigned from the package name first, then
from the category the programme declares. **Long-press a programme →
Transfer to ministry** to reassign it. Once transferred, the same submenu
offers **Return to its own ministry**. Transfers are filed in the preferences
file `ministries`. Search shows a plain list, without ministry headers. The
keywords are kept in `Ministry.java`.

### ■ Exit visa

No programme leaves the terminal without papers. **Redact** and **Banish**
first lay **Form 7-B** before the citizen: applicant, ministry, destination
(`THE ARCHIVE (REDACTION)` or `OBLIVION (UNINSTALL)`) and a reason for
leaving, from *Insufficient loyalty* to *No reason. (This is noted.)*
**Stamp** brings down `APPROVED`, and only then is the programme redacted, or
Android asked to uninstall it. Each visa is numbered, `EXIT VISA № 0001`,
and the defection goes on the record. The papers count the defections.
Declassifying needs no visa: the state always welcomes a return.

### ■ Ministry quotas

**Menu → Settings → Regulations → Ministry quotas** (off by default) gives
each ministry a daily allowance: Communication and Enlightenment 60 minutes,
Joy 45, Movement 30, Prosperity 20. Labour and Administration have none and
never close. Once a ministry's allowance is spent, its programmes are `CLOSED
FOR INVENTORY` until midnight. The citizen may **Comply**, or **Reopen
(noted)**, which opens that ministry again for the rest of the day and writes
it on the record. Quotas need usage access. Without it they are not
measured, and nothing closes. The papers list each ministry's spending.

### ■ The waiting room

A rationed programme (the Ministry of Joy) is not opened at once. The
citizen draws a number, `YOUR NUMBER IS 0047`, and waits about three seconds
while the counter calls `NOW SERVING 0044`, `0045`, `0046`. Then `PROCEED TO
THE COUNTER` and the programme opens. Numbers rise through the day and start
again each morning. **Back** or **Leave the queue** abandons the place.

### ■ The informant

With usage access, the weekly report names the programme that took most of
the week's ration: `THE INFORMANT REPORTS: MOST OF THE RATION WENT TO …`.
The informant reports only to the citizen. Nothing leaves the phone.

### ■ Curfew

- From 23:00 to 06:00 the terminal is covered by `REST IS COMPULSORY`, with
  the duration of the curfew so far, the time, and three permitted actions:
  PHONE, ALARM and EMERGENCY (dials 112). Programmes already open are not
  blocked. The curfew covers the terminal only.
- **Hold** `HOLD TO REQUEST AN EXEMPTION` for 30 minutes of ordinary use. A tap
  only explains that it must be held. Each exemption lowers today's standing
  by one grade.
- From 22:55 the ticker counts down (`CURFEW IN 5 MINUTES. PREPARE FOR REST`) and
  the uniform darkens a little more each minute.
- **Menu → Duties → Curfew drill** shows the curfew screen at any hour.
  Holding the button ends the drill without penalty.
- **Menu → Settings → Curfew** suspends it (`OFF`) and reinstates it
  (`ON`). It is in force by default.
- The ration, exemptions and the curfew setting are filed in the preferences
  file `citizen`.

<a id="total-harmony"></a>

## 🗂️ VII. TOTAL HARMONY

Established in 1.5. Paperwork is introduced. Joy is rationed. Every provision
is issued in English and German.

### ■ Papers

Tap the papers row on the terminal (citizen number and standing), or go to
**menu → Papers & permanent record**. The papers have four parts:

- **Identity card.** Emblem, number, standing, issued on the day the launcher
  was installed, valid PERMANENTLY, issued by THE STATE, with a barcode made
  from the number. `THIS CARD IS THE PROPERTY OF THE COLLECTIVE. SO IS THE
  BEARER.`
- **Statistics of the day.** Attendance, attention used, exemptions, joy
  coupons, self-criticisms and record entries. COLLECTIVE HARMONY always
  stands at 99.something percent. NORM FULFILMENT always exceeds 100%. Both
  change daily. Both are invented.
- **Ministry quotas**: what each ministry has spent today against its
  allowance (see [Ministry quotas](#-ministry-quotas)).
- **The stamp album**, described below. The statistics also count stamps,
  defections, entry visas and comrades.
- **The drill schedule**: today's drills and how each was answered.
- **The passport**: the latest entry visas, described below.
- **The counter.** **Check a citizen number** takes any 21-digit number and
  checks its Damm digit: `VALID` or `FORGED`. One mistyped digit, or two
  neighbours swapped, is always caught. The citizen's own number is
  recognised: `THIS IS YOU. YOU MAY PASS.` Forgeries are noted on the record.
- **Article of the day** from the Constitution.
- **Self-criticism and the permanent record**, described below.
- **Save the card as a picture** puts it in Pictures/Laibach. **Share the
  card (specimen)** hands the same picture to any app through the share
  menu. Either way, `SPECIMEN` is printed across it and along its foot: `A
  WORK OF FICTION · CONFERS NO RIGHTS`. The shared copy lives in the
  launcher's cache and is swept away after a day.

### ■ The stamp album

Seventeen stamps, each for a first deed the state wishes to see again: the
first roll call, 7 and 30 days present, the first confession, a promotion, a
curfew drill, checking one's own papers, a forgery confiscated, an exit
visa, a text censored, a print developed, a weekly report read, Foundation
Day, a drill answered, the first entry visa, a comrade registered and a
poster printed. Stamps not yet issued are drawn in outline. An issued stamp
bears its date. Tap it to see it awarded again. The album keeps only the
date of each stamp, in the preferences file `stamps`.

### ■ Ceremonies

A promotion is not a toast. When the standing rises during the day (a
forgiven confession lifts it a grade), the terminal is covered by a medal on
its ribbon, its sunburst turning, with the state's fanfare: `PROMOTION. YOU
ARE RAISED TO …`. Attendance is decorated the same way at 7, 30, 100 and 365
days: the **Order of Attendance**. `THE COLLECTIVE APPLAUDS. BRIEFLY.` A tap
returns the citizen to work. The fanfare follows the Regulations' sound
switch.

### ■ Foundation Day

Each year, on the day the launcher was first installed, the state celebrates
its founding: a ceremony, a stamp, a note on the record and **one extra joy
coupon** for the day. A terminal founded on 29 February observes the day on
28 February in other years.

### ■ The permanent record

**Off until the citizen asks for it.** Turn it on with **Settings →
Permanent record** or **Keep the record** in the papers. Until then nothing
is written down, the papers say `THE RECORD IS NOT KEPT`, and the
statistics, the weekly report and the case file show the record as `NOT
KEPT`.

Once kept, the state notes what the citizen does in the launcher: transfers,
banishments, redactions, changes of uniform and language, curfew exemptions
and drills, decrees obeyed, joy spent and roll calls. The record keeps the
last 100 entries, in the preferences file `record`. **Hold** `HOLD TO DELETE
THE RECORD` to empty it. One entry remains: `RECORD DELETED. THE DELETION HAS
BEEN NOTED.` Turning the record off again, after a confirmation, destroys
every entry.

Wherever this README says something is noted on the record, it is noted
only while the record is kept.

### ■ The Constitution

Thirteen articles, such as *All citizens are equal. Their screen time is
not.* Open it with **menu → The Constitution** or from the article of the
day. **I do not agree** is accepted with `Your disagreement has been noted.
You agree.`

### ■ Self-criticism

**Submit self-criticism** lists eight faults, such as *I charged my phone
before it was necessary*. Any of them may be confessed, including *I have
nothing to confess*. The first confession of the day is forgiven and raises
today's standing by one grade: `YOU ARE FORGIVEN. YOU ARE NOT FORGOTTEN.`
Further confessions that day are filed without effect.

### ■ Morning roll call

The first time the terminal is shown after 06:00, it is covered by `MORNING
ROLL CALL` until the citizen presses **PRESENT**. Each day answered extends
the attendance streak. Back counts as absence and resets the streak. **Menu →
Duties → Report for roll call** summons it at any hour.

### ■ The drill schedule

**Menu → Duties → Drill schedule** orders up to eight drills a day, each at
an hour the citizen chooses. At its hour a drill is called on the roll-call
form, `DRILL 18:30`, and answered **PRESENT** or **ABSENT**. An absence is
written down but does not break the attendance streak. Only the latest drill
is called: one passed over while the phone lay untouched stays missed. A
drill whose hour has already passed when it is ordered waits until tomorrow.
Tap a drill in the schedule to strike it. The Regulations show the hours
(`OFF` when none are ordered). The hours and the day's answers are kept in
the preferences file `drills`.

### ■ The passport

Each time the phone enters a Wi-Fi network after a quarter of an hour or
more away from every network, the papers earn an **entry visa**. The
passport page in the papers shows the latest nine as stamps, each with its
date and number, set at a slight angle. The launcher asks Android only
whether Wi-Fi is up. It never asks the network's name, so no location
permission is needed, and the record says only `ENTRY VISA STAMPED. THE
NETWORK IS NOT NAMED.` The count and the dates of the last 24 visas are kept
in the preferences file `passport`.

### ■ The citizen exchange

**Menu → Citizen exchange** shows the citizen's number as a QR code holding
a `laibach://comrade/…` link. A comrade points their phone's own camera app
at it, and the link opens the exchange on their phone, which checks the Damm
digit and asks before registering. **Type in a comrade's number** does the
same by hand. Forged numbers, the citizen's own number and comrades already
registered are refused. The register lists each comrade's number and the day
they were registered. Tap one to strike them. Only the numbers pass between
the phones: nothing goes over a network, and the launcher asks for no camera
permission. The register is kept in the preferences file `comrades`.

### ■ Joy coupons

The Ministry of Joy issues **5 coupons a day**. Each opening of a Joy
programme spends one, and a toast reports how many remain. When none remain,
the citizen is offered **Renounce joy** or **Open anyway (noted)**. Each
"open anyway" lowers today's standing by one grade. Coupons and debts are
reset at midnight.

### ■ The dead drop

**Settings → Dead drop → Sealed note** leaves a note of up to 140 letters for
the citizen alone. It lies on the home screen under the date as a sealed
decree, every letter blacked out. **Hold** it and it is unsealed for six
seconds, then sealed again. The record notes that a note was left and that
the drop was opened, never what the note says. **Empty it** takes it away.
The note stays on the phone.

### ■ Lesser provisions

- A search that finds nothing reports `NO SUCH PROGRAMME HAS EVER EXISTED.`
- At 15% battery or less, when not charging, the proclamation becomes `LIFE
  FORCE LOW. REPORT TO A CHARGING STATION.`

<a id="synthetic-state"></a>

## 📡 VIII. SYNTHETIC STATE

Version 2.0. The terminal is set in motion. It boots, it glitches, it watches
and it broadcasts. Everything below is drawn in code (`Cyber.java` and
the effect files beside it, `Fx.java`). No images or fonts were added.

| Effect | Description |
|---|---|
| **Boot** | On a cold start, and after an hour's absence, LAIBACH OS 2.0 comes up as a typed terminal: mounting the citizen number, loading ideology, calibrating enthusiasm (112%), scanning for dissent (none). The screen then glitches into the terminal. Tap to skip. |
| **Backdrop** | The uniform's scene moves behind the clock. In SPECTRE a giant cogwheel turns slowly against a dashed dial, glyphs (`LAIBACH`, `NSK`, binary, Cyrillic, `✚ ■ ▲`) fall like data rain over the upper half, and a neon grid floor advances from the horizon. |
| **CRT glass** | Scanlines, a vignette and a refresh band rolling down every 7 s lie over everything, including the curfew, the roll call and the papers. Like an old set, the glass flickers faintly now and then of its own accord. |
| **Glitches** | The whole state fails at once, at moments no one can foresee: the quiet between failures is drawn at random (mostly 3 to 20 seconds), a third of them come as a stutter of two to four quick tears, and about one in twenty-four is a breakdown of a second or more. Each glitch picks its own kinds and strength: the split into the uniform's two neons, slices torn sideways (leaving a hole or a ghost), a shake, dropped blocks, a slipping vertical hold, snow and a flash. Every glitching text has a temper of its own, so a weak glitch passes some by and catches others late. The clock tears by itself as the minute turns. |
| **The eye** | Beside the status in the masthead, an eye watches. It blinks every six seconds or so and has a red recording light. |
| **Pulse** | An ECG line under the ration bar shows the citizen's heartbeat as the state assumes it: 64 BPM at standing A, plus 9 for each grade lower, plus 12 when the ration is overdrawn. |
| **Broadcast** | A red ticker above the command line runs the state's news: `YOUR THOUGHTS ARE BACKED UP HOURLY`, `THE ALGORITHM LOVES YOU`, `DISSENT HAS BEEN DEPRECATED`, and the citizen's own number, under observation. It opens with the weather, which is controlled: `WEATHER: CONTROLLED. SUN 23° ABOVE THE HORIZON, AS PLANNED. SUNSET AT 17:58 BY DECREE`. The sun is worked out from the clock and the time zone alone, at the capital's latitude; no location is asked for. |
| **Badges** | Every programme's icon is recoloured in two tones of the uniform, put under scanlines in a disc and set into the uniform's emblem in the accent colour, with a neon ring. Each programme receives a file number, `ID·F150`. |
| **Emblem** | The disc glows in the second neon, pulsing four times a turn. The gear advances like machinery, a tooth a second, snapped into place and held. |
| **Motion** | The papers and Volkskunst rise into place over the terminal and sink away when dismissed. The papers' sections, the regulations' lines and the menu's rows report one after another. Controls give a little under the finger, wide ones less than small. A new proclamation of the day slides in over the old. All of it shares one easing (`Motion.java`): fast out of the gate, a long settle. |

**Menus and dialogs.** The menu, Uniforms, the programme menu, their
submenus and every dialog (the Constitution, self-criticism, joy coupons,
Volkskunst) wear the uniform: the uniform's paper with a rule-coloured
border, an accent strip on top, a caption in accent capitals, condensed
capitals for the items, a filled accent square for the uniform in force and
an accent triangle for a submenu. A submenu opens in place; its caption (`◀`)
leads back. Light uniforms (KAPITAL, SOUND OF MUSIC) receive dark ink on
paper. Toasts remain the phone's own. The code is in `StateMenu.java`.

### ■ The seal (lock screen)

When the screen comes on, a sealed terminal lies over Android's own lock
screen: the time, the power ration, standing, submissions today, curfew and
surveillance as a readout, the uniform's scene and a ticker. The content of
notifications is never read. The seal says only that the Collective has said
everything.

- **Fingerprint.** On phones with a sensor under the screen, the seal (the
  uniform's emblem) sits exactly over the sensor. Some systems disable the
  sensor while another window covers the lock screen, so touching the seal
  hands over at once: `PRESENT YOUR FINGER TO THE STATE.` The phone's own lock
  comes up with the sensor running. Lift the finger and present it again. The
  launcher never examines the finger itself. A fingerprint unlock counts as a
  submission. The sensor's position is read from the system, or from a table
  of known phones. Elsewhere the seal sits at the middle of the bottom edge.
- **Hold to submit.** Where the seal is not over a sensor, holding it for 1.4 s
  submits. If the phone is secured, Android's own PIN or pattern screen comes
  up. The launcher never sees it and never stores it. Short taps, slips off
  the seal and taps right after the screen comes on are refused: `HESITATION
  HAS BEEN NOTED.` Touches right after the screen comes on are ignored on
  every phone.
- **Emergency.** `EMERGENCY 112` remains in the corner.

Menu → Settings → **Seal** lifts the seal (`OFF`) and restores it (`ON`).

### ■ The phone's own lock screen in uniform

Menu → Settings → **Uniform on the lock screen** sets the
uniform's scene, still, with its motto, as the phone's own lock-screen
picture, so that the system's lock and PIN screens wear it too. The seal is
lifted at the same time, so a single press on the fingerprint sensor unlocks
the phone. A change of uniform or language redraws the picture.

Only the lock screen's picture is set. The home picture is never touched, and
the state never reads the citizen's own picture. Switching it `OFF`
removes the uniform; the lock screen returns to the picture it showed before.
If the lock screen had a separate picture of its own, it shows the home
picture instead. Choose its own again in Settings → Wallpaper.

### ■ Pacify the display

Menu → Settings → **Animation** `OFF` pacifies the display: it stops every
effect. The backdrop and the glass remain as a still picture, and the screen
draws nothing while idle. `ON` sets it all in motion
again; with it off, screens also open and close without the uniform's motion.
Effects also stop by themselves in battery saver, and when Android's
animations are switched off (Developer options or accessibility). They run on
one shared 30 fps clock, and only while the terminal is visible.

<a id="mobilisation"></a>

## 🚩 IX. MOBILISATION

Version 2.3. Twenty measures, enacted together. The state is connected to the
calendar, the battery and the band's own news.

### ■ On the terminal

- **Mobilisation orders.** The band's future concerts, read from the public
  events feed on [laibach.org](https://www.laibach.org/future-events/), are
  issued as call-ups: `MOBILISATION ORDER: 30 JAN 2027 · LJUBLJANA, SI ·
  CANKARJEV DOM`. The next order runs in the ticker. Tap the ticker, or
  choose **Mobilisation orders** in the menu, for the full list, with a link
  to each ticket page. Sold-out dates are marked. The list is fetched at most
  every three hours and kept for when there is no network.
- **Bulletins.** Besides the order, the ticker carries the number of further
  call-ups, the newest Laibach TV transmission (`NOW ON LAIBACH TV`) and, on
  a low battery, the austerity notice.
- **The Laibach TV strip.** Under the decree: `ON AIR · LAIBACH TV`, with the
  newest video's still and title. Tap it to report to Laibach TV.
- **Austerity.** At 15% battery or less, and not charging, the state
  suspends colour and motion. The terminal turns grey and stands still until
  the charger goes in. This is entered in the record once a day.
- **The ration warning.** Once the day's two hours of attention are spent,
  opening a *rationed* programme requires a decision: **Comply** (discipline
  noted) or **Open anyway (noted)**. A programme is declared **Rationed** or
  **Essential** in its long-press menu.

### ■ Routines

- **Reveille.** The state's own alarm. Settings → Reveille sets the time;
  Reveille tone chooses the bell or the state's fanfare. It appears over the lock screen,
  wakes the phone from Doze and is shown by Android as the next alarm. It
  stops only when the citizen answers the roll call with PRESENT. It survives
  a reboot, a change of time and an update.
- **Mobilise.** Duties → Mobilise, for 30 minutes, 1 hour or 2 hours.
  Only Communication, Movement and *essential* programmes remain on the list.
  The footer reads `MOBILISED UNTIL 15:30 · 14 STOOD DOWN — STAND DOWN`. Tap
  it to stand down early. Mobilisation ends by itself.
- **The weekly report.** Issued every Sunday, the first time the terminal is
  seen: attention spent on each of the last seven days against the ration,
  days overdrawn, unlocks, attendance, new record entries, the curfew and the
  citizen's standing. Also available from the menu and from the papers. The
  attention lines need usage access. Without it, the report says so.
- **The five-year plan.** See [Uniforms](#uniforms).

### ■ Power

- **The charging screen.** Connecting the charger while the terminal is up
  shows `POWER RATION REPLENISHED` with the life force, for a few seconds. Tap
  to dismiss.
- **The lock screen**, when dressed, now carries the plan day and the
  uniform's motto, low down, beneath the phone's own clock.

### ■ Export of papers

Papers → **Papers for export**. Everything is written to the phone's shared
storage, into a `Laibach` folder, and nowhere else.

| Export | Contents | Destination |
|---|---|---|
| **Save the card as a picture** | The citizen card, overprinted SPECIMEN so that it is never mistaken for a document | PNG in `Pictures/Laibach` |
| **Share the card (specimen)** | The same picture, lent to the app the citizen chooses through the share menu | JPEG in the launcher's cache, swept away after a day |
| **Print the case file** | Citizen number, standing, the weekly report and the permanent record | PDF in `Download/Laibach` |
| **Back up the settings** | All of the launcher's preferences (uniform, plan, language, hidden, rationed and essential programmes, ministries, curfew, reveille, sounds, the record, the stamp album, the exit visas, the drills, the passport and the comrades) | JSON in `Download/Laibach` |
| **Restore the settings** | Reads the newest backup there and redraws the terminal | — |

The dressed lock-screen picture belongs to the phone and is excluded from
the backup.

### ■ Senses

- **Sounds** (Settings → Sounds). Off by default; switch them on or off there at any time. All of them are
  synthesised in the app: a boot chime pitched for each uniform, and three
  rising pings when the power ration is replenished. The reveille rings
  regardless of this setting.
- **Issue sounds to the phone** (Settings). Writes the state's sounds as
  ringtones, notification sounds and alarms to `Ringtones/Laibach`,
  `Notifications/Laibach` and `Alarms/Laibach`: the bell, the anthem, the
  decree, the stamp, the charging pings and a call for each uniform. Choose
  them in the phone's own sound settings; the launcher never sets one itself.
  Every sound is synthesised by the launcher; none is taken from a record.
- **Morse** (Settings → Morse vibration). Off by default. When new decrees arrive,
  and when the reveille rings, the phone taps L A I B A C H in Morse on the
  citizen's hand. Calls keep their own vibration; Android permits no other
  app to set it.
- **Anthem of the hour** (Settings). Off by default. On the hour, while the
  home screen is open, a short brass chime, pitched for the hour. It is silent
  when the phone is silent or set to vibrate, under do not disturb, during a
  call, through the curfew and from 22:00 to 07:00.
- **Typewriter** (Settings). Off by default. Keys strike as words are blacked
  out at the censor's desk, and a line is typed, with its bell and carriage
  return, when the memo is issued.
- Both are heard only while the sounds are on.

### ■ Quick-settings tiles

Three tiles for the notification shade: **Curfew** (in force / suspended),
**Pacify** (still / in motion) and **Dress** (the lock screen in uniform or
plain). The citizen installs them from the shade's edit view.

### ■ Accessibility

The emblem, the eye, the statistics, the life force and the TV strip are
labelled for TalkBack. The moving decorations are hidden from it. The HIGH
CONTRAST uniform is described under [Uniforms](#uniforms).

<a id="laibach-tv"></a>

## 📺 X. LAIBACH TV

The state broadcaster carries one channel: Laibach's own YouTube channel
(`UCE1VRNkKm2u6xI_No7SCKVQ`). It is listed with the other programmes under
the Ministry of Enlightenment as **Laibach TV**. Watching it is a duty, not a
joy, so it costs no joy coupon. It also has its own launcher entry, so other
launchers list it too.

- **The programme guide** is a row of chips: LATEST (the channel's uploads),
  then the channel's own playlists: Official videos, Featured, Alamut, Musick,
  Jesus Christ Superstars, Opus Dei, Early videos, Remixes, Documents and
  Documentaries, and ★ WATCH LATER. They are kept in `PROGRAMMES` in
  `BroadcastActivity.java`.
- **Source.** YouTube's public Atom feeds (`/feeds/videos.xml?channel_id=` and
  `?playlist_id=`). No account, no API key, no sign-in. Each feed holds at
  most the newest 15 videos, so every programme shows up to 15. Entries
  uploaded by other channels are excluded, even when they appear in one of
  the playlists. Feeds are kept for 30 minutes while the launcher runs. Stills
  (`mqdefault.jpg`) are shown in grey. The one on air is in colour.
- **The player** is YouTube's IFrame player from `youtube-nocookie.com` in a
  WebView, with no related videos and no annotations. It may go full screen,
  which turns the phone to landscape. Back leaves full screen.
- **Resume.** A transmission left part-way through resumes where the citizen
  stopped. The list shows `RESUMES AT 3:12`.
- **Watch later.** Long-press a video to set it aside. The **★ WATCH LATER**
  programme lists them, newest first. Long-press again to withdraw one.
- **Withheld by design:** search, comments, Shorts, subscriptions,
  recommendations and autoplay. When a video ends, the screen reads
  `TRANSMISSION ENDED. REMAIN TUNED.` Nothing follows.
- **No exit.** Every link in the player is blocked: the logo, the title,
  *Watch on YouTube*, the end screen. Each reports `THE WIDER YOUTUBE IS NOT
  PERMITTED.`
- **Restricted transmissions.** Some owners forbid embedding. The player
  reports this as error 101 or 150 (153 when it cannot tell where it is
  embedded). The screen then offers to open **that one video** in the YouTube
  app or the browser. Any other error reads `SIGNAL LOST`.
- Playback pauses when the screen is left. There is no listening in the
  background.
- Every video watched is entered in the permanent record as `TRANSMISSION
  RECEIVED: <title>`.

The real YouTube app is not affected. To keep only Laibach TV, redact YouTube
(long-press → Redact) and it leaves the list. To forbid it outright, use
Android's own controls (Digital Wellbeing or Family Link).

<a id="volkskunst"></a>

## 🖼️ XI. VOLKSKUNST

A framed gallery of the citizen's own pictures, modelled on the fan-art
section of the Laibach website. Open the menu (tap the gear) and choose
**Volkskunst (your art)**. The empty gallery says `THE PEOPLE'S ART AWAITS
YOUR CONTRIBUTION.`

- **■ SUBMIT PICTURES / + SUBMIT** opens Android's system photo picker. The
  citizen chooses what to submit. The launcher needs no storage permission
  and sees nothing else on the phone.
- Each submitted picture is copied into the launcher's private folder
  (`files/volkskunst/`), turned upright according to its EXIF tag and scaled
  to at most 2048 px on the long side. **The original stays where it was and
  is never altered.** The newest submissions come first.
- Pictures are shown one at a time, in greyscale with a little extra
  contrast, inside a frame in the uniform's ink colour. The counter
  (`01 / 04`) stands at the top right.
- **Swipe** left or right to move between pictures. The ends resist.
- The caption reads `■ TITLE — FROM: NAME` (`TITEL — VON: NAME` in German).
  **Long-press the picture**, tap the caption or tap **LABEL** to amend it.
  New pictures are filed as `UNTITLED`, under the last author entered (or
  `UNKNOWN`). When a single picture is submitted, the caption box opens at
  once.
- **REMOVE** asks for confirmation, then deletes only the launcher's copy.
- Back or ◀ closes the gallery.

Titles and authors are filed in the preferences file `volkskunst`.
Uninstalling the launcher destroys the copies and captions. The citizen's own
photos are untouched.

### ■ Laibach Camera (the darkroom)

**Laibach Camera** has its own icon and appears in every app's share menu
as **Submit to the censor**. It opens from the gear menu too (Art & camera).

- **A photograph** comes from the phone's own camera app (asked for a single
  picture, so the launcher needs no camera permission), from the system photo
  picker, or from another app's share menu.
- **Development.** The print is developed in the manner of a uniform. Eyes
  are blacked out where the phone's face finder sees them. The citizen adds
  bars and paints people out by hand.
- **The print** is framed with a stamp, the seal, a decree and its number. It
  is filed in `Pictures/Laibach` or shared. It is drawn from pixels alone, so
  no location, time or make of camera leaves with it.
- The photograph itself stays only while it is on the table.
- **Withdraw the camera** (Settings → Laibach Camera `OFF`) hides its icon
  and its share entry; `ON` issues it again.

### ■ The censor's desk (text)

Text has its own **Submit to the censor** in every app's share menu. The
text is laid on the censor's desk, typed on paper, with the bars already in
place:

- **Blacked out by rule:** links, e-mail addresses, handles and telephone
  numbers; every number; names (capitalised words inside a sentence, a rule
  the censor suspends for German, where every noun is capitalised); and a
  short list of unsuitable words in all three languages (*freedom*, *truth*,
  *protest*, *border*, *election* and their kin).
- **By hand:** a tap on a word blacks it out or lifts the bar.
- **Issue** shares the text as a decree: the number, the ministry, the stamp
  (`CENSORED` or `APPROVED`), the text with `█` bars as long as the words,
  and the Department of Correspondence. **As a memo** types it on paper
  under the seal with a rubber stamp and shares it as a picture. **Copy**
  puts the censored version on the clipboard; the original is never copied.
- The redaction is plain pattern-matching on the phone. Nothing is sent
  anywhere, and the text is gone when the desk is cleared.
- Opened from the share menu only (no icon). Settings → The censor's desk
  `OFF` takes it out of the share menu.

### ■ The poster press

**Menu → Art & camera → Poster press** prints a slogan of up to 60 letters
as a propaganda poster in one of three manners, in the uniform's paper, ink
and accent:

- **Constructivist**: a circle, a diagonal and a star, with the slogan set
  flush left below.
- **Woodcut**: a sun over the horizon in cut rays, framed like a block
  print. The gouge marks fall differently for each slogan.
- **Cold war**: the emblem in the sights of a radar, `NOTICE TO ALL
  CITIZENS`, with the slogan on a band of ink below.

The uniform suggests a manner; any of the three may be chosen.

The proof follows the slogan as it is typed. **Save** puts the poster
(1080 × 1350) in `Pictures/Laibach`. **Share** hands it to any app through
the share menu, from the launcher's cache. The record notes that a poster
was printed and in which manner, never its slogan.

<a id="language-of-instruction"></a>

## 🗣️ XII. LANGUAGE OF INSTRUCTION

English (by default), German or Slovenian. Request another in the menu under
**Language · Sprache · Jezik**. The choice is filed (`lang` in the `laibach`
preferences) and is enforced on the terminal, its menus and dialogs, the
papers, Laibach TV and the Volkskunst gallery. Names of programmes remain as
Android issues them.

Slovenian (since 2.3) is looked up from the English text in `slovene.txt`, a
plain table beside `Slovene.java`: one text a line, English and Slovenian split
by a tab. Corrections are made there; no code changes.
A text without a Slovenian version is delivered in English. Dates and numbers
follow the Slovenian locale.

<details>
<summary><b>■ The official glossary (English · Deutsch · Slovenščina)</b></summary>

| English | Deutsch | Slovenščina |
|---|---|---|
| PERMITTED PROGRAMMES | ERLAUBTE PROGRAMME | DOVOLJENI PROGRAMI |
| COMMAND ▶ REQUEST A PROGRAMME | BEFEHL ▶ PROGRAMM BEANTRAGEN | UKAZ ▶ ZAHTEVAJTE PROGRAM |
| SERVICE · RATION LEFT · LIFE FORCE | DIENSTZEIT · RESTZUTEILUNG · LEBENSKRAFT | SLUŽBA · PREOSTANEK OBROKA · ŽIVLJENJSKA SILA |
| CITIZEN № · STANDING | BÜRGER NR. · ANSEHEN | DRŽAVLJAN ŠT. · UGLED |
| DECREE · MINISTRY OF … | ERLASS · MINISTERIUM FÜR … | ODLOK · MINISTRSTVO ZA … |
| REST IS COMPULSORY | RUHE IST PFLICHT | POČITEK JE OBVEZEN |
| Regulations | Vorschriften | Predpisi |
| Transfer to ministry | Ministerium wechseln | Premesti na ministrstvo |
| PLAN DAY 297 | PLANTAG 297 | DAN NAČRTA 297 |
| APPROVED ART OF THE DAY | GENEHMIGTE KUNST DES TAGES | ODOBRENA UMETNINA DNEVA |
| N REDACTED — DECLASSIFY | N GESCHWÄRZT — FREIGEBEN | N ZAKRITIH — RAZKRIJ |
| HIDE REDACTED | GESCHWÄRZTE VERBERGEN | SKRIJ ZAKRITE |
| Redact / Declassify | Schwärzen / Freigeben | Zakrij / Razkrij |
| Dossier (app info) | Akte (App-Info) | Dosje (podatki o aplikaciji) |
| Banish (uninstall) | Verbannen (deinstallieren) | Izženi (odstrani) |
| Suspended by order | Auf Anordnung gesperrt | Ukinjeno po ukazu |
| Uniforms | Uniformen | Uniforme |
| Defect to another home screen | Zu anderem Startbildschirm überlaufen | Prebegni k drugemu domačemu zaslonu |
| OFF GRID (REPORTED) · VPN (NOTED) | NETZFREI (GEMELDET) · VPN (VERMERKT) | BREZ OMREŽJA (PRIJAVLJENO) · VPN (ZABELEŽENO) |
| LAIBACH LIFE™ | LAIBACH LEBEN™ | LAIBACH LIFE™ |

</details>

<a id="what-the-state-does-not-see"></a>

## 🔒 XIII. WHAT THE STATE DOES NOT SEE

Only if the citizen asks it to keep the permanent record does the state note
what they do *in the launcher*. It never looks further.

> [!NOTE]
> - **Usage.** The ration, the quotas and the informant read Android's own
>   usage events on the phone. Which programme was used, and for how long, is
>   added up there and never sent anywhere.
> - **Notifications.** Decrees read only which programme posted a
>   notification and how many are waiting. Never the title. Never the text.
> - **Photos.** The system photo picker shows the launcher only the pictures
>   the citizen chooses. No storage permission is needed. The original is
>   never altered.
> - **Wallpaper.** The launcher never reads the citizen's own wallpaper. It
>   sets only the lock-screen picture, and only when told to.
> - **Unlocking.** The launcher never examines the finger and never sees or
>   stores the PIN.
> - **Wi-Fi.** The passport asks Android only whether Wi-Fi is up. It never
>   asks which network, needs no location permission and keeps only the
>   dates.
> - **Comrades.** The exchange passes citizen numbers by QR code or by hand.
>   The phone's own camera app reads the code; the launcher asks for no
>   camera permission and sends nothing.
> - **Posters.** The slogan is never written to the record.
> - **Exports** are written only to the `Laibach` folders in `Pictures` and
>   `Download`.
> - **The network** (the `INTERNET` permission) is used only for the approved art (plain page and image
>   requests to www.laibach.org), the mobilisation orders (laibach.org's
>   public events feed) and Laibach TV (YouTube's public feeds and the
>   `youtube-nocookie.com` player). No account, no key, no sign-in.

<a id="chronicle"></a>

## 🧾 XIV. CHRONICLE

The state was built in stages. Each stage was fulfilled ahead of schedule.

| Issue | Name | Fulfilled |
|---|---|---|
| **1.3** | — | The launcher takes its name, LAIBACH LIFE™, and the approved art of the day. |
| **1.4** | The Collective | Papers, the attention ration, decrees, ministries and the curfew. See [The Collective](#the-collective). |
| **1.5** | Total Harmony | The identity card, the permanent record, the Constitution, self-criticism, the morning roll call and joy coupons. See [Total Harmony](#total-harmony). |
| **2.0** | Synthetic State | The terminal boots, glitches, watches and broadcasts. Every programme wears the state's badge. See [Synthetic State](#synthetic-state). |
| **2.3** | Mobilisation | Reveille, mobilisation orders, the weekly report, austerity, the charging screen, export of papers, quick-settings tiles and Slovenian. The menu opens with a single tap on the turning gear. See [Mobilisation](#mobilisation). |
| **2.3.1** | — | The terminal no longer names the make of the phone. It serves every Android citizen alike. |
| **2.4** | The Darkroom | Laibach Camera and its darkroom, the state's sounds issued to the phone as ringtones, ten formations, and the **Regulations**: the launcher's own settings, with sounds on or off. The gear menu is regrouped into Art & camera and Duties. |
| **2.4.1** | — | The censor's desk: text shared to the launcher comes back blacked out, as a decree or as a typed memo. |
| **2.4.2** | — | Every citizen is renumbered: the full 64-bit Android ID as 20 digits and a Damm check digit. No two phones share a number. |
| **2.5** | The Plan | The daily norm, ministry quotas and inventory, the waiting room, the informant in the weekly report, and the number check at the counter. The terminal is rebuilt from separate organs. |
| **2.6** | The Record | The exit visa for every redaction and banishment, ceremonies for promotions and attendance, Foundation Day with its extra joy coupon, the stamp album, and the identity card shared as a specimen. |
| **2.7** | The Weather | Controlled weather from the sun's true position, worked out from the clock alone; a five-minute curfew warning; the dead drop; the anthem of the hour; and the typewriter at the censor's desk. |
| **2.8** | The Exchange | The drill schedule, the passport and its entry visas, the citizen exchange by QR code, and the poster press. |
| **2.8.1** | — | The permanent record is off until the citizen turns it on. A record kept by an earlier issue is destroyed when 2.8.1 first starts. |
| **2.9** | Interference | The glitches are unforeseeable: random intervals, stutters and breakdowns, seven kinds and a temper for every text; the clock tears as the minute turns. Screens rise in and sink away, sections and menu rows report in order, controls give under the finger, and the gear ticks. The seal's clock keeps the citizen's 12 or 24 hours. See [Synthetic State](#synthetic-state). |

<details>
<summary><b>🔍 Findings of the inspectorate</b></summary>

- The type is Roboto Flex (a variable font) where the system provides it,
  drawn at `wdth 25` and `wght 900–1000` for the condensed, monumental look.
  Nothing is downloaded or bundled. Where the font is missing, text falls back
  to `sans-serif-condensed`.
- Icons pass through a saturation-0 colour matrix before the uniform is
  applied. Colour is reserved for the state.
- The gear turns only while the terminal is visible and halts as soon as a
  programme is in front. It advances in half-degree steps, about 30 frames a
  second, not at the display's full refresh rate.
- Memory: about 172 MB PSS with the wallpaper (1.3). The large variable-font
  clock accounts for most of it.
- The first tap on the clock may bring the Clock app's own request for
  notification permission. It comes from the Clock app, not from the state.
- Inspected on 24 October 2026 on an Android 17 phone (SDK 37): the four
  uniforms then in issue, search and the Go key, the Home reset, redaction and
  declassification, show-redacted, the clock → alarms, the long-press menu
  and its uniform submenu, cycling proclamations, and logcat with no crashes
  after the correction below.
- 2.3 was inspected on 24 October 2026 on the same phone: the gear menu and
  its submenus, the TV strip, the mobilisation order in the ticker and the
  full list, setting a reveille, the weekly report, all four exports and
  restore. Not yet inspected on the phone: Slovenian, the charging screen,
  austerity, the reveille ringing, the tiles and Morse.
- Volkskunst was inspected on 24 October 2026 with four generated placeholder
  images, one stored sideways with an EXIF rotation tag: picking, import,
  rotation, greyscale, the frame, swiping and the ends, captions, persistence
  after reopening, the KAPITAL uniform, and removal. logcat showed no crashes.
  The placeholders were destroyed afterwards and the gallery left empty.
- Corrected during inspection: opening the alarm list requires
  `com.android.alarm.permission.SET_ALARM`. Without it the app crashed. The
  permission is now declared, and `open()` also catches `SecurityException`.
- LAIBACH LIFE™ 1.3 was inspected on 24 October 2026: the first wallpaper
  fetch and five more by tapping the credit, captions in all the site's
  formats, English and German on the terminal, the menus and the empty
  gallery, the return to English, and logcat with no crashes after the
  corrections below. Not inspected: the daily refresh at midnight and the
  offline "withheld" state.
- Corrected during 1.3 inspection: the first fetch failed with a division by
  zero (`inSampleSize` starts at 0, not 1). The date line and the device line
  were shortened to fit on one line. In German, the VPN note pushed LAIBACH
  onto two lines; the note now has its own line in the status block.
- LAIBACH LIFE™ 1.4 was inspected on 24 October 2026 at about 01:30, with the
  curfew in force: the curfew screen, an exemption, the drill and its end,
  abolition and restoration, the papers and the ration (estimated, without
  usage access), decree long-press, transfer to the Ministry of Joy and back,
  the ministry headers while scrolling, and German on the terminal and in the
  menus. logcat showed no crashes. Not inspected: real decrees and measured
  usage, as notification access and usage access were not granted at the
  time. Grant them from menu → Settings → Permissions.
- Corrected during 1.4 inspection: the monument occupied so much room that
  only about two programmes were visible; it now scrolls away with the list.
  In German, `1STD 59M` collided with the battery value; durations now use H
  and M in both languages. `PERMITTED UNTIL 06:00` is now centred.

</details>

<br>

<p align="center">
  <img src="docs/brand/seal.png" width="96" alt="The seal of the Collective">
</p>

<p align="center">
  <sub><b>THIS DOCUMENT HAS BEEN APPROVED. YOUR READING OF IT HAS BEEN NOTED.</b></sub><br>
  <sub>■ LAIBACH LIFE™ ■ ISSUED BY THE LAIBACH COLLECTIVE ■ ORIGINAL ART AND SLOGANS ONLY ■</sub>
</p>
