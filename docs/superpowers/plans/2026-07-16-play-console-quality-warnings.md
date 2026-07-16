# Google Play Quality Warnings Implementation Plan

> **For Codex:** Execute this plan task-by-task and verify the generated release AAB before claiming completion.

**Goal:** Restore R8 optimization in the Android release artifact, preserve supported edge-to-edge behavior, and prevent future Google Play uploads when release optimization silently becomes disabled.

**Root cause:** The uploaded 1.3.0 AAB was shrunk and obfuscated but not optimized. RevenueCat KMP 2.10.2 pulled the Amazon store SDK, whose consumer rules include `-dontoptimize`. RevenueCat KMP 3.x makes Amazon opt-in and removes the obsolete iOS `PurchasesHybridCommon` bridge.

**Edge-to-edge decision:** Keep `androidx.activity.enableEdgeToEdge()` and the existing inset-aware Compose layouts. The deprecated status/navigation bar calls reported by Play originate inside AndroidX's API 35 implementation, not app code. Reimplementing edge-to-edge with direct window color setters would retain the deprecated calls and transfer ownership of them to the app.

---

### Task 1: Add release-artifact regression tests

**Files:**
- Create: `scripts/tests/verify-android-release-quality-test.sh`
- Create: `scripts/verify-android-release-quality.sh`

- [ ] Write fixture-based tests that reject disabled R8 optimization.
- [ ] Write tests that reject missing R8 metadata or mapping output.
- [ ] Write a passing fixture with shrinking, obfuscation, and optimization enabled.
- [ ] Run the test before implementation and confirm it fails.
- [ ] Implement the AAB metadata verifier.
- [ ] Run the test and confirm it passes.

### Task 2: Remove the dependency that disables R8 optimization

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `iosApp/project.yml`
- Modify: `iosApp/CleanContactsAI.xcodeproj/project.pbxproj`
- Modify: `iosApp/CleanContactsAI.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`

- [ ] Pin RevenueCat KMP to `3.3.0`.
- [ ] Remove the obsolete `PurchasesHybridCommon` Swift package from the XcodeGen spec.
- [ ] Regenerate the Xcode project and confirm the bridge is absent.
- [ ] Remove stale Swift package resolution pins.
- [ ] Confirm the Android release dependency graph no longer contains `purchases-store-amazon`, `purchases-hybrid-common`, or `-dontoptimize`-providing RevenueCat artifacts.

### Task 3: Gate Google Play uploads on the generated AAB

**Files:**
- Modify: `.github/workflows/deploy-android.yml`

- [ ] Run the release-quality verifier immediately after `bundleRelease`.
- [ ] Keep artifact upload and Google Play upload after the verification gate.
- [ ] Ensure script and test paths trigger the Android deployment workflow when changed.

### Task 4: Verify edge-to-edge ownership and release behavior

**Files:**
- Verify: `androidApp/src/main/java/com/ogabassey/contactscleaner/MainActivity.kt`
- Verify: Android UI source under `composeApp/src`

- [ ] Confirm `enableEdgeToEdge()` remains called before `super.onCreate`.
- [ ] Confirm app source does not directly call deprecated status/navigation bar color APIs.
- [ ] Confirm routed screens continue to consume system-bar/safe-drawing insets.

### Task 5: Build and inspect the release

- [ ] Run focused script tests.
- [ ] Compile Android and iOS KMP targets.
- [ ] Build a release AAB with local test signing if necessary.
- [ ] Run the verifier against the actual AAB.
- [ ] Inspect `r8.json` and confirm optimization, shrinking, and obfuscation are all enabled.
- [ ] Confirm the AAB contains `proguard.map`.

### Task 6: Publish for review

- [ ] Review the diff for generated-project churn and unrelated changes.
- [ ] Commit the scoped changes.
- [ ] Push `codex/fix-play-quality-warnings`.
- [ ] Open a pull request with root-cause evidence and Play Console expectations.
- [ ] Wait for checks and address actionable review feedback.
