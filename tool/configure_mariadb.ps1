[CmdletBinding()]
param(
    [string]$DbHost = '',
    [ValidateRange(1, 65535)][int]$DbPort = 3306,
    [string]$DbName = 'rental_system',
    [string]$DbUser = 'teamuser'
)

$ErrorActionPreference = 'Stop'
$repoDir = Split-Path -Parent $PSScriptRoot
$relativeConfig = '.run/DeptRental_MariaDB.local.run.xml'
$configPath = Join-Path $repoDir $relativeConfig

Push-Location $repoDir
try {
    & git check-ignore --quiet $relativeConfig
    if ($LASTEXITCODE -ne 0) {
        throw 'Private run configuration is not ignored by Git. Update .gitignore first.'
    }
} finally {
    Pop-Location
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$form = New-Object System.Windows.Forms.Form
$form.Text = 'DeptRental - MariaDB connection'
$form.ClientSize = New-Object System.Drawing.Size(460, 310)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'FixedDialog'
$form.MaximizeBox = $false
$form.MinimizeBox = $false
$form.TopMost = $true

$intro = New-Object System.Windows.Forms.Label
$intro.Text = 'Enter your team DB connection. Password is hidden.' + [Environment]::NewLine + 'Saved only in a Git-ignored IntelliJ run configuration.'
$intro.SetBounds(18, 14, 425, 40)
$form.Controls.Add($intro)
$fields = @{}
$rows = @(
    @('DB host', 'host', $DbHost),
    @('Port', 'port', [string]$DbPort),
    @('Database', 'database', $DbName),
    @('User', 'user', $DbUser),
    @('Password', 'password', '')
)
for ($index = 0; $index -lt $rows.Count; $index++) {
    $label = New-Object System.Windows.Forms.Label
    $label.Text = $rows[$index][0]
    $label.SetBounds(18, (65 + 33 * $index), 105, 25)
    $field = New-Object System.Windows.Forms.TextBox
    $field.Text = $rows[$index][2]
    $field.SetBounds(128, (63 + 33 * $index), 310, 25)
    if ($rows[$index][1] -eq 'password') { $field.UseSystemPasswordChar = $true }
    $fields[$rows[$index][1]] = $field
    $form.Controls.Add($label)
    $form.Controls.Add($field)
}
$save = New-Object System.Windows.Forms.Button
$save.Text = 'Save'
$save.SetBounds(242, 254, 92, 32)
$save.Add_Click({
    $parsedPort = 0
    if ([string]::IsNullOrWhiteSpace($fields['host'].Text) -or
        [string]::IsNullOrWhiteSpace($fields['database'].Text) -or
        [string]::IsNullOrWhiteSpace($fields['user'].Text) -or
        [string]::IsNullOrEmpty($fields['password'].Text) -or
        ![int]::TryParse($fields['port'].Text, [ref]$parsedPort) -or
        $parsedPort -lt 1 -or $parsedPort -gt 65535) {
        [System.Windows.Forms.MessageBox]::Show('Fill every field and enter a valid port.', 'DeptRental') | Out-Null
        return
    }
    $form.DialogResult = [System.Windows.Forms.DialogResult]::OK
    $form.Close()
})
$cancel = New-Object System.Windows.Forms.Button
$cancel.Text = 'Cancel'
$cancel.SetBounds(346, 254, 92, 32)
$cancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
$form.AcceptButton = $save
$form.CancelButton = $cancel
$form.Controls.Add($save)
$form.Controls.Add($cancel)
$form.Add_Shown({ $fields['password'].Focus() })

if ($form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) {
    $form.Dispose()
    throw 'DB configuration cancelled. Existing backend settings were not changed.'
}

try {
    $document = New-Object System.Xml.XmlDocument
    $component = $document.CreateElement('component')
    $component.SetAttribute('name', 'ProjectRunConfigurationManager')
    $document.AppendChild($component) | Out-Null
    $workspacePath = Join-Path $repoDir '.idea\workspace.xml'
    $existing = $null
    if (Test-Path -LiteralPath $workspacePath) {
        [xml]$workspace = Get-Content -LiteralPath $workspacePath -Raw
        $existing = $workspace.SelectSingleNode('//configuration[@type="SpringBootApplicationConfigurationType"]')
    }
    if ($existing) {
        $configuration = $document.ImportNode($existing, $true)
    } else {
        $configuration = $document.CreateElement('configuration')
        $configuration.SetAttribute('type', 'SpringBootApplicationConfigurationType')
        $configuration.SetAttribute('factoryName', 'Spring Boot')
        $mainClass = $document.CreateElement('option')
        $mainClass.SetAttribute('name', 'SPRING_BOOT_MAIN_CLASS')
        $mainClass.SetAttribute('value', 'kr.ac.cyci.deptrental.DeptRentalApiApplication')
        $configuration.AppendChild($mainClass) | Out-Null
        $method = $document.CreateElement('method')
        $method.SetAttribute('v', '2')
        $build = $document.CreateElement('option')
        $build.SetAttribute('name', 'Make')
        $build.SetAttribute('enabled', 'true')
        $method.AppendChild($build) | Out-Null
        $configuration.AppendChild($method) | Out-Null
    }
    $configuration.SetAttribute('name', 'DeptRental - MariaDB (local 8080)')
    $configuration.RemoveAttribute('nameIsGenerated')
    $configuration.SetAttribute('default', 'false')
    $workingDirectory = $configuration.SelectSingleNode('option[@name="WORKING_DIRECTORY"]')
    if (!$workingDirectory) {
        $workingDirectory = $document.CreateElement('option')
        $workingDirectory.SetAttribute('name', 'WORKING_DIRECTORY')
        $configuration.AppendChild($workingDirectory) | Out-Null
    }
    $workingDirectory.SetAttribute('value', '$PROJECT_DIR$')
    $environment = $configuration.SelectSingleNode('envs')
    if (!$environment) {
        $environment = $document.CreateElement('envs')
        $configuration.AppendChild($environment) | Out-Null
    }
    $values = [ordered]@{
        SPRING_PROFILES_ACTIVE = 'mariadb'
        SERVER_ADDRESS = '127.0.0.1'
        SERVER_PORT = '8080'
        DB_HOST = $fields['host'].Text.Trim()
        DB_PORT = $fields['port'].Text.Trim()
        DB_NAME = $fields['database'].Text.Trim()
        DB_USER = $fields['user'].Text.Trim()
        DB_PASSWORD = $fields['password'].Text
    }
    foreach ($key in $values.Keys) {
        $entry = $environment.SelectSingleNode("env[@name='$key']")
        if (!$entry) {
            $entry = $document.CreateElement('env')
            $entry.SetAttribute('name', $key)
            $environment.AppendChild($entry) | Out-Null
        }
        $entry.SetAttribute('value', $values[$key])
    }
    $component.AppendChild($configuration) | Out-Null
    $directory = Split-Path -Parent $configPath
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $settings = New-Object System.Xml.XmlWriterSettings
    $settings.Indent = $true
    $settings.Encoding = New-Object System.Text.UTF8Encoding($false)
    $writer = [System.Xml.XmlWriter]::Create($configPath, $settings)
    try { $document.Save($writer) } finally { $writer.Dispose() }
    Write-Output 'Private MariaDB configuration saved. No credentials were printed.'
    Write-Output 'IntelliJ run configuration: DeptRental - MariaDB (local 8080)'
} finally {
    $fields['password'].Text = ''
    $form.Dispose()
}
