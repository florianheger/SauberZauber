# Feature: Notifications

A user can turn on push notifications per device (phone menu behind the avatar, desktop sidebar).

The other user gets a notification when a task is marked done:
- for themself: "Sam hat Bad putzen erledigt"
- for the other user: "Sam hat Bad putzen für dich eingetragen"

The user who pressed the button is never notified. Undoing a completion and correcting counts in the edit view do not notify. Tapping a notification opens the app.

It must work in the installed PWA on iOS (added to the home screen from Safari; iOS 16.4+) and on Android (installed from Firefox). Other browsers should work too. In Safari on iOS without installing, the app explains that it has to be added to the home screen first. Logging out stops notifications on that device.

Push is optional: without the VAPID keys (`APP_VAPID_*`) the switch is hidden.
