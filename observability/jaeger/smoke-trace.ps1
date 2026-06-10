param(
    [string]$Namespace = "monitoring",
    [string]$ServiceName = "circleguard-smoke",
    [string]$OperationName = "sprint1-jaeger-smoke",
    [int]$QueryPort = 16686,
    [int]$OtlpHttpPort = 4318,
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

function Wait-HttpOk {
    param(
        [string]$Uri,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = $null

    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $Uri -TimeoutSec 3 | Out-Null
            return
        } catch {
            $lastError = $_.Exception.Message
            Start-Sleep -Seconds 1
        }
    }

    throw "Timed out waiting for $Uri. Last error: $lastError"
}

function Convert-ToUnixNanoString {
    param([DateTimeOffset]$DateTime)

    $unixMilliseconds = $DateTime.ToUnixTimeMilliseconds()
    $extraTicks = $DateTime.Ticks % [TimeSpan]::TicksPerMillisecond
    return (($unixMilliseconds * 1000000L) + ($extraTicks * 100L)).ToString()
}

function New-HexId {
    param([int]$ByteCount)

    $bytes = [byte[]]::new($ByteCount)
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
        return [System.BitConverter]::ToString($bytes).Replace("-", "").ToLowerInvariant()
    } finally {
        $rng.Dispose()
    }
}

$queryForward = $null
$otlpForward = $null

try {
    Write-Host "Starting port-forward to Jaeger query on 127.0.0.1:$QueryPort..."
    $queryForward = Start-Process -FilePath "kubectl" -ArgumentList @(
        "port-forward",
        "-n", $Namespace,
        "svc/jaeger",
        "${QueryPort}:16686"
    ) -WindowStyle Hidden -PassThru

    Write-Host "Starting port-forward to Jaeger OTLP HTTP on 127.0.0.1:$OtlpHttpPort..."
    $otlpForward = Start-Process -FilePath "kubectl" -ArgumentList @(
        "port-forward",
        "-n", $Namespace,
        "svc/jaeger",
        "${OtlpHttpPort}:4318"
    ) -WindowStyle Hidden -PassThru

    Wait-HttpOk -Uri "http://127.0.0.1:$QueryPort/api/services" -TimeoutSeconds $TimeoutSeconds

    $traceId = New-HexId -ByteCount 16
    $spanId = New-HexId -ByteCount 8
    $startTime = [DateTimeOffset]::UtcNow
    $endTime = $startTime.AddMilliseconds(125)

    $payload = @{
        resourceSpans = @(
            @{
                resource = @{
                    attributes = @(
                        @{
                            key = "service.name"
                            value = @{ stringValue = $ServiceName }
                        },
                        @{
                            key = "deployment.environment"
                            value = @{ stringValue = "sprint1" }
                        }
                    )
                }
                scopeSpans = @(
                    @{
                        scope = @{
                            name = "circleguard-jaeger-smoke"
                            version = "1.0.0"
                        }
                        spans = @(
                            @{
                                traceId = $traceId
                                spanId = $spanId
                                name = $OperationName
                                kind = 1
                                startTimeUnixNano = Convert-ToUnixNanoString -DateTime $startTime
                                endTimeUnixNano = Convert-ToUnixNanoString -DateTime $endTime
                                attributes = @(
                                    @{
                                        key = "test.name"
                                        value = @{ stringValue = $OperationName }
                                    },
                                    @{
                                        key = "sprint"
                                        value = @{ stringValue = "1" }
                                    }
                                )
                                status = @{ code = 1 }
                            }
                        )
                    }
                )
            }
        )
    }

    $json = $payload | ConvertTo-Json -Depth 20 -Compress
    Write-Host "Sending OTLP HTTP trace $traceId/$spanId as service.name=$ServiceName..."
    Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$OtlpHttpPort/v1/traces" -ContentType "application/json" -Body $json -TimeoutSec 10 | Out-Null

    $queryUri = "http://127.0.0.1:$QueryPort/api/traces?service=$([uri]::EscapeDataString($ServiceName))&operation=$([uri]::EscapeDataString($OperationName))&limit=20"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $result = $null

    while ((Get-Date) -lt $deadline) {
        $result = Invoke-RestMethod -Method Get -Uri $queryUri -TimeoutSec 10
        if ($result.data -and $result.data.Count -gt 0) {
            break
        }

        Start-Sleep -Seconds 2
    }

    if (-not $result.data -or $result.data.Count -eq 0) {
        throw "Jaeger did not return trace '$OperationName' for service '$ServiceName' before timeout."
    }

    $firstTrace = $result.data[0]
    Write-Host "Jaeger smoke trace found."
    Write-Host "service.name=$ServiceName"
    Write-Host "operation=$OperationName"
    Write-Host "traceID=$($firstTrace.traceID)"
    Write-Host "spans=$($firstTrace.spans.Count)"
    Write-Host "query=$queryUri"
} finally {
    foreach ($process in @($queryForward, $otlpForward)) {
        if ($process -and -not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
}
