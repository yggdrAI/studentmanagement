$src = "f:\Coding\studentmanagement\src\main\java"
$out = "f:\Coding\studentmanagement\out"

New-Item -ItemType Directory -Force -Path $out | Out-Null

# Gather all .java files
$files = Get-ChildItem -Path $src -Recurse -Filter "*.java" | ForEach-Object { '"' + $_.FullName + '"' }

Write-Host "Found $(@($files).Count) source files:"
$files | ForEach-Object { Write-Host "  $_" }

$argList = ($files -join " ")
$cmd = "javac -d `"$out`" $argList"

Write-Host "`nCompiling..."
Invoke-Expression $cmd
if ($LASTEXITCODE -eq 0) {
    Write-Host "`nCompile SUCCESS. Running app..."
    Set-Location $out
    java com.sms.StudentManagementApplication
} else {
    Write-Host "`nCompile FAILED (exit $LASTEXITCODE)"
}
