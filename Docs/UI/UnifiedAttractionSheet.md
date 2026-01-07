# Unified Attraction Bottom Sheet Architecture

## Overview

Unified attraction bottom sheet as the single component for displaying attraction details across the app, eliminating the separate detail screen.

## Architecture Change

### Before (Separate Components)
- **AttractionBottomSheet**: Quick preview on map with "Подробнее" button
- **AttractionDetailScreen**: Full-screen details with all information
- Navigation flow: Map → BottomSheet → DetailScreen

### After (Unified Component)
- **AttractionBottomSheet**: Complete attraction details (always mounted)
- Photo gallery opens fullscreen PhotoViewer modal (not navigation)
- Navigation flow: Map/List/Search/Favorites → BottomSheet (modal)

## Benefits

✅ **Consistent UX**: Same component everywhere (map markers, list items, search results, favorites)  
✅ **Simplified Navigation**: No back stack management for detail screens  
✅ **Performance**: Modal sheet is faster than navigation transitions  
✅ **Code Maintenance**: Single source of truth for attraction UI  
✅ **Platform Alignment**: Matches React Native UnifiedAttractionSheet pattern

## Implementation Details

### AttractionBottomSheet.kt

**Removed Parameters:**
- `onNavigateToDetail: () -> Unit` - no longer needed

**Added State:**
```kotlin
var showPhotoViewer by remember { mutableStateOf(false) }
var selectedPhotoIndex by remember { mutableStateOf(0) }
```

**Removed UI:**
- "Подробнее" button at bottom

**Added UI:**
- PhotoViewer modal composable (fullscreen photo gallery)

### Navigation Changes

**AdygyesNavHost.kt:**
- ❌ Removed `AttractionDetail` route composable
- ❌ Removed `AttractionDetailScreen` import
- ✅ SearchScreen receives `mapViewModel` parameter
- ✅ FavoritesScreen receives `mapViewModel` parameter

**MapScreenContainer.kt:**
- ❌ Removed `onAttractionClick` parameter
- ✅ Only navigation is `onNavigateToFavorites`

**MapScreen.kt:**
- ❌ Removed `onAttractionClick` parameter
- ✅ List items call `viewModel.selectAttractionById()`
- ❌ Removed detail screen return logic (LaunchedEffect)

### Search & Favorites Integration

**SearchScreen.kt:**
```kotlin
onAttractionClick = { attractionId ->
    mapViewModel.selectAttractionById(attractionId)
    onBackClick() // Return to map with bottom sheet open
}
```

**FavoritesScreen.kt:**
```kotlin
onAttractionClick = { attractionId ->
    mapViewModel.selectAttractionById(attractionId)
    onNavigateBack?.invoke() // Return to map with bottom sheet open
}
```

### MapViewModel Cleanup

**Removed State:**
```kotlin
// ❌ Navigation tracking states removed
_selectedFromDetailScreen
_returnToDetailAttractionId
_shouldReturnToDetail
_attractionIdToShowOnMap
```

**Removed Functions:**
```kotlin
// ❌ Detail screen functions removed
selectAttractionFromDetailScreen()
clearReturnToDetail()
setAttractionToShowOnMap()
clearAttractionToShowOnMap()
```

**Preserved Functions:**
```kotlin
// ✅ Core selection functions kept
selectAttraction(attraction)
selectAttractionById(id)
selectAttractionFromPanel(attraction, mapView)
clearSelection()
```

## User Flow Examples

### 1. Map Marker Click
```
User clicks marker → MapScreen calls viewModel.selectAttraction()
→ Bottom sheet opens with full details
→ User can view photos, reviews, info, etc.
→ Swipe down to close → Back to map
```

### 2. List Item Click
```
User clicks list item → MapScreen calls viewModel.selectAttractionById()
→ Bottom sheet opens with full details
→ User can interact with all content
→ Swipe down to close → Back to list
```

### 3. Search Result Click
```
User searches → Clicks result → SearchScreen calls mapViewModel.selectAttractionById()
→ Navigate back to MapScreen → Bottom sheet auto-opens
→ User sees attraction on map with details
```

### 4. Favorites Item Click
```
User opens favorites → Clicks item → FavoritesScreen calls mapViewModel.selectAttractionById()
→ Navigate back to MapScreen → Bottom sheet auto-opens + map centers
→ User sees attraction location with details
```

### 5. Photo Gallery Interaction
```
User in bottom sheet → Clicks photo thumbnail
→ PhotoViewer modal opens (fullscreen, swipeable)
→ User swipes through photos, pinch-zoom
→ Back/swipe down → Returns to bottom sheet (not navigation)
```

## Technical Notes

### Always Mounted Pattern

The bottom sheet is **always mounted** in MapScreen but visibility controlled by `selectedAttraction` state:

```kotlin
selectedAttraction?.let { attraction ->
    AttractionBottomSheet(
        attraction = attraction,
        onDismiss = { viewModel.clearSelection() },
        // ... other callbacks
    )
}
```

### State Persistence

MapViewModel maintains selected attraction across:
- ✅ Screen rotations
- ✅ Navigation (search/favorites → map)
- ✅ View mode changes (map ↔ list)
- ✅ Category filter changes

### Modal Hierarchy

1. **MapScreen** (base)
2. **AttractionBottomSheet** (modal over map)
3. **PhotoViewer** (modal over bottom sheet)

Each level can be dismissed independently without breaking navigation stack.

## Migration Checklist

- [x] Remove onNavigateToDetail from AttractionBottomSheet
- [x] Add PhotoViewer modal to bottom sheet
- [x] Remove "Подробнее" button
- [x] Remove AttractionDetail route from NavHost
- [x] Update MapScreenContainer signature
- [x] Update MapScreen signature and list handlers
- [x] Update SearchScreen to use mapViewModel
- [x] Update FavoritesScreen to use mapViewModel
- [x] Clean MapViewModel detail screen logic
- [x] Remove unused LaunchedEffects in MapScreen
- [x] Test all entry points (map, list, search, favorites)
- [x] Verify PhotoViewer modal works correctly
- [x] Verify back navigation flows

## Future Enhancements

- [ ] Add deep linking support (URL → auto-open bottom sheet)
- [ ] Add share functionality from bottom sheet
- [ ] Add "Get Directions" integration with Yandex Maps
- [ ] Add bottom sheet expansion states (collapsed/half/full)
- [ ] Consider adding peek height customization

## Related Files

- `AttractionBottomSheet.kt` - Main unified component
- `PhotoGallery.kt` - Contains PhotoViewer composable
- `ModernAttractionComponents.kt` - Reusable UI sections
- `MapViewModel.kt` - State management
- `AdygyesNavHost.kt` - App navigation
- `MapScreenContainer.kt`, `MapScreen.kt` - Map integration
- `SearchScreen.kt`, `FavoritesScreen.kt` - Other entry points

## Comparison with RN Version

Both platforms now have consistent architecture:

| Feature | React Native | Kotlin Android |
|---------|-------------|----------------|
| Component | UnifiedAttractionSheet | AttractionBottomSheet |
| Pattern | Always mounted | Always mounted (conditional) |
| Photo View | ImageViewer modal | PhotoViewer modal |
| Navigation | No detail screen | No detail screen |
| Entry Points | Map, List, Search, Favorites | Map, List, Search, Favorites |
| Dismissal | Swipe down | Swipe down |
| State Mgmt | Redux (attractionStore) | ViewModel (MapViewModel) |
