# This script downloads a portable version of Maven and runs the Spring Boot app.
$ErrorActionPreference = "Stop"

$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
$mavenZip = "maven.zip"
$mavenFolder = "apache-maven-3.9.6"

if (-not (Test-Path $mavenFolder)) {
    Write-Host "Downloading Portable Maven... This might take a few seconds."
    Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $mavenZip -DestinationPath "." -Force
    Remove-Item -Path $mavenZip -Force
}

Write-Host "Compiling and Running the Spring Boot Application..."
& ".\$mavenFolder\bin\mvn.cmd" spring-boot:run
