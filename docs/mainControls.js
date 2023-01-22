import {_keyMapDefaults, _actionMap, _keyMap, _chordMaps, _chordLayout, actionMap, oldAsciiKeyReplacementDictionary} from "./maps.js";
import hex2Bin from "../../../../_snowpack/pkg/hex-to-bin.js";
export class MainControls {
}
MainControls.abortController1 = new AbortController();
MainControls.abortController2 = new AbortController();
MainControls._chordmapId = "Default";
MainControls._chordmapCountOnDevice = 50;
MainControls._firmwareVersion = "0";
MainControls._chordMapIdCounter = 0;
MainControls.count = 0;
MainControls.CONFIG_ID_ENABLE_SERIAL_LOG = "01";
MainControls.CONFIG_ID_ENABLE_SERIAL_RAW = "02";
MainControls.CONFIG_ID_ENABLE_SERIAL_CHORD = "03";
MainControls.CONFIG_ID_ENABLE_SERIAL_KEYBOARD = "04";
MainControls.CONFIG_ID_ENABLE_SERIAL_MOUSE = "05";
MainControls.CONFIG_ID_ENABLE_SERIAL_DEBUG = "06";
MainControls.CONFIG_ID_ENABLE_SERIAL_HEADER = "07";
MainControls.CONFIG_ID_ENABLE_HID_KEYBOARD = "0A";
MainControls.CONFIG_ID_PRESS_THRESHOLD = "0B";
MainControls.CONFIG_ID_RELEASE_THRESHOLD = "0C";
MainControls.CONFIG_ID_ENABLE_HID_MOUSE = "14";
MainControls.CONFIG_ID_SCROLL_DELAY = "15";
MainControls.CONFIG_ID_ENABLE_SPURRING = "1E";
MainControls.CONFIG_ID_SPUR_KILLER_TOGGLE = "1F";
MainControls.CONFIG_ID_SPUR_KILLER = "20";
MainControls.CONFIG_ID_ENABLE_CHORDING = "28";
MainControls.CONFIG_ID_CHAR_KILLER_TOGGLE = "29";
MainControls.CONFIG_ID_CHAR_COUNTER_KILLER = "2A";
function compare(a, b) {
  if (a === b) {
    return 0;
  }
  const a_components = a.split(".");
  const b_components = b.split(".");
  const len = Math.min(a_components.length, b_components.length);
  for (let i = 0; i < len; i++) {
    if (parseInt(a_components[i]) > parseInt(b_components[i])) {
      return 1;
    }
    if (parseInt(a_components[i]) < parseInt(b_components[i])) {
      return -1;
    }
  }
  if (a_components.length > b_components.length) {
    return 1;
  }
  if (a_components.length < b_components.length) {
    return -1;
  }
  return 0;
}
export async function selectBase() {
  await sendCommandString("SELECT BASE");
  await readGetOneAndToss();
}
export async function sendCommandString(commandString) {
  console.log(commandString);
  if (MainControls.serialPort) {
    const encoder = new TextEncoder();
    const writer = MainControls.serialPort.writable.getWriter();
    await writer.write(encoder.encode(commandString + "\r\n"));
    writer.releaseLock();
    console.log("writing " + commandString + "\r\n");
  } else {
    console.log("serial port is not open yet");
  }
}
export async function readGetOneAndToss() {
  const {value, done} = await MainControls.lineReader.read().catch(console.error);
  if (value) {
    console.log("toss value of: " + value);
  } else {
    console.log("value is null");
  }
}
export async function readGetOneAndTossCommitAll(virtualId) {
  const myTimeout = await setTimeout(pressCommitButton, 1e4, virtualId);
  const {value, done} = await MainControls.lineReader.read().catch(console.error);
  if (value) {
    console.log("toss value of: " + value);
  } else {
    console.log("value is null");
  }
  clearTimeout(myTimeout);
}
export async function readGetOneAndReturnOne() {
  const {value, done} = await MainControls.lineReader.read().catch(console.error);
  if (value) {
    return value;
  } else {
    console.log("value is null");
  }
}
export async function selectConfig() {
  await sendCommandString("SELECT CONFIG");
  await readGetOneAndToss();
}
export async function readGetChordmapCount() {
  const {value, done} = await MainControls.lineReader.read();
  if (value) {
    MainControls._chordmapCountOnDevice = parseInt(value);
    console.log(MainControls._chordmapCountOnDevice);
  }
}
export async function enableSerialChordOutput(val) {
  console.log("enableSerialChordOutput(" + val.toString() + ")");
  await selectConfig();
  if (val == true) {
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_SERIAL_CHORD + " 01");
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_HID_KEYBOARD + " 00");
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_HID_MOUSE + " 00");
  } else {
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_SERIAL_CHORD + " 00");
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_HID_KEYBOARD + " 01");
    await sendCommandString("SET " + MainControls.CONFIG_ID_ENABLE_HID_MOUSE + " 01");
  }
  await selectBase();
}
export async function cancelReader() {
  if (MainControls.serialPort) {
    if (MainControls.lineReader) {
      await MainControls.lineReader.cancel().then(() => {
        console.log("cleared line reader");
      });
      console.log(MainControls.abortController1);
      await MainControls.abortController1.abort();
      console.log(MainControls.serialPort.readable);
      await MainControls.lineReaderDone.catch(() => {
      });
    }
  }
}
export function convertHexadecimalPhraseToAsciiString(hexString) {
  let asciiString = "";
  console.log("convertHexadecimalPhraseToAsciiString()");
  for (let i = 0; i < hexString.length; i += 2) {
    asciiString += actionMap[parseInt(hexString.substr(i, 2), 16)];
  }
  console.log(asciiString);
  return asciiString;
}
async function readGetSomeChordmaps(expectedLineCount = 100) {
  console.log("readGetSome(" + expectedLineCount + ")");
  let i = 0;
  const checker = true;
  while (checker) {
    const {value} = await MainControls.lineReader.read();
    i++;
    if (value) {
      const arrValue = [...value];
      const strValue = String(arrValue.join(""));
      console.log(strValue);
      const hexChordString = strValue[2];
      const hexAsciiString = strValue.substr(17, strValue.length);
      const strValues = ["", "", "", ""];
      strValues[0] = convertHexadecimalChordToHumanChord(hexChordString);
      strValues[1] = convertHexadecimalPhraseToAsciiString(hexAsciiString);
      strValues[2] = hexChordString;
      strValues[3] = hexAsciiString;
      console.log(strValues);
      _chordMaps.push([convertHexadecimalChordToHumanString(hexChordString), strValues[1]]);
      appendToRow(strValues);
    }
    if (i >= expectedLineCount) {
      break;
    }
  }
}
export async function readGetHexChord() {
  let hexChordString = "";
  if (MainControls.serialPort) {
    if (MainControls._chordmapId == "CHARACHORDER" && compare(MainControls._firmwareVersion, "0.9.0") == -1) {
      await readGetOneAndToss();
      console.log("i did indeed enter here");
    }
    const {value, done} = await MainControls.lineReader.read();
    if (done) {
      console.log("reader is done");
    } else {
      console.log(["value", value]);
      if (value) {
        const arrValue = [...value];
        const strValue = String(arrValue.join(""));
        console.log(strValue);
        hexChordString = strValue.substr(0, 16);
        await readGetOneAndToss();
      }
    }
  }
  return hexChordString;
}
export function convertHexadecimalChordToHumanString(hexString) {
  let humanString = "";
  console.log(hexString);
  if (hexString.length <= 0) {
    hexString = "00";
  }
  const bigNum = BigInt("0x" + hexString);
  if (MainControls._chordmapId == "CHARACHORDER") {
    const decString = String(bigNum).split("");
    console.log(decString);
    console.log(MainControls._chordmapId);
    for (let i = 0; i < decString.length; i++) {
      if (decString[i] != "0") {
        if (humanString.length > 0) {
          humanString += " + ";
        }
        console.log({
          i,
          "decString[i]": decString[i],
          "decString.length": decString.length,
          decString,
          "10exp": decString.length - i - 1,
          decChordComp: decString[i] * 10 ** (decString.length - i - 1),
          noteId: chord_to_noteId(decString[i] * 10 ** (decString.length - i - 1))
        });
        let noteId;
        let actionId;
        if (decString[i] % 2 == 1) {
          noteId = chord_to_noteId(decString[i] * 10 ** (decString.length - i - 1));
          actionId = _keyMapDefaults[0][noteId];
          if (actionId == 0) {
            actionId = 512 + noteId;
          }
          humanString += actionMap[actionId];
        } else {
          noteId = chord_to_noteId((decString[i] - 1) * 10 ** (decString.length - i - 1));
          actionId = _keyMapDefaults[0][noteId];
          if (actionId == 0) {
            actionId = 512 + noteId;
          }
          humanString += actionMap[actionId];
          humanString += " + ";
          noteId = chord_to_noteId(1 * 10 ** (decString.length - i - 1));
          actionId = _keyMapDefaults[0][noteId];
          if (actionId == 0) {
            actionId = 512 + noteId;
          }
          humanString += actionMap[actionId];
        }
      }
      if (humanString.indexOf("m + k") != -1 || humanString.indexOf("m + k") != 0) {
        humanString = humanString.replace("m + k", "m + c");
      }
    }
  } else if (MainControls._chordmapId == "CHARACHORDERLITE") {
    console.log("ChordLite " + bigNum);
    const binString = bigNum.toString(2);
    console.log(binString);
    for (let i = 0; i < binString.length; i++) {
      if (binString[i] == "1") {
        if (humanString.length > 0) {
          humanString += " + ";
        }
        humanString += _keyMap[64 - binString.length + i];
        if (_keyMap[64 - binString.length + i] == "GTM" || _keyMap[64 - binString.length + i] == "0x0061") {
          console.log("The two values " + _keyMap[64 - binString.length + i]);
        }
      }
    }
  } else {
    console.log("ChordLite " + bigNum);
    const binString = bigNum.toString(2);
    console.log(binString);
    for (let i = 0; i < binString.length; i++) {
      if (binString[i] == "1") {
        if (humanString.length > 0) {
          humanString += " + ";
        }
        humanString += _keyMap[64 - binString.length + i];
        if (_keyMap[64 - binString.length + i] == "GTM" || _keyMap[64 - binString.length + i] == "0x0061") {
          console.log("The two values " + _keyMapDefaults[64 - binString.length + i]);
        }
      }
    }
  }
  console.log(humanString);
  return humanString;
}
function checkBin(n) {
  return /^[01]{1,64}$/.test(n);
}
function checkDec(n) {
  return /^[0-9]{1,64}$/.test(n);
}
function checkHex(n) {
  return /^[0-9A-Fa-f]{1,64}$/.test(n);
}
function pad(s, z) {
  s = "" + s;
  return s.length < z ? pad("0" + s, z) : s;
}
function unpad(s) {
  s = "" + s;
  return s.replace(/^0+/, "");
}
function backpad(s, z) {
  s = "" + s;
  return s.length < z ? backpad(s + "0", z) : s;
}
function Dec2Bin(n) {
  if (!checkDec(n) || n < 0)
    return 0;
  return n.toString(2);
}
function Dec2Hex(n) {
  if (!checkDec(n) || n < 0)
    return 0;
  return n.toString(16);
}
function Bin2Dec(n) {
  if (!checkBin(n))
    return 0;
  return parseInt(n, 2).toString(10);
}
function Bin2Hex(n) {
  if (!checkBin(n))
    return 0;
  return parseInt(n, 2).toString(16);
}
function Hex2Dec(n) {
  if (!checkHex(n))
    return 0;
  return parseInt(n, 16).toString(10);
}
export function convertHexadecimalChordToHumanChord(hexChord) {
  let humanChord = "";
  const binChord = pad(hex2Bin(hexChord), 128);
  console.log(hexChord);
  console.log(binChord);
  const chainIndex = binChord.substring(0, 8);
  for (let i = 0; i < 12; i++) {
    const binAction = binChord.substring(8 + i * 10, 8 + (i + 1) * 10);
    const actionCode = Bin2Dec(binAction);
    if (actionCode != 0) {
      if (humanChord.length > 0) {
        humanChord += " + ";
      }
      const humanStringPart = actionMap[actionCode];
      humanChord += humanStringPart;
    } else {
      break;
    }
  }
  console.log("final humanChord " + humanChord);
  return humanChord;
}
export function chord_to_noteId(chord) {
  const part1 = 5 * Math.floor(Math.log10(chord));
  const part2 = Math.floor(chord / 10 ** Math.floor(Math.log10(chord)) + 1) / 2;
  const part3 = Math.log10(chord);
  const full = Math.floor(5 * Math.floor(Math.log10(chord)) + Math.floor(chord / 10 ** Math.floor(Math.log10(chord)) + 1) / 2);
  console.log([chord, part1, part2, part3, full]);
  return full;
}
export async function setupLineReader() {
  if (MainControls.serialPort) {
    console.log("setupLineRader()");
    const decoder = new TextDecoderStream();
    MainControls.abortController1 = new AbortController();
    MainControls.abortController2 = new AbortController();
    MainControls.lineReaderDone = MainControls.serialPort.readable.pipeTo(decoder.writable, {preventAbort: true, signal: MainControls.abortController1.signal});
    const inputStream = decoder.readable.pipeThrough(new TransformStream(new LineBreakTransformer(), {signal: MainControls.abortController2.signal}));
    MainControls.lineReader = await inputStream.getReader();
    console.log("setup line reader");
    document.getElementById("statusDiv").innerHTML = "status: opened serial port and listening";
  } else {
    console.log("serial port is not open yet");
  }
}
class LineBreakTransformer {
  constructor() {
    this.chunks = "";
  }
  transform(chunk, controller) {
    this.chunks += chunk;
    const lines = this.chunks.split("\r\n");
    this.chunks = lines.pop();
    lines.forEach((line) => controller.enqueue(line));
  }
  flush(controller) {
    controller.enqueue(this.chunks);
  }
}
export function appendToList(str) {
  const ul = document.getElementById("list");
  const li = document.createElement("li");
  li.appendChild(document.createTextNode(str[0] + " " + str[1]));
  ul.appendChild(li);
}
export function ascii_to_hexa(arr) {
  for (let i = 0; i < arr.length; i++) {
    arr[i] = Number(arr[i].charCodeAt(0)).toString(16);
  }
}
export function convertHumanStringToHexadecimalPhrase(humanString) {
  let hexString = "";
  for (let i = 0; i < humanString.length; i++) {
    const hex = Number(humanString.charCodeAt(i)).toString(16);
    hexString += hex;
  }
  hexString = hexString.toUpperCase();
  console.log(hexString);
  return hexString;
}
function replaceOldAsciiKeys(inputKey) {
  inputKey = inputKey.split(" + ");
  let finishedInputKey = "";
  for (let i = 0; i < inputKey.length; i++) {
    if (oldAsciiKeyReplacementDictionary.hasOwnProperty(inputKey[i])) {
      finishedInputKey += oldAsciiKeyReplacementDictionary[inputKey[i]];
    } else {
      finishedInputKey += inputKey[i];
    }
    if (inputKey.length - 1 > 0 && i != inputKey.length - 1) {
      finishedInputKey += " + ";
    }
  }
  return finishedInputKey;
}
export function convertHumanStringToHexadecimalChord(humanString) {
  console.log(humanString);
  let hexString = "";
  let bigNum = BigInt(0);
  const humanStringParts = humanString.split(" + ");
  console.log("these are the parts " + humanStringParts);
  humanStringParts.forEach(async (part) => {
    part = replaceOldAsciiKeys(part);
    console.log("This is the part " + part);
    const actionId = _actionMap.indexOf(part);
    console.log("ActionID: " + actionId);
    if (MainControls._chordmapId == "CHARACHORDER") {
      let keyId;
      if (actionId < 512) {
        keyId = _keyMapDefaults[0].indexOf(actionId);
        console.log(keyId);
      } else {
        keyId = actionId - 512;
      }
      console.log(keyId);
      bigNum += BigInt(noteId_to_chord(keyId));
      console.log(bigNum);
    } else if (MainControls._chordmapId == "CHARACHORDERLITE") {
      let keyId;
      if (actionId < 512) {
        console.log("I am here");
        keyId = _keyMapDefaults[1].indexOf(_actionMap[actionId]);
        console.log(keyId);
      } else {
        keyId = actionId - 512;
      }
      console.log(keyId);
      bigNum += BigInt(2n ** BigInt(keyId));
      console.log(bigNum);
    } else {
    }
  });
  console.log(bigNum);
  hexString = bigNum.toString(16).toUpperCase();
  hexString = "0".repeat(16 - hexString.length) + hexString;
  console.log(hexString);
  return hexString;
}
export function noteId_to_chord(note) {
  return BigInt(2 * ((note - 1) % 5) + 1) * BigInt(10) ** BigInt(Math.floor((note - 1) / 5));
}
export async function readGetOneChordmap() {
  console.log("readGetOneChordmap()");
  const {value} = await MainControls.lineReader.read();
  const spliter = value.split(" ");
  console.log(spliter);
  if (value) {
    const arrValue = [...spliter];
    const strValue = arrValue;
    let hexChordString = "";
    hexChordString = strValue[3];
    let hexAsciiString = "";
    hexAsciiString = strValue[4];
    const strValues = ["", "", "", ""];
    console.log("StrValue " + convertHexadecimalChordToHumanChord(hexChordString));
    strValues[0] = convertHexadecimalChordToHumanChord(hexChordString);
    strValues[1] = convertHexadecimalPhraseToAsciiString(hexAsciiString);
    strValues[2] = hexChordString;
    strValues[3] = hexAsciiString;
    _chordMaps.push([convertHexadecimalChordToHumanChord(hexChordString), strValues[1]]);
    appendToRow(strValues);
  }
}
export async function commitChordLayout() {
  console.log("readGetOneChordMapLayout()");
  const {value} = await MainControls.lineReader.read();
  console.log("Chord layout array " + value);
  if (value) {
    const arrValue = [...value];
    const strValue = String(arrValue.join(""));
    console.log(strValue);
    let hexChordString = "";
    hexChordString = strValue.substr(0, 16);
    let hexAsciiString = "";
    hexAsciiString = strValue.substr(17, strValue.length);
    const strValues = ["", "", "", ""];
    const myArray = value.split(" ");
    strValues[0] = myArray[1];
    strValues[1] = myArray[2];
    strValues[2] = myArray[3];
    strValues[3] = myArray[4];
    strValues[4] = myArray[5];
    strValues[5] = myArray[6];
    console.log("HEHEHEHEHEHHEEH " + myArray);
    _chordLayout.push(value);
    appendLayoutToRow(strValues);
  }
}
export async function readGetOneChordLayout() {
  console.log("readGetOneChordMapLayout()");
  const {value} = await MainControls.lineReader.read();
  console.log("Chord layout array " + value);
  if (value) {
    const arrValue = [...value];
    const strValue = String(arrValue.join(""));
    console.log(strValue);
    let hexChordString = "";
    hexChordString = strValue.substr(0, 16);
    let hexAsciiString = "";
    hexAsciiString = strValue.substr(17, strValue.length);
    const strValues = ["", "", "", ""];
    const myArray = value.split(" ");
    strValues[0] = myArray[1];
    strValues[1] = myArray[2];
    strValues[2] = myArray[3];
    strValues[3] = myArray[4];
    strValues[4] = myArray[5];
    strValues[5] = myArray[6];
    console.log("HEHEHEHEHEHHEEH " + myArray);
    _chordLayout.push(value);
    appendLayoutToRow(strValues);
  }
}
export function appendLayoutToRow(data2, isFromFile = false) {
  if (data2[4] != "2") {
    const dataTable2 = document.getElementById("layoutDataTable");
    const row = dataTable2.insertRow(-1);
    const cells = [];
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    cells.push(row.insertCell(-1));
    const btnEdit = document.createElement("div");
    const chordTextOrig = document.createElement("div");
    const phraseTextOrig = document.createElement("div");
    const chordTextNew = document.createElement("div");
    const phraseTextInput = document.createElement("div");
    const btnDelete = document.createElement("input");
    const btnRevert = document.createElement("input");
    const btnCommit = document.createElement("input");
    const virtualId = MainControls._chordMapIdCounter;
    console.log("ChordMap Counter: " + virtualId);
    cells[0].innerHTML = virtualId;
    cells[0].setAttribute("style", "border: 1px solid #D3D3D3;");
    MainControls._chordMapIdCounter++;
    chordTextOrig.id = virtualId.toString() + "-chordorig";
    chordTextOrig.innerHTML = data2[1];
    cells[2].appendChild(chordTextOrig);
    cells[2].setAttribute("style", "border: 1px solid #D3D3D3;");
    phraseTextOrig.id = virtualId.toString() + "-phraseorig";
    phraseTextOrig.innerHTML = data2[2];
    cells[3].appendChild(phraseTextOrig);
    cells[3].setAttribute("style", "border: 1px solid #D3D3D3;");
    chordTextNew.id = virtualId.toString() + "-chordnew";
    chordTextNew.innerHTML = data2[3];
    cells[4].appendChild(chordTextNew);
    cells[4].setAttribute("style", "border: 1px solid #D3D3D3; ");
    phraseTextInput.onchange = function() {
      const element = document.getElementById(virtualId.toString() + "-commit");
      element.disabled = false;
    };
    if (isFromFile) {
      phraseTextInput.value = data2[1];
    }
  }
}
export function appendToRow(data2, isFromFile = false) {
  const dataTable2 = document.getElementById("dataTable");
  const row = dataTable2.insertRow(-1);
  const cells = [];
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  cells.push(row.insertCell(-1));
  const btnEdit = document.createElement("input");
  const chordTextOrig = document.createElement("div");
  const phraseTextOrig = document.createElement("div");
  const chordTextNew = document.createElement("div");
  const phraseTextInput = document.createElement("input");
  const btnDelete = document.createElement("input");
  const btnRevert = document.createElement("input");
  const btnCommit = document.createElement("input");
  const virtualId = MainControls._chordMapIdCounter;
  console.log("ChordMap Counter: " + virtualId);
  cells[0].innerHTML = virtualId;
  cells[0].setAttribute("style", "border: 1px solid #D3D3D3;");
  MainControls._chordMapIdCounter++;
  btnEdit.id = virtualId.toString() + "-edit";
  btnEdit.type = "button";
  btnEdit.className = "buttonEdit";
  btnEdit.value = "edit chord";
  btnEdit.setAttribute("style", "background-color: #4CAF50;border: 1px solid white; color: white;padding: 1px 15px;text-align: center;text-decoration: none;display: inline-block; font-size: 16px;");
  cells[1].appendChild(btnEdit);
  cells[1].setAttribute("style", "border: 1px solid #D3D3D3;");
  btnEdit.onclick = async function() {
    const btn = document.getElementById(virtualId.toString() + "-edit");
    if (btn.value == "edit chord") {
      btn.value = "listening";
      await enableSerialChordOutput(true);
      const hexChord = await readGetHexChord();
      console.log("Listening Hex Chord " + convertHexadecimalChordToHumanString(hexChord));
      if (hexChord != null) {
        console.log(hexChord + " Original Hex Value");
        const element = document.getElementById(virtualId.toString() + "-chordnew");
        element.innerHTML = convertHexadecimalChordToHumanString(hexChord);
        const elementT = document.getElementById(virtualId.toString() + "-commit");
        elementT.disabled = false;
        console.log("hexChord is " + hexChord);
      }
      await enableSerialChordOutput(false);
    } else {
      console.log("cancelling lineReader");
      console.log(await MainControls.lineReader);
      await cancelReader();
      await setupLineReader();
      console.log("cancelled lineReader");
    }
    btn.value = "edit chord";
  };
  chordTextOrig.id = virtualId.toString() + "-chordorig";
  chordTextOrig.innerHTML = replaceOldAsciiKeys(data2[0]);
  console.log("Output of current chord " + data2);
  cells[2].appendChild(chordTextOrig);
  cells[2].setAttribute("style", "border: 1px solid #D3D3D3;");
  phraseTextOrig.id = virtualId.toString() + "-phraseorig";
  phraseTextOrig.innerHTML = data2[1];
  cells[3].appendChild(phraseTextOrig);
  cells[3].setAttribute("style", "border: 1px solid #D3D3D3;");
  chordTextNew.id = virtualId.toString() + "-chordnew";
  chordTextNew.innerHTML = "";
  cells[4].appendChild(chordTextNew);
  cells[4].setAttribute("style", "border: 1px solid #D3D3D3; ");
  phraseTextInput.id = virtualId.toString() + "-phraseinput";
  phraseTextInput.setAttribute("type", "text");
  phraseTextInput.setAttribute("style", "color:black");
  phraseTextInput.value = "";
  cells[5].setAttribute("style", "color: white; border: 1px solid white;border-right: 1px solid #D3D3D3;");
  cells[5].appendChild(phraseTextInput);
  cells[5].setAttribute("style", "border: 1px solid #D3D3D3;");
  phraseTextInput.onchange = function() {
    const element = document.getElementById(virtualId.toString() + "-commit");
    element.disabled = false;
  };
  btnDelete.id = virtualId.toString() + "-delete";
  btnDelete.type = "button";
  btnDelete.className = "buttonDelete";
  btnDelete.value = "delete";
  btnDelete.setAttribute("style", "background-color: #f44336; border: 1px solid white;color: white;padding: 1px 15px;text-align: center;text-decoration: none;display: inline-block;font-size: 16px;");
  cells[6].appendChild(btnDelete);
  cells[6].setAttribute("style", "border: 1px solid #D3D3D3;");
  btnDelete.onclick = function() {
    const element = document.getElementById(virtualId.toString() + "-chordnew");
    element.innerHTML = "DELETE";
    const elementDelete = document.getElementById(virtualId.toString() + "-delete");
    elementDelete.disabled = true;
    const elementCommit = document.getElementById(virtualId.toString() + "-commit");
    elementCommit.disabled = false;
  };
  btnRevert.id = virtualId.toString() + "-revert";
  btnRevert.type = "button";
  btnRevert.className = "buttonRevert";
  btnRevert.value = "revert";
  btnRevert.setAttribute("style", "background-color: green; border: 1px solid white; color: white; padding: 1px 15px; text-align: center; display: inline-block; font-size: 16px;");
  cells[7].appendChild(btnRevert);
  cells[7].setAttribute("style", "border: 1px solid #D3D3D3;");
  btnRevert.onclick = function() {
    const element = document.getElementById(virtualId.toString() + "-chordnew");
    element.innerHTML = "";
    const elementPhase = document.getElementById(virtualId.toString() + "-phraseinput");
    elementPhase.value = "";
    const elementDelete = document.getElementById(virtualId.toString() + "-delete");
    elementDelete.disabled = false;
    const elementCommit = document.getElementById(virtualId.toString() + "-commit");
    elementCommit.disabled = true;
  };
  btnCommit.id = virtualId.toString() + "-commit";
  btnCommit.type = "button";
  btnCommit.className = "buttonCommit";
  btnCommit.value = "commit";
  btnCommit.disabled = true;
  btnCommit.setAttribute("style", "border: 1px solid white;color: white;padding: 1px 15px;text-align: center;display: inline-block;font-size: 16px;hover: background: #00ff00;");
  cells[8].appendChild(btnCommit);
  btnCommit.onclick = async function(distinguisher) {
    const check = document.getElementById(virtualId.toString() + "-delete");
    if (check.disabled) {
      document.getElementById(virtualId.toString() + "-");
      await sendCommandString("CML C4 " + data2[2]);
      await readGetOneAndToss();
      const i = this.parentNode.parentNode.rowIndex;
      console.log("deleting row " + i.toString());
      dataTable2.deleteRow(i);
    } else {
      const chordNew = document.getElementById(virtualId.toString() + "-chordnew");
      if (chordNew.innerHTML.length > 0) {
        const phraseinput = document.getElementById(virtualId.toString() + "-phraseinput");
        if (phraseinput.value.length > 0) {
          const chordNewIn = document.getElementById(virtualId.toString() + "-chordnew");
          const phraseInputIn = document.getElementById(virtualId.toString() + "-phraseinput");
          const hexChord = await convertHumanChordToHexadecimalChord(chordNewIn.innerHTML);
          const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(phraseInputIn.value);
          await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
          console.log("ChordNew In" + chordNewIn.innerHTML);
          console.log("ChordNew In" + phraseInputIn.value);
          const chordorig2 = document.getElementById(virtualId.toString() + "-chordorig");
          const hexChordOrigToDelete = await convertHumanChordToHexadecimalChord(chordorig2.innerHTML);
          await sendCommandString("CML C4 " + hexChordOrigToDelete);
          await readGetOneAndToss();
          const phraseorig = document.getElementById(virtualId.toString() + "-phraseorig");
          const phraseinput2 = document.getElementById(virtualId.toString() + "-phraseinput");
          phraseorig.innerHTML = phraseinput2.value;
        } else {
          const element = document.getElementById(virtualId.toString() + "-chordnew");
          ;
          const elementPhase = document.getElementById(virtualId.toString() + "-phraseorig");
          ;
          const hexChord = await convertHumanChordToHexadecimalChord(element.innerHTML);
          const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(elementPhase.innerHTML);
          await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
          const s = elementPhase.innerHTML.split(",");
          await sendCommandString("VAR B4 A" + element.innerHTML + " " + s[0] + " " + s[1]);
          await readGetOneAndToss();
          const chordorig2 = document.getElementById(virtualId.toString() + "-chordorig");
          ;
          const hexChordOrigToDelete = await convertHumanChordToHexadecimalChord(chordorig2.innerHTML);
          await sendCommandString("CML C4 " + hexChordOrigToDelete);
        }
        const phraseinput3 = document.getElementById(virtualId.toString() + "-phraseinput");
        ;
        const chordorig = document.getElementById(virtualId.toString() + "-chordorig");
        ;
        const chordnew = document.getElementById(virtualId.toString() + "-chordnew");
        ;
        const delete2 = document.getElementById(virtualId.toString() + "-delete");
        ;
        const commit2 = document.getElementById(virtualId.toString() + "-commit");
        ;
        phraseinput3.value = "";
        chordorig.innerHTML = chordnew.innerHTML;
        chordnew.innerHTML = "";
        delete2.disabled = false;
        commit2.disabled = true;
      } else {
        const check2 = document.getElementById(virtualId.toString() + "-phraseinput");
        ;
        if (check2.value.length > 0) {
          const chordorig = document.getElementById(virtualId.toString() + "-chordorig");
          const phraseinput5 = document.getElementById(virtualId.toString() + "-phraseinput");
          ;
          const hexChord = await convertHumanChordToHexadecimalChord(chordorig.innerHTML);
          const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(phraseinput5.value);
          await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
          const phraseorig3 = document.getElementById(virtualId.toString() + "-phraseorig");
          ;
          const phraseinput3 = document.getElementById(virtualId.toString() + "-phraseinput");
          ;
          const chordnew = document.getElementById(virtualId.toString() + "-chordnew");
          ;
          const delete3 = document.getElementById(virtualId.toString() + "-delete");
          ;
          const commit3 = document.getElementById(virtualId.toString() + "-commit");
          ;
          phraseorig3.innerHTML = phraseinput3.innerHTML;
          phraseinput3.value = "";
          chordnew.innerHTML = "";
          delete3.disabled = false;
          commit3.disabled = true;
        }
      }
    }
  };
  if (isFromFile) {
    phraseTextInput.value = data2[1];
    btnCommit.disabled = false;
  }
}
export const asyncCallWithTimeout = async (asyncPromise, timeLimit, virtualId) => {
  let timeoutHandle;
  const commitButton = document.getElementById(virtualId.toString() + "-commit");
  const timeoutPromise = new Promise((_resolve, reject) => {
    timeoutHandle = setTimeout(() => _resolve(commitButton?.click()), timeLimit);
  });
  return Promise.race([asyncPromise, timeoutPromise]).then((result) => {
    clearTimeout(timeoutHandle);
    return result;
  });
};
export async function clickCommit(virtualId) {
  const check = document.getElementById(virtualId.toString() + "-delete");
  if (check.disabled) {
    document.getElementById(virtualId.toString() + "-");
    await sendCommandString("CML C4 " + data[2]);
    await readGetOneAndToss();
    const i = this.parentNode.parentNode.rowIndex;
    console.log("deleting row " + i.toString());
    dataTable.deleteRow(i);
  } else {
    const chordNew = document.getElementById(virtualId.toString() + "-chordnew");
    if (chordNew.innerHTML.length > 0) {
      const phraseinput = document.getElementById(virtualId.toString() + "-phraseinput");
      if (phraseinput.value.length > 0) {
        const chordNewIn = document.getElementById(virtualId.toString() + "-chordnew");
        const phraseInputIn = document.getElementById(virtualId.toString() + "-phraseinput");
        const hexChord = await convertHumanChordToHexadecimalChord(chordNewIn.innerHTML);
        const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(phraseInputIn.value);
        await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
        const chordorig2 = document.getElementById(virtualId.toString() + "-chordorig");
        const hexChordOrigToDelete = await convertHumanChordToHexadecimalChord(chordorig2.innerHTML);
        await sendCommandString("CML C4 " + hexChordOrigToDelete);
        const phraseorig = document.getElementById(virtualId.toString() + "-phraseorig");
        const phraseinput2 = document.getElementById(virtualId.toString() + "-phraseinput");
        phraseorig.innerHTML = phraseinput2.value;
      } else {
        const element = document.getElementById(virtualId.toString() + "-chordnew");
        ;
        const elementPhase = document.getElementById(virtualId.toString() + "-phraseorig");
        ;
        const hexChord = await convertHumanChordToHexadecimalChord(element.innerHTML);
        const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(elementPhase.innerHTML);
        await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
        const s = elementPhase.innerHTML.split(",");
        await sendCommandString("VAR B4 A" + element.innerHTML + " " + s[0] + " " + s[1]);
        const chordorig2 = document.getElementById(virtualId.toString() + "-chordorig");
        ;
        const hexChordOrigToDelete = await convertHumanChordToHexadecimalChord(chordorig2.innerHTML);
        await sendCommandString("CML C4 " + hexChordOrigToDelete);
      }
      const phraseinput3 = document.getElementById(virtualId.toString() + "-phraseinput");
      ;
      const chordorig = document.getElementById(virtualId.toString() + "-chordorig");
      ;
      const chordnew = document.getElementById(virtualId.toString() + "-chordnew");
      ;
      const delete2 = document.getElementById(virtualId.toString() + "-delete");
      ;
      const commit2 = document.getElementById(virtualId.toString() + "-commit");
      ;
      phraseinput3.value = "";
      chordorig.innerHTML = chordnew.innerHTML;
      chordnew.innerHTML = "";
      delete2.disabled = false;
      commit2.disabled = true;
    } else {
      const check2 = document.getElementById(virtualId.toString() + "-phraseinput");
      ;
      if (check2.value.length > 0) {
        const chordorig = document.getElementById(virtualId.toString() + "-chordorig");
        const phraseinput5 = document.getElementById(virtualId.toString() + "-phraseinput");
        ;
        const hexChord = await convertHumanChordToHexadecimalChord(chordorig.innerHTML);
        const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(phraseinput5.value);
        console.log("Chord Original " + chordorig);
        await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
        const phraseorig3 = document.getElementById(virtualId.toString() + "-phraseorig");
        ;
        const phraseinput3 = document.getElementById(virtualId.toString() + "-phraseinput");
        ;
        const chordnew = document.getElementById(virtualId.toString() + "-chordnew");
        ;
        const delete3 = document.getElementById(virtualId.toString() + "-delete");
        ;
        const commit3 = document.getElementById(virtualId.toString() + "-commit");
        ;
        phraseorig3.innerHTML = phraseinput3.innerHTML;
        phraseinput3.value = "";
        chordnew.innerHTML = "";
        delete3.disabled = false;
        commit3.disabled = true;
      }
    }
  }
  await readGetOneAndToss();
}
export function pressCommitButton(virtualId) {
  const commitButton = document.getElementById(virtualId.toString() + "-commit");
  clickCommit(virtualId);
}
export async function commitTo(virtualId) {
  const commitButton = document.getElementById(virtualId.toString() + "-commit");
  if (commitButton.disabled == false) {
    commitButton.click();
  }
  const chordorig = document.getElementById(virtualId.toString() + "-chordorig");
  const phraseinput5 = document.getElementById(virtualId.toString() + "-phraseinput");
  ;
  const hexChord = await convertHumanChordToHexadecimalChord(chordorig.innerHTML);
  const hexPhrase = await convertHumanPhraseToHexadecimalPhrase(phraseinput5.value);
  await sendCommandString("CML C3 " + hexChord + " " + hexPhrase);
  await readGetOneAndToss();
  console.log("Done sending command");
}
function convertHumanChordToHexadecimalChord(humanChord) {
  console.log("convertHumanChordToHexadecimalChord()");
  console.log(humanChord);
  let hexChord = "";
  const humanChordParts = humanChord.split(" + ");
  const decChordParts = [];
  humanChordParts.forEach((part) => {
    const actionCode = actionMap.indexOf(part);
    actionCode == -1 ? console.log("ActionCode does not exisit") : decChordParts.push(actionCode);
  });
  decChordParts.sort(function(a, b) {
    return b - a;
  });
  const chainIndex = 0;
  let binChord = pad(Dec2Bin(chainIndex), 8);
  for (let i = 0; i < decChordParts.length; i++) {
    if (i < 12) {
      binChord += pad(Dec2Bin(decChordParts[i]), 10);
    }
  }
  binChord = backpad(binChord, 128);
  console.log(binChord);
  for (let i = 0; i < 16; i++) {
    hexChord += pad(Bin2Hex(binChord.substring(i * 8, (i + 1) * 8)), 2);
  }
  hexChord = hexChord.toUpperCase();
  console.log("This is the hexChord " + hexChord);
  return hexChord;
}
function convertHumanPhraseToHexadecimalPhrase(humanPhrase) {
  console.log("convertHumanPhraseToHexadecimalPhrase()");
  console.log(humanPhrase);
  let hexPhrase = "";
  for (let i = 0; i < humanPhrase.length; i++) {
    const actionCode = humanPhrase.charCodeAt(i);
    const hexPhrasePart = pad(Dec2Hex(actionCode), 2);
    hexPhrase += hexPhrasePart;
  }
  hexPhrase = hexPhrase.toUpperCase();
  console.log("This is the hex human phrase " + hexPhrase);
  return hexPhrase;
}
export async function readGetNone() {
  console.log(" ");
}
