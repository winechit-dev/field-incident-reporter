# 004 ios-verification

**Goal:** check the iOS app's photo, location and sync pieces by driving the simulator, since unit tests cannot reach the system pickers and prompts.
**Lanes:** mobile (iOS)

## Evidence
Run on 2026-11-15 on an iPhone 17 Pro simulator (iOS 26.2) against the seeded API. The UI was driven with idb, which reads the app's accessibility tree and taps by label.

| Flow | Result |
| --- | --- |
| Sign-in, list | Pass. The Technicians and incidents come from the real API. |
| Location | Pass. The permission prompt shows the Info.plist explanation; after allowing it the simulator's fix `16.8409, 96.1735` fills the form and the typed address is kept. |
| Gallery picker | Pass. PHPicker opens, the photo attaches, and the thumbnail loads from the local file. |
| Compression | Pass. A 2400x1800, 223 KB photo became a 1600x1200, 94 KB JPEG in Application Support. After upload the local copy is removed. |
| Report to the API | Pass. The server held the title, Safety, High, the coordinates, the address and one photo; the list showed it synced. |
| Offline retry | Pass. With the API down the report showed *Waiting to sync*. The API returned 12 s after the submit and the report arrived 17.9 s later, about 30 s after the submit, which is the third attempt of the 10 s, doubling schedule. The badge cleared. |
| Pull-to-refresh | Pass. An Operator's resolution changed the row from *Open* to *Resolved*. |
| Edit | Pass. The title and severity reached the server with the photo intact. |
| Delete | Pass. Undo restored the incident and nothing was deleted; without Undo it left the list at once and the server removed it after the window. |
| Camera | **Not verified.** The system camera opens and cancelling returns to the form unharmed, but the simulator's shutter produces no photo, so a real capture is untested. |
| Background tasks | **Inconclusive.** Registration does not crash the app and a launch was simulated with the debugger, but the report arrived at 12.2 s, after the foreground retry's 10 s attempt, so the background handler cannot be told apart from it. iOS does not run these tasks by itself in the simulator, so a real device is needed. |

## A bug this found
A form's view model outlived the form, on both platforms. After Cancel, reopening showed the old text. After a submit, reopening found the state already "done" and closed the form at once, so "Report incident" appeared dead until the app restarted. Fixed by giving every visit to a form its own view model (`Navigator`, with `NavigatorTest`, run on Android and the iOS simulator). On iOS, Cancel then reopen now shows an empty title and the form reopens after a submit. Not rechecked by hand on Android.

## Open questions
- Re-run the background-task check with the debugger attached before the report is submitted, or on a real device.
