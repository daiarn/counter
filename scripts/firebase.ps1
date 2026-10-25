# Invoke the downloaded official CLI without its Windows welcome-shell launcher.
# The executable is a local build tool, not an app asset.
$env:FIREPIT_VERSION = '1.1.0'
$counterCli = Join-Path $PSScriptRoot '..\app\build\tools\firebase.exe'
if (-not (Test-Path -LiteralPath $counterCli)) {
    $counterNodeCli = Join-Path $env:USERPROFILE '.cache/firebase/tools/lib/node_modules/firebase-tools/lib/bin/firebase.js'
    if (Test-Path -LiteralPath $counterNodeCli) {
        & node $counterNodeCli @args
        exit $LASTEXITCODE
    }
    throw 'Firebase CLI is missing. Install firebase-tools or see README.md for the official installation options.'
}
& $counterCli @args
exit $LASTEXITCODE
