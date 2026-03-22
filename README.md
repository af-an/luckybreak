<div align="center">

![Lucky Break Title](https://luckybreak.sirv.com/Images/lb03.png)

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b447?style=flat-square&logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-supported-dbb045?style=flat-square)
![Forge](https://img.shields.io/badge/Forge-supported-e07a29?style=flat-square)
![NeoForge](https://img.shields.io/badge/NeoForge-supported-cf6029?style=flat-square)
![Quilt](https://img.shields.io/badge/Quilt-supported-9b59b6?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

[![](https://img.shields.io/badge/Modrinth-00AF5C.svg?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/lucky-break) 
[![](https://img.shields.io/badge/CurseForge-F16436.svg?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/lucky-break)

</div>

---

Lucky Break Mod adds **Lucky Blocks** that can explode when you break them. One block might spawn a full blacksmith's shop and a chest full of diamonds. The next one might drop you in a lava pit. The one after that traps you in iron bars and an anvil above your head.

Beyond the blocks themselves there's a full **Lucky toolkit** (sword, bow, pickaxe, axe, shovel, hoe), a **Lucky Potion**, and a **Lucky Compass** that hunts down the nearest Lucky Block for you. There's a wishing well you can throw a coin into. A chicken that lays gold and other goodies. Lucky Blocks generate in the Nether and End dimensions as well.

Available for **Fabric, Forge, NeoForge, and Quilt**.

---

## Table of Contents

- [Lucky Block](#lucky-block)
- [Lucky Compass](#lucky-compass)
- [Lucky Tools](#lucky-tools)
- [Events](#events)
- [The Wishing Well](#the-wishing-well)
- [The Golden Hen](#the-golden-hen)
- [World Generation](#world-generation)
- [Configuration](#configuration)
- [Commands](#commands)
- [Installation](#installation)

---

## Lucky Block

<details>
<summary>Show Recipe</summary>

Craft by placing 8 gold ingots around a dropper.
![The Lucky Blocks](https://luckybreak.sirv.com/Images/lb-craft.png)

</details>

The odds:

| Block | Lucky | Average | Unlucky |
|:---|:---:|:---:|:---:|
| **Lucky Block** | 20% | 60% | 20% |

Break and a random event occurs. Silk touch will drop the block. 

---

## Lucky Compass

<details>
<summary>Spoiler</summary>

[![Watch the video](https://img.youtube.com/vi/lAup8jiqpxQ/0.jpg)](https://www.youtube.com/watch?v=lAup8jiqpxQ)

</details>

Right-click to find the nearest Lucky Block. The compass needle locks onto the nearest one it finds, and it stays pointed there until the block is broken.

Durability consumed scales with distance — closer blocks cost less uses. Blocked in The Nether and The End.

---

## Lucky Tools

<details>
<summary>Spoiler</summary>

![Lucky Tools Overview](https://luckybreak.sirv.com/Images/creative-tab.png)

</details>

Six tools and a potion. All gold-tier material, all repairable with gold ingots, all carrying their own flavor of random effects.

---

### Lucky Sword

*7 attack · 1.6 speed*

On hit, rolls a chance for lightning, or launches the target with a knockback fire trail. Sometimes temporarily picks up 1–2 random enchantments from a wide pool (Sharpness, Smite, Fire Aspect, Looting, Sweeping Edge, Mending...) that last 3–8 seconds before disappearing.

---

### Lucky Bow

<details>
<summary>Spoiler</summary>

[![Watch the video](https://img.youtube.com/vi/Up8Zzl3TRBs/0.jpg)](https://www.youtube.com/watch?v=Up8Zzl3TRBs)

</details>

*Fires normal / spectral / tipped arrows*

The arrow type randomizes each shot. But the weird part is the hit effects — each shot carries random effects:

- **TNT Trail** — primed TNT along the arrow's path
- **Block Transform** — converts hit blocks into ores or random block types
- **Fire Spread** — ignites the impact zone
- **Entity Bounce** — launches the target upward
- **Entity Lightning** — lightning strike on hit
- **Entity Potion** — random debuff (slowness, blindness, levitation, poison...)
- **Entity Block Transform** — feet-block turns to water or lava
- **Chicken Rain** — what it sounds like
- **Party Pop** — festive particles, nothing more
- **Random Arrow Types** — rerolls arrow type per effect

25% chance each shot to temporarily pick up 1–3 random enchantments for the next few shots, then lose them.

---

### Lucky Pickaxe

*Diamond mining tier · 3.0 attack*

Every block you mine has a chance to roll a side effect. Most of the time nothing happens. Sometimes:

<details>
<summary>View full effect table</summary>

| Effect | Chance | What happens |
|:---|:---:|:---|
| TNT Transform | 1–6 primed TNT replace the mined block |
| Bedrock Transform | Block becomes bedrock |
| Lucky Block Transform | Block becomes a Lucky Block |
| Block Type Transform | Block becomes stone, cobblestone, dirt, a log, or a deepslate ore |
| Bonus Drop | Gold nugget / charcoal / gold ingot / diamond / emerald drops pop out |
| Golden Hen Spawn | A Golden Hen materializes at the dig site |
| XP Burst | Experience orbs scatter around you |
| Ore Vein Burst | Nearby ore veins explode into floating item drops |
| Hostile Spawn | A hostile mob appears nearby |
| Friendly Spawn | A friendly mob appears nearby |
| Seismic Burst | Cave-in style block disruption |

Effects are shuffled each check — only one fires per mine.
</details>

---

### Lucky Axe

*8.0 attack · 1.0 speed*

Chopping wood can trigger a leaf storm (drops sticks and sometimes an apple), summon 1–3 bees, launch the cut log upward, pop a piñata of sticks/apples/charcoal/honeycomb/berries/gems, or occasionally transform nearby blocks into Lucky Blocks. Lightning on block break and temporary enchantments also possible.

---

### Lucky Shovel

*3.5 attack · 2.0 speed*

Digging has a chance per block to drop something: gold nuggets, flint, iron nuggets, clay, sand, red sand, gravel, mud, snowballs, bone meal, gunpowder, quartz, copper, lapis, glowstone, string, amethyst, prismarine, slimeball, nautilus shell, and actual gems. Sandstorm applies slowness and weakness in a 4-block radius. Lucky Block Transform and a Treasure Burst are also possibilities.

---

### Lucky Hoe

*2.0 attack · 2.0 speed*

Tilling has a chance to auto-plant a random seed in the freshly tilled block, then a chance to immediately advance it 1–3 growth stages. Crop bloom grows nearby crops (3-block radius) 1–2 steps. Hitting mobs has a 30% chance to drop food — apple, potato, carrot, golden carrot, or cake. A piñata hit can scatter seeds and root vegetables.

---

### Lucky Potion

<details>
<summary>Spoiler</summary>

[![Watch the video](https://img.youtube.com/vi/JcDKrs5oxXQ/0.jpg)](https://www.youtube.com/watch?v=JcDKrs5oxXQ)

</details>

Drink it. Receive 4–15 simultaneous random positive effects drawn from a weighted pool of 19 options: Speed II, Haste II, Strength, Instant Health II, Jump Boost II, Regen II, Resistance, Fire Resistance, Water Breathing, Invisibility, Night Vision, Health Boost II, Absorption II, Saturation II, Luck, Slow Falling, Conduit Power, Dolphin's Grace, and Hero of the Village.

---

## Events

<details>
<summary>Spoiler</summary>

[![Watch the video](https://img.youtube.com/vi/hXiN_9QRB-g/0.jpg)](https://www.youtube.com/watch?v=hXiN_9QRB-g)
[![Watch the video](https://img.youtube.com/vi/TBjl68XZaAg/0.jpg)](https://www.youtube.com/watch?v=TBjl68XZaAg)

</details>

Many events across three tiers: Lucky, Average, and Unlucky:

Lucky Events

| Event | What happens |
|:---|:---|
| **Golden Armor Stand** | An armor stand in full gold armor with a random Lucky Tool in hand materializes at the break point. |
| **Golden Hen Gift** | 1–2 Golden Hens drop in nearby. A chime plays. They'll start laying something eventually. |
| **Golden Rain** | 9–14 items rain from above — gold ingots, emeralds, XP bottles, golden apples, glow berries. |
| **Iron Beacon** | An iron block pyramid assembles itself with a beacon at the top. Clears the space above first. |
| **Loot Chest (Lucky)** | A chest on a gem-block base filled with diamonds, enchanted diamond tools, horse armor, chainmail sets, netherite scraps. |
| **Rainbow Column** | Colored wool columns fall from the sky. A diamond / gold / iron / emerald block lands on top. |
| **Starfall Blessing** | Temporary Regen II + Absorption + Speed. 3 star-shaped fireworks. 4–7 reward items drop from above. |
| **Tamed Companions** | 1–3 named cats (Luna, Mochi...), 1–3 named wolves (Rex, Ghost...), 1–2 named parrots — all tamed, all yours. Wolves get random collar colors. |
| **Treasure Block** | A single block materializes: diamond (35%), gold (35%), emerald (29%), or netherite (1%). |
| **Wishing Well (Lucky)** | See [The Wishing Well](#the-wishing-well) — better loot variant. |

Average Events

| Event | What happens |
|:---|:---|
| **Blacksmith House** | A full 9×8×11 blacksmith building spawns with a loot chest inside. |
| **Bounce House** | A room made entirely of slime blocks appears nearby. "How high can you jump?" |
| **Choose Wisely** | Two Lucky Block variants spawn 3 blocks apart. The sign tells you one is lucky and one isn't. Choose wisely and good luck. |
| **Friendly Circle** | 4–7 random peaceful mobs spawn in a ring around you — wolves, cats, foxes, camels, sniffers, pandas, bees, turtles, armadillos, llamas, and more. |
| **Gem Sprinkle** | Gems shower outward in a radial burst — iron, gold, redstone, lapis, emeralds, diamonds, amethyst. |
| **Gold Pedestal** | A Lucky Block falls from 8 blocks above. Break the landed block for another chance, although you may want to collect the block underneath first. |
| **Loot Chest** | Mid-tier chest with tools, ingots, and crafting materials. |
| **Mob Arena** | Cobblestone walls rise around you. 1–5 waves of zombies with weighted armor tiers (leather → iron). A support chest with a sword, shield, and consumables appears nearby. Survive for loot. Leave the arena and you forfeit. |
| **Rainbow Sheep** | Sheep in all 16 wool colors scatter around you. |
| **Random Tree** | A bonemeal-grown tree sprouts within 8 blocks — dark oak, jungle, spruce, cherry, mangrove, pale oak (with a 15% chance to include a Creaking Heart), and more. |
| **Wishing Well** | See [The Wishing Well](#the-wishing-well). |

Unlucky Events

| Event | What happens |
|:---|:---|
| **Cobweb Snare** | Cobwebs fill a sphere around you. 1–3 silverfishes materialize. Slowness II + Weakness added. |
| **Iron Bars Trap** | You're caged in iron bars. Then either a heavily damaged anvil drops from 16 blocks above you or lava pours in from 3 blocks up. |
| **Lava Pit** | A 10-block radius, 30-block deep pit opens under you, walls one block thick, lava at the bottom. |
| **Night Ambush** | Night falls. Blindness for 8 seconds. 8–15 zombies and spiders converge from a 5-block radius. |
| **Night Riders** | Thunderstorm. Night. 4–8 skeleton riders in enchanted armor (8–20 enchantment cost). Killing them drops 3× XP and bonus loot. |
| **Puffer Tank** | A glass water tank containing exactly one pufferfish encloses you. "Glub glub..." |
| **TNT Rain** | Slowness II for 10 seconds. Then 15–30 TNT launch upward with varied trajectories. |
| **Wishing Well (Unlucky)** | See [The Wishing Well](#the-wishing-well) — the bad one. |

---

## The Wishing Well

<details>
<summary>Spoiler</summary>

[![Watch the video](https://img.youtube.com/vi/ZAW0F2viTpw/0.jpg)](https://www.youtube.com/watch?v=ZAW0F2viTpw)

</details>

Three variants, one structure — a well built from stone and stone bricks.

Every variant works the same way to start: you receive a **Coin** in your inventory with the message to make a wish. The well stays active for 45 seconds. Throw the coin into the water basin at the top. What happens next depends on which well you got.

**Normal well** — Food rains around the well for 15 seconds. Bread, cooked meats, golden carrot, apple, melon slices, cookie, baked potato, pumpkin pie, salmon, cod, rabbit stew, golden apple. Heart particles and bubble pop effects.

**Lucky well** — Resource items instead of food. Enchanted Golden Apples are a favorite. The loot includes iron through gold, redstone, lapis, emeralds, diamonds, Lucky Potions, and netherite scrap.

**Unlucky well** — The coin hits the water and the TNT priming sound plays. Night falls. The water in the basin converts to lava. 2–4 TNT appear in a nearby radius. 3–6 skeletons and zombies close in. Fire spreads in a 9-block radius. There's smoke while everything goes wrong.

---

## The Golden Hen

<details>
<summary>Spoiler</summary>

![The Golden Hen](https://luckybreak.sirv.com/Images/lucky-chicken.png)

</details>

A chicken that glows and lays gold nuggets and more. If a golden hen dies, it drops a gold ingot.

Breeding is not possible and Golden chicks are only available in creative.

You can get them from breaking a Lucky Block or by mining with the Lucky pickaxe.

---

## World Generation

Lucky Blocks generate in all three dimensions.

**Overworld** — Rare single Lucky Block placements on the surface. No structure, just the block sitting there.

**Nether** — A pedestal made of cracked nether bricks with a Lucky Block in the center. Generates across Nether biomes. Very common in the Nether.

**The End** — A cage of waxed oxidized copper bulbs and chiseled copper columns with a Lucky Block inside. Very rare in the End, but also very lucky.

---

## Configuration

<details>
<summary>Spoiler</summary>

![Settings Menu](https://luckybreak.sirv.com/Images/settings-menu.png)

</details>

**In-game config screen** — Press `Ctrl + K` at any time to open a live tier-chance editor for the standard Lucky Block. Drag the percentages, save and close the screen, and changes take effect immediately. Accessible through Forge and NeoForge's config settings. Also accessible through ModMenu for Fabric if you have it installed. For Quilt, keyboard command `Ctrl + K` is the only method to open config screen.

---

## Commands

All under `/luckybreak`. Permission level follows standard Minecraft operator levels.

```
/luckybreak <event> <lucky / average / unlucky> <event name>
```
Fires a random Lucky Block event of the given tier at the target player. Defaults to yourself.

```
/luckybreak <item> <item name> <effect name>
```
Fires a specific Lucky Tool effect by name.

---

## Installation

**Drop the `.jar` into your `mods/` folder for the appropriate loader. 

**Fabric REQUIRES Fabric API: [Modrinth](https://modrinth.com/mod/fabric-api); [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api)


**Optional:** [ModMenu](https://modrinth.com/mod/modmenu) (Fabric ONLY) gives you a config button in the mod list that opens the Lucky Break config screen. Not required — `Ctrl + K` always works. For Quilt, keyboard command `Ctrl + K` is the only method to open config screen.

If you're building from source:

```sh
# Fabric / NeoForge / Quilt / Forge — same command for all
./gradlew build
```

Output jar lands in `build/libs/`.

---

## License

MIT License with Additional Requirements

Copyright (c) 2026 af-an
Attribution required — see below

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

Attribution — Any distribution, public display, or derivative work based on this Software must include clear and visible credit to the original author(s). Credit must include:

The name "Lucky Break" and a link to the original project (if applicable)

The author name(s) as specified in the project metadata

This attribution must appear in a location reasonably visible to end users (e.g., in-game credits, mod list description, or a prominent section of documentation)

No Misrepresentation — You may not use the name of the original project or its authors to promote your derivative work without explicit written permission.

Preservation of Notice — The above copyright notice and these conditions shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

<div align="center">

*Something blew up? It was probably supposed to.*

</div>
