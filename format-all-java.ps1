# format-all-java.ps1

$formatter = "tools\google-java-format-1.28.0-all-deps.jar"

Get-ChildItem -Path . -Recurse -Include *.java | ForEach-Object {
    Write-Host "Formatting $($_.FullName)..."
    java -jar $formatter --replace $_.FullName
}

Write-Host "`n✅ All Java files formatted."
