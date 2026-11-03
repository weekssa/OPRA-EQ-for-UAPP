# JA11 B2 sibling ground-truth capture proposal

Status: **PLANNING AND EQUIPMENT SELECTION ONLY — NOT EXERCISED.** No device was
connected, captured, configured or purchased. This proposal establishes the
decision needed to observe an official FiiO app selecting a reconstruction
filter on a sibling device.

## Purpose

Source inspection cannot establish which value the official app sends, whether
it follows selection with Apply or Save, or whether a UI label changes the actual
reconstruction response. A passive bus trace of the official app can establish
the observed command sequence on one exact sibling. It cannot establish JA11
command equivalence or JA11 capability.

## Acquisition decision

Equipment acquisition is a planned step and requires a separate owner decision.
Before that decision, confirm current stock, current seller quotation, taxes and
shipping, host compatibility, exact analyzer topology, and the sibling's exact
official-app/FiiO Control filter control on its intended firmware. An already
available or borrowable compatible device or analyzer should be used first.

Observed advertised candidates:

| Role | Candidate | Advertised price when checked | Evidence and caveat |
|---|---|---:|---|
| Filter-capable sibling | FiiO KA15 | US$99.99; EU €99.99 | US seller was out of stock when checked; EU authorized distributor was in stock. FiiO documents five named digital filters and FiiO Control support. Recheck current availability and exact app/firmware eligibility before acquisition approval. |
| Inline analyzer | Great Scott Gadgets Cynthion | US$180 board; US$269 enclosed | Crowd Supply listed both in stock when checked. Manufacturer documents passive low/full/high-speed USB 2.0 capture. Use analyzer mode only. |
| Alternative analyzer | Total Phase Beagle USB 480, TP320510 | US$1,295 | Manufacturer listed limited availability when checked and documents non-intrusive low/full/high-speed USB 2.0 capture. |

Source links:

- <https://audio46.com/products/fiio-ka15-portable-dac-amp>
- <https://fiio.eu/product/fiio-ka15-dac-and-headphone-amplifier>
- <https://fiiosupport.freshdesk.com/support/solutions/articles/69000869739-ka15-menu-introduction-ka15-filter-introduction>
- <https://www.fiio.com/KA15>
- <https://www.crowdsupply.com/great-scott-gadgets/cynthion>
- <https://www.greatscottgadgets.com/cynthion/>
- <https://www.totalphase.com/products/beagle-usb480/>

No candidate is selected or authorized for purchase by this document.

## Separate hardware-session approval

After acquisition and setup, record and obtain approval for one exact session
covering:

- Sibling model and USB identity; firmware version if available.
- Official FiiO app or FiiO Control web-control version and provenance.
- Capture host, analyzer, cables/adapters and passive topology.
- Original filter value, other applicable baseline settings, and restoration target.
- Exact owner actions and expected behavior if the app re-enumerates the device.

The analyzer is passive. Selecting a different filter through the official app
and restoring it are sibling **setting writes**; explicit session approval must
cover both. There are no JA11 writes, filter writes, firmware actions or
discretionary Apply/Save controls in this proposal.

## Capture procedure

1. Start passive capture before opening the relevant official-app control screen.
2. Record the original named filter and other applicable baseline state.
3. Select one different named filter through the official app and capture all
   requests, replies and any automatically following Apply or Save traffic.
4. Return to the original named filter through the same app and capture that
   restoration sequence.
5. Do not press a separate Apply or Save control unless the approved session
   explicitly includes it.
6. Preserve the raw capture with SHA-256, timestamps, OS/app/version, USB
   descriptors, endpoints, transfer lengths and a timeline of owner actions.

Stop on ambiguous identity, broken capture, unexpected reset/state change, or
failed restoration. Do not automatically repeat the session.

## Acceptance and limits

The capture demonstrates the command/value and observed Apply/Save sequence used
by the official app for that sibling. Its absence of Apply/Save is bounded to the
observed action and capture window. Actual roll-off, phase and non-oversampling
behavior still requires suitable measurement. Restore through the official app
and available readback; persistent-state restoration is a separate claim.

All findings remain **sibling-specific**. They do not establish JA11 capability,
command equivalence, value mappings or persistence.
