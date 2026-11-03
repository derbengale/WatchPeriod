Add-Type -AssemblyName System.Drawing

# Renders the app's blood drop (same path as ic_launcher_foreground) into legacy bitmap
# launcher icons, so no Android-template robot can show up on any surface.
function New-DropIcon {
    param([int]$Size, [bool]$Round, [string]$Path)

    $bmp = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    $bg = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 0x1C, 0x1B, 0x1F))
    if ($Round) {
        $g.FillEllipse($bg, 0, 0, $Size - 1, $Size - 1)
    } else {
        $r = $Size * 0.22
        $d = 2 * $r
        $rect = [System.Drawing.RectangleF]::new(0, 0, $Size - 1, $Size - 1)
        $gp = [System.Drawing.Drawing2D.GraphicsPath]::new()
        $gp.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
        $gp.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
        $gp.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
        $gp.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
        $gp.CloseFigure()
        $g.FillPath($bg, $gp)
        $gp.Dispose()
    }

    # viewport 108x108 -> icon; the drop is scaled to 66 % of the icon height
    $scale = ($Size * 0.66) / 64.0
    function P([double]$x, [double]$y) {
        return [System.Drawing.PointF]::new(
            [single](($x - 54) * $script:scale + $Size / 2),
            [single](($y - 54) * $script:scale + $Size / 2))
    }
    $script:scale = $scale

    $p0 = P 54 22
    $c1 = P 54 22
    $c2 = P 32 50
    $p1 = P 32 64
    $arcTopLeft = P 32 42
    $arcRect = [System.Drawing.RectangleF]::new(
        $arcTopLeft.X,
        $arcTopLeft.Y,
        [single](44 * $scale),
        [single](44 * $scale))
    $c3 = P 76 50
    $p2 = P 76 64

    $drop = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $drop.AddBezier($p0, $c1, $c2, $p1)
    # 180° -> 0° with a negative sweep runs through the bottom of the circle (GDI+ y grows down)
    $drop.AddArc($arcRect, 180, -180)
    $drop.AddBezier($p2, $c3, $p0, $p0)
    $drop.CloseFigure()

    $red = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 0xE5, 0x39, 0x35))
    $g.FillPath($red, $drop)

    # soft highlight inside the drop
    $hx = (43 - 54) * $scale + $Size / 2
    $hy = (48 - 54) * $scale + $Size / 2
    $hw = 9 * $scale
    $hh = 21 * $scale
    $hi = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(64, 255, 255, 255))
    $g.FillEllipse($hi, [single]$hx, [single]$hy, [single]$hw, [single]$hh)

    $g.Dispose()
    $red.Dispose()
    $hi.Dispose()
    $bg.Dispose()
    $drop.Dispose()
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Path is derived from the script location so the script also runs on other
# machines and checkouts.
$root = Join-Path $PSScriptRoot 'wear\src\main\res'
$buckets = @{ 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
foreach ($kv in $buckets.GetEnumerator()) {
    $dir = Join-Path $root ("mipmap-" + $kv.Key)
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    New-DropIcon -Size $kv.Value -Round $false -Path (Join-Path $dir 'ic_launcher.png')
    New-DropIcon -Size $kv.Value -Round $true -Path (Join-Path $dir 'ic_launcher_round.png')
    Get-ChildItem $dir -Filter '*.webp' | Remove-Item -Force
}
Get-ChildItem -Recurse $root -Include *.webp,*.png | ForEach-Object { $_.FullName }
