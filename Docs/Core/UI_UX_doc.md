# UI/UX Design Document

**Last Updated:** 2026-01-06  
**Status:** Implemented ✅

## Design System

### Color Palette
Based on Adygea flag colors and Material Design 3:

#### Primary Colors
- **Primary Green:** #2E7D32 (Forest Green)
- **Primary Gold:** #FFB300 (Amber)
- **Primary Container:** #A5D6A7
- **On Primary:** #FFFFFF

#### Secondary Colors
- **Secondary:** #00897B (Teal)
- **Secondary Container:** #B2DFDB
- **On Secondary:** #FFFFFF

#### Surface Colors
- **Surface:** #FAFAFA
- **Surface Variant:** #E0E0E0
- **Background:** #FFFFFF
- **On Surface:** #212121

#### Semantic Colors
- **Error:** #D32F2F
- **Success:** #388E3C
- **Warning:** #FFA000
- **Info:** #1976D2

#### Dark Theme Colors
- **Primary Dark:** #1B5E20
- **Surface Dark:** #121212
- **Background Dark:** #000000

### Typography (Material Design 3)
- **Display Large:** 57sp, Roboto
- **Display Medium:** 45sp, Roboto
- **Display Small:** 36sp, Roboto
- **Headline Large:** 32sp, Roboto Medium
- **Headline Medium:** 28sp, Roboto Medium
- **Headline Small:** 24sp, Roboto Medium
- **Title Large:** 22sp, Roboto Regular
- **Title Medium:** 16sp, Roboto Medium
- **Title Small:** 14sp, Roboto Medium
- **Body Large:** 16sp, Roboto Regular
- **Body Medium:** 14sp, Roboto Regular
- **Body Small:** 12sp, Roboto Regular
- **Label Large:** 14sp, Roboto Medium
- **Label Medium:** 12sp, Roboto Medium
- **Label Small:** 11sp, Roboto Medium

### Spacing System
- **Extra Small:** 4dp
- **Small:** 8dp
- **Medium:** 16dp
- **Large:** 24dp
- **Extra Large:** 32dp
- **XXL:** 48dp

### Component Specifications

#### Map Screen
- **Map View:** Full screen with overlays
- **Marker Style:** 
  - Size: 48x48dp
  - Shape: Circular with photo or colored background
  - Border: 2dp white stroke
  - Clustering: Enabled at zoom < 14
- **Controls Position:**
  - Zoom: Bottom right, 16dp margin
  - Location: Bottom right above zoom, 8dp spacing
  - Search Bar: Top, 16dp margin

#### Attraction Card (Bottom Sheet)
- **Height:** 
  - Collapsed: 120dp
  - Expanded: 60% of screen height
- **Content:**
  - Header: Photo gallery (16:9 ratio)
  - Title: Headline Medium
  - Category chip: Label Medium
  - Rating: 5 stars with numeric value
  - Description: Body Medium
  - Action buttons: 48dp height

#### Search Screen
- **Search Bar:**
  - Height: 56dp
  - Leading icon: Search icon
  - Trailing icon: Clear/Filter
  - Background: Surface variant
- **Results List:**
  - Item height: 72dp
  - Leading: 40x40dp image
  - Title: Title Medium
  - Subtitle: Body Small
  - Divider: 1dp

#### Favorites Screen
- **Layout:** LazyVerticalGrid
- **Columns:** 2 (portrait), 3 (landscape)
- **Card:**
  - Aspect ratio: 3:4
  - Image: 16:9 top section
  - Padding: 8dp
  - Elevation: 2dp

#### Settings Screen
- **Section Headers:** Title Small, Primary color
- **List Items:**
  - Height: 56dp
  - Leading icon: 24x24dp
  - Title: Body Large
  - Toggle/Chevron: Trailing

### Interaction Patterns

#### Touch Targets
- Minimum: 48x48dp
- Recommended: 56x56dp for primary actions

#### Navigation
- **Bottom Navigation:** Hidden on map, visible on other screens
- **Back Navigation:** Top app bar with back arrow
- **Gestures:** 
  - Swipe right: Back
  - Swipe up: Expand bottom sheet
  - Pinch: Zoom map

#### Animations
- **Duration:**
  - Short: 150ms
  - Medium: 300ms
  - Long: 500ms
- **Easing:** Material standard easing
- **Transitions:**
  - Screen: Shared element for images
  - Bottom sheet: Spring animation
  - FAB: Scale and fade

#### Loading States
- **Skeleton screens** for lists
- **Circular progress** for actions
- **Shimmer effect** for cards
- **Pull-to-refresh** gesture

#### Error States
- **Empty states:** Illustration + message + action
- **Error messages:** Snackbar for transient, inline for persistent
- **Offline mode:** Banner at top with retry action

### Accessibility

#### Requirements
- **Touch targets:** Min 48x48dp
- **Color contrast:** WCAG AA standard (4.5:1)
- **Content descriptions:** All interactive elements
- **Focus indicators:** Visible keyboard navigation
- **Screen reader:** TalkBack support
- **Font scaling:** Support up to 200%

#### Labels
- Meaningful button labels
- Descriptive content descriptions
- Grouped related information
- Announce state changes

### Responsive Design

#### Phone (Portrait)
- **Columns:** 1-2
- **Navigation:** Bottom navigation
- **Map controls:** Overlay buttons

#### Phone (Landscape)
- **Columns:** 2-3
- **Navigation:** Rail navigation
- **Map controls:** Side panel

#### Tablet (7"+)
- **Columns:** 2-4
- **Navigation:** Rail navigation
- **Layout:** Master-detail

#### Tablet (10"+)
- **Columns:** 3-6
- **Navigation:** Permanent drawer
- **Layout:** Two-pane

### Localization

#### Supported Languages
- Russian (primary)
- English

#### RTL Support
- Not required for initial release

#### Text Expansion
- Allow 30% expansion for translations
- Use wrap_content for text views
- Avoid fixed widths

### Performance Guidelines

#### Image Loading
- **Thumbnails:** 200x200dp max
- **Full images:** Progressive loading
- **Cache:** Memory and disk caching
- **Format:** WebP preferred, JPEG fallback

#### List Performance
- **Lazy loading:** For all lists
- **View recycling:** RecyclerView/LazyColumn
- **Pagination:** 20 items per page
- **Placeholder:** While loading

#### Map Performance
- **Marker limit:** 100 visible markers
- **Clustering:** For > 50 markers
- **Render:** 60 FPS target
- **Cache:** Tile caching enabled

### Brand Guidelines

#### Logo
- Adygyes text logo with mountain icon
- Minimum size: 120x40dp
- Clear space: 8dp around

#### Icon Style
- Material Icons outlined
- Custom icons: 24x24dp, 2dp stroke

#### Photography
- Natural Adygea landscapes
- Bright, vibrant colors
- 16:9 ratio for headers
- 1:1 ratio for thumbnails

#### Voice & Tone
- Friendly and welcoming
- Informative but concise
- Local cultural respect
- Clear call-to-actions

### Component Library

#### Custom Components (Implemented ✅)
1. **AttractionCard:** Reusable card with image, title, rating
2. **CategoryChip:** Colored chip with icon and label
3. **MapMarkerView:** Custom marker with photo (Dual-Layer System)
4. **SearchBar:** Enhanced search with filters
5. **RatingBar:** 5-star rating display
6. **PhotoGallery:** Swipeable image viewer with zoom
7. **EmptyState:** Illustration with message
8. **LoadingShimmer:** Skeleton loading effect
9. **SearchResultsPanel:** Interactive panel (Expanded/Half states)
10. **CompactAttractionCard:** Compact 80dp cards for search
11. **CategoryCarousel:** Horizontal scrollable categories
12. **AttractionBottomSheet:** Modal sheet for attraction details
13. **CategoryFilterBottomSheet:** Filter selection interface

#### Auth & Reviews Components (Stage 12 ✅)
14. **AuthModal:** Login/Register modal (Email/Password)
15. **ReviewSection:** Container for attraction reviews
16. **ReviewCard:** Individual review card
17. **RatingSummaryBlock:** Rating summary with CTA
18. **WriteReviewModal:** Modal for writing reviews
19. **StatusBadge:** Review status badges
20. **InteractiveRating:** Clickable star rating

### Testing Checklist

#### Visual Testing ✅ COMPLETED
- [x] All screens in light/dark mode
- [x] Different screen sizes
- [x] Font scaling 100%, 150%, 200%
- [x] Color blind modes
- [x] Landscape orientation

#### Interaction Testing ✅ COMPLETED
- [x] Touch targets >= 48dp
- [x] Loading states display correctly
- [x] Error states handle gracefully
- [x] Animations smooth at 60fps
- [x] Gestures work intuitively

#### Accessibility Testing ✅ COMPLETED
- [x] Screen reader navigation
- [x] Keyboard navigation
- [x] Focus indicators visible
- [x] Color contrast passes
- [x] Content descriptions present
