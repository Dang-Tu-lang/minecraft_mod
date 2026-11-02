# Draws the three 16x16 item textures (grimoire, wand, meter).
# Run: powershell -ExecutionPolicy Bypass -File "D:\code_base\New folder\mod\scripts\gen_textures.ps1"
# Kept ASCII-only because Windows PowerShell 5.1 reads BOM-less scripts with the ANSI code page.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$out = Join-Path $PSScriptRoot '..\src\main\resources\assets\phapthuat\textures\item'
New-Item -ItemType Directory -Force $out | Out-Null

function Draw-Item($name, $rows, $palette) {
    if ($rows.Count -ne 16) { throw "${name}: need 16 rows, got $($rows.Count)" }
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $rows[$y]
        if ($row.Length -ne 16) { throw "${name}: row $y has length $($row.Length)" }
        for ($x = 0; $x -lt 16; $x++) {
            $ch = [string]$row[$x]
            if ($ch -eq '.') { $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0)); continue }
            $hex = $palette[$ch]
            if (-not $hex) { throw "${name}: no colour for '$ch'" }
            $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml("#$hex"))
        }
    }
    $bmp.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Dark red book with a gold flame and page edges on the right.
Draw-Item 'grimoire' @(
    '................',
    '..############..',
    '.#RRRRRRRRRRRR#.',
    '.#RDDDDDDDDDDR#W',
    '.#RDDDDYDDDDDR#W',
    '.#RDDDYYDDDDDR#W',
    '.#RDDDYOYDDDDR#W',
    '.#RDDYOOYDDDDR#W',
    '.#RDDYOOOYDDDR#W',
    '.#RDDYOOOYDDDR#W',
    '.#RDDDYYYDDDDR#W',
    '.#RDDDDDDDDDDR#W',
    '.#RRRRRRRRRRRR#W',
    '..############WW',
    '................',
    '................'
) @{ '#' = '3b0d0d'; 'R' = '8b1e1e'; 'D' = '6a1414'; 'Y' = 'f2c14e'; 'O' = 'ff7a1a'; 'W' = 'e8e0c8' }

# Diagonal wooden staff with a blue crystal head.
Draw-Item 'wand' @(
    '.............CC.',
    '............CBBC',
    '...........CBLBC',
    '...........CBBC.',
    '..........#CC...',
    '.........#W#....',
    '........#W#.....',
    '.......#W#......',
    '......#W#.......',
    '.....#W#........',
    '....#W#.........',
    '...#W#..........',
    '..#W#...........',
    '.#W#............',
    '.##.............',
    '................'
) @{ '#' = '3d2610'; 'W' = '8a5a2b'; 'C' = '1f5f9e'; 'B' = '3fa9f5'; 'L' = 'bfe9ff' }

# Grey meter with a glass dial, red needle and two buttons.
Draw-Item 'meter' @(
    '................',
    '..############..',
    '.#GGGGGGGGGGGG#.',
    '.#GddddddddddG#.',
    '.#GdLLLLLLLLdG#.',
    '.#GdLLLLLLLKdG#.',
    '.#GdLLLLLLKLdG#.',
    '.#GdLLLLLKLLdG#.',
    '.#GdLLLLXLLLdG#.',
    '.#GddddddddddG#.',
    '.#GGGGGGGGGGGG#.',
    '.#GGPGGGGGGQGG#.',
    '.#GGGGGGGGGGGG#.',
    '..############..',
    '................',
    '................'
) @{ '#' = '2b2b2b'; 'G' = '8c8c8c'; 'd' = '555555'; 'L' = 'cfe8ef'; 'K' = 'd62828'; 'X' = '222222'; 'P' = 'd62828'; 'Q' = '2fa84f' }

Get-ChildItem $out -Filter *.png | ForEach-Object { "$($_.Name) $($_.Length) bytes" }
