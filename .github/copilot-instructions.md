<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# SafecallKids Android App - Copilot Instructions

This is an Android application project that blocks phone calls from unknown numbers (numbers not in the user's contacts list). The app is designed specifically for child protection.

## Project Context
- **Language**: Kotlin
- **Platform**: Android (minSdk 24, targetSdk 34)
- **Architecture**: Standard Android architecture with Services and BroadcastReceivers
- **Purpose**: Block unwanted calls for children's safety

## Key Components
- `MainActivity.kt`: Main UI for permissions and status
- `CallBlockingService.kt`: Background service that runs protection
- `CallReceiver.kt`: BroadcastReceiver that intercepts incoming calls
- `ContactsHelper.kt`: Helper class to manage contacts and phone number matching

## Important Considerations
- Follow Android permission best practices
- Handle phone number normalization for different country formats
- Consider Android 10+ restrictions on call blocking
- Maintain compatibility with different Android versions
- Focus on child safety and user-friendly interface

## Code Style
- Use Kotlin idioms and best practices
- Follow Android Architecture Guidelines
- Include proper error handling and logging
- Add comments for complex phone number matching logic
- Ensure proper resource management (cursors, etc.)

## Testing Notes
- Phone call functionality requires physical device testing
- Permissions need to be tested on different Android versions
- Consider edge cases for phone number formats
