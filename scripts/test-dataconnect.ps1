# Run after: scripts/firebase.ps1 emulators:start --only dataconnect --project demo-counter
# This test only uses the loopback emulator and synthetic identities/data.
$ErrorActionPreference = 'Stop'
$connectorUrl = 'http://127.0.0.1:9399/v1/projects/demo-counter/locations/europe-central2/services/counter-f0787-service/connectors/counter'
$visitId = [guid]::NewGuid().ToString()

function Invoke-Operation($operation, $variables, $owner = 'test-owner-a', $mutation = $false, $provider = 'anonymous') {
    $impersonate = if ($null -eq $owner) { @{unauthenticated=$true} } else {
        @{authClaims=@{sub=$owner; firebase=@{sign_in_provider=$provider}}}
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

# Recovery uses an owner-scoped list for both anonymous and Google identities.
$ownList = Invoke-Operation 'ListVisits' @{offset=0}
Assert-Condition (($ownList.data.visits | ForEach-Object { ([guid]$_.id).ToString() }) -contains $visitId) 'Owner list omitted its deletion marker'
$otherList = Invoke-Operation 'ListVisits' @{offset=0} 'test-owner-b' $false 'google.com'
Assert-Condition (($otherList.data.visits | ForEach-Object { ([guid]$_.id).ToString() }) -notcontains $visitId) 'Google user could read another owner history'

$serviceUrl = 'http://127.0.0.1:9399/v1/projects/demo-counter/locations/europe-central2/services/counter-f0787-service'
$pageOwner = 'paging-' + [guid]::NewGuid().ToString()
$rows = @(1..501 | ForEach-Object {
    @{id=[guid]::NewGuid().ToString(); ownerUid=$pageOwner; type='BOTH'; occurredAt='2026-01-01T10:00:00Z'; note='synthetic'; deleted=$false; revision='3'}
})
$copyQuery = 'mutation RecoverVisits($rows: [Visit_Data!]!) @transaction { visit_insertMany(data: $rows) }'
for ($index = 0; $index -lt $rows.Count; $index += 100) {
    $batch = @($rows | Select-Object -Skip $index -First 100)
    $body = @{query=$copyQuery; variables=@{rows=$batch}} | ConvertTo-Json -Depth 8
    $seeded = Invoke-RestMethod -Method Post -Uri "${serviceUrl}:executeGraphql" -ContentType 'application/json' -Body $body
    Assert-Condition ($seeded.errors.Count -eq 0) 'Administrator insert-only recovery mutation failed'
}
$firstPage = Invoke-Operation 'ListVisits' @{offset=0} $pageOwner $false 'google.com'
$secondPage = Invoke-Operation 'ListVisits' @{offset=500} $pageOwner $false 'google.com'
Assert-Condition ($firstPage.data.visits.Count -eq 500) 'First recovery page did not contain 500 rows'
Assert-Condition ($secondPage.data.visits.Count -eq 1) 'Second recovery page omitted rows beyond the page limit'
Assert-Condition ((@($firstPage.data.visits.id) + @($secondPage.data.visits.id) | Sort-Object -Unique).Count -eq 501) 'Recovery pages repeated or lost records'

$denied = $false
try {
    $noAuth = Invoke-Operation 'ListVisits' @{offset=0} $null
    $denied = $noAuth.errors.Count -gt 0
} catch [Microsoft.PowerShell.Commands.HttpResponseException] {
    $denied = [int]$_.Exception.Response.StatusCode -in 401,403
}
Assert-Condition $denied 'Unauthenticated recovery list was allowed'
Write-Output 'PASS: recovery owner isolation, Google access, deletion markers, 501-row pagination, admin copy mutation, and unauthenticated denial'
