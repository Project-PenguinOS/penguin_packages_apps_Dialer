#!/usr/bin/env python3

import pathlib
import re
import sys
import urllib.request

VERSION = "5.0.5"
ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "java/com/android/dialer/icons/res/drawable"

ICONS = {
    "quantum_ic_call_vd_theme_24": "phone_fill",
    "quantum_ic_phone_vd_theme_24": "phone_fill",
    "quantum_ic_videocam_vd_theme_24": "videocam_fill",
    "quantum_ic_videocam_vd_white_24": "videocam_fill",
    "quantum_ic_message_vd_theme_24": "chat_bubble_text",
    "quantum_ic_person_add_vd_theme_24": "person_badge_plus",
    "quantum_ic_block_vd_theme_24": "nosign",
    "quantum_ic_delete_vd_theme_24": "trash",
    "quantum_ic_content_copy_vd_theme_24": "doc_on_doc",
    "quantum_ic_edit_vd_theme_24": "pencil",
    "quantum_ic_info_outline_vd_theme_24": "info_circle",
    "quantum_ic_close_vd_theme_24": "xmark",
    "quantum_ic_more_vert_vd_theme_24": "ellipsis_circle",
    "quantum_ic_search_vd_theme_24": "search",
    "quantum_ic_call_made_vd_theme_24": "arrow_up_right",
    "quantum_ic_call_received_vd_theme_24": "arrow_down_left",
    "quantum_ic_call_missed_vd_theme_24": "arrow_down_left",
    "ic_dialpad_delete": "delete_left",
    "quantum_ic_mic_off_vd_theme_24": "mic_slash_fill",
    "quantum_ic_volume_up_vd_theme_24": "speaker_3_fill",
    "quantum_ic_pause_vd_theme_24": "pause_fill",
    "quantum_ic_dialpad_vd_theme_24": "circle_grid_3x3_fill",
    "quantum_ic_call_end_vd_theme_24": "phone_down_fill",
    "quantum_ic_call_end_vd_theme_36": "phone_down_fill",
    "quantum_ic_call_merge_vd_theme_24": "arrow_merge",
    "quantum_ic_swap_calls_vd_theme_24": "arrow_swap",
    "ic_addcall_white": "phone_fill_badge_plus",
    "ic_settings_chevron": "chevron_right",
    "ic_nav_back_chevron": "chevron_left",
}

DEFAULTS = {
    "ic_dialpad_delete": {"width": "28dp", "height": "28dp", "autoMirrored": "true"},
    "ic_settings_chevron": {"autoMirrored": "true", "tint": "?android:attr/textColorSecondary"},
    "ic_nav_back_chevron": {"autoMirrored": "true"},
}

VIEWPORT = 64
OFFSET = (VIEWPORT - 56) / 2

HEADER = """<?xml version="1.0" encoding="utf-8"?>
"""


def original_attrs(name):
    attrs = {"width": "24dp", "height": "24dp"}
    for path in (ROOT / "java").rglob(f"res/drawable*/{name}.xml"):
        if OUT in path.parents:
            continue
        text = path.read_text()
        if "<vector" not in text:
            continue
        for attr in ("width", "height", "tint", "autoMirrored"):
            m = re.search(rf'android:{attr}="([^"]+)"', text)
            if m:
                attrs[attr] = m.group(1)
        break
    attrs.update(DEFAULTS.get(name, {}))
    return attrs


def fetch(f7):
    url = f"https://cdn.jsdelivr.net/npm/framework7-icons@{VERSION}/svg/{f7}.svg"
    with urllib.request.urlopen(url) as response:
        svg = response.read().decode()
    assert 'viewBox="0 0 56 56"' in svg, f7
    return re.findall(r'<path[^>]* d="([^"]+)"', svg)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, f7 in sorted(ICONS.items()):
        attrs = original_attrs(name)
        paths = "\n".join(
            f'    <path\n        android:fillColor="@android:color/white"\n'
            f'        android:pathData="{d}"/>'
            for d in fetch(f7))
        extra = "".join(
            f'\n    android:{a}="{attrs[a]}"' for a in ("tint", "autoMirrored") if a in attrs)
        (OUT / f"{name}.xml").write_text(HEADER.format(f7=f7) + f"""<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{attrs['width']}"
    android:height="{attrs['height']}"
    android:viewportWidth="{VIEWPORT}"
    android:viewportHeight="{VIEWPORT}"{extra}>
  <group android:translateX="{OFFSET:g}" android:translateY="{OFFSET:g}">
{paths}
  </group>
</vector>
""")
        print(f"{name} <- {f7}")


if __name__ == "__main__":
    sys.exit(main())
