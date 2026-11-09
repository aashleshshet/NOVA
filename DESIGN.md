# NOVA design system

Native Android / Material 3. The final visual direction is Nothing OS-inspired: an uncluttered monochrome canvas, dot-matrix clock, restrained red accents, Outfit typography and a shallow glass dock. Operate mode: launch apps in one tap; edit through explicit controls and optional direct manipulation.

## Tokens
- AMOLED background #000000; dark #121212; light #F4F3EF; custom opaque background.
- Accent #FF4A44; foreground chosen from black/white by background luminance. Custom accents receive an independently contrast-safe on-primary.
- Display: vector dot-matrix live clock; Outfit controls. Material headline/title/body/label scale for controls. Optional platform sans or serif.
- Spacing: 4/8/12/16/24/32dp. Touch targets at least 48dp. Grid user-controlled 3–6 columns; dock 3–6 slots.
- Shape: 28dp main surfaces, circular icon defaults with optional rounded or square masks, standard Material controls.
- Glass: translucent surface with one thin highlight, restrained sheen and solid fallback under power-saving or reduced effects. No per-frame wallpaper blur or screenshot sampling.
- Motion: Material fade/size transitions; system animation-duration scale respected by Compose. No perpetual decorative animation.

## Surfaces
Home: live date and large clock; page grid; widget panels; stable dock; app drawer and Studio access. Empty home tells users how to pin their real apps.
Drawer: autofocus search, sort selection, real icons and app labels, long press actions.
Studio: live miniature preview followed by grouped native controls, reversible changes, backup/restore, widget and privacy settings.

## Accessibility
System bars and IME insets; TalkBack semantics; labeled icons; large-font layouts scroll rather than clip. Drag editing has button alternatives. Actual app names, device time and widget contents only. No illustrative fake tiles.

## Nothing OS–inspired revision (user direction, 2026-11-09)
Monochrome is now the default: black/#121212 ground, #232323 control surfaces, warm off-white text, and #FF4A44 accents reserved for active controls. A custom vector dot matrix renders the live clock and NOVA wordmark; no external brand assets or affiliation claims. Round grayscale app icons are optional and always use the real installed artwork. The control centre has large native action tiles, real battery/network status, media-volume adjustment, permission-gated brightness/flashlight, and Android-owned connectivity/focus panels. Swipe down opens it by default; Studio can remap the gesture. Existing layout and theme choices remain persistent.
