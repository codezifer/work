package de.carsten.android.muzzic.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Global constants for the application.
 */
const val PREVIEW_DARK_MODE = android.content.res.Configuration.UI_MODE_NIGHT_YES
const val PLAYING_QUEUE = "Playing Queue"
const val BULLET_POINT = "•"

/**
 * Standard Spacing System (based on 4dp/8dp grid)
 */
val SPACING_NONE = 0.dp
val SPACING_TINY = 2.dp
val SPACING_SMALL = 4.dp
val SPACING_MEDIUM = 8.dp
val SPACING_NORMAL = 12.dp
val SPACING_LARGE = 16.dp
val SPACING_EXTRA_LARGE = 24.dp
val SPACING_HUGE = 32.dp

/**
 * Layout Tokens
 * These define the structure and overlay behavior of the app.
 */
val APP_FLOATING_MARGIN = 16.dp
val APP_BOTTOM_NAV_HEIGHT = 80.dp
val UI_OVERLAY_OFFSET = 120.dp // Offset to stack components above the floating nav bar
val SCREEN_CONTENT_BOTTOM_PADDING = 112.dp // Padding for scrollable content to clear the nav bar
val SNACKBAR_BOTTOM_PADDING = 112.dp

/**
 * Component Specific Dimensions
 */
val GLASS_PANEL_CORNER_RADIUS = 14.dp
val CARD_CORNER_RADIUS = 12.dp
val CORNER_RADIUS_SMALL = 8.dp
val CORNER_RADIUS_FULL = 25.dp
val CARD_INTERNAL_PADDING = 6.dp
val CARD_CONTENT_SPACING = 2.dp
val CARD_CONTENT_HEIGHT = 64.dp
val FAST_SCROLL_ITEM_MIN_HEIGHT = 22.dp // Minimum height per FastScroller item (letter or dot) to avoid overlap
val CARD_SELECTION_ICON_PADDING = 4.dp
val ALBUM_ART_PADDING = 22.dp
val VOLUME_BAR_WIDTH = 50.dp
val VOLUME_BAR_HEIGHT = 200.dp
val VOLUME_PREVIEW_WIDTH = 100.dp

val GRID_SPACING = 6.dp
val GRID_HORIZONTAL_PADDING = 12.dp

/**
 * Indicator Slider Specific Dimensions
 */
val INDICATOR_SLIDER_OFFSET_Y = (-40).dp
val INDICATOR_SLIDER_PADDING_WITH_TAIL = 14.dp
val INDICATOR_SLIDER_PADDING_TOP_WITH_TAIL = 10.dp
val INDICATOR_SLIDER_PREVIEW_PADDING = 60.dp
val TOOLBAR_MIN_WIDTH = 200.dp

/**
 * Chart Specific Dimensions
 */
val CHART_LABEL_SPACING = 20.dp
val CHART_AXIS_SPACING = 40.dp
val CHART_PREVIEW_WIDTH_LARGE = 300.dp
val CHART_PREVIEW_WIDTH_MEDIUM = 200.dp
val CHART_PREVIEW_HEIGHT_LARGE = 200.dp
val CHART_PREVIEW_HEIGHT_SMALL = 100.dp
val CHART_FULL_ROUNDING = 50.dp

/**
 * Border and Elevation Tokens
 */
val BORDER_WIDTH_THIN = 0.5.dp
val BORDER_WIDTH_NORMAL = 1.dp
val BORDER_WIDTH_THICK = 2.dp
val BORDER_WIDTH_FAT = 3.dp
val ELEVATION_SMALL = 4.dp
val ELEVATION_MEDIUM = 8.dp
val ELEVATION_LARGE = 12.dp

/**
 * Glass Effect Design Tokens
 */
val GLASS_BORDER_WIDTH = 0.5.dp
const val GLASS_BORDER_ALPHA = 0.2f
const val GLASS_BORDER_ALPHA_STRONG = 0.5f
val GLASS_GLOW_RADIUS = 4.dp

/**
 * Effects
 */
val BLUR_RADIUS_DEFAULT = 6.dp
val BLUR_RADIUS_LARGE = 12.dp
const val BACKGROUND_OVERLAY_ALPHA = 0.3f
const val GLASS_CONTAINER_ALPHA = 0.7f

/**
 * Glass Effect Overlay Alphas
 */
const val GLASS_OVERLAY_ALPHA_FAINT = 0.1f
const val GLASS_OVERLAY_ALPHA_SOFT = 0.3f
const val GLASS_OVERLAY_ALPHA_NORMAL = 0.5f
const val GLASS_OVERLAY_ALPHA_LOW = 0.4f
const val GLASS_OVERLAY_ALPHA_MEDIUM = 0.6f
const val GLASS_OVERLAY_ALPHA_HIGH = 0.7f
const val GLASS_OVERLAY_ALPHA_DRAGGING = 0.8f

/**
 * Icon Sizes
 */
val ICON_SIZE_TINY = 12.dp
val ICON_SIZE_SMALL = 16.dp
val ICON_SIZE_MEDIUM = 24.dp
val ICON_SIZE_LARGE = 40.dp
val ICON_SIZE_DRAG_HANDLE = 20.dp
val ICON_SIZE_EXTRA_LARGE = 48.dp
val ICON_SIZE_PLAYLIST_THUMB = 56.dp
val ICON_SIZE_PLAYER_MAIN = 72.dp
val ICON_SIZE_PLAYER_PLAY_PAUSE = 44.dp
val ICON_SIZE_PLAY_BUTTON_LARGE = 100.dp
val ICON_SIZE_CONTROL_SMALL = 32.dp
val ICON_SIZE_FAST_SCROLL_THUMB = 50.dp

/**
 * FastScroller Tokens
 */
const val FAST_SCROLL_HIDE_DELAY_MS = 2000L
val ELEVATION_FAST_SCROLL_IDLE = 2.dp
const val OPACITY_FAST_SCROLL_BACKGROUND = 0.4f
const val OPACITY_FAST_SCROLL_BORDER = 0.4f
const val OPACITY_FAST_SCROLL_HANDLE = 0.8f

/**
 * Statistics and Chart Tokens
 */
const val CHART_BAR_ROTATION = -45f
const val CHART_PIE_HOLE_RADIUS = 0.4f
val CHART_PIE_SIZE = 160.dp
const val CHART_PERCENTAGE_THRESHOLD = 5
val RATING_STAR_SIZE = 10.dp
val RANK_COLUMN_WIDTH = 32.dp
const val PERCENTAGE_FACTOR = 100
const val OPACITY_MEDIUM = 0.6f

/**
 * Typography - Font Sizes
 */
val FONT_SIZE_TINY = 9.sp
val FONT_SIZE_SMALL = 10.sp
val FONT_SIZE_CAPTION = 12.sp
val FONT_SIZE_BODY = 14.sp
val FONT_SIZE_SUBTITLE = 16.sp
val FONT_SIZE_TITLE = 18.sp
val FONT_SIZE_LARGE_TITLE = 20.sp
val FONT_SIZE_HUGE_TITLE = 24.sp

// Legacy/Compatibility Aliases (to be replaced gradually)
val MAINTITLE_FONTSIZE = FONT_SIZE_SUBTITLE
val SUBTITLE_ICONSIZE = 14.dp
val SUBTITLE_FONTSIZE = FONT_SIZE_TINY
val SUBTITLE_SPACING = 2.dp
val SUBTITLE_ICON_TEXT_SPACING = 1.dp

/**
 * Fixed thumbnail edge length for collage tiles (56.dp thumb split into a 3x3 grid).
 */
const val COLLAGE_TILE_PX = 128

/**
 * Fixed edge length for full-width card covers (aspect 1.1-1.4).
 */
const val CARD_COVER_PX = 512

/**
 * Fixed edge length for blurred backgrounds; blur hides detail so a small size is sufficient.
 */
const val BLUR_BG_PX = 256

/**
 * Fixed edge length for the player detail artwork.
 */
const val PLAYER_ART_PX = 1024
