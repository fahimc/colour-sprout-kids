# Logo Source Notes

The shipped logo is stored in two forms:

- Editable SVG source: `docs/logo/colour_my_world_logo.svg`
- Android runtime vector: `app/src/main/res/drawable/colour_my_world_logo.xml`

The logo was redesigned on 2026-07-11 to follow the supplied paint-splash reference style: large glossy bubble lettering, a rainbow paint splash, a small `My` pill, droplets, stars, and paint-tool marks.

Shape source repo:

- Repository: `https://github.com/fahimc/svg-shape-harvester`
- Harvest commit used locally: `7a3ea909 Add downloaded SVG corpus`
- Local scratch checkout: `%TEMP%/svg-shape-harvester-codex`

Harvested shapes used:

| Shape id | Name | Source | License | Local path in harvester |
| --- | --- | --- | --- | --- |
| `game-icons:lorc-splash` | splash | Game-icons.net | CC BY 3.0 / public domain for selected icons | `downloads/game-icons/lorc-splash.svg` |
| `bootstrap-icons:icons-droplet-fill` | droplet-fill | Bootstrap Icons | MIT | `downloads/bootstrap-icons/icons-droplet-fill.svg` |
| `bootstrap-icons:icons-star-fill` | star-fill | Bootstrap Icons | MIT | `downloads/bootstrap-icons/icons-star-fill.svg` |
| `bootstrap-icons:icons-palette-fill` | palette-fill | Bootstrap Icons | MIT | `downloads/bootstrap-icons/icons-palette-fill.svg` |
| `bootstrap-icons:icons-brush-fill` | brush-fill | Bootstrap Icons | MIT | `downloads/bootstrap-icons/icons-brush-fill.svg` |
| `lucide:icons-sparkles` | sparkles | Lucide | ISC | `downloads/lucide/icons-sparkles.svg` |

The word shapes are converted outlines from the local Windows `Comic Sans MS Bold` font, then layered with original colours, shadows, white outlines, and highlight marks in this repo.
