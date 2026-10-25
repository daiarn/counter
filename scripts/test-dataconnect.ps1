# Run after: scripts/firebase.ps1 emulators:start --only dataconnect --project demo-counter
# This test only uses the loopback emulator and synthetic identities/data.
$ErrorActionPreference = 'Stop'
$connectorUrl = 'http://127.0.0.1:9399/v1/projects/demo-counter/locations/europe-central2/services/counter-f0787-service/connectors/counter'
$visitId = [guid]::NewGuid().ToString()

function Invoke-Operation($operation, $variables, $owner = 'test-owner-a', $mutation = $false) {
    $impersonate = if ($null -eq $owner) { @{unauthenticated=$true} } else {
        @{authClaims=@{sub=$owner; firebase=@{sign_in_provider='anonymous'}}}
    }
    $body = @{operationName=$operation; variables=$variables; extensions=@{impersonate=$impersonate}} |
        ConvertTo-Json -Depth 8
    $method = if ($mutation) { 'impersonateMutation' } else { 'impersonateQuery' }
    Invoke-RestMethod -Method Post -Uri "${connectorUrl}:$method" -ContentType 'application/json' -Body $body
}

function Assert-Condition($condition, $message) {
    if (-not $condition) { throw $message }
}

$variables = @{id=$visitId; type='BOTH'; occurredAt='2026-01-01T10:00:00Z'; note='test'; deleted=$false; revision='1'}
$created = Invoke-Operation 'CreateVisit' $variables 'test-owner-a' $true
Assert-Condition ($created.errors.Count -eq 0) 'Anonymous owner could not create visit'
$own = Invoke-Operation 'GetVisit' @{id=$visitId}
Assert-Condition ($own.data.visit.revision -eq '1') 'Owner could not read visit'
$other = Invoke-Operation 'GetVisit' @{id=$visitId} 'test-owner-b'
Assert-Condition ($null -eq $other.data.visit) 'Another owner could read visit'

$variables.revision = '2'
$foreignUpdate = Invoke-Operation 'UpdateVisit' $variables 'test-owner-b' $true
Assert-Condition ($foreignUpdate.data.visit_updateMany -eq 0) 'Another owner could update visit'
$variables.deleted = $true
$variables.note = ''
$deleted = Invoke-Operation 'UpdateVisit' $variables 'test-owner-a' $true
Assert-Condition ($deleted.data.visit_updateMany -eq 1) 'Deletion marker was not saved'
$variables.revision = '1'
$variables.deleted = $false
$stale = Invoke-Operation 'UpdateVisit' $variables 'test-owner-a' $true
Assert-Condition ($stale.data.visit_updateMany -eq 0) 'A stale upload overwrote deletion'
$variables.revision = '2'
$duplicate = Invoke-Operation 'UpdateVisit' $variables 'test-owner-a' $true
Assert-Condition ($duplicate.data.visit_updateMany -eq 0) 'A duplicate revision changed the row'
$final = Invoke-Operation 'GetVisit' @{id=$visitId}
Assert-Condition ($final.data.visit.revision -eq '2') 'Newer revision was lost'

# Auth failures may be returned as GraphQL errors or an HTTP error status.
$denied = $false
try {
    $noAuth = Invoke-Operation 'GetVisit' @{id=$visitId} $null
    $denied = $noAuth.errors.Count -gt 0
} catch [Microsoft.PowerShell.Commands.HttpResponseException] {
    $denied = [int]$_.Exception.Response.StatusCode -in 401,403
}
Assert-Condition $denied 'Unauthenticated read was allowed'
Write-Output 'PASS: anonymous access, owner isolation, deletion, stale retry, duplicate retry, and unauthenticated denial'
