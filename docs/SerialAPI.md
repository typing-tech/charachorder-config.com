# Serial API
The Serial API allows developers to add and remove chords and change configurational parameters on the CharaChorder device. The serial connection operates at a baud rate of 115200 bps. In general a success returns a 0 at the end, while a failure returns a number greater than zero, which represents an error code.


## Commands Overview
Commands are all caps ASCII characters. The return is always 1 line and includes the command in the return line along with some of the relevant input arguments as well. This makes it more restful and stateless as compared to previous versions of the SerialAPI.
| Dev Status | Command | Description |
| - | - | - |
| available | CMD | Lists available commands. |
| available | ID | Identifies device, such as 'CHARACHORDER ONE M0'. |
| available | VERSION | Returns the current firmware version, such as '1.5.16' |
| available | CML | Used for getting, setting (adding or overwriting), and deleting chordmaps. |
| available | VAR | Used for getting and settings parameters. This includes setting custom chordmaps. |
| available | RST | Restarts/reboots the microcontroller hardware. It has additional arguments for Factory and Bootloader. |
| available | RAM | prints the current amount of SRAM available. This is primarily used for debugging. |
| available | SIM | Simulates/injects a chord and outputs the chord output if the chord exists in the chord library. This is primarily used for debugging. |



________________________________
### CMD
The CMD command lists out all of the commands in the Serial API. This shouldn't be necessary if you already have this Serial API documentation though. All the commands are returned in one comma-delimited line. all commands are uppercase ASCII characters.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CMD | |
| OUTPUT | 0 | Command | Chars | CMD | |
| OUTPUT | 1 | Command List | Chars | CMD,ID,VERSION,CML,VAR,RST,RAM,SIM | comma delimited |

Example(s):
| I/O | Message |
| - | - |
| INPUT | CMD |
| OUTPUT | CMD CMD,ID,VERSION,CML,VAR,RST,RAM,SIM |

```python
ser.write(b'CMD\r\n');
```



________________________________
### ID
The ID command returns the ASCII name of the device, including the chipset code. This can be used to identify the correct serial device attached to the computer.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | ID | |
| OUTPUT | 0 | Command | Chars | ID | |
| OUTPUT | 1 | Company | Chars | CHARACHORDER | |
| OUTPUT | 2 | Device | Chars | ONE | ONE or LITE |
| OUTPUT | 3 | Chipset | Chars | M0 | M0 or S2 |

Example(s):
| I/O | Message |
| - | - |
| INPUT | ID |
| OUTPUT | ID CHARACHORDER ONE M0 |

```python
ser.write(b'ID\r\n');
```



________________________________
### VERSION
The VERSION command returns the current version of the firmware.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VERSION | |
| OUTPUT | 0 | Command | Chars | VERSION | |
| OUTPUT | 1 | Command List | Chars | 1.5.16 | period delimited of MAJOR.MINOR.BUILD |

Example(s):
| I/O | Message |
| - | - |
| INPUT | VERSION |
| OUTPUT | VERSION 1.5.16 |

```python
ser.write(b'VERSION\r\n');
```



________________________________
### CML
The CML command provides access to the Chordmap Library.

| CML SubCommand | Code | Description |
| - | - | - |
| GET_CHORDMAP_COUNT | C0 | Gets the (decimal) number of chordmaps. |
| GET_CHORDMAP_BY_INDEX | C1 | Gets a chordmap by the index number (hexadecimal uint16) if within range. |
| GET_CHORDMAP_BY_CHORD | C2 | Gets a chordmaps by the chord (hexadecimal) value if it is found in the library. |
| SET_CHORDMAP_BY_CHORD | C3 | Sets a chordmap with a chord and output bytes (hexadecimal). |
| DEL_CHORDMAP_BY_CHORD | C4 | Deletes a chordmap from the library if the chord exists. |

| GET_CHORDMAP_COUNT | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CML | |
| INPUT | 1 | SubCommand | Hexadecimal CML Code | C0 | get chordmap count |
| OUTPUT | 0 | Command | Chars | CML | |
| OUTPUT | 1 | SubCommand | Hexadecimal CML Code | C0 | |
| OUTPUT | 2 | Data Out | Decimal Number | 1347 | |

| GET_CHORDMAP_BY_INDEX | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CML | |
| INPUT | 1 | SubCommand | Hexadecimal CML Code | C1 | get chordmap by index |
| INPUT | 2 | Index | Decimal | 522 | |
| OUTPUT | 0 | Command | Chars | CML | |
| OUTPUT | 1 | SubCommand | Hexadecimal CML Code | C1 | |
| OUTPUT | 2 | Index | Decimal | 522 | |
| OUTPUT | 3 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | this will be 0 if index is out of bounds |
| OUTPUT | 4 | Phrase | Hexadecimal CCActionCodes List | 6361727065206469656D | "carpe diem"; this will be "0" if index is out of bounds |

| GET_CHORDMAP_BY_CHORD | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CML | |
| INPUT | 1 | SubCommand | Hexadecimal CML Code | C2 | get chordmap by chord |
| INPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | 0 | Command | Chars | CML | |
| OUTPUT | 1 | SubCommand | Hexadecimal CML Code | C2 | |
| OUTPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | 3 | Phrase | Hexadecimal CCActionCodes List | 6361727065206469656D | "carpe diem"; this will be "0" if chordmap is not in the library |

| SET_CHORDMAP_BY_CHORD | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CML | |
| INPUT | 1 | SubCommand | Hexadecimal CML Code | C3 | set chordmap by chord |
| INPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| INPUT | 3 | Phrase | Hexadecimal CCActionCodes List | 6361727065206469656D | "carpe diem" |
| OUTPUT | 0 | Command | Chars | CML | |
| OUTPUT | 1 | SubCommand | Hexadecimal CML Code | C3 | |
| OUTPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | 3 | Phrase | Hexadecimal CCActionCodes List | 6361727065206469656D | "carpe diem"; this will be "0" if there was a problem adding this chordmap to the library |
| OUTPUT | 4 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if the chordmap did not exist or the deletion was unsuccessful |

| DEL_CHORDMAP_BY_CHORD | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | CML | |
| INPUT | 1 | SubCommand | Hexadecimal CML Code | C4 | delete chordmap by chord |
| INPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | 0 | Command | Chars | CML | |
| OUTPUT | 1 | SubCommand | Hexadecimal CML Code | C4 | |
| OUTPUT | 2 | Chord | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | this will be "0" if the chordmap did not exist or the deletion was unsuccessful |
| OUTPUT | 3 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if the chordmap did not exist or the deletion was unsuccessful |




Example(s):
| GET_CHORDMAP_COUNT: | Message | Notes |
| - | - | - |
| INPUT | CML C0 | |
| OUTPUT | CML C0 1347 | |

| GET_CHORDMAP_BY_INDEX: | Message | Notes |
| - | - | - |
| INPUT | CML C1 522 | |
| OUTPUT | CML C1 000000000000C1AE46DED6731EC20F2A 6361727065206469656D | |

| GET_CHORDMAP_BY_CHORD: | Message | Notes |
| - | - | - |
| INPUT | CML C2 00000000E4E2B0160F84B20ACE7638C0 | |
| OUTPUT | CML C2 00000000E4E2B0160F84B20ACE7638C0 6361727065206469656D | |

| SET_CHORDMAP_BY_CHORD: | Message | Notes |
| - | - | - |
| INPUT | CML C3 00000000E4E2B0160F84B20ACE7638C0 6361727065206469656D | |
| OUTPUT | CML C3 00000000E4E2B0160F84B20ACE7638C0 6361727065206469656D 0 | |

| DEL_CHORDMAP_BY_CHORD: | Message | Notes |
| - | - | - |
| INPUT | CML C4 00000000E4E2B0160F84B20ACE7638C0 | |
| OUTPUT | CML C4 00000000E4E2B0160F84B20ACE7638C0 0 | |






________________________________
### VAR
The VAR command provides access to customizable parameters. This includes access to custom keymaps.

| VAR SubCommand | Code | Description |
| - | - | - |
| CMD_VAR_COMMIT | B0 | Commits any parameter changes to persistent memory. |
| CMD_VAR_GET_PARAMETER | B1 | Gets the value of a parameter. |
| CMD_VAR_SET_PARAMETER | B2 | Sets the value of a parameter. |
| CMD_VAR_GET_KEYMAP | B3 | Gets the value of a key in a keymap. |
| CMD_VAR_SET_KEYMAP | B4 | Sets the value of a key in a keymap. |

| Keymap Codes | Code | Description |
| - | - | - |
| Primary | A1 | The default primary keymap. In the CharaChorder One this is called the Alpha keymap, while on the CharaChorder Lite this defaults to a Qwerty layout. |
| Secondary | A2 | The default secondary keymap. In the CharaChorder One this is called the Num-shift keymap, while on the CharaChorder Lite this provides some additional function and numpad keys. |
| Tertiary | A3 | The default tertiary keymap. In the CharaChorder One this is called the Function keymap, while on the CharaChorder Lite this is a copy of the secondary keymap. |

| Parameter Codes | Code | Description |
| - | - | - |
| Enable Serial Header | 01 | boolean 0 or 1, default is 0 |
| Enable Serial Logging | 02 | boolean 0 or 1, default is 0 |
| Enable Serial Debugging | 03 | boolean 0 or 1, default is 0 |
| Enable Serial Raw | 04 | boolean 0 or 1, default is 0 |
| Enable Serial Chord | 05 | boolean 0 or 1, default is 0 |
| Enable Serial Keyboard | 06 | boolean 0 or 1, default is 0 |
| Enable Serial Mouse | 07 | boolean 0 or 1, default is 0 |
| Enable USB HID Keyboard | 11 | boolean 0 or 1, default is 1 |
| Enable Character Entry | 12 | boolean 0 or 1 |
| GUI-CTRL Swap Mode | 13 | Boolean 0 or 1; 1 swaps keymap 0 and 1. (CCL only) |
| Key Scan Duration | 14 | scan rate described in milliseconds; default is 5ms = 200Hz |
| Key Debounce Press Duration | 15 | debounce time in milliseconds; default is 20ms |
| Key Debounce Release Duration | 16 | debounce time in milliseconds; default is 20ms |
| Keyboard Output Character Microsecond Delays | 17 | delay time in microseconds; default is 50us |
| Enable USB HID Mouse | 21 | boolean 0 or 1; default is 1 |
| Slow Mouse Speed | 22 | pixels to move at the mouse poll rate; default is 2 = 100px/s |
| Fast Mouse Speed | 23 | pixels to move at the mouse poll rate; default is 10 = 500px/s |
| Enable Active Mouse | 24 | boolean 0 or 1; moves mouse back and forth every 60s |
| Mouse Scroll Speed | 25 | default is 1 |
| Mouse Poll Duration | 26 | poll rate described in milliseconds; default is 20ms = 50Hz |
| Enable Chording | 31 | boolean 0 or 1 |
| Enable Chording Character Counter Killer | 32 | boolean 0 or 1; default is 1 |
| Chording Character Counter Killer Timer | 33 | 0-255 deciseconds; default is 40 or 4.0 seconds |
| Chord Detection Press Tolerance(ms) | 34 | 1-50 milliseconds |
| Chord Detection Release Tolerance(ms) | 35 | 1-50 milliseconds |
| Enable Spurring | 41 | boolean 0 or 1; default is 1 |
| Enable Spurring Character Counter Killer | 42 | boolean 0 or 1; default is 1 |
| Spurring Character Counter Killer Timer | 43 | 0-255 seconds; default is 240 |
| Enable Arpeggiates | 51 | boolean 0 or 1; default is 1 |
| Arpeggiate Tolerance | 54 | in deciseconds; default 0.5s |
| Enable Compound Chording (coming soon) | 61 | boolean 0 or 1; default is 1 |
| Compound Tolerance | 64 | in deciseconds; default 1.5s |
| LED Brightness | 81 | 0-50 (CCL only) |
| LED Color Code | 82 | Color Codes to be listed (CCL only) |
| Enable LED Key Highlight (coming soon) | 83 | boolean 0 or 1 |
| Operating System | 91 | OS Codes to be listed |
| Enable Realtime Feedback | 92 | boolean 0 or 1; default is 1 |
| Enable CharaChorder Ready on startup | 93 | boolean 0 or 1; default is 1 |


| Operating System Codes | Code | Description |
| - | - | - |
| Windows | 0 | |
| Mac | 1 | |
| Linux | 2 | |
| iOS | 3 | |
| Android | 4 | |
| Unknown | 255 | |



| CMD_VAR_COMMIT: | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VAR | |
| INPUT | 1 | SubCommand | Hexadecimal VAR Code | B0 | commit parameters to memory |
| OUTPUT | 0 | Command | Chars | VAR | |
| OUTPUT | 1 | SubCommand | Hexadecimal VAR Code | B0 | |
| OUTPUT | 2 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if there was a problem commiting |

| CMD_VAR_GET_PARAMETER: | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VAR | |
| INPUT | 1 | SubCommand | Hexadecimal VAR Code | B1 | get parameter value |
| INPUT | 2 | Parameter Code | Hexadecimal Parameter Code | 2E | |
| OUTPUT | 0 | Command | Chars | VAR | |
| OUTPUT | 1 | SubCommand | Hexadecimal VAR Code | B1 | |
| OUTPUT | 2 | Parameter Code | Hexadecimal Parameter Code | 2E | |
| OUTPUT | 3 | Data Out | Decimal Number | 38 | |
| OUTPUT | 4 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if the VAR Code or Parameter Code doesn't exist |

| CMD_VAR_SET_PARAMETER: | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VAR | |
| INPUT | 1 | SubCommand | Hexadecimal VAR Code | B2 | set parameter value |
| INPUT | 2 | Parameter Code | Hexadecimal Parameter Code | 2E | |
| INPUT | 3 | Data In | Decimal Number | 46 | |
| OUTPUT | 0 | Command | Chars | VAR | |
| OUTPUT | 1 | SubCommand | Hexadecimal VAR Code | B2 | |
| OUTPUT | 2 | Parameter Code | Hexadecimal Parameter Code | 2E | |
| OUTPUT | 3 | Data Out | Decimal Number | 46 | will be a "00" (double zero) if the VAR Code or Parameter Code doesn't exist or the input value is out of range |
| OUTPUT | 4 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if there was a problem |

| CMD_VAR_GET_KEYMAP: | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VAR | |
| INPUT | 1 | SubCommand | Hexadecimal VAR Code | B3 | get keymap parameter value |
| INPUT | 2 | Keymap | Hexadecimal Keymap Code | A0 | |
| INPUT | 3 | Index | Decimal Number | 24 | For CC1, 0-89 are valid. For CCL, 0-66 are valid. |
| OUTPUT | 0 | Command | Chars | VAR | |
| OUTPUT | 1 | SubCommand | Hexadecimal VAR Code | B3 | |
| OUTPUT | 2 | Keymap | Hexadecimal Keymap Code | A0 | |
| OUTPUT | 3 | Index | Decimal Number | 24 | |
| OUTPUT | 4 | Action Id | Decimal Number | 111 | Valid action Ids range from 8 thru 2047. |
| OUTPUT | 5 | Success | Boolean Number | 0 | This will be "0" on success, or greater than zero for an error if either the Keymap Code or Index are out of range. |

| CMD_VAR_SET_KEYMAP: | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | VAR | |
| INPUT | 1 | SubCommand | Hexadecimal VAR Code | B4 | set keymap parameter value |
| INPUT | 2 | Keymap | Hexadecimal Keymap Code | A0 | |
| INPUT | 3 | Index | Decimal Number | 24 | For CC1, 0-89 are valid. For CCL, 0-66 are 
| INPUT | 4 | Action Id | Decimal Number | 112 | Valid action Ids range from 8 thru 2047. |
| OUTPUT | 0 | Command | Chars | VAR | |
| OUTPUT | 1 | SubCommand | Hexadecimal VAR Code | B3 | |
| OUTPUT | 2 | Keymap | Hexadecimal Keymap Code | A0 | |
| OUTPUT | 3 | Index | Decimal Number | 24 | |
| OUTPUT | 4 | Action Id | Decimal Number | 112 | Valid action Ids range from 8 thru 2047. Returns a "00" if either the Keymap Code or Index or Action Id are out of range. |
| OUTPUT | 5 | Success | Boolean Number | 1 | This will be "0" on success, or greater than zero for an error if the chordmap did not exist or the deletion was unsuccessful |



Example(s):
| CMD_VAR_COMMIT: | Message | Notes |
| - | - | - |
| INPUT | VAR B0 | |
| OUTPUT | VAR B0 1 | |

| CMD_VAR_GET_PARAMETER: | Message | Notes |
| - | - | - |
| INPUT | VAR B1 2E | |
| OUTPUT | VAR B1 2E 38 0 | |

| CMD_VAR_SET_PARAMETER: | Message | Notes |
| - | - | - |
| INPUT | VAR B2 2E 46 | |
| OUTPUT | VAR B2 2E 46 0 | |

| CMD_VAR_GET_KEYMAP: | Message | Notes |
| - | - | - |
| INPUT | VAR B3 A0 24 | |
| OUTPUT | VAR B3 A0 24 111 0 | |

| CMD_VAR_SET_KEYMAP: | Message | Notes |
| - | - | - |
| INPUT | VAR B2 A0 24 112 | |
| OUTPUT | VAR B2 A0 24 112 0 | |




________________________________
### RST
The RST command restart the CharaChorder device. This will most likely also break the current Serial connection, and a new connection will need to be made. If the COMMIT command has not been called before a RESTART command, then the device will revert to the last settings stored in the non-volatile memory.
- The RESTART subcommand restarts the microcontroller.
- The FACTORY subcommand performs a factory reset of the flash and emulated eeprom. During the process, the flash chip is erased.
- The BOOTLOADER subcommand restarts the device into a bootloader mode.
- The PARAMS subcommand resets the parameters to factory defaults and commits.
- The KEYMAPS subcommand resets the keymaps to the factory defaults and commits.
- The STARTER subcommand adds starter chordmaps. This does not clear the chordmap library, but adds to it, replacing those that have the same chord.
- The CLEARCML subcommand permanently deletes all the chordmaps stored in the device memory.
- (in progress) The UPGRADECML subcommand attemps to upgrade chordmaps that the system detects are older.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | RST | |
| OUTPUT | 0 | Command | Chars | RST | without optional command, this just restarts the device |
| OUTPUT | 1 | Option | Chars | BOOTLOADER | BOOTLOADER (resets to bootloader) or FACTORY (factory reset chordmap library and parameters) |

Example(s):
| I/O | Message |
| - | - |
| RESTART: | - |
| INPUT | RST |
| OUTPUT | RST |
| BOOTLOADER: | - |
| INPUT | RST BOOTLOADER |
| OUTPUT | RST BOOTLOADER |
| FACTORY: | - |
| INPUT | RST STARTER |
| OUTPUT | RST STARTER |

```python
ser.write(b'RST BOOTLOADER\r\n');
```



________________________________
### RAM
The RAM command returns the current number of bytes availabe in SRAM. This is useful for debugging when there is a suspected heap or stack issue.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | RAM | |
| OUTPUT | 0 | Command | Chars | RAM |  |
| OUTPUT | 1 | Bytes Available | Decimal | 425 | |

Example(s):
| I/O | Message |
| - | - |
| INPUT | RAM |
| OUTPUT | RAM 425 |

```python
ser.write(b'RAM\r\n');
```



________________________________
### SIM
The The SIM command provides a way to inject a chord or key states to be processed by the device. This is primarily used for debugging.

| I/O | Index | Name | Type | Example | Notes |
| - | - | - | - | - | - |
| INPUT | 0 | Command | Chars | SIM | |
| INPUT | 1 | SubCommand | Chars | CHORD | CHORD or KEYSTATE; may change this to hexadecimal codes |
| INPUT | 2 | Data In | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | chords should be 32 characters |
| OUTPUT | 0 | Command | Chars | SIM |  |
| OUTPUT | 1 | SubCommand | Chars | CHORD | |
| OUTPUT | 2 | Data In | Hexadecimal Number | 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | 3 | Data Out | Hexadecimal CCActionCodes List | 6361727065206469656D | "carpe diem" |


Example(s):
| I/O | Message | Notes |
| - | - | - |
| INPUT | SIM CHORD 000000000000C1AE46DED6731EC20F2A | |
| OUTPUT | SIM CHORD 000000000000C1AE46DED6731EC20F2A 6361727065206469656D | |
| - | - | - |
| INPUT | SIM CHORD 00000000E4E2B0160F84B20ACE7638C0 | |
| OUTPUT | SIM CHORD 00000000E4E2B0160F84B20ACE7638C0 0 | returns a 0 if there's no chordmap in the library |

```python
ser.write(b'SIM CHORD 000000000000C1AE46DED6731EC20F2A\r\n');
```




________________________________
