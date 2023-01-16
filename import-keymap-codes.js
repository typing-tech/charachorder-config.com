import * as FS from "node:fs/promises"

const main = async () => {
  const codes = []

  const doc = await FS.readFile("docs/CCOS Key Remapping Reference Guide.txt", {encoding: "latin1"})
  const lines = doc.split("\r\n")

  const header = lines[0]
  const keys = header.split("\t")
  console.log(keys)

  for (let line of lines.slice(1)) {
    const tokens = line.split("\t")
    if (tokens[0].length == 0) continue

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
