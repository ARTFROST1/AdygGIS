# API Setup Instructions

## Yandex MapKit API Key Setup

To run the Adygyes app, you need to configure the Yandex MapKit API key.

### Step 1: Get Yandex MapKit API Key

1. Go to [Yandex Developer Console](https://developer.tech.yandex.ru/)
2. Create a new project or select an existing one
3. Enable the "MapKit Mobile SDK" service
4. Generate an API key for MapKit

### Step 2: Configure the API Key

1. Create a `local.properties` file in the root directory of the project (if it doesn't exist)
2. Add the following line to `local.properties`:

```properties
YANDEX_MAPKIT_API_KEY=your_actual_api_key_here
```

Replace `your_actual_api_key_here` with your actual Yandex MapKit API key.

### Step 3: Build and Run

After adding the API key to `local.properties`, you can build and run the app:

```bash
./gradlew assembleDebug
```

## Important Notes

- The `local.properties` file is gitignored for security reasons
- Never commit API keys to version control
- Each developer needs to set up their own `local.properties` file
- The app will show a warning in logs if the API key is not configured

## Troubleshooting

If you see the error "You need to set the API key before using MapKit!", it means:

1. The `local.properties` file doesn't exist
2. The API key is not set in `local.properties`
3. The API key is invalid or expired

Make sure to follow the setup steps above to resolve this issue.

## Development Mode

For development purposes, you can temporarily use a demo API key by uncommenting and modifying this line in `AdygyesApplication.kt`:

```kotlin
// MapKitFactory.setApiKey("your-demo-key-here")
```

However, this is not recommended for production builds.
