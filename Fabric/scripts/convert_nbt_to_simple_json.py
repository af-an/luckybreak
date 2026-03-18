import json
import sys
from pathlib import Path
import nbtlib

default_src = Path("src/main/resources/data/luckybreak/structure/wishing_well.nbt")
default_out = Path("src/main/resources/data/luckybreak/simple_structures/wishing_well.json")

src = Path(sys.argv[1]) if len(sys.argv) > 1 else default_src
out = Path(sys.argv[2]) if len(sys.argv) > 2 else default_out

n = nbtlib.load(str(src))
palette = n["palette"]
blocks = n["blocks"]
size = [int(v) for v in n["size"]]

result = {
    "size": size,
    "origin": [0, 0, 0],
    "ignore_air": True,
    "replace_air_only": False,
    "blocks": [],
}

for b in blocks:
    pos = [int(v) for v in b["pos"]]
    pal = palette[int(b["state"])]
    entry = {"pos": pos, "block": str(pal["Name"])}
    props = pal.get("Properties")
    if props:
        entry["properties"] = {str(k): str(v) for k, v in props.items()}
    result["blocks"].append(entry)

out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(result, indent=2), encoding="utf-8")
print(out)
