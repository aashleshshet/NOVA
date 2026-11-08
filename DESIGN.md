# NOVA design system

Native Android / Material 3. The supplied brief pins the visual direction: an uncluttered AMOLED canvas, pale lime accent, Outfit typography and a shallow glass dock. Operate mode: launch apps in one tap; edit through explicit controls and optional direct manipulation.

## Tokens
- AMOLED background #000000; dark #101418; light #F5F6F1; custom opaque background.
- Accent #D6F4A0; foreground chosen from black/white by background luminance. Custom accents receive an independently contrast-safe on-primary.
- Display: Outfit light; 64sp clock. Material headline/title/body/label scale for controls. Optional platform sans or serif.
- Spacing: 4/8/12/16/24/32dp. Touch targets at least 48dp. Grid user-controlled 3–6 columns; dock 3–6 slots.
- Shape: 28dp main surfaces, 16dp icon masks, standard Material controls.
- Glass: translucent surface with one thin highlight, restrained sheen and solid fallback under power-saving or reduced effects. No per-frame wallpaper blur or screenshot sampling.
- Motion: Material fade/size transitions; system animation-duration scale respected by Compose. No perpetual decorative animation.

## Surfaces
Home: live date and large clock; page grid; widget panels; stable dock; app drawer and Studio access. Empty home tells users how to pin their real apps.
Drawer: autofocus search, sort selection, real icons and app labels, long press actions.
Studio: live miniature preview followed by grouped native controls, reversible changes, backup/restore, widget and privacy settings.

## Accessibility
System bars and IME insets; TalkBack semantics; labeled icons; large-font layouts scroll rather than clip. Drag editing has button alternatives. Actual app names, device time and widget contents only. No illustrative fake tiles.
