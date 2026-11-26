# DESIGN REFERENCE (KOTLIN / JETPACK COMPOSE)

- **Source:** [Fontpair Playground](https://fontpair.co/playground/mallow-vellum?heading=mallow&body=vellum&color=121212&icons=1)
- **Target Platform:** Android (Kotlin 100%, Jetpack Compose, Material Design 3)

---

## 1. Typography Rules

- **Headings & Display:** Use **Mallow** for all headings (`displayLarge`–`displaySmall`, `headlineLarge`–`headlineSmall`, `titleLarge`).
- **Body & Captions:** Use **Vellum** for all body text (`bodyLarge`–`bodySmall`), captions, labels (`labelLarge`–`labelSmall`), and input copy.
- **Strict Prohibition:** Do NOT substitute with Roboto, Inter, system-ui, Georgia, or any system fallback font.
- **Font Weight Policy:**
  - Body: `FontWeight.W400` (Normal) and `FontWeight.W500` (Medium).
  - Headings: `FontWeight.W500` (Medium) to `FontWeight.W600` (SemiBold).
  - Do NOT use `FontWeight.Bold` / `W700+` unless explicitly requested.
- **Letter Spacing:**
  - Headings: Slightly loose to normal (`-0.01.em` to `0.em` / `-0.25.sp` to `0.sp`).
  - Body: Normal (`0.sp` to `0.25.sp`).
- **Typographic Scale Mapping:**

| Scale Role | Compose Token | Font | Size (`sp`) | Line Height (`sp`) | Weight | Letter Spacing |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Hero / Display** | `displayLarge` | Mallow | `56.sp` (48–64sp) | `64.sp` | `FontWeight.SemiBold` | `-0.01.em` |
| **Display Alt** | `displayMedium` | Mallow | `48.sp` | `56.sp` | `FontWeight.SemiBold` | `-0.01.em` |
| **Heading 1** | `headlineLarge` | Mallow | `36.sp` | `44.sp` | `FontWeight.SemiBold` | `0.em` |
| **Heading 2** | `headlineMedium`| Mallow | `28.sp` | `36.sp` | `FontWeight.Medium` | `0.em` |
| **Heading 3** | `headlineSmall` / `titleLarge` | Mallow | `22.sp` | `28.sp` | `FontWeight.Medium` | `0.em` |
| **Subheading** | `titleMedium` | Mallow | `18.sp` | `24.sp` | `FontWeight.Medium` | `0.em` |
| **Body (Default)** | `bodyLarge` | Vellum | `16.sp` | `24.sp` | `FontWeight.Normal` | `0.sp` |
| **Body Muted** | `bodyMedium` | Vellum | `14.sp` | `20.sp` | `FontWeight.Normal` | `0.sp` |
| **Caption / Meta** | `bodySmall` / `labelSmall` | Vellum | `13.sp` | `18.sp` | `FontWeight.Normal` | `0.sp` |
| **Button / Label** | `labelLarge` | Vellum | `14.sp` | `20.sp` | `FontWeight.Medium` | `0.1.sp` |

---

## 2. Font Asset Setup in Android

Store bundled `.ttf` / `.otf` files under `app/src/main/res/font/` using strict Android lower_snake_case naming:

```text
app/src/main/res/font/
├── mallow_regular.ttf       # Weight 400
├── mallow_medium.ttf        # Weight 500
├── mallow_semibold.ttf      # Weight 600
├── vellum_regular.ttf       # Weight 400
└── vellum_medium.ttf        # Weight 500
```

### Kotlin Font Family Declarations (`Type.kt`)

```kotlin
package com.example.hello.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.hello.R

val MallowFontFamily = FontFamily(
    Font(R.font.mallow_regular, FontWeight.Normal),
    Font(R.font.mallow_medium, FontWeight.Medium),
    Font(R.font.mallow_semibold, FontWeight.SemiBold)
)

val VellumFontFamily = FontFamily(
    Font(R.font.vellum_regular, FontWeight.Normal),
    Font(R.font.vellum_medium, FontWeight.Medium)
)

val DesignTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.01).em
    ),
    displayMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.01).em
    ),
    headlineLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.em
    ),
    headlineMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.em
    ),
    headlineSmall = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.em
    ),
    titleLarge = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.em
    ),
    titleMedium = TextStyle(
        fontFamily = MallowFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.em
    ),
    bodyLarge = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VellumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)
```

---

## 3. Color Rules & Material 3 Palette

- **Background:** `#FFFFFF` — screen background and card surfaces.
- **Foreground:** `#161616` — all primary text, headings, and active icons.
- **Foreground Muted:** `#3A3A3A` — secondary text, placeholders, and metadata.
- **Primary:** `#121212` — CTAs, active states, key UI accents.
- **Accent:** `#404040` — pressed states, hover/focus, secondary emphasis.
- **Border:** `#E2E8F0` — dividers, input borders, and card outlines.
- **Strict Rule:** Do NOT use pure black (`#000000`). `#161616` is the deepest value for text/foreground, and `#121212` is for primary fills. Do NOT introduce default colors (such as random blues, purples, or greys).

### Kotlin Color Constants (`Color.kt`)

```kotlin
package com.example.hello.ui.theme

import androidx.compose.ui.graphics.Color

val PaletteBackground = Color(0xFFFFFFFF)
val PaletteForeground = Color(0xFF161616)
val PaletteForegroundMuted = Color(0xFF3A3A3A)
val PalettePrimary = Color(0xFF121212)
val PaletteAccent = Color(0xFF404040)
val PaletteBorder = Color(0xFFE2E8F0)
```

### Material 3 ColorScheme Integration (`Theme.kt`)

```kotlin
package com.example.hello.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val DesignColorScheme = lightColorScheme(
    primary = PalettePrimary,
    onPrimary = Color.White,
    primaryContainer = PaletteAccent,
    onPrimaryContainer = Color.White,
    secondary = PaletteAccent,
    onSecondary = Color.White,
    background = PaletteBackground,
    onBackground = PaletteForeground,
    surface = PaletteBackground,
    onSurface = PaletteForeground,
    surfaceVariant = PaletteBackground,
    onSurfaceVariant = PaletteForegroundMuted,
    outline = PaletteBorder,
    outlineVariant = PaletteBorder
)
```

---

## 4. Icon Rules

- **Icon Set:** Feather Icons exclusively.
- **Android Integration:** Store Feather SVG icons as Android Vector Drawables under `res/drawable/ic_feather_<name>.xml`.
- **Alternative:** Feather Compose icons via `painterResource(R.drawable.ic_feather_*)` or Compose Vector Asset.
- **Prohibition:** Do NOT mix in Material Symbols, Heroicons, or Lucide icons with mismatched stroke weights.
- **Size Scale (Compose `Modifier.size`):**
  - Inline / Trailing icons: `16.dp`
  - Standard UI / Button / Input icons: `20.dp`
  - Feature / Header / App Bar icons: `24.dp`
- **Default Tint:** `tint = MaterialTheme.colorScheme.onSurface` (resolves to `#161616`) or `MaterialTheme.colorScheme.onSurfaceVariant` (`#3A3A3A`).

---

## 5. Design Philosophy, Shapes & Elevation

- **Core Philosophy — Ease of Use & Seamlessness:** Prioritize frictionless, intuitive interactions and fluid visual continuity over rigid boxed frames. Surfaces rely on soft tonal background fills (`surfaceContainer`, `surfaceContainerHigh`, and curated tag palette containers) and connected Material 3 Expressive container geometry (`24.dp` outer corners / `4.dp` inner seams) rather than `1.dp` structural borders.
- **Tag & Chip Standard (Borderless Pill-to-Squircle, ADR-0038):**
  - **Zero Borders:** All tags (`TonalTagPill`), filter/suggestion chips (`TonalFilterPill`, `MinimalSelectableChip`), and tag color dots/swatches render exclusively with background fills (`0.dp` border in both unselected and selected states).
  - **Shape Morph:** Unselected and static display tags are full stadium pills (`CircleShape`, corner radius `height / 2`). When selected, interactive tags, chips, and color swatches smoothly morph in the draw phase to an `8.dp` squircle (`RoundedCornerShape(8.dp)`).
  - **Uniform Proportional Sizing:** Every tag pill and selectable chip shares a uniform `28.dp` height, proportional 2:1 padding (`horizontal = 12.dp, vertical = 6.dp`), and natural text-relative width across all screens.

```kotlin
val DesignShapes = Shapes(
    small = RoundedCornerShape(8.dp),    // Selected tag squircle radius
    medium = RoundedCornerShape(16.dp),  // Standard dialogs / inner cards
    large = RoundedCornerShape(24.dp)    // Connected M3 Expressive outer container radius
)
```

---

## 6. Jetpack Compose Component Recipes

### A. Primary Button
- Background: `PalettePrimary` (`#121212`)
- Content: `Color.White` (`#FFFFFF`)
- Corner Shape: `RoundedCornerShape(8.dp)`
- Minimum touch target: `48.dp`

```kotlin
@Composable
fun DesignPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PalettePrimary,
            contentColor = Color.White,
            disabledContainerColor = PaletteBorder,
            disabledContentColor = PaletteForegroundMuted
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
```

### B. Surface Card
- Background: `MaterialTheme.colorScheme.surfaceContainer` (soft tonal fill)
- Border: `0.dp` (borderless tonal separation for seamlessness)
- Padding: `16.dp` to `24.dp`
- Shape: `RoundedCornerShape(24.dp)` (or `shapeFor(index, totalItems)` for connected groups)

```kotlin
@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
```

### C. Text Input / Field
- Border: `1.dp` solid `PaletteBorder` (`#E2E8F0`)
- Text Color: `PaletteForeground` (`#161616`)
- Placeholder Color: `PaletteForegroundMuted` (`#3A3A3A`)
- Shape: `RoundedCornerShape(8.dp)`

```kotlin
@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = PaletteForegroundMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = PaletteForeground),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PalettePrimary,
            unfocusedBorderColor = PaletteBorder,
            focusedContainerColor = PaletteBackground,
            unfocusedContainerColor = PaletteBackground,
            cursorColor = PalettePrimary
        ),
        modifier = modifier.fillMaxWidth()
    )
}
```

### D. Text Link / Action Trigger
- Color: `PalettePrimary` (`#121212`)
- Pressed state: `PaletteAccent` (`#404040`)
- No underline by default

```kotlin
@Composable
fun DesignTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = PalettePrimary,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = PaletteAccent),
            onClick = onClick
        )
    )
}
```

### E. Divider
- Color: `PaletteBorder` (`#E2E8F0`)
- Thickness: `1.dp`

```kotlin
@Composable
fun DesignDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = PaletteBorder
    )
}
```

---

## 7. Implementation & Architectural Invariants

1. **Tokens Over Raw Values:** Never hardcode raw hex values (e.g. `Color(0xFF161616)`) or font family strings directly inside feature composables. Always reference `MaterialTheme.colorScheme`, `MaterialTheme.typography`, or the defined design tokens.
2. **Density Independence:** All dimensions, padding, margins, borders, and icon dimensions must use `dp`. All text sizes and line heights must use `sp`. Never use raw `px`.
3. **Touch Targets:** Maintain minimum touch targets of `48.dp` for all interactive elements in accordance with Android accessibility guidelines.
4. **Seamlessness & Consistency:** Design for ease of use and seamlessness. Keep decorative elements minimal and prefer soft tonal background fills over structural 1dp borders. Tags and selectable chips must remain strictly borderless (`0.dp` border), pill-shaped (`CircleShape`) when unselected, morphing to an `8.dp` squircle when selected, with uniform `28.dp` height and `12.dp × 6.dp` padding everywhere.
5. **Motion Philosophy — Everything moves with a purpose in animation and motion design:**
   - Every animation must answer *"why does this move?"* (spatial continuity, state change, foreground focus, or tactile press confirmation). Never animate for decoration on high-frequency actions.
   - Use Emil Kowalski's custom curves from `MotionTokens` (`StrongEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` for entrances/exits and `StrongEaseInOut` for on-screen morphs). Never use `ease-in` on UI transitions.
   - Keep UI transitions under `300ms` with **asymmetric enter/exit timing** (faster exits than entrances, e.g., `180ms` enter / `130ms` exit).
   - Popovers must be **origin-aware** (`TransformOrigin` anchored to their trigger) and never scale from `0f` (start at `0.94f` + opacity).
   - Use **subtle blur (`2.dp–3.dp`, API 31+ `BlurEffect` in `graphicsLayer`)** purposefully to bridge state crossfades or establish depth behind floating popovers — never heavy blur.
   - Every pressable primary/action button provides tactile `0.96f` press feedback in `graphicsLayer` (GPU-only `transform`/`alpha`).
   - Respect `LocalReducedMotion`: remove positional/scale/blur motion while keeping gentle opacity/color transitions for comprehension.
