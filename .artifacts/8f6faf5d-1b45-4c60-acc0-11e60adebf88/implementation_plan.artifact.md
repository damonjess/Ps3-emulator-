# Fix "Unable to Download" and Automate Installation

The user reported that the download notification shows "unable to download". This is typically a failure within the `DownloadManager` service. I will move the download destination to the app's private external storage to avoid permission conflicts and ensure the post-download installation works reliably.

## User Review Required

> [!IMPORTANT]
> I am moving the temporary download location to the app's internal "Downloads" folder (`Android/data/com.retrorts/files/Downloads`). This ensures the app has guaranteed write access without needing broad public storage permissions for the initial download. Once downloaded, it will still be moved/unzipped to your game library.

## Proposed Changes

### [RetroRTS app](file:///C:/Users/Damon/StudioProjects/Ps3-emulator-/RetroRTS/app)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Damon/StudioProjects/Ps3-emulator-/RetroRTS/app/src/main/AndroidManifest.xml)
- Add `android:usesCleartextTraffic="true"` to the `<application>` tag to prevent issues with non-HTTPS redirects.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Damon/StudioProjects/Ps3-emulator-/RetroRTS/app/src/main/java/com/retrorts/MainActivity.kt)
- **Change Download Destination**: Use `setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)` instead of the public directory.
- **Enhanced Error Logging**: Query the `DownloadManager` for the failure reason (e.g., `COLUMN_REASON`) if the status is `STATUS_FAILED` and show it to the user via Toast.
- **Ensure Installation Flow**: Verify that the unzip/move logic correctly handles the new private file location.

## Verification Plan

### Manual Verification
1.  Open the app and go to the Download tab.
2.  Attempt to download "Homebrew: 240p Test Suite".
3.  Observe the notification bar to ensure the download completes.
4.  Verify that the "Installed to Library!" toast appears and the game is visible in the Library tab.
