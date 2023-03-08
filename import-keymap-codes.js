import * as FS from "node:fs/promises"
import * as windows1252 from "windows-1252"

const unescapeQuotes = (s) => {
  if (s.startsWith('"') && s.endsWith('"')) {
    s = s.substring(1, s.length - 1)
    const buf = []
    for (let i = 0; i < s.length; ++i) {
      const ch = s[i]
      if (ch === '"') {
        buf.push(s[i + 1])
        ++i
      } else {
        buf.push(ch)
      }
    }
    return buf.join("")
  } else {
    return s
  }
}

const main = async () => {
  const codes = []

  const docBytes = await FS.readFile(
    "docs/CCOS Key Remapping Reference Guide.txt",
    {encoding: "binary"})
  const doc = windows1252.decode(docBytes)
  const lines = doc.split("\r\n").splice(1)

  const header = lines[0]
  const keys = header.split("\t")
  console.log(keys)

  for (let line of lines.slice(1)) {
    const tokens = line.split("\t")
    if (tokens[0].length == 0) continue

    tokens[2] = unescapeQuotes(tokens[2])
    tokens[3] = unescapeQuotes(tokens[3])
    tokens[4] = unescapeQuotes(tokens[4])

    // const entry = {}
    // for (let i = 0; i < keys.length; ++i) {
    //   const k = keys[i]
    //   entry[k] = tokens[i]
    // }
    // codes.push(entry)

    codes.push(tokens)
  }

  const json = JSON.stringify(codes, null, 4)
  await FS.writeFile("src/main/app/keymap_codes.json", json, {encoding: "utf8"})
}

main()
