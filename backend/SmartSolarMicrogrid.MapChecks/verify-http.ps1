# Member 4: exercise actual model binding and read-only map responses against a running API.
param([string]$BaseUrl = 'http://localhost:5224/api')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$original = @(Invoke-RestMethod "$BaseUrl/stations")
$map = Invoke-RestMethod "$BaseUrl/member4/maps/stations"
$originalIds = @($original.stationId | Sort-Object)
$mapIds = @($map.stations.stationId | Sort-Object)
if (Compare-Object $originalIds $mapIds) { throw 'Map and station IDs differ.' }
$fixture = @($map.stations | Where-Object { $null -ne $_.location } | Select-Object -First 1)
if ($fixture.Count -eq 0) { throw 'Provide at least one station with valid coordinates for this check.' }
$station = $fixture[0]
$name = [uri]::EscapeDataString($station.stationName.ToLowerInvariant())
$search = Invoke-RestMethod "$BaseUrl/member4/maps/stations?query=$name"
if ($station.stationId -notin $search.stations.stationId) { throw 'Case-insensitive search failed.' }
foreach ($item in $search.stations) {
    if (-not $item.stationName.ToLowerInvariant().Contains($station.stationName.ToLowerInvariant())) {
        throw 'Search query was not applied by the endpoint.'
    }
}
$lat = $station.location.latitude.ToString([Globalization.CultureInfo]::InvariantCulture)
$lng = $station.location.longitude.ToString([Globalization.CultureInfo]::InvariantCulture)
$near = Invoke-RestMethod "$BaseUrl/member4/maps/stations?latitude=$lat&longitude=$lng&radiusKm=25"
if ($near.stations[0].distanceKm -ne 0) { throw 'Origin station must have zero distance.' }
$previousDistance = -1
foreach ($item in $near.stations) {
    if ($item.distanceKm -gt 25 -or $item.distanceKm -lt $previousDistance) { throw 'Radius or ordering failed.' }
    $previousDistance = $item.distanceKm
}
foreach ($invalid in @('latitude=7', 'radiusKm=25', 'latitude=91&longitude=0',
    'latitude=0&longitude=181', 'latitude=0&longitude=0&radiusKm=-1',
    'latitude=NaN&longitude=0', ('query=' + ('x' * 101)))) {
    try {
        $null = Invoke-WebRequest "$BaseUrl/member4/maps/stations?$invalid" -UseBasicParsing
        throw "Expected HTTP 400 for $invalid"
    } catch {
        if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 400) { throw }
    }
}
Write-Output "PASS: station contract, search binding, radius/distance ordering, and seven invalid-input cases. Stations: $($map.stations.Count)."
