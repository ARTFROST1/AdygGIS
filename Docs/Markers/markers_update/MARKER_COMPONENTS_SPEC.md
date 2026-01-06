# 🎨 Техническая спецификация новых компонентов маркеров

**Версия:** 1.0  
**Дата:** 2025-09-24  
**Статус:** Спецификация готова к реализации

---

## 📦 Компонент: CircularImageMarker

### Описание
Круглая кнопка-маркер с изображением достопримечательности, отображаемая поверх карты.

### API Спецификация

```kotlin
@Composable
fun CircularImageMarker(
    attraction: Attraction,
    screenPosition: Offset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = 52.dp,
    borderWidth: Dp = 2.dp,
    showLoadingIndicator: Boolean = true,
    animateAppearance: Boolean = true
)
```

### Параметры:
- `attraction` - данные о достопримечательности (включая URL изображения)
- `screenPosition` - позиция на экране в пикселях (Offset)
- `onClick` - callback при нажатии на маркер
- `isSelected` - выбран ли маркер (для визуального выделения)
- `size` - размер маркера (по умолчанию 52dp)
- `borderWidth` - толщина белой обводки
- `showLoadingIndicator` - показывать ли индикатор загрузки
- `animateAppearance` - анимировать ли появление маркера

### Визуальная структура:

```
┌─────────────────────────┐
│  ┌───────────────────┐  │  <- Shadow (elevation 4dp)
│  │                   │  │
│  │   AsyncImage      │  │  <- Круглое изображение
│  │   (from URL or    │  │     с clip(CircleShape)
│  │    fallback)      │  │
│  │                   │  │
│  └───────────────────┘  │  <- White border (2dp)
└─────────────────────────┘
         52dp
```

### Состояния:

1. **Loading State**
   - Показать CircularProgressIndicator
   - Или placeholder изображение категории

2. **Loaded State**
   - Отобразить загруженное изображение
   - Применить масштабирование если нужно

3. **Error State**
   - Показать иконку категории как fallback
   - Цвет фона зависит от категории

4. **Selected State**
   - Увеличить размер на 20% (анимированно)
   - Добавить дополнительную тень
   - Возможно, добавить пульсацию

### Анимации:

```kotlin
// Появление маркера
val scale = animateFloatAsState(
    targetValue = if (animateAppearance) 1f else 0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

// Нажатие на маркер
val clickScale = animateFloatAsState(
    targetValue = if (isPressed) 0.9f else 1f,
    animationSpec = tween(100)
)

// Выбранное состояние
val selectionScale = animateFloatAsState(
    targetValue = if (isSelected) 1.2f else 1f,
    animationSpec = spring()
)
```

---

## 📦 Компонент: MarkerOverlay

### Описание
Контейнер для отображения всех маркеров поверх карты с управлением их позициями.

### API Спецификация

```kotlin
@Composable
fun MarkerOverlay(
    mapView: MapView,
    attractions: List<Attraction>,
    selectedAttraction: Attraction?,
    onMarkerClick: (Attraction) -> Unit,
    modifier: Modifier = Modifier,
    enableClustering: Boolean = false,
    clusteringThreshold: Int = 5,
    animationDuration: Int = 300
)
```

### Функционал:

1. **Позиционирование маркеров**
```kotlin
Box(modifier = modifier.fillMaxSize()) {
    attractions.forEach { attraction ->
        val screenPos = remember(mapCameraPosition) {
            MapCoordinateConverter.geoToScreen(
                mapView,
                attraction.location.latitude,
                attraction.location.longitude
            )
        }
        
        screenPos?.let { position ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            position.x.toInt() - markerSize / 2,
                            position.y.toInt() - markerSize
                        )
                    }
            ) {
                CircularImageMarker(
                    attraction = attraction,
                    screenPosition = position,
                    onClick = { onMarkerClick(attraction) },
                    isSelected = attraction.id == selectedAttraction?.id
                )
            }
        }
    }
}
```

2. **Оптимизация видимости**
```kotlin
// Показывать только видимые маркеры
val visibleAttractions = remember(mapBounds, attractions) {
    attractions.filter { attraction ->
        mapView.map.visibleRegion.contains(
            Point(
                attraction.location.latitude,
                attraction.location.longitude
            )
        )
    }
}
```

3. **Обработка перекрытий (z-index)**
```kotlin
// Сортировка маркеров по широте (северные выше)
val sortedAttractions = visibleAttractions.sortedByDescending { 
    it.location.latitude 
}
```

---

## 📦 Утилита: MapCoordinateConverter

### Описание
Конвертер географических координат в экранные координаты.

### API Спецификация

```kotlin
object MapCoordinateConverter {
    
    /**
     * Преобразует географические координаты в экранные
     * @return Offset если точка видима, null если за пределами экрана
     */
    fun geoToScreen(
        mapView: MapView,
        latitude: Double,
        longitude: Double
    ): Offset? {
        val geoPoint = Point(latitude, longitude)
        val screenPoint = mapView.mapWindow.worldToScreen(geoPoint)
        
        return screenPoint?.let {
            Offset(it.x, it.y)
        }
    }
    
    /**
     * Преобразует экранные координаты в географические
     */
    fun screenToGeo(
        mapView: MapView,
        screenX: Float,
        screenY: Float
    ): Point? {
        val screenPoint = ScreenPoint(screenX, screenY)
        return mapView.mapWindow.screenToWorld(screenPoint)
    }
    
    /**
     * Проверяет, видима ли точка на экране
     */
    fun isPointVisible(
        mapView: MapView,
        latitude: Double,
        longitude: Double
    ): Boolean {
        val point = Point(latitude, longitude)
        return mapView.map.visibleRegion.contains(point)
    }
    
    /**
     * Вычисляет расстояние между точками в пикселях
     */
    fun pixelDistance(
        mapView: MapView,
        point1: Point,
        point2: Point
    ): Float {
        val screen1 = geoToScreen(mapView, point1.latitude, point1.longitude)
        val screen2 = geoToScreen(mapView, point2.latitude, point2.longitude)
        
        return if (screen1 != null && screen2 != null) {
            val dx = screen2.x - screen1.x
            val dy = screen2.y - screen1.y
            sqrt(dx * dx + dy * dy)
        } else {
            Float.MAX_VALUE
        }
    }
}
```

---

## 📦 Data Class: MarkerState

### Описание
Состояние маркера для управления в ViewModel.

```kotlin
data class MarkerState(
    val attraction: Attraction,
    val screenPosition: Offset?,
    val isVisible: Boolean,
    val isSelected: Boolean,
    val isLoading: Boolean,
    val imageUrl: String?,
    val clusterGroup: Int? = null
)

data class MarkerOverlayState(
    val markers: List<MarkerState> = emptyList(),
    val clusters: List<MarkerCluster> = emptyList(),
    val selectedMarkerId: String? = null,
    val isUpdating: Boolean = false
)

data class MarkerCluster(
    val id: String,
    val centerPosition: Offset,
    val markerIds: List<String>,
    val displayCount: Int
)
```

---

## 🎨 Дизайн-система для маркеров

### Цветовая схема по категориям:

```kotlin
object MarkerColors {
    fun getColorForCategory(category: AttractionCategory, isDark: Boolean): Color {
        return when (category) {
            AttractionCategory.NATURE -> if (isDark) Color(0xFF66BB6A) else Color(0xFF4CAF50)
            AttractionCategory.CULTURE -> if (isDark) Color(0xFFAB47BC) else Color(0xFF9C27B0)
            AttractionCategory.HISTORY -> if (isDark) Color(0xFF8D6E63) else Color(0xFF795548)
            AttractionCategory.ADVENTURE -> if (isDark) Color(0xFFFF7043) else Color(0xFFFF5722)
            AttractionCategory.RECREATION -> if (isDark) Color(0xFF29B6F6) else Color(0xFF03A9F4)
            AttractionCategory.GASTRONOMY -> if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
            AttractionCategory.RELIGIOUS -> if (isDark) Color(0xFF90A4AE) else Color(0xFF607D8B)
            AttractionCategory.ENTERTAINMENT -> if (isDark) Color(0xFFEC407A) else Color(0xFFE91E63)
        }
    }
}
```

### Размеры:

```kotlin
object MarkerDimensions {
    val DefaultSize = 52.dp
    val SmallSize = 44.dp
    val LargeSize = 60.dp
    val BorderWidth = 2.dp
    val SelectedBorderWidth = 3.dp
    val ShadowElevation = 4.dp
    val SelectedShadowElevation = 8.dp
}
```

---

## 🔄 Интеграция с MapScreen

### Изменения в MapScreen:

```kotlin
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val mapView = remember { mutableStateOf<MapView?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Базовая карта
        AndroidView(
            factory = { context ->
                MapView(context).apply {
                    mapView.value = this
                    // ... инициализация карты
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Слой с маркерами
        mapView.value?.let { map ->
            MarkerOverlay(
                mapView = map,
                attractions = uiState.filteredAttractions,
                selectedAttraction = uiState.selectedAttraction,
                onMarkerClick = { attraction ->
                    viewModel.selectAttraction(attraction)
                }
            )
        }
        
        // Остальные UI элементы (поиск, навигация и т.д.)
        // ...
    }
}
```

### Обработка событий карты:

```kotlin
// В MapScreen, внутри AndroidView factory:
mapView.map.addCameraListener { _, _, _, _ ->
    // Обновить позиции маркеров при движении камеры
    viewModel.updateMarkerPositions()
}
```

---

## 🔄 Изменения в MapViewModel

### Новые методы:

```kotlin
class MapViewModel @Inject constructor(
    // ... dependencies
) : ViewModel() {
    
    private val _markerOverlayState = MutableStateFlow(MarkerOverlayState())
    val markerOverlayState: StateFlow<MarkerOverlayState> = _markerOverlayState.asStateFlow()
    
    fun updateMarkerPositions() {
        viewModelScope.launch {
            val updatedMarkers = _uiState.value.filteredAttractions.map { attraction ->
                MarkerState(
                    attraction = attraction,
                    screenPosition = calculateScreenPosition(attraction),
                    isVisible = isAttractionVisible(attraction),
                    isSelected = attraction.id == _uiState.value.selectedAttraction?.id,
                    isLoading = false,
                    imageUrl = attraction.imageUrl
                )
            }
            
            _markerOverlayState.update { 
                it.copy(markers = updatedMarkers)
            }
        }
    }
    
    private fun calculateScreenPosition(attraction: Attraction): Offset? {
        // Использовать MapCoordinateConverter
        return null // placeholder
    }
    
    private fun isAttractionVisible(attraction: Attraction): Boolean {
        // Проверить видимость
        return true // placeholder
    }
}
```

---

## 🧪 Тестирование компонентов

### Unit тесты:

```kotlin
@Test
fun `CircularImageMarker displays loading state`() {
    composeTestRule.setContent {
        CircularImageMarker(
            attraction = testAttraction,
            screenPosition = Offset(100f, 100f),
            onClick = {},
            showLoadingIndicator = true
        )
    }
    
    composeTestRule
        .onNodeWithTag("marker_loading_${testAttraction.id}")
        .assertIsDisplayed()
}

@Test
fun `MarkerOverlay positions markers correctly`() {
    // Test marker positioning logic
}

@Test
fun `MapCoordinateConverter converts coordinates correctly`() {
    // Test coordinate conversion
}
```

### UI тесты:

```kotlin
@Test
fun `Clicking on marker opens bottom sheet`() {
    composeTestRule.setContent {
        MapScreen(navController = testNavController)
    }
    
    // Подождать загрузку
    composeTestRule.waitUntil { 
        composeTestRule
            .onAllNodesWithTag("circular_marker")
            .fetchSemanticsNodes().isNotEmpty()
    }
    
    // Кликнуть по маркеру
    composeTestRule
        .onNodeWithTag("circular_marker_${testAttraction.id}")
        .performClick()
    
    // Проверить, что bottom sheet открылся
    composeTestRule
        .onNodeWithTag("attraction_bottom_sheet")
        .assertIsDisplayed()
}
```

---

## 📈 Метрики производительности

### Целевые показатели:
- **Frame rate:** 60 FPS при движении карты
- **Touch response:** < 100ms
- **Image loading:** < 2 секунды
- **Memory usage:** < 50MB для маркеров
- **CPU usage:** < 20% при idle

### Профилирование:

```kotlin
// Использовать Compose Compiler Metrics
// ./gradlew assembleRelease -PcomposeCompilerReports=true

// Отслеживать recompositions
@Composable
fun CircularImageMarker(
    // ...
) {
    val recompositionCount = remember { mutableStateOf(0) }
    
    SideEffect {
        recompositionCount.value++
        Timber.d("CircularImageMarker recomposed: ${recompositionCount.value} times")
    }
    // ...
}
```

---

*Документ готов для начала реализации компонентов*
