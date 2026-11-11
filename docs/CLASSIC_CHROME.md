# A6300 Classic Chrome tuning trial

Status: provisional recipe adjustment, not a calibrated Fujifilm transform.

## Evidence and target

The connected ILCE-6300 reports firmware 2.01. JPEGs _VTA5135 and _VTA5136
have matching exposure: 1/60 s, f/2.8, ISO 800, exposure compensation 0.
Both report automatic white balance. The camera moved slightly between frames.
The recipe image (_VTA5136, inferred from contrast/saturation metadata and the
shooting sequence) has a warm shift relative to the normal image. Sampled wall
RGB means are approximately (210,210,206) and (222,218,205). These are observations
of this scene, not measurements against a certified neutral target.

Fujifilm describes Classic Chrome as subdued, with suppressed magenta and cool
shadows: https://www.fujifilm-x.com/global/products/film-simulation/classic-chrome/

The user supplied a CIELAB a*b* ColorChecker comparison. Its X-T3 Classic Chrome
panel reports mean camera chroma 83.14% and gray mean delta-a 0.03, delta-b 0.54.
The chart's original source and test conditions have not been established.
83.14% is chroma relative to the chart reference, not accuracy, not relative to
Provia, and not a Sony saturation setting. The plot lacks L* and tone-response
data, so it cannot specify a full colour transform or a hardware profile.

Fuji X Weekly recipes are additional settings on top of Fuji's built-in
simulation, not settings that transfer numerically to Sony. This trial targets
the base Classic Chrome character, not a particular Fuji X Weekly recipe.

## Changes in this trial

| Setting | Previous built-in | Trial |
|---|---:|---:|
| WB amber-blue | A3 | 0 |
| WB green-magenta | G1 | 0 |
| Exposure compensation | -1/3 EV | 0 |
| PP Color Phase | +2 | 0 |

Creative Style saturation -1, contrast +2, PP saturation -8, black level -3,
six colour-depth values, detail and the existing PP mode byte are unchanged.
Changing fewer controls allows the next real-camera comparison to isolate tint
and exposure changes. Remaining values have not been fitted to ColorChecker data.

PP application caching now includes all generated PP writes and the target slot,
so an update with the same recipe name but different PP values can be applied.
The original APK and changed source files were copied to
`out/classic-chrome-reference/before-tuning/` before this edit.

## Outstanding hardware verification

Build verification: the trial APK was compiled using the existing unchanged native
library, signed with the same certificate as the previous APK, and verified for
minimum API 10. Artifact: `dist/AnhvtLab-ClassicChrome-trial.apk`.
SHA-256: `a7eb85b81ca7ea5a1bed50a2cd08e4c292d2a972a5b39530d41f2b73f36e02eb`.
PMCA installation to the connected camera completed successfully. This establishes
installation only; the new recipe has not yet been applied and photographed or
power-cycle verified in this session.

Tests: all 183 recipe/parameter tests passed. With a workspace-local temporary
directory the full suite reported 555 successful, 2 aborted and 8 failed tests;
all eight failures were Windows refusing symbolic-link creation in unrelated
transfer tests. Reports: `out/classic-chrome-reference/test/`.

- Sony metadata for both latest JPEGs decodes as `Gamma Still - Standard/Neutral
  (PP2)`. ExifTool maps raw value 0 to that label for Standard/Neutral Creative
  Style too; this does NOT establish that PP2 is enabled.
- The code calls cluster 12 `Gamma/Color Mode`, although Sony exposes Gamma and
  Color Mode separately. No new slot or enum mapping is assumed in this trial.
  Read both items in the native camera menu after applying the recipe to verify.
- JPEG EXIF Gamma 2.2 is not evidence of a selected PP gamma curve.
- Selecting a recipe stages its WB mode (Auto here); setting Kelvin in the native
  camera menu beforehand does not guarantee that it remains selected afterwards.

After installing, select Factory in the recipe browser, then Classic Chrome, to
discard previously remembered edits of that recipe. Apply it with the centre
button, exit, and power-cycle before checking the native PP menu and taking the
next comparison. Use matching manual exposure and set the same fixed WB in each
look after applying it. Compare neutral patches, skin, foliage, blue and red under
daylight and indoor light. A physical ColorChecker and measured Fuji targets are
needed for numerical patch matching; a display or a printout of this plot is not
a substitute for a characterised chart.
