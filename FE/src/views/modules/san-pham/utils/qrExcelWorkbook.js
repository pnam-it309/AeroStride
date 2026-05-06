const XML_HEADER = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
const RELS_NS = 'http://schemas.openxmlformats.org/package/2006/relationships'
const MAIN_NS = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'
const DOC_REL_NS = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'
const DRAWING_NS = 'http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing'
const A_NS = 'http://schemas.openxmlformats.org/drawingml/2006/main'
const CRC_TABLE = (() => {
  const table = new Uint32Array(256)
  for (let i = 0; i < 256; i += 1) {
    let crc = i
    for (let j = 0; j < 8; j += 1) {
      crc = (crc & 1) ? (0xedb88320 ^ (crc >>> 1)) : (crc >>> 1)
    }
    table[i] = crc >>> 0
  }
  return table
})()

const textEncoder = new TextEncoder()

const encodeText = (value) => textEncoder.encode(value)

const escapeXml = (value) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&apos;')

const toZipDosDate = () => 0
const toZipDosTime = () => 0

const crc32 = (bytes) => {
  let crc = 0xffffffff
  for (let i = 0; i < bytes.length; i += 1) {
    crc = CRC_TABLE[(crc ^ bytes[i]) & 0xff] ^ (crc >>> 8)
  }
  return (crc ^ 0xffffffff) >>> 0
}

const concatUint8Arrays = (chunks) => {
  const totalLength = chunks.reduce((sum, chunk) => sum + chunk.length, 0)
  const result = new Uint8Array(totalLength)
  let offset = 0
  chunks.forEach((chunk) => {
    result.set(chunk, offset)
    offset += chunk.length
  })
  return result
}

export const buildZipBlob = (entries, type = 'application/zip') => {
  const localParts = []
  const centralParts = []
  let offset = 0

  entries.forEach((entry) => {
    const nameBytes = encodeText(entry.name)
    const dataBytes = entry.data instanceof Uint8Array ? entry.data : encodeText(entry.data)
    const checksum = crc32(dataBytes)

    const localHeader = new Uint8Array(30 + nameBytes.length)
    const localView = new DataView(localHeader.buffer)
    localView.setUint32(0, 0x04034b50, true)
    localView.setUint16(4, 20, true)
    localView.setUint16(6, 0, true)
    localView.setUint16(8, 0, true)
    localView.setUint16(10, toZipDosTime(), true)
    localView.setUint16(12, toZipDosDate(), true)
    localView.setUint32(14, checksum, true)
    localView.setUint32(18, dataBytes.length, true)
    localView.setUint32(22, dataBytes.length, true)
    localView.setUint16(26, nameBytes.length, true)
    localView.setUint16(28, 0, true)
    localHeader.set(nameBytes, 30)
    localParts.push(localHeader, dataBytes)

    const centralHeader = new Uint8Array(46 + nameBytes.length)
    const centralView = new DataView(centralHeader.buffer)
    centralView.setUint32(0, 0x02014b50, true)
    centralView.setUint16(4, 20, true)
    centralView.setUint16(6, 20, true)
    centralView.setUint16(8, 0, true)
    centralView.setUint16(10, 0, true)
    centralView.setUint16(12, toZipDosTime(), true)
    centralView.setUint16(14, toZipDosDate(), true)
    centralView.setUint32(16, checksum, true)
    centralView.setUint32(20, dataBytes.length, true)
    centralView.setUint32(24, dataBytes.length, true)
    centralView.setUint16(28, nameBytes.length, true)
    centralView.setUint16(30, 0, true)
    centralView.setUint16(32, 0, true)
    centralView.setUint16(34, 0, true)
    centralView.setUint16(36, 0, true)
    centralView.setUint32(38, 0, true)
    centralView.setUint32(42, offset, true)
    centralHeader.set(nameBytes, 46)
    centralParts.push(centralHeader)

    offset += localHeader.length + dataBytes.length
  })

  const centralDirectoryOffset = offset
  const centralDirectory = concatUint8Arrays(centralParts)
  const endOfCentralDirectory = new Uint8Array(22)
  const eocdView = new DataView(endOfCentralDirectory.buffer)
  eocdView.setUint32(0, 0x06054b50, true)
  eocdView.setUint16(4, 0, true)
  eocdView.setUint16(6, 0, true)
  eocdView.setUint16(8, entries.length, true)
  eocdView.setUint16(10, entries.length, true)
  eocdView.setUint32(12, centralDirectory.length, true)
  eocdView.setUint32(16, centralDirectoryOffset, true)
  eocdView.setUint16(20, 0, true)

  return new Blob([...localParts, centralDirectory, endOfCentralDirectory], { type })
}

export const dataUrlToBytes = (dataUrl) => {
  const [, base64Content = ''] = String(dataUrl || '').split(',')
  const binaryString = window.atob(base64Content)
  const bytes = new Uint8Array(binaryString.length)
  for (let i = 0; i < binaryString.length; i += 1) {
    bytes[i] = binaryString.charCodeAt(i)
  }
  return bytes
}

export const sanitizeFileNamePart = (value, fallback = 'file') => {
  const normalizedValue = String(value || fallback || '')
    .trim()
    .replace(/[<>:"/\\|?*\x00-\x1F]/g, '_')
    .replace(/\s+/g, '_')
    .slice(0, 80)

  return normalizedValue || fallback
}

export const exportQrImageZip = ({ fileName, items }) => {
  const usedFileNames = new Set()
  const entries = (items || []).map((item, index) => {
    const baseName = sanitizeFileNamePart(
      item?.baseName || item?.name || `qr_${index + 1}`,
      `qr_${index + 1}`
    )

    let nextFileName = `${baseName}.png`
    let duplicateIndex = 1
    while (usedFileNames.has(nextFileName)) {
      duplicateIndex += 1
      nextFileName = `${baseName}_${duplicateIndex}.png`
    }
    usedFileNames.add(nextFileName)

    return {
      name: nextFileName,
      data: dataUrlToBytes(item?.dataUrl)
    }
  })

  const zipBlob = buildZipBlob(entries, 'application/zip')
  triggerBlobDownload(zipBlob, fileName)
}

const columnName = (index) => {
  let value = index
  let result = ''
  while (value > 0) {
    const remainder = (value - 1) % 26
    result = String.fromCharCode(65 + remainder) + result
    value = Math.floor((value - 1) / 26)
  }
  return result
}

const stringCell = (columnIndex, rowIndex, value) => {
  const ref = `${columnName(columnIndex)}${rowIndex}`
  return `<c r="${ref}" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>`
}

const buildSheetXml = (rows) => {
  const worksheetRows = [
    `<row r="1" spans="1:6">
      ${stringCell(1, 1, 'STT')}
      ${stringCell(2, 1, 'San pham')}
      ${stringCell(3, 1, 'Ma SKU')}
      ${stringCell(4, 1, 'Mau sac')}
      ${stringCell(5, 1, 'Kich thuoc')}
      ${stringCell(6, 1, 'QR Code')}
    </row>`
  ]

  rows.forEach((row, index) => {
    const excelRow = index + 2
    worksheetRows.push(`
      <row r="${excelRow}" spans="1:6" ht="78" customHeight="1">
        <c r="A${excelRow}"><v>${index + 1}</v></c>
        ${stringCell(2, excelRow, row.productName)}
        ${stringCell(3, excelRow, row.sku)}
        ${stringCell(4, excelRow, row.color)}
        ${stringCell(5, excelRow, row.size)}
        ${stringCell(6, excelRow, '')}
      </row>
    `)
  })

  return `${XML_HEADER}
    <worksheet xmlns="${MAIN_NS}" xmlns:r="${DOC_REL_NS}">
      <sheetPr />
      <dimension ref="A1:F${rows.length + 1}" />
      <sheetViews>
        <sheetView workbookViewId="0" />
      </sheetViews>
      <sheetFormatPr defaultRowHeight="15" />
      <cols>
        <col min="1" max="1" width="8" customWidth="1" />
        <col min="2" max="2" width="28" customWidth="1" />
        <col min="3" max="3" width="16" customWidth="1" />
        <col min="4" max="4" width="14" customWidth="1" />
        <col min="5" max="5" width="14" customWidth="1" />
        <col min="6" max="6" width="18" customWidth="1" />
      </cols>
      <sheetData>${worksheetRows.join('')}</sheetData>
      <pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3" />
      <drawing r:id="rId1" />
    </worksheet>`
}

const buildDrawingXml = (imageCount) => {
  const anchors = []
  for (let index = 0; index < imageCount; index += 1) {
    const rowIndex = index + 1
    anchors.push(`
      <xdr:twoCellAnchor editAs="oneCell">
        <xdr:from>
          <xdr:col>5</xdr:col>
          <xdr:colOff>60000</xdr:colOff>
          <xdr:row>${rowIndex}</xdr:row>
          <xdr:rowOff>60000</xdr:rowOff>
        </xdr:from>
        <xdr:to>
          <xdr:col>6</xdr:col>
          <xdr:colOff>-60000</xdr:colOff>
          <xdr:row>${rowIndex + 1}</xdr:row>
          <xdr:rowOff>-60000</xdr:rowOff>
        </xdr:to>
        <xdr:pic>
          <xdr:nvPicPr>
            <xdr:cNvPr id="${index + 1}" name="QR ${index + 1}" />
            <xdr:cNvPicPr />
          </xdr:nvPicPr>
          <xdr:blipFill>
            <a:blip r:embed="rId${index + 1}" />
            <a:stretch><a:fillRect /></a:stretch>
          </xdr:blipFill>
          <xdr:spPr>
            <a:xfrm>
              <a:off x="0" y="0" />
              <a:ext cx="914400" cy="914400" />
            </a:xfrm>
            <a:prstGeom prst="rect"><a:avLst /></a:prstGeom>
          </xdr:spPr>
        </xdr:pic>
        <xdr:clientData />
      </xdr:twoCellAnchor>
    `)
  }

  return `${XML_HEADER}
    <xdr:wsDr xmlns:xdr="${DRAWING_NS}" xmlns:a="${A_NS}" xmlns:r="${DOC_REL_NS}">
      ${anchors.join('')}
    </xdr:wsDr>`
}

const buildDrawingRelsXml = (imageCount) => `${XML_HEADER}
  <Relationships xmlns="${RELS_NS}">
    ${Array.from({ length: imageCount }, (_, index) => `
      <Relationship
        Id="rId${index + 1}"
        Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image"
        Target="../media/image${index + 1}.png"
      />
    `).join('')}
  </Relationships>`

const buildSheetRelsXml = () => `${XML_HEADER}
  <Relationships xmlns="${RELS_NS}">
    <Relationship
      Id="rId1"
      Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing"
      Target="../drawings/drawing1.xml"
    />
  </Relationships>`

const buildWorkbookXml = (sheetName) => `${XML_HEADER}
  <workbook xmlns="${MAIN_NS}" xmlns:r="${DOC_REL_NS}">
    <sheets>
      <sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1" />
    </sheets>
  </workbook>`

const buildWorkbookRelsXml = () => `${XML_HEADER}
  <Relationships xmlns="${RELS_NS}">
    <Relationship
      Id="rId1"
      Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
      Target="worksheets/sheet1.xml"
    />
    <Relationship
      Id="rId2"
      Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
      Target="styles.xml"
    />
  </Relationships>`

const buildRootRelsXml = () => `${XML_HEADER}
  <Relationships xmlns="${RELS_NS}">
    <Relationship
      Id="rId1"
      Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
      Target="xl/workbook.xml"
    />
  </Relationships>`

const buildStylesXml = () => `${XML_HEADER}
  <styleSheet xmlns="${MAIN_NS}">
    <fonts count="1">
      <font>
        <sz val="11" />
        <name val="Calibri" />
        <family val="2" />
      </font>
    </fonts>
    <fills count="2">
      <fill><patternFill patternType="none" /></fill>
      <fill><patternFill patternType="gray125" /></fill>
    </fills>
    <borders count="1">
      <border><left /><right /><top /><bottom /><diagonal /></border>
    </borders>
    <cellStyleXfs count="1">
      <xf numFmtId="0" fontId="0" fillId="0" borderId="0" />
    </cellStyleXfs>
    <cellXfs count="1">
      <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" />
    </cellXfs>
    <cellStyles count="1">
      <cellStyle name="Normal" xfId="0" builtinId="0" />
    </cellStyles>
  </styleSheet>`

const buildContentTypesXml = () => `${XML_HEADER}
  <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml" />
    <Default Extension="xml" ContentType="application/xml" />
    <Default Extension="png" ContentType="image/png" />
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml" />
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml" />
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml" />
    <Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml" />
  </Types>`

export const triggerBlobDownload = (blob, fileName) => {
  const fileUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = fileUrl
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(fileUrl)
}

export const exportQrRowsToWorkbook = ({ fileName, sheetName, rows, qrDataUrls }) => {
  const sheetLabel = String(sheetName || 'QR Code').replace(/[\[\]\*\/\\\?\:]/g, ' ').slice(0, 31) || 'QR Code'
  const entries = [
    { name: '[Content_Types].xml', data: buildContentTypesXml() },
    { name: '_rels/.rels', data: buildRootRelsXml() },
    { name: 'xl/workbook.xml', data: buildWorkbookXml(sheetLabel) },
    { name: 'xl/_rels/workbook.xml.rels', data: buildWorkbookRelsXml() },
    { name: 'xl/worksheets/sheet1.xml', data: buildSheetXml(rows) },
    { name: 'xl/worksheets/_rels/sheet1.xml.rels', data: buildSheetRelsXml() },
    { name: 'xl/styles.xml', data: buildStylesXml() },
    { name: 'xl/drawings/drawing1.xml', data: buildDrawingXml(qrDataUrls.length) },
    { name: 'xl/drawings/_rels/drawing1.xml.rels', data: buildDrawingRelsXml(qrDataUrls.length) }
  ]

  qrDataUrls.forEach((dataUrl, index) => {
    entries.push({
      name: `xl/media/image${index + 1}.png`,
      data: dataUrlToBytes(dataUrl)
    })
  })

  const workbookBlob = buildZipBlob(entries, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
  triggerBlobDownload(workbookBlob, fileName)
}
