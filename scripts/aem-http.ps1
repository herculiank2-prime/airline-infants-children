<#
.SYNOPSIS
  Small HTTP helper for the local AEM SDK: basic auth, Referer and CSRF token, readable errors.
  Dot-source it:  . ./scripts/aem-http.ps1; $aem = New-AemClient -Base http://localhost:4502
#>
Add-Type -AssemblyName System.Net.Http

function New-AemClient([string] $Base = "http://localhost:4502", [string] $User = "admin", [string] $Password = "admin") {
    $client = New-Object System.Net.Http.HttpClient
    $client.Timeout = [TimeSpan]::FromMinutes(5)
    $client.BaseAddress = New-Object System.Uri $Base
    $pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${User}:${Password}"))
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Basic", $pair)
    $client.DefaultRequestHeaders.Add("Referer", "$Base/")
    $token = ($client.GetStringAsync("/libs/granite/csrf/token.json").Result | ConvertFrom-Json).token
    $client.DefaultRequestHeaders.Add("CSRF-Token", $token)
    return $client
}

function Invoke-Aem($Client, [string] $Method, [string] $Path, $Body = $null, [string] $ContentType = "application/json", [hashtable] $Headers = @{}) {
    $request = New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($Method)), $Path
    foreach ($key in $Headers.Keys) { $request.Headers.TryAddWithoutValidation($key, $Headers[$key]) | Out-Null }
    if ($Body -is [hashtable]) {
        $pairs = New-Object 'System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]'
        foreach ($key in $Body.Keys) {
            foreach ($value in @($Body[$key])) {
                $pairs.Add((New-Object 'System.Collections.Generic.KeyValuePair[string,string]' $key, ([string]$value)))
            }
        }
        $request.Content = New-Object System.Net.Http.FormUrlEncodedContent (, $pairs)
    } elseif ($null -ne $Body) {
        $request.Content = New-Object System.Net.Http.StringContent $Body, ([Text.Encoding]::UTF8), $ContentType
    }
    $response = $Client.SendAsync($request).Result
    [pscustomobject]@{
        Status = [int]$response.StatusCode
        ETag   = if ($response.Headers.ETag) { $response.Headers.ETag.ToString() } else { $null }
        Body   = $response.Content.ReadAsStringAsync().Result
    }
}
