# Picture Profile storage and stability changes

The A6300 fork's PP payload contains the selected slot, the existing mode enum,
black level, saturation, phase, R/G/B/C/M/Y depth and detail. This change preserves
that existing payload; it does not establish a new Gamma/Color Mode hardware map.

## Files and sharing

- Existing non-PP recipes still write YAML format 2 and QR version 1, unchanged.
- PP recipes write YAML format 3, family `sony-a6300-picture-profile`, with all
  twelve `pp-*` keys from `PictureProfiles.KEYS`. Missing or invalid values reject
  the whole file. Versions 1 and 2 remain readable.
- PP QR codes use version byte 2, family byte 0. Bytes 2..12 retain their old
  meanings. Bytes 13..24 are the twelve PP values in `PictureProfiles.KEYS` order;
  signed offsets are two's-complement bytes. The ASCII name starts at byte 25.
  Version-1 links are unchanged. Older camera/phone apps must support this new
  version to consume PP links; they must not interpret one as a legacy look.
- New recipe reads the active PP payload from the existing hardware slots.
  Rename, files and QR retain it. If a slot cannot be read or validated, saving
  fails rather than storing an incomplete look. Previously saved files that
  already lost their PP data cannot be reconstructed automatically.
- New recipe and Read Recipe QR Code are available under hold MENU. New recipe
  captures the applied camera settings; apply any chip edits first.

## Memory and diagnostics

The process has one crash handler with a static sink, with no Activity reference.
Logging failure still delegates to Android's original handler. Routine logs use
one background worker, at most 32 queued events, message length limits and idle
worker timeout. Fatal records use a separate bounded `RECIPLAB/LOGS/CRASH.TXT`;
Save a bug report includes that record. A native process crash or power failure
cannot be guaranteed to reach a Java exception handler.

Profile application keys are cached by immutable recipe and PP slot instead of
formatting native write lists on each redraw. English and Vietnamese share the
same loaded font instance. Recipe-card export closes its native sink on error.

The always-hidden action bar and sponsor view, their unreachable MainActivity
actions and an unused translated message were removed. The streamlined direct
centre-button apply behavior remains. About is accessible for installed-version,
camera and source/attribution information. Recipe colour values are unchanged by
these stability fixes.

## Validation boundaries

Host regression tests cover PP round trips for every PP recipe, signed limits,
all nine slots, malformed/incomplete payloads, rename/capture/reapply, legacy
formats, cache reuse, handler delegation and fatal/report/background logging.
Android views and lifecycle integration must still be exercised on the camera.
The existing A6300 PP memory map, full PP live-preview matching, and long-running
on-camera stability are not established by a green host build.

Deployment on 2026-11-12: `dist/AnhvtLab-Stability.apk` was compiled, aligned,
signed with the existing certificate and verified for API 10, then installed on
the connected ILCE-6300 successfully. `AnhvtLab.apk` and `dist/AnhvtLab.apk` also
contain that build. SHA-256:
`02ceb01bef4029f61947e4ea525440a8946f69358784d696148c915ca41d45e8`.
Size is 766172 bytes, versus 764426 for the preceding colour trial; the small
increase adds PP storage and diagnostics while reducing redraw allocations.

Final host run: 574 tests started, 564 successful, 2 aborted and 8 failed solely
because Windows refused symbolic-link creation in existing transfer tests.
Reports: `out/stability-fix-test/verified-reports/`. A separate before/after
probe now preserves PP through file, QR and custom capture for both Classic
Chrome and Canon Portrait. Bytecode inspection confirms the static crash sink
has no synthetic Activity reference. Previous sources and APK are retained under
`out/before-stability/`.

Installation success is not an on-camera stress-test result. After unplugging
USB and power-cycling, exercise repeated app opens, rapid recipe scrolling,
New recipe, QR sharing/reading and Save a bug report before assessing stability.
