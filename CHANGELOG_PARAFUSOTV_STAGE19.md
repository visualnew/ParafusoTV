# ParafusoTV — Stage 19

## Mobile/TV final adaptation

- Phone browsing keeps the user's preferred orientation.
- Fullscreen playback automatically enters landscape on phones.
- The previous phone orientation is restored when playback closes.
- Android TV / Fire TV are never forced to rotate.
- Orientation changes continue to use the existing `configChanges` path and preserve the TV rail/navigation state.
- Added a dedicated view tag resource for safely retaining the previous requested orientation.

Build remains to be validated in an environment with the Gradle distribution and Android dependencies available.
