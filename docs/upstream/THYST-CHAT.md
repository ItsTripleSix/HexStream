# ThystTV chat-overlay adaptation

Source: [tzii/ThystTV](https://github.com/tzii/ThystTV), reviewed at [`ae3b1c6c784c1ebc4b8096c3a8df5791827787e4`](https://github.com/tzii/ThystTV/tree/ae3b1c6c784c1ebc4b8096c3a8df5791827787e4). ThystTV is based on Xtra and distributes its source under GNU AGPL v3.

Credit: **tzii (Simone Perini) and ThystTV contributors**. Relevant original work includes:

| Original change | Authorship and purpose |
| --- | --- |
| [`9bc95bdd76432b5a71e1c98324776b96464bb37f`](https://github.com/tzii/ThystTV/commit/9bc95bdd76432b5a71e1c98324776b96464bb37f) | Simo / tzii: preserve the existing chat by reparenting its view, avoiding reconnects. |
| [`49a03e9aad463960cba3060c626677bc74a5ea49`](https://github.com/tzii/ThystTV/commit/49a03e9aad463960cba3060c626677bc74a5ea49) | Simo / tzii: hide floating chat on minimize, refine resizing and default position. |
| [`e785c8052e61682245a31f933f96d08cb13a05a1`](https://github.com/tzii/ThystTV/commit/e785c8052e61682245a31f933f96d08cb13a05a1) | Simone Perini / tzii: keep the floating surface dark over video. |

HexStream adapts these principles into a small player-owned controller with optional background opacity, high-contrast text and a single existing ChatFragment view. Beta.6 simplifies the presentation to a frameless, full-height left/right column over the full-size video, transparent by default. The movable/resizable beta.5 window and its toolbar are removed. It retains its brightness/volume/minimize gestures and show/hide chat behavior. The overlay is in-app only and requests no system draw-over-other-apps permission.

The default remains docked chat. Overlay mode is selected from Settings → Chat or the landscape player menu. Hidden chat, portrait, audio/chat-only playback, mini-player and Picture-in-Picture suppress the overlay. The existing live-chat composer is available inside the overlay, including replies/emotes; authorization and message submission remain in the original chat flow. Keyboard and visible player controls get reserved input space. Overlay preferences are global; obsolete beta.5 position/size values are ignored. No channel-specific list is bundled.

Keep this attribution, the existing Xtra credits and the AGPL license with redistributed source. The Xtra synchronization checkpoint in [SYNC.json](SYNC.json) remains independent of this feature adaptation.
