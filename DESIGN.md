---
name: "EKT assistant surfaces"
description: "Scoped catalogue workspace for /assistant and /cart; existing homepage identity is preserved."
colors:
  primary: "#1760ec"
  primary-hover: "#124ec6"
  ink: "#153057"
  muted: "#536780"
  ground: "#edf5ff"
  surface: "#fff"
  secondary-surface: "#e8f0fe"
  secondary-ink: "#24519a"
  hover-surface: "#dfeaff"
  card-border: "#d8e6f5"
  field-border: "#c7d6e8"
  focus: "#175ad0"
  stock-surface: "#eaf7ef"
  stock-ink: "#176142"
  warning-surface: "#fff5df"
  warning-ink: "#674713"
  warning-border: "#ecd39a"
  error-surface: "#fff0ef"
  error-ink: "#8f3029"
typography:
  headline:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "-.025em"
  detail-title:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "21px"
    fontWeight: 700
    lineHeight: 1.35
  title:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 700
    lineHeight: 1.4
  body:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: 1.5
  caption:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.5
  price:
    fontFamily: "PT Sans, \"Helvetica Neue\", Arial, sans-serif"
    fontSize: "30px"
    fontWeight: 700
    lineHeight: 1.5
rounded:
  tag: "6px"
  field: "8px"
  action: "9px"
  navigation: "10px"
  card: "14px"
  panel: "16px"
spacing:
  "8": "8px"
  "10": "10px"
  "12": "12px"
  "14": "14px"
  "16": "16px"
  "18": "18px"
  "20": "20px"
  "24": "24px"
  "28": "28px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    typography: "{typography.label}"
    rounded: "{rounded.action}"
    padding: "10px 17px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-secondary:
    backgroundColor: "{colors.secondary-surface}"
    textColor: "{colors.secondary-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.action}"
    padding: "10px 17px"
  button-secondary-hover:
    backgroundColor: "{colors.hover-surface}"
  quantity-field:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.field}"
    padding: "9px"
    width: "100%"
  action-navigation:
    backgroundColor: "{colors.surface}"
    textColor: "#214678"
    rounded: "{rounded.navigation}"
    padding: "10px 12px"
  product-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "14px"
  stock-tag:
    backgroundColor: "{colors.stock-surface}"
    textColor: "{colors.stock-ink}"
    rounded: "{rounded.tag}"
    padding: "4px 7px"
  detail-panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.panel}"
    padding: "24px"
---

# Design System: EKT assistant surfaces

## Overview

**Creative North Star: "The catalogue workspace"**

Pale blue ground carries white product and detail surfaces, with blue controls and PT Sans typography. The assistant keeps source uncertainty beside selection and explicit confirmation; its visual hierarchy is practical and compact.

This system applies only to /assistant and /cart. The existing homepage retains source banners, imagery and content; its later refinement uses scoped .ekt-home blue/navy tokens without replacing global defaults. The assistant’s scoped blue variables do not replace the global identity. Source evidence: frontend/src/assistant/assistant.css, components.tsx and AssistantPage.tsx, with inherited defaults from frontend/src/index.css.

**Key Characteristics:**
- Pale blue ground and white panels.
- Blue actions with visible focus and selection.
- Contained product photography and tabular numbers.
- Separate scrolling regions and a mobile native dialog.

## Colors

Cool blue action and ground colors frame white catalogue surfaces; amber and red carry uncertainty and errors rather than decoration.

### Primary
- **Action Blue:** `primary` drives primary buttons, the circular search submit and selected card outline. `primary-hover` deepens button hover.
- **Quiet Blue:** `secondary-surface` and `secondary-ink` support secondary controls; `hover-surface` marks their hover response.

### Secondary
- **Stock Green:** `stock-surface` and `stock-ink` distinguish stock information.
- **Caution Amber:** `warning-surface`, `warning-border` and `warning-ink` keep source uncertainty visible.
- **Error Red:** `error-surface` and `error-ink` mark actionable failures.

### Neutral
- **Blue Ground / White Surface:** `ground` separates independent white `surface` panels.
- **Deep Ink / Muted Ink:** `ink` carries headings and body text; `muted` carries supporting labels.
- **Quiet Edges:** `card-border` and `field-border` distinguish containers and quantity fields; `focus` is a separate visible interaction outline.

**The Scoped Identity Rule.** Apply these blue workspace tokens only inside the assistant and cart surfaces; preserve the homepage’s existing tokens.

**The Source Visibility Rule.** Keep warning and conflict treatments beside the affected product information.

## Typography

**Display Font:** No separate display face; PT Sans headings inherit the same family as the body.
**Body Font:** PT Sans with Helvetica Neue, Arial and sans-serif fallbacks.

Compact headings and restrained weight changes organize technical product data. PT Sans is shared with the existing home. Numeric prices, quantities and specification values use tabular numerals.

### Hierarchy
- **Headline:** page title; mobile reduces to 18px.
- **Detail title:** selected product and modal headings.
- **Title:** section headings within detail and cart; result section heading uses 19px.
- **Body:** base assistant copy; message paragraphs use 1.6 line height and description copy uses 14px/1.7.
- **Label:** primary and secondary button labels.
- **Caption:** articles, provenance and explanatory copy.
- **Price:** detail value; product cards use 23px/1.2, reducing to 21px on mobile.

These are the applied sizes rather than a fabricated modular scale. Small stock/spec/helper text in the shipped mobile surface is not a recommended reusable type role.

## Layout

The assistant is a 100dvh flex column with an 82px header, a status band and a remaining-height workspace. Desktop uses a 1.85fr / 1fr split with a 340px detail minimum, 18px workspace padding and 20px gap. Results use three equal columns with 12px gaps. The result/history region and detail panel scroll independently; the composer occupies a separate flex row.

Between 851px and 1100px, results use two columns, the workspace is 1.5fr / 1fr with a 320px detail minimum, and detail padding is 18px. At 850px and below, the workspace becomes one column with 12px padding; results remain two columns with 10px gaps. Detail moves to a native dialog. The header is 70px, and the search input becomes 16px. At 1600px and above, outer padding responds to a 1550px content span and product images grow.

The cart scrolls as a single content region with a 1050px maximum width, 30px desktop padding and 18px 12px mobile padding. Spacing is a compact even-value vocabulary rather than a single strict grid.

## Elevation & Depth

Ordinary cards are flat, separated by background and borders. The composer has a subtle ambient shadow; the dialog adds a larger shadow and a translucent backdrop. The exact shadow and backdrop values are carried in the sidecar.

**The Quiet Surface Rule.** Use tonal separation and thin borders for ordinary product cards; reserve the stronger ambient shadow for modal depth.

## Shapes

Rounded rectangles differentiate density: smaller tags and fields, medium action buttons and navigation, larger product cards and panels. Avatars and the search submit are circular. Message bubbles use an asymmetric square top corner to distinguish incoming from outgoing copy. Cards use a thin border; selected cards increase to 2px and reduce padding by 1px to preserve size.

## Components

### Buttons

Primary blue and secondary pale blue actions share a 44px minimum height, centered icon/text alignment and medium rounded corners. Primary hover deepens the fill; secondary hover uses the shared hover surface. Disabled controls retain their geometry at 0.48 opacity and a not-allowed cursor. The assistant focus-visible outline is 3px with a 3px offset. Mobile primary labels use 13px and tighter horizontal padding.

### Chips

Stock is a compact informational tag, not an interactive filter. Zero stock receives a separate muted rose treatment. Preserve the label text so color is not the sole distinction; do not infer purchase availability from the badge.

### Cards / Containers

White product cards use thin blue borders and contained imagery. Selection uses the blue outline; hover changes the border to a lighter blue. Product titles wrap long strings. Detail panels have larger corners and padding, and their own scrolling area. Cart rows share a white container with fine dividers.

### Inputs / Fields

Quantity fields use a white fill, thin field border, small corners, visible label and 44px minimum height. Search is borderless inside a white composer; it includes a labeled field and circular blue submit control. The composer has its own low shadow. Inputs inherit the assistant focus-visible treatment and use a blue caret for search.

### Navigation

Catalogue shortcuts are white rounded buttons with blue inline SVG icons, wrapping naturally. The header’s pale blue cart link remains visible on mobile while the subtitle and server text hide. Pagination uses secondary buttons and tabular page counts.

### Product detail and confirmation dialog

A native dialog carries mobile detail and confirmation. It is at most 540px wide on desktop with viewport-limited height; mobile leaves 10px around its edges. The white heading stays sticky during scrolling, with a 44px close button. Warnings and actionable errors remain inside the active dialog. A distinct confirmation action follows proposal values.

### Motion and imagery

Only the product border has an authored transition (150ms ease). Reduced-motion preference disables transitions and smooth scrolling within the assistant. Product imagery uses real remote EKT catalogue URLs with contain sizing; failures show an inline SVG box and a text fallback. No new generated raster is part of this system.

## Do's and Don'ts

### Do:
- Do preserve the homepage identity when extending assistant surfaces.
- Do keep product images contained and preserve their aspect ratio.
- Do use inline SVG paths for functional icons and retain text labels or accessible names.
- Do retain explicit warning, empty, loading and error states beside their active content.
- Do keep the composer in its own non-scrolling layout row and retain the mobile dialog close control.

### Don't:
- Don't present uncertain catalogue fields as confirmed values.
- Don't crop product photography to fill the image area.
- Don't turn a cart proposal into a visually implicit confirmation.

Not canonized: external-link arrow glyphs and mobile 10px stock/spec/helper text are present in the build; they are not reusable icon or type rules. This documentation does not repair source code or certify unavailable positive certificate/replacement data.

## Cart and homepage refinement — 23 September 2026

Cart rows use a 120px image column plus flexible content, and 72px on small screens. Name and image open the existing Detail inside the native modal; the background list remains mounted and its scroll position is restored. White rows on the pale blue ground keep the existing typography and actions. Quantity uses a 44px minus/plus control, 16px input text, explicit Apply, and a separate pale-red Delete. Errors stay with their row; price changes require another modal confirmation. Zero is never an implicit delete.

The homepage extends its previous layout and imagery with scoped `--color-primary:#0868aa`, `--color-primary-dark:#123552`, ground `#f4f8fc`, field `#eff4f9`, 12–14px card radii and 14px grid gaps. It retains PT Sans. Source: frontend/src/pages/HomePage.css. The new search forwards to the real assistant.

The assistant launcher is a navy `#0c304e` link with pale-blue SVG bot avatar, a desktop hint, and an accessible compact toggle. It sits 28px right /24px bottom on desktop and 16px plus safe-area inset on mobile. It describes catalogue search without AI and makes no online-status claim. WhatsApp remains a functional footer link. Source: frontend/src/components/layout/AssistantLauncher.tsx and .css. No generated raster assets added.
