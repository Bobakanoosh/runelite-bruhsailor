# Sidecar generation — batch test
Generated: 2026-05-03T03:52:30.693Z
Steps: 1.1.1, 1.4.6, 2.1.34, 2.1.42, 3.1.10

---

## Step 1.1.1 — 1.1: Tutorial island up to and including Wintertodt

**Step text:**

```
Choose a swag look for your account (older guides will mention that you need to start as a female character - this is no longer required). Make sure you have two step authenticator enabled. For most of the guide it is assumed you carry your GP stack. All lamps, unless mentioned otherwise, go on Herblore until the end of Chapter 2 (Song Of The Elves completed). Grab a knife, jug and fill it with water, 2 buckets (hop once), cabbage, leather boots. Talk to Duke Horacio to start Rune Mysteries, burn logs to 15 firemaking, bank 5 ashes. Grab 110 logs and bank them.
```

**items_needed (raw):** `none`
**gp_stack:** `25 gp`

**Detected quests (deterministic, may include false positives):**
- RUNE_MYSTERIES — matched: `Rune Mysteries`
- SONG_OF_THE_ELVES — matched: `Song of the Elves`

**Generated mapping (1942ms):**

```json
{
  "contentHash": "a6c548211f69149b04bcabb4bae7db90ea9fde78",
  "title": "1.1: Tutorial island up to and including Wintertodt",
  "questIds": [
    "RUNE_MYSTERIES"
  ],
  "items": [
    {
      "id": 946,
      "name": "Knife",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1935,
      "name": "Jug",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1925,
      "name": "Bucket",
      "qty": 2,
      "source": "content"
    },
    {
      "id": 1965,
      "name": "Cabbage",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1061,
      "name": "Leather boots",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1511,
      "name": "Logs",
      "qty": 110,
      "source": "content"
    }
  ],
  "abstractItems": [
    {
      "label": "cash stack",
      "rawToken": "25 gp"
    },
    {
      "label": "experience allocation",
      "rawToken": "All lamps go on Herblore until end of Chapter 2"
    }
  ],
  "unresolvedItems": [
    {
      "rawToken": "water",
      "qty": 1
    },
    {
      "rawToken": "5 ashes",
      "qty": 5
    }
  ],
  "verified": false,
  "verifierConfidence": null,
  "verifierFlags": []
}
```

---

## Step 1.4.6 — 1.4: 62 farming, Desert Treasure, earth battlestaves

**Step text:**

```
You should be at least level 11 hunter by now from bird runs (you didn’t forget, did you?). Teleport to Lumbridge, collect a few anti-dragon shields from the Duke. Enter Zanaris and complete Fairytale I (make sure to take the low level agility shortcut to the cosmic altar for the diary) (Oziris recommends this safespot for flinching: https://i.imgur.com/2Ejgx0h.png). Leave Zanaris and run to Draynor, start Fairytale II (bank shields, withdraw the other items). Free Prince Ali with your key, do A Porcine of Interest, buy a mirror shield, rock hammer, two facemasks, earmuffs, nose peg, spiny helmet, unlit bug lantern, insulated boots, boots of stone, bag of salt, slayer gloves, 30 ice coolers and as many enchanted gems as you can carry, grab 10 grain. Return to Martin the Master Farmer (rebank), continue Fairytale II until you have access to fairy rings. Go to ring CIR (diary step), ALS (McGrubor’s Wood, diary step, run out and plant jute seeds for the diary, return to the fairy ring), BIS (Ardougne zoo, diary step), AIR (diary step), optional AIR to Auburnvale to purchase an addy axe (only if you do not have 60 woodcutting yet), DIS (diary step), AJP (Varlamore) and do The Ribbiting Tale of a Lily Pad Labour Dispute in full and include this fairy ring in future hardwood tree farming runs, also grab a sweetcorn for Recipe for Disaster, optional: also do Twilight’s Promise here, this requires some combat, fairy ring BJR (Fisher Realm) to continue Holy Grail (grab the holy grail, don’t blow the whistle), fairy ring to Hazelmere for The Grand Tree (CLS), fairy ring BLS and start The Garden of Death, exploring the cave below until you have enough words to be sent to Lake Molch, fairy ring DKS to start Olaf’s Quest, kill 5 rock crabs for the diary, catch a cerulean twitch for the diary.
```

**items_needed (raw):** `7500 gp, air runes, earth staff, law runes, dramen staff, pink skirt, paste, wig, 3 beersAfter banking: 3 jute seeds, seed dibber, rake, ultracompost, magic secateurs, trout, 2 ropes, knife, rune sword, steel axe, bronze key, magic whistle, bark sample, bird snare`
**gp_stack:** `444615 gp`

**Detected quests (deterministic, may include false positives):**
- THE_GRAND_TREE — matched: `The Grand Tree`
- HOLY_GRAIL — matched: `Holy Grail`
- A_PORCINE_OF_INTEREST — matched: `A Porcine of Interest`
- THE_GARDEN_OF_DEATH — matched: `The Garden of Death`
- THE_RIBBITING_TALE_OF_A_LILY_PAD_LABOUR_DISPUTE — matched: `The Ribbiting Tale of a Lily Pad Labour Dispute`
- AGILITY — matched: `Agility`
- WOODCUTTING — matched: `Woodcutting`

**Generated mapping (5896ms):**

```json
{
  "contentHash": "099550b1435a68c5c47ec68687c8bff27e492768",
  "title": "1.4: 62 farming, Desert Treasure, earth battlestaves",
  "questIds": [
    "FAIRYTALE_I",
    "FAIRYTALE_II",
    "A_PORCINE_OF_INTEREST",
    "THE_RIBBITING_TALE_OF_A_LILY_PAD_LABOUR_DISPUTE",
    "HOLY_GRAIL",
    "THE_GRAND_TREE",
    "THE_GARDEN_OF_DEATH",
    "OLAF_QUEST"
  ],
  "items": [
    {
      "id": 556,
      "name": "Air rune",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1385,
      "name": "Staff of earth",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 563,
      "name": "Law rune",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 772,
      "name": "Dramen staff",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1013,
      "name": "Pink skirt",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 2424,
      "name": "Paste",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 2419,
      "name": "Wig",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 5343,
      "name": "Seed dibber",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 5341,
      "name": "Rake",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 21483,
      "name": "Ultracompost",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 7409,
      "name": "Magic secateurs",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 333,
      "name": "Trout",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 954,
      "name": "Rope",
      "qty": 2,
      "source": "items_needed"
    },
    {
      "id": 946,
      "name": "Knife",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1289,
      "name": "Rune sword",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1353,
      "name": "Steel axe",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 2418,
      "name": "Bronze key",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 16,
      "name": "Magic whistle",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 783,
      "name": "Bark sample",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 10006,
      "name": "Bird snare",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1540,
      "name": "Anti-dragon shield",
      "qty": null,
      "source": "content"
    },
    {
      "id": 4156,
      "name": "Mirror shield",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 4162,
      "name": "Rock hammer",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 4164,
      "name": "Facemask",
      "qty": 2,
      "source": "content"
    },
    {
      "id": 4166,
      "name": "Earmuffs",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 4168,
      "name": "Nose peg",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 4551,
      "name": "Spiny helmet",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 7051,
      "name": "Unlit bug lantern",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 7159,
      "name": "Insulated boots",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 23037,
      "name": "Boots of stone",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 4161,
      "name": "Bag of salt",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 6708,
      "name": "Slayer gloves",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 6696,
      "name": "Ice cooler",
      "qty": 30,
      "source": "content"
    },
    {
      "id": 4155,
      "name": "Enchanted gem",
      "qty": null,
      "source": "content"
    },
    {
      "id": 1947,
      "name": "Grain",
      "qty": 10,
      "source": "content"
    },
    {
      "id": 5306,
      "name": "Jute seed",
      "qty": 3,
      "source": "content"
    },
    {
      "id": 5986,
      "name": "Sweetcorn",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 19,
      "name": "Holy grail",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1357,
      "name": "Adamant axe",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 9967,
      "name": "Cerulean twitch",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1917,
      "name": "Beer",
      "qty": 3,
      "source": "content"
    }
  ],
  "abstractItems": [
    {
      "label": "gp",
      "rawToken": "7500 gp"
    },
    {
      "label": "cash stack",
      "rawToken": "444615 gp"
    }
  ],
  "unresolvedItems": [],
  "verified": false,
  "verifierConfidence": null,
  "verifierFlags": []
}
```

---

## Step 2.1.34 — 2.1: Quests and combat, unlock Miscellania

**Step text:**

```
Keep wearing your ring of charos (a). Teleport to Falador. Run south to Port Sarim. Start and complete the Pandemonium quest. Whenever sailing you get some XP and a speed boost for trimming sails, so try to consistently click the sail when there’s a burst of wind. Do port courier tasks (between Pandemonium and Port Sarim) until level 13 sailing, aiming to finish the level at Port Sarim. Complete the following charting tasks along the way:

  - Look at the port task board in Port Sarim.

  - Interact with the salvaging station in Port Sarim.

  - Treacherous rock formation near Draynor Village.

  - Spyglass to get a good view of the Wizards’ Tower from the north.

  - Spyglass to get a good view of Karamja Shipyard from the east.

  - Wreck of the Salty Grouper south west of Tutorial Island.

  - Spyglass to get a good view of the Pandemonium from the cave entrance, also dig up the small key for the salvaging station schematic.

  - Spyglass to admire Charin’ Charles McAtless in the cave on the Pandemonium, also mine 22 lead ore in the cave. Superheat 10 of them into 5 bars.
```

**items_needed (raw):** `tbd`
**gp_stack:** `670k gp`

**Detected quests (deterministic, may include false positives):**
- PANDEMONIUM — matched: `Pandemonium`

**Generated mapping (1261ms):**

```json
{
  "contentHash": "9372e87dc8eb62e1dec02aac195707fd0b3a8fda",
  "title": "2.1: Quests and combat, unlock Miscellania",
  "questIds": [
    "PANDEMONIUM"
  ],
  "items": [],
  "abstractItems": [
    {
      "label": "cash stack",
      "rawToken": "670k gp"
    }
  ],
  "unresolvedItems": [
    {
      "rawToken": "ring of charos (a)",
      "qty": 1
    }
  ],
  "verified": false,
  "verifierConfidence": null,
  "verifierFlags": []
}
```

---

## Step 2.1.42 — 2.1: Quests and combat, unlock Miscellania

**Step text:**

```
Teleport to Barbarian Assault, do at least one full queen run in any role and buy a granite platebody. Recommended: continue Barbarian Assault to obtain a Fighter Torso.

  - Using ranging is generally recommended as Attacker in barbarian assault, even with low ranged level and trash tier shortbows. For all other roles wear flex or defensive gear.
```

**items_needed (raw):** `rune sword, amulet of strength, berserker helm, etc. (henceforth ‘melee gear’)`
**gp_stack:** `450k gp`

**Detected quests (deterministic, may include false positives):**
- (none)

**Generated mapping (1687ms):**

```json
{
  "contentHash": "e1413928cddc5e158ae48508796b4eef9fca0aaa",
  "title": "2.1: Quests and combat, unlock Miscellania",
  "questIds": [],
  "items": [
    {
      "id": 1289,
      "name": "Rune sword",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 3751,
      "name": "Berserker helm",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 1725,
      "name": "Amulet of strength",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 10551,
      "name": "Fighter torso",
      "qty": null,
      "source": "content"
    }
  ],
  "abstractItems": [
    {
      "rawToken": "melee gear",
      "label": "melee gear"
    },
    {
      "rawToken": "flex or defensive gear",
      "label": "flex or defensive gear"
    },
    {
      "rawToken": "450k gp",
      "label": "450k gp"
    }
  ],
  "unresolvedItems": [
    {
      "rawToken": "granite platebody",
      "qty": null
    }
  ],
  "verified": false,
  "verifierConfidence": null,
  "verifierFlags": []
}
```

---

## Step 3.1.10 — 3.1: Slayer and the red prison

**Step text:**

```
Catch 20k red chinchompas between Tal Teklan and Kastori TalTeklanChinSpot.png or 16k black chinchompas (for 92 ranged, if you have extras then can consider throwing for more Ranged XP or using them for your first fire cape if you’re good at speed strats) in the wilderness. Follow this guide for catching chins: OSRS Easy 1t Hunter Guide (In-Depth). If you want to catch red chinchompas but the Varlamore spot is too competitive, e.g. with bots, the second best option is catching them northeast of Prifddinas. Throw the chins in the cave to get to 70 ranging, complete the Western Provinces Hard Diary (this involves getting one Zulrah kill), upgrade your void to Elite, (optionally: grab an offtask fire cape at some point along the way), return and throw the chins to get to level 92 ranging.

  - Make sure to use ranging potions and a bonecrusher (with at least 24k charges), your elite void ranged, mixed hide boots, a berserker ring (i), an amulet of strength, a crystal halberd for speccing, a mixed hide cape and carry death charge runes. Also note that chins are not lost in death, so you could carry more and extend the trip. Use medium fuse. Use protect melee and eagle eye, the regular potion drops will maintain your prayer (but bring a few to be safe, along with a teleport out). Also note that chins are not lost in death, so you could carry more and extend the trip.

  - Example setup for catching chins.

  - Example setup for throwing chins. However, this gear is outdated - use mixed hide boots, a berserker ring (i), an amulet of strength, a crystal halberd for speccing, a mixed hide cape and carry death charge runes.

  - Example setup for Zulrah diary kill, using blood barrage(/blitz). https://streamable.com/l64fsw example kill.

  - Pre-99 Chinchompa Calculatorfor checking the numbers.

  - If you want to go for a lower ranged level https://i.imgur.com/j291MJI.png shows the max hits of the corrupted bow (t3 bow in the corrupted gauntlet) by level with eagle eye, levels 89 and 87 are reasonable alternative stopping points. This will make CG more difficult and slower and also reduce your max hits once you do get the bow though.

  - If you do not want to train with chinchompas (which are best by far) you could alternatively afk it in the nightmare zone. The nightmare zone has super ranging pots, making it the best afk ranged training method by far. The recommended afk range NMZ method is:

  - Wear (elite) void, a magic shortbow (i), rune arrows, amulet of glory, shayzien boots (5).

  - Use the Hawk Eye (10%) prayer, no overhead.

  - Rock cake(/DS2 orb) down to 1hp, use absorption potions, repeat this approximately every 6-9 minutes (it’s fine to go to higher health in between and lose more absorption charges, the goal is to have it afk. Some other guides will recommend resetting every minute or flicking rapid heal, this is a waste of time).

  - Use the super ranging potions (duh), resetting along with the rock cake + absorption pots + prayer pot sips (for hawk eye).

  - Use hard custom rumble (not standard!). Bitterkoekje’s DPS Calculator has a tab for NMZ DPS on set 1, so verify for yourself. Generally the recommended list of enemies is:

  - Moss Giant

  - Sand Snake

  - Count Draynor

  - King Roald

  - Witch’s Experiment (this choice might depend on level, use the DPS calculator to verify).

  - Optional and not recommended: you could detour for a zombie axe from armoured zombies at Zemouregal’s Fort (the higher level ones are better) with barrage spells, then use that along with a rune crossbow at the Royal Titans, specifically Eldric the Ice King, with the goal of unlocking the Deadeye prayer. This allows you to reduce the ranged level target from 92 to 89. Alternatively, you can knock out this scroll (and the other one) after CG, getting much faster kills.
```

**items_needed (raw):** `red/black chins, elite void ranged, ranging pots, glory, shayzien boots 5, red dragonhide shield optional.`
**gp_stack:** `500k gp`

**Detected quests (deterministic, may include false positives):**
- DRAGON_SLAYER_II — matched: `DS2`
- WESTERN_HARD — matched: `Western Provinces Hard Diary`

**Generated mapping (4040ms):**

```json
{
  "contentHash": "e98ec7a49591538393a6f10faf3686721c60ce83",
  "title": "3.1: Slayer and the red prison",
  "questIds": [
    "WESTERN_HARD"
  ],
  "items": [
    {
      "id": 1704,
      "name": "Amulet of glory",
      "qty": null,
      "source": "items_needed"
    },
    {
      "id": 9977,
      "name": "Red chinchompa",
      "qty": 20000,
      "source": "content"
    },
    {
      "id": 11959,
      "name": "Black chinchompa",
      "qty": 16000,
      "source": "content"
    },
    {
      "id": 13116,
      "name": "Bonecrusher",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 29286,
      "name": "Mixed hide boots",
      "qty": null,
      "source": "content"
    },
    {
      "id": 11773,
      "name": "Berserker ring (i)",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 1725,
      "name": "Amulet of strength",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 23987,
      "name": "Crystal halberd",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 29289,
      "name": "Mixed hide cape",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 13378,
      "name": "Shayzien boots (5)",
      "qty": null,
      "source": "content"
    },
    {
      "id": 12788,
      "name": "Magic shortbow (i)",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 892,
      "name": "Rune arrow",
      "qty": null,
      "source": "content"
    },
    {
      "id": 2379,
      "name": "Rock cake",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 28810,
      "name": "Zombie axe",
      "qty": 1,
      "source": "content"
    },
    {
      "id": 9185,
      "name": "Rune crossbow",
      "qty": 1,
      "source": "content"
    }
  ],
  "abstractItems": [
    {
      "label": "cash stack",
      "rawToken": "500k gp"
    },
    {
      "label": "ranging potions",
      "rawToken": "ranging pots"
    }
  ],
  "unresolvedItems": [
    {
      "rawToken": "elite void ranged",
      "qty": null
    },
    {
      "rawToken": "ranging potion",
      "qty": null
    },
    {
      "rawToken": "death charge runes",
      "qty": null
    },
    {
      "rawToken": "absorption potion",
      "qty": null
    },
    {
      "rawToken": "super ranging potion",
      "qty": null
    },
    {
      "rawToken": "prayer potion",
      "qty": null
    },
    {
      "rawToken": "red dragonhide shield optional",
      "qty": null
    }
  ],
  "verified": false,
  "verifierConfidence": null,
  "verifierFlags": []
}
```
