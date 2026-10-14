# EQ Library 0.8.3 Beta v1 owner guide

This guide will be completed against the frozen installed candidate before requesting phone readiness. It will include the exact beta identity, automated gate summary, wireless-install status, bounded test sequence, the physical JA11 power-removal step, stop conditions, and how to inspect the final result in the app.

Expected product behavior:

- Cancel or dismiss staged edits without a write.
- Apply a changed volume/program for the current session only.
- Set as Default opens a confirmation with both volume and program; Cancel returns to the editor without writing.
- Save defaults performs the reviewed operation and reports success only after the requested pair and original microphone state are verified on fresh sessions.
- Stop on the first failed or uncertain transaction. Keep the exact beta installed with its data on either outcome.

Final tested volume/program are intentionally retained and reported. Microphone/UAC restoration is not waived by retaining those settings.
