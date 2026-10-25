param([string]$Serial = 'emulator-5554')
$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Tests are restricted to emulator serials. Never run them on a phone containing real visits.' }
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$devices = & $adb devices
if (-not ($devices -match "^$([regex]::Escape($Serial))\s+device$")) { throw "Emulator $Serial is not online." }
$previousSerial = $env:ANDROID_SERIAL
try {
    $env:ANDROID_SERIAL = $Serial
    foreach ($testClass in @('com.daiarn.counter.ui.CounterUiTest', 'com.daiarn.counter.data.VisitDatabaseTest')) {
        & (Join-Path $PSScriptRoot '../gradlew.bat') connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$testClass"
        if ($LASTEXITCODE -ne 0) { throw "Emulator regression tests failed: $testClass" }
    }
} finally { $env:ANDROID_SERIAL = $previousSerial }
