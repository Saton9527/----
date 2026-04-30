param(
    [Parameter(Mandatory = $true)]
    [string]$SourceMarkdown,

    [Parameter(Mandatory = $true)]
    [string]$TemplateDocx,

    [Parameter(Mandatory = $true)]
    [string]$OutputDocx
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Escape-XmlText {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    $value = $Text.Replace("&", "&amp;")
    $value = $value.Replace("<", "&lt;")
    $value = $value.Replace(">", "&gt;")
    $value = $value.Replace('"', "&quot;")
    return $value
}

function New-ParagraphXml {
    param(
        [string]$Text,
        [string]$Style,
        [switch]$Center,
        [string]$PPrExtra,
        [string]$RPrExtra
    )

    $pPr = ""
    if ($Style -or $Center.IsPresent -or $PPrExtra) {
        $pPr = "<w:pPr>"
        if ($Style) {
            $pPr += '<w:pStyle w:val="' + $Style + '"/>'
        }
        if ($Center.IsPresent) {
            $pPr += '<w:jc w:val="center"/>'
        }
        if ($PPrExtra) {
            $pPr += $PPrExtra
        }
        $pPr += "</w:pPr>"
    }

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return "<w:p>$pPr</w:p>"
    }

    $rPr = ""
    if ($RPrExtra) {
        $rPr = "<w:rPr>$RPrExtra</w:rPr>"
    }

    $runs = New-Object System.Collections.Generic.List[string]
    $citationMatches = [regex]::Matches($Text, '(\[\d{1,3}\]|【\d{1,3}】)')
    if ($citationMatches.Count -eq 0) {
        $escaped = Escape-XmlText $Text
        $runs.Add('<w:r>' + $rPr + '<w:t xml:space="preserve">' + $escaped + '</w:t></w:r>')
    }
    else {
        $index = 0
        foreach ($match in $citationMatches) {
            if ($match.Index -gt $index) {
                $plainText = $Text.Substring($index, $match.Index - $index)
                $runs.Add('<w:r>' + $rPr + '<w:t xml:space="preserve">' + (Escape-XmlText $plainText) + '</w:t></w:r>')
            }

            $citation = $match.Value
            if ($citation -match '^\[(\d{1,3})\]$') {
                $citation = '【' + $Matches[1] + '】'
            }
            $supRPr = '<w:rPr>'
            if ($RPrExtra) {
                $supRPr += $RPrExtra
            }
            $supRPr += '<w:vertAlign w:val="superscript"/></w:rPr>'
            $runs.Add('<w:r>' + $supRPr + '<w:t xml:space="preserve">' + (Escape-XmlText $citation) + '</w:t></w:r>')
            $index = $match.Index + $match.Length
        }

        if ($index -lt $Text.Length) {
            $tailText = $Text.Substring($index)
            $runs.Add('<w:r>' + $rPr + '<w:t xml:space="preserve">' + (Escape-XmlText $tailText) + '</w:t></w:r>')
        }
    }

    return '<w:p>' + $pPr + ($runs -join '') + '</w:p>'
}

function New-PageBreakParagraphXml {
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'
}

function New-SectionBreakParagraphXml {
    param([string]$SectionXml)

    return '<w:p><w:pPr>' + $SectionXml + '</w:pPr></w:p>'
}

function New-HeadingParagraphXml {
    param(
        [string]$Text,
        [int]$Level,
        [switch]$NoPageBreakBefore
    )

    $pPrExtra = '<w:spacing w:line="360" w:lineRule="auto"/>'
    $rPrExtra = '<w:rFonts w:eastAsia="黑体"/>'
    switch ($Level) {
        1 {
            if ($NoPageBreakBefore.IsPresent) {
                $pPrExtra = '<w:spacing w:before="360" w:beforeLines="150" w:line="360" w:lineRule="auto"/>'
            }
            else {
                $pPrExtra = '<w:pageBreakBefore/><w:spacing w:before="360" w:beforeLines="150" w:line="360" w:lineRule="auto"/>'
            }
            $rPrExtra = '<w:rFonts w:eastAsia="黑体"/><w:sz w:val="32"/><w:szCs w:val="32"/>'
        }
        2 {
            $pPrExtra = '<w:keepNext/><w:spacing w:line="360" w:lineRule="auto"/>'
            $rPrExtra = '<w:rFonts w:eastAsia="黑体"/><w:sz w:val="30"/><w:szCs w:val="30"/>'
        }
        3 {
            $pPrExtra = '<w:keepNext/><w:spacing w:line="360" w:lineRule="auto"/>'
            $rPrExtra = '<w:rFonts w:eastAsia="黑体"/><w:sz w:val="28"/><w:szCs w:val="28"/>'
        }
        default {
            $pPrExtra = '<w:keepNext/><w:spacing w:line="360" w:lineRule="auto"/>'
            $rPrExtra = '<w:rFonts w:eastAsia="黑体"/><w:sz w:val="24"/><w:szCs w:val="24"/>'
        }
    }

    return New-ParagraphXml -Text $Text -PPrExtra $pPrExtra -RPrExtra $rPrExtra
}

function Normalize-ChapterTitle {
    param([string]$Text)

    if ($Text -match '^第\s*\d+\s*章\s*(.+)$') {
        $numMatch = [regex]::Match($Text, '\d+')
        if ($numMatch.Success) {
            return ($numMatch.Value + ' ' + $matches[1].Trim())
        }
        return $matches[1].Trim()
    }
    return $Text
}

function New-TableXml {
    param([object[]]$Rows)

    if (-not $Rows -or $Rows.Count -eq 0) {
        return ""
    }

    $maxCols = 0
    foreach ($row in $Rows) {
        if ($row.Count -gt $maxCols) {
            $maxCols = $row.Count
        }
    }

    if ($maxCols -le 0) {
        return ""
    }

    $grid = ""
    for ($i = 0; $i -lt $maxCols; $i++) {
        $grid += '<w:gridCol w:w="2400"/>'
    }

    $rowsXml = ""
    for ($r = 0; $r -lt $Rows.Count; $r++) {
        $cellsXml = ""
        for ($c = 0; $c -lt $maxCols; $c++) {
            $text = ""
            if ($c -lt $Rows[$r].Count) {
                $text = [string]$Rows[$r][$c]
            }
            $escaped = Escape-XmlText $text
            $rPr = ""
            if ($r -eq 0) {
                $rPr = "<w:rPr><w:b/></w:rPr>"
            }
            $cellsXml += '<w:tc><w:tcPr><w:tcW w:w="2400" w:type="dxa"/></w:tcPr><w:p><w:r>' + $rPr + '<w:t xml:space="preserve">' + $escaped + '</w:t></w:r></w:p></w:tc>'
        }
        $rowsXml += "<w:tr>$cellsXml</w:tr>"
    }

    $tbl = @"
<w:tbl>
  <w:tblPr>
    <w:tblStyle w:val="97"/>
    <w:tblBorders>
      <w:top w:val="single" w:sz="8" w:space="0" w:color="000000"/>
      <w:left w:val="single" w:sz="8" w:space="0" w:color="000000"/>
      <w:bottom w:val="single" w:sz="8" w:space="0" w:color="000000"/>
      <w:right w:val="single" w:sz="8" w:space="0" w:color="000000"/>
      <w:insideH w:val="single" w:sz="8" w:space="0" w:color="000000"/>
      <w:insideV w:val="single" w:sz="8" w:space="0" w:color="000000"/>
    </w:tblBorders>
  </w:tblPr>
  <w:tblGrid>$grid</w:tblGrid>
  $rowsXml
</w:tbl>
"@

    return $tbl
}

function Flush-TableBuffer {
    param([string[]]$Buffer)

    if (-not $Buffer -or $Buffer.Count -eq 0) {
        return @()
    }

    $rows = @()
    foreach ($line in $Buffer) {
        $trimmed = $line.Trim()
        if (-not ($trimmed -like '|*')) {
            continue
        }

        if ($trimmed -match '^\|[\s:\-|]+\|$') {
            continue
        }

        $parts = @()
        foreach ($part in $trimmed.Trim('|').Split('|')) {
            $parts += $part.Trim()
        }
        $rows += ,$parts
    }

    $tableXml = New-TableXml -Rows $rows
    if (-not $tableXml) {
        return @()
    }

    return @($tableXml)
}

function Convert-MarkdownToWordXml {
    param([string[]]$Lines)

    $xmlParts = @()
    $tableBuffer = @()
    $inCodeBlock = $false
    $isFirstH1 = $true
    $mode = 'body'
    $inFrontMatter = $false
    $frontMatterClosed = $false
    $suppressNextH1PageBreak = $false

    $titleSectionXml = '<w:sectPr><w:type w:val="nextPage"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1418" w:right="1134" w:bottom="1418" w:left="1701" w:header="851" w:footer="992" w:gutter="0"/><w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>'
    $frontMatterSectionXml = '<w:sectPr><w:type w:val="nextPage"/><w:headerReference w:type="default" r:id="rId13"/><w:footerReference w:type="default" r:id="rId11"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1418" w:right="1134" w:bottom="1418" w:left="1701" w:header="851" w:footer="992" w:gutter="0"/><w:pgNumType w:fmt="upperRoman" w:start="1"/><w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>'
    $bodySectionXml = '<w:sectPr><w:headerReference w:type="default" r:id="rId13"/><w:footerReference w:type="default" r:id="rId14"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1418" w:right="1134" w:bottom="1418" w:left="1701" w:header="851" w:footer="992" w:gutter="0"/><w:pgNumType w:fmt="decimal" w:start="1"/><w:cols w:space="425"/><w:docGrid w:type="lines" w:linePitch="312"/></w:sectPr>'

    foreach ($line in $Lines) {
        if ($line.TrimStart().StartsWith('```')) {
            if ($tableBuffer.Count -gt 0) {
                $xmlParts += Flush-TableBuffer -Buffer $tableBuffer
                $tableBuffer = @()
            }
            $inCodeBlock = -not $inCodeBlock
            continue
        }

        if ($inCodeBlock) {
            $xmlParts += New-ParagraphXml -Text $line -Style '71'
            continue
        }

        if ($line.Trim() -like '|*') {
            $tableBuffer += $line
            continue
        }

        if ($tableBuffer.Count -gt 0) {
            $xmlParts += Flush-TableBuffer -Buffer $tableBuffer
            $tableBuffer = @()
        }

        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        if ($line -match '^(#{1,4})\s+(.*)$') {
            $level = $matches[1].Length
            $text = $matches[2].Trim()

            if ($level -eq 1 -and $isFirstH1) {
                $xmlParts += New-ParagraphXml -Text $text -Style '49'
                $isFirstH1 = $false
                $mode = 'body'
                }
            else {
                if ($text -eq '中文摘要') {
                    $xmlParts += New-SectionBreakParagraphXml -SectionXml $titleSectionXml
                    $xmlParts += New-ParagraphXml -Text $text -Style '55'
                    $mode = 'cn_abstract'
                    $inFrontMatter = $true
                }
                elseif ($text -eq 'Abstract') {
                    $xmlParts += New-PageBreakParagraphXml
                    $xmlParts += New-ParagraphXml -Text $text -Style '59'
                    $mode = 'en_abstract'
                }
                elseif ($text -eq '目录') {
                    $xmlParts += New-PageBreakParagraphXml
                    $xmlParts += New-ParagraphXml -Text $text -Style '60'
                    $mode = 'toc'
                }
                else {
                    if ($level -eq 1 -and $inFrontMatter -and -not $frontMatterClosed) {
                        $xmlParts += New-SectionBreakParagraphXml -SectionXml $frontMatterSectionXml
                        $frontMatterClosed = $true
                        $inFrontMatter = $false
                        $suppressNextH1PageBreak = $true
                    }
                    if ($level -eq 1) {
                        $text = Normalize-ChapterTitle -Text $text
                    }
                    if ($level -eq 1 -and $suppressNextH1PageBreak) {
                        $xmlParts += New-HeadingParagraphXml -Text $text -Level $level -NoPageBreakBefore
                        $suppressNextH1PageBreak = $false
                    }
                    else {
                        $xmlParts += New-HeadingParagraphXml -Text $text -Level $level
                    }
                    $mode = 'body'
                }
            }
            continue
        }

        if ($mode -eq 'toc') {
            $tocText = $line.Trim()
            $tocText = Normalize-ChapterTitle -Text $tocText
            $xmlParts += New-ParagraphXml -Text $tocText -Style '111'
            continue
        }

        if ($line.StartsWith('关键词：')) {
            $xmlParts += New-ParagraphXml -Text $line.Trim() -Style '58'
            continue
        }

        if ($line.StartsWith('Keywords:')) {
            $xmlParts += New-ParagraphXml -Text $line.Trim() -Style '57'
            continue
        }

        if ($line -like '图 *' -or $line -like '表 *') {
            $xmlParts += New-ParagraphXml -Text $line.Trim() -Style '70'
            continue
        }

        if ($line.TrimStart() -like '- *' -or $line -match '^\d+\.\s+') {
            $xmlParts += New-ParagraphXml -Text $line.Trim() -Style '3'
            continue
        }

        if ($mode -eq 'en_abstract') {
            $xmlParts += New-ParagraphXml -Text $line -Style '56'
            continue
        }

        if ($mode -eq 'cn_abstract') {
            $xmlParts += New-ParagraphXml -Text $line -Style '3'
            continue
        }

        $xmlParts += New-ParagraphXml -Text $line -Style '3'
    }

    if ($tableBuffer.Count -gt 0) {
        $xmlParts += Flush-TableBuffer -Buffer $tableBuffer
    }

    return @{
        BodyXml = [string]::Join("`n", $xmlParts)
        FinalSectionXml = $bodySectionXml
    }
}

$sourcePath = (Resolve-Path $SourceMarkdown).Path
$templatePath = (Resolve-Path $TemplateDocx).Path
$outputPath = [System.IO.Path]::GetFullPath($OutputDocx)
$outputDir = Split-Path -Parent $outputPath

if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

$lines = Get-Content -LiteralPath $sourcePath -Encoding UTF8
$conversionResult = Convert-MarkdownToWordXml -Lines $lines
$documentBody = $conversionResult.BodyXml
$finalSectionXml = $conversionResult.FinalSectionXml

$documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
    xmlns:o="urn:schemas-microsoft-com:office:office"
    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
    xmlns:v="urn:schemas-microsoft-com:vml"
    xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
    xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    xmlns:w10="urn:schemas-microsoft-com:office:word"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
    xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
    xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
    xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
    xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
    mc:Ignorable="w14 wp14">
  <w:body>
    $documentBody
    $finalSectionXml
  </w:body>
</w:document>
"@

$tempRoot = Join-Path $outputDir ('.docx_build_' + [guid]::NewGuid().ToString('N'))
$extractDir = Join-Path $tempRoot 'extracted'
$zipPath = Join-Path $tempRoot 'template.zip'
$rebuiltZip = Join-Path $tempRoot 'rebuilt.zip'

New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
Copy-Item -LiteralPath $templatePath -Destination $zipPath -Force
Expand-Archive -LiteralPath $zipPath -DestinationPath $extractDir -Force
Set-Content -LiteralPath (Join-Path $extractDir 'word\document.xml') -Value $documentXml -Encoding UTF8

$headerXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
    xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
    xmlns:o="urn:schemas-microsoft-com:office:office"
    xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
    xmlns:v="urn:schemas-microsoft-com:vml"
    xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
    xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    xmlns:w10="urn:schemas-microsoft-com:office:word"
    xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
    xmlns:w15="http://schemas.microsoft.com/office/word/2012/wordml"
    xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
    xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
    xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
    xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
    xmlns:wpsCustomData="http://www.wps.cn/officeDocument/2013/wpsCustomData"
    mc:Ignorable="w14 w15 wp14">
  <w:p>
    <w:pPr>
      <w:pBdr>
        <w:bottom w:val="single" w:sz="4" w:space="1" w:color="auto"/>
      </w:pBdr>
      <w:jc w:val="center"/>
      <w:rPr>
        <w:rFonts w:eastAsia="宋体"/>
        <w:sz w:val="18"/>
        <w:szCs w:val="18"/>
      </w:rPr>
    </w:pPr>
    <w:r>
      <w:rPr>
        <w:rFonts w:eastAsia="宋体"/>
        <w:sz w:val="18"/>
        <w:szCs w:val="18"/>
      </w:rPr>
      <w:t>$(Escape-XmlText $lines[0].TrimStart('#').Trim())</w:t>
    </w:r>
  </w:p>
</w:hdr>
"@

$headerPath = Join-Path $extractDir 'word\header6.xml'
if (Test-Path $headerPath) {
    Set-Content -LiteralPath $headerPath -Value $headerXml -Encoding UTF8
}

if (Test-Path $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

Compress-Archive -Path (Join-Path $extractDir '*') -DestinationPath $rebuiltZip -Force
Move-Item -LiteralPath $rebuiltZip -Destination $outputPath -Force
Remove-Item -LiteralPath $tempRoot -Recurse -Force

Write-Output $outputPath
