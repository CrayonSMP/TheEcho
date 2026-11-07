# TheEcho Wiki

I know, is AI generated, but it do what it should.

Reference for config and behaviors, so you don't have to dig through the code.

## Behaviors at a glance

| Behavior | Type | Key | Class |
|---|---|---|---|
| Compounding Furnace | Block | `theecho:compoundingfurnace` | `CompoundingFurnaceBehavior` |
| Scythe | Item | `theecho:scythe` | `ScytheBehavior` |

---

## Compounding Furnace

### Block config (CraftEngine)

```yaml
behavior:
  type: theecho:compoundingfurnace
```

The block needs a Boolean property `lit`.

### `config.yml` (plugin)

Keys are kebab-case.

```yaml
debug-mode: false
compounding-furnace:
  inventory-title: "<dark_gray>Compounding Furnace"   # MiniMessage
  slots:
    input1-slot: 10
    input2-slot: 12
    fuel-slot: 28
    output-slot: 24
    process-tool-slot: 14
    process-slot: 22
  items:
    processing-items:        # progress indicator, first = 0 %, last = done
      - minecraft:gray_stained_glass_pane
      - mynamespace:progress_1
```

### Recipes

Folder: `plugins/TheEcho/recipes/*.yml`. Reload with `/theecho reload`.

```yaml
recipes:
  mynamespace:iron_alloy:
    type: compoundingfurnace
    fuel: minecraft:coal[amount=1]
    processing_tool: minecraft:iron_pickaxe
    input1: minecraft:iron_ingot[amount=2]
    input2: minecraft:copper_ingot
    output: mynamespace:alloy_ingot[amount=1]
    processing_time: 200      # ticks (20 = 1 s)
```

### CraftEngineRecipe (recipe viewer)

Optional. If the plugin `CraftEngineRecipe` is installed, all compounding furnace recipes are published to it on startup and on `/theecho reload` (display only, the furnace logic stays in TheEcho).

```yaml
compounding-furnace:
  cer-workstation: "theecho:compounding_furnace"   # item id shown as the workstation
```

- `cer-workstation` must be a CraftEngine item id (`namespace:path`) with an icon.
- Fuel and tool are shown as normal ingredients with a label; the processing time is shown as a note.
- A server admin has to assign the recipes to a category once in `/cer manage`, otherwise they stay hidden (CER whitelist model).

- Item format: `namespace:id` or `namespace:id[amount=N]` (default 1).
- Always write vanilla items with the `minecraft:` namespace.
- The recipe ID must be a valid namespaced key.
- `type` must be `compoundingfurnace`.

---

## Scythe

### Item config (CraftEngine)

```yaml
behavior:
  type: theecho:scythe
  dropMultiplier: 2.0
```

| Option | Type | Default | Meaning |
|---|---|---|---|
| `dropMultiplier` | double | `1.0` | Multiplier applied to the drops (fractions are rounded randomly) |

Works on blocks with the tag `minecraft:crops` (CraftEngine) and on vanilla crops from `Tag.CROPS`. Triggered by left-click.

---

## Adding a new behavior

### Block behavior

1. Create a class: `extends BukkitBlockBehavior`, plus a `FACTORY` (`BlockBehaviorFactory`) that reads its options from the `ConfigSection`.
2. Register it in `BlockRegistry#registerBehaviors`:

```java
BlockBehaviors.register(behaviorKey("myblock"), MyBlockBehavior.FACTORY);
```

### Item behavior

1. Create a class: `extends ItemBehavior`, plus a `FACTORY` (`ItemBehaviorFactory`).
2. Register it in `ItemRegistry#registerBehaviors`:

```java
ItemBehaviors.register(behaviorKey("my_item"), MyItemBehavior.FACTORY);
```

### Reading options from the YAML

In the factory, via the `ConfigSection`:

```java
section.getDouble("dropMultiplier", 1.0)
```

Then add the option to this wiki under that behavior.

### With a listener (e.g. left-click)

`ItemBehavior` only has right-click hooks. For other events:

1. Write a **single** listener and register it in `ItemRegistry#registerListeners`.
2. The factory puts every instance into a `Map<Key, Behavior>`.
3. The listener looks up the matching behavior by item ID (see `ScytheListener`).

Leave `reloadItems()` empty: CraftEngine calls the factory again on `/ce reload`.

---

## Commands

| Command | Permission | Effect |
|---|---|---|
| `/theecho reload` | `theecho.reload` | Reloads `config.yml` and the recipes |