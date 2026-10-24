# GhostHID Execution and Lifecycle Flows

This document details the control flows and execution sequences implemented in GhostHID.

## 1. Automated Windows Unlock and Login Flow

The login flow wakes the target Windows display, clears the lock screen overlay, tabs between credential fields, types the password from the secure vault, and submits the authentication form.

```mermaid
sequenceDiagram
    autonumber
    actor Operator as Operator / Agent
    participant CLI as ghid CLI
    participant Vault as macOS Keychain Vault
    participant ADB as ADB Transport
    participant Phone as HidService (Android)
    participant Host as Target Windows Host

    Operator->>CLI: ghid login [-u user -p pass]
    opt Credentials not passed via CLI
        CLI->>Vault: Get(username)
        Vault-->>CLI: return password
    end

    Note over CLI,Phone: Step 1: Wake Display & Lock Screen
    CLI->>ADB: Broadcast ACTION_CMD (combo "ctrl+alt+del")
    ADB->>Phone: sendCombo("ctrl+alt+del")
    Phone->>Host: HID Report ID 1 (Down: 0x05, 0x4C / Up: 0x00)
    CLI->>CLI: Sleep(delays.wake = 1.0s)

    Note over CLI,Phone: Step 2: Dismiss Screen Saver / Overlay
    CLI->>ADB: Broadcast ACTION_CMD (combo "esc")
    ADB->>Phone: sendCombo("esc")
    Phone->>Host: HID Report ID 1 (Down: 0x00, 0x29 / Up: 0x00)
    CLI->>CLI: Sleep(delays.overlay = 1.0s)

    opt Username Injection Needed
        Note over CLI,Phone: Step 3: Type Username
        CLI->>ADB: Broadcast ACTION_TYPE (text username)
        loop Each character in username
            ADB->>Phone: typeText(char)
            Phone->>Host: HID Report ID 1 (Key Down, Key Up)
        end
        CLI->>CLI: Sleep(delays.user = 1.2s)
        CLI->>ADB: Broadcast ACTION_CMD (combo "tab")
        ADB->>Phone: sendCombo("tab")
        Phone->>Host: HID Report ID 1 (Down: 0x00, 0x2B / Up: 0x00)
        CLI->>CLI: Sleep(delays.tab = 0.8s)
    end

    Note over CLI,Phone: Step 4: Type Password
    CLI->>ADB: Broadcast ACTION_TYPE (text password)
    loop Each character in password
        ADB->>Phone: typeText(char)
        Phone->>Host: HID Report ID 1 (Key Down, Key Up)
    end
    CLI->>CLI: Sleep(delays.pass = 1.2s)

    Note over CLI,Phone: Step 5: Submit Form
    CLI->>ADB: Broadcast ACTION_CMD (combo "enter")
    ADB->>Phone: sendCombo("enter")
    Phone->>Host: HID Report ID 1 (Down: 0x00, 0x28 / Up: 0x00)
    CLI->>CLI: Sleep(delays.submit = 1.0s)
    CLI-->>Operator: Login sequence completed
```

### Delay Configuration Defaults
Timings are configurable in `~/.config/ghid/config.json`:
- `delays.wake`: 1.0s (Wait for display panel to power up after Ctrl+Alt+Del).
- `delays.overlay`: 1.0s (Wait for Windows lock screen swipe layer to slide away after Esc).
- `delays.user`: 1.2s (Pacing after username entry).
- `delays.tab`: 0.8s (Wait for UI focus to settle into password input field).
- `delays.pass`: 1.2s (Pacing before final submission).
- `delays.submit`: 1.0s (Cooldown after Enter).

---

## 2. Combo and Action Engine Execution

The combo engine parses chained action strings containing keyboard chords, text typing steps, and mouse gestures separated by timed transitions (`->{seconds}`).

```mermaid
flowchart TD
    Input([Input String e.g. 'win+s ->{0.7} type:rustdesk ->{0.4} enter']) --> Split[Split string on '->']
    
    subgraph StepParsing["Per-Step Parsing"]
        Step[Step Segment] --> CheckType{Prefix 'type:' or 'text:'?}
        CheckType -->|Yes| ParseText[Extract literal string]
        CheckType -->|No| CheckMouse{Matches Mouse Alias?}
        CheckMouse -->|Yes| ParseMouse[Map to MouseAction: buttons, wheel]
        CheckMouse -->|No| ParseKeys[Split on '+' and validate Windows tokens]
        
        ParseText --> StepObj[Construct ComboStep]
        ParseMouse --> StepObj
        ParseKeys --> StepObj
    end

    Split --> Step
    StepObj --> ValidateGrammar{Validate Step Sequence}
    
    ValidateGrammar -->|Invalid Token or Alias| ReturnErr[Reject: Return Validation Error]
    ValidateGrammar -->|Valid| DispatchLoop[Iterate and Execute Steps]

    subgraph Execution["Execution Loop"]
        DispatchLoop --> ExecStep{Step Type}
        ExecStep -->|Keyboard Chord| SendKbd[Send Key Down Report -> Sleep 60ms -> Send Key Up Report]
        ExecStep -->|Text String| LoopChars[Loop Chars: Map ASCII -> Send Down/Up 25ms each]
        ExecStep -->|Mouse Action| SendMouse[Send Report ID 2 -> Sleep 45ms -> Send Report ID 2 Up]
        
        SendKbd --> StepDelay[Sleep for Step Duration]
        LoopChars --> StepDelay
        SendMouse --> StepDelay
        StepDelay --> HasMore{More Steps?}
        HasMore -->|Yes| DispatchLoop
        HasMore -->|No| Complete([Execution Complete])
    end

    classDef proc fill:#E8F8F5,stroke:#1ABC9C,stroke-width:2px,color:#0E6251
    classDef dec fill:#FEF9E7,stroke:#F39C12,stroke-width:2px,color:#7D6608
    classDef err fill:#FDEDEC,stroke:#E74C3C,stroke-width:2px,color:#78281F
    classDef start fill:#EBF5FB,stroke:#3498DB,stroke-width:2px,color:#1B4F72

    class Split,ParseText,ParseMouse,ParseKeys,StepObj,DispatchLoop,SendKbd,LoopChars,SendMouse,StepDelay proc
    class CheckType,CheckMouse,ValidateGrammar,ExecStep,HasMore dec
    class ReturnErr err
    class Input,Complete start
```

### HID Timing Details
- **Keystroke down/up report interval:** 60 ms hold time before zeroing report.
- **Character typing interval:** 25 ms down report hold, 25 ms up report hold (up to 20 characters per second).
- **Mouse click interval:** 45 ms down report hold, 45 ms up report hold.
- **Double click:** Sends two click sequences separated by an 80 ms pause.

---

## 3. Bluetooth Connection and SDP Registration Flow

When the Android service starts, it must acquire the Bluetooth HID profile proxy, register its SDP service record with the local Bluetooth daemon, and initiate connection with the target laptop.

```mermaid
sequenceDiagram
    autonumber
    participant CLI as ghid CLI
    participant ADB as ADB Transport
    participant Service as HidService
    participant BTAdapter as Android BluetoothAdapter
    participant BTDaemon as BluetoothHidDevice Proxy
    participant Host as Target Host Controller

    CLI->>ADB: am start-foreground-service com.sj010.ghid_headless/.HidService
    ADB->>Service: onCreate()
    Service->>Service: createNotificationChannel()
    Service->>Service: startForeground(1001, "GhostHID Gateway: Ready")
    Service->>BTAdapter: getProfileProxy(HID_DEVICE)
    BTAdapter-->>Service: onServiceConnected(BluetoothProfile proxy)
    
    Service->>BTDaemon: registerApp(SDP_SETTINGS, callback)
    BTDaemon->>Host: Broadcast SDP Record ("GhostHID Keyboard & Mouse")
    BTDaemon-->>Service: onAppStatusChanged(registered=true)
    Service->>Service: writeStatusFile("registered": true)

    CLI->>ADB: am broadcast ACTION_CMD --es cmd "connect" --es address "38:FC:98:E8:E3:87"
    ADB->>Service: connectTarget(address)
    Service->>BTDaemon: connect(targetDevice)
    BTDaemon->>Host: L2CAP Connection Request (PSM 0x11, 0x13)
    Host-->>BTDaemon: L2CAP Connection Accepted
    BTDaemon-->>Service: onConnectionStateChanged(STATE_CONNECTED)
    Service->>Service: writeStatusFile("connected": true, "state": "CONNECTED")
    Service->>Service: updateNotification("Connected to LP-TZD-0X114650")
    
    CLI->>ADB: cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json
    ADB-->>CLI: Return JSON (connected: true)
    CLI-->>CLI: Ready for command dispatch
```

---

## 4. ACPI Sleep and Zero-Battery Teardown Sequence

When putting a target computer to sleep, GhostHID executes a clean shutdown to ensure no battery drain occurs on the phone while the laptop sleeps.

```mermaid
sequenceDiagram
    autonumber
    actor Operator as Operator / Script
    participant CLI as ghid CLI
    participant ADB as ADB Transport
    participant Service as HidService
    participant BTDaemon as BluetoothHidDevice Proxy
    participant Target as Windows Target Host

    Operator->>CLI: ghid sleep --countdown 5
    
    Note over CLI,Target: Trigger Windows Sleep Sequence
    CLI->>ADB: Broadcast ACTION_CMD (combo "win+x")
    ADB->>Service: sendCombo("win+x")
    Service->>Target: Keystroke Win+X (Quick Link Menu)
    CLI->>CLI: Sleep(1.2s)
    
    CLI->>ADB: Broadcast ACTION_CMD (combo "u")
    ADB->>Service: sendCombo("u")
    Service->>Target: Keystroke U (Shut down or sign out sub-menu)
    CLI->>CLI: Sleep(0.6s)

    CLI->>ADB: Broadcast ACTION_CMD (combo "s")
    ADB->>Service: sendCombo("s")
    Service->>Target: Keystroke S (Enter Modern Standby / Sleep)
    
    Note over CLI: Operator Feedback Countdown
    loop Countdown 5 down to 1
        CLI->>Operator: Display countdown tick
        CLI->>CLI: Sleep(1.0s)
    end

    Note over CLI,Service: Clean Teardown
    CLI->>ADB: Broadcast ACTION_CMD --es cmd "stop"
    ADB->>Service: stopServiceCompletely()
    Service->>BTDaemon: disconnect(targetDevice)
    BTDaemon->>Target: Close L2CAP channels
    Service->>BTDaemon: unregisterApp()
    Service->>Service: stopForeground(STOP_FOREGROUND_REMOVE)
    Service->>Service: stopSelf()
    Service-->>ADB: Service process terminated
    CLI-->>Operator: Complete: Bluetooth disconnected, phone service stopped
```

---

## 5. System Diagnostic Loop (`ghid doctor`)

The diagnostic command verifies every component in the hardware and software pipeline, pinpointing configuration errors.

```mermaid
flowchart TD
    Start([Run ghid doctor]) --> CheckADB{ADB Binary in PATH?}
    CheckADB -->|No| FailADB[Issue: ADB binary not found]
    CheckADB -->|Yes| ScanDevices{Device connected via USB or Wi-Fi?}

    ScanDevices -->|No| FailDev[Issue: No Android devices detected]
    ScanDevices -->|Unauthorized| FailAuth[Issue: Device unauthorized - approve RSA key on phone]
    ScanDevices -->|Yes| CheckAPK{Package com.sj010.ghid_headless installed?}

    CheckAPK -->|No| FailAPK[Issue: Gateway APK not installed on device]
    CheckAPK -->|Yes| CheckBT{Phone Bluetooth Radio Enabled?}

    CheckBT -->|No| FailBT[Issue: Bluetooth disabled on phone]
    CheckBT -->|Yes| ReadStatus{Fetch status.json via ADB}

    ReadStatus -->|File missing| StartGateway[Trigger am start-foreground-service]
    StartGateway --> PollStatus[Wait for SDP registration]
    PollStatus --> CheckReg{Registered == true?}
    ReadStatus --> CheckReg

    CheckReg -->|No| FailReg[Issue: Bluetooth HID profile registration failed]
    CheckReg -->|Yes| CheckConn{Connected == true?}

    CheckConn -->|No| WarnConn[Notice: Gateway registered, not connected to target]
    CheckConn -->|Yes| CheckVault{macOS Keychain accessible?}

    WarnConn --> CheckVault
    CheckVault -->|No| WarnVault[Notice: Keychain inaccessible - memory vault active]
    CheckVault -->|Yes| CheckCreds{Windows credentials cached in vault?}

    CheckCreds --> Summary[Compile DoctorReport struct]
    WarnVault --> Summary
    FailADB --> Summary
    FailDev --> Summary
    FailAuth --> Summary
    FailAPK --> Summary
    FailBT --> Summary
    FailReg --> Summary

    Summary --> OutputFormat{JSON Flag passed?}
    OutputFormat -->|Yes| PrintJSON[Emit structured JSON]
    OutputFormat -->|No| PrintTUI[Render formatted doctor status card]

    classDef pass fill:#E8F8F5,stroke:#1ABC9C,stroke-width:2px,color:#0E6251
    classDef check fill:#EBF5FB,stroke:#3498DB,stroke-width:2px,color:#1B4F72
    classDef fail fill:#FDEDEC,stroke:#E74C3C,stroke-width:2px,color:#78281F
    classDef warn fill:#FEF9E7,stroke:#F39C12,stroke-width:2px,color:#7D6608

    class Start,CheckADB,ScanDevices,CheckAPK,CheckBT,ReadStatus,CheckReg,CheckConn,CheckVault,CheckCred,OutputFormat check
    class Summary,PrintJSON,PrintTUI pass
    class FailADB,FailDev,FailAuth,FailAPK,FailBT,FailReg fail
    class WarnConn,WarnVault warn
```


---

## 6. Unified Quick Send Flow (Type vs. Raw HID)

Quick Send provides a single unified entry point for ad-hoc keystrokes and low-level hex reports. The system defaults to string typing but switches to raw 16-hex transmission when toggled or flagged.

```mermaid
sequenceDiagram
    autonumber
    actor User as Operator / Agent
    participant Interface as TUI / CLI (`ghid quick`)
    participant Gateway as gateway.Gateway
    participant ADB as adb.Manager
    participant Phone as HidCommandReceiver
    participant Host as Target Computer

    alt Default Mode: Virtual Keyboard Type
        User->>Interface: Enter text string (e.g. "ghid quick hello world")
        Interface->>Gateway: Type("hello world")
        Gateway->>ADB: Broadcast ACTION_CMD (`--es cmd "type" --es text "hello world"`)
        ADB->>Phone: Relay type intent
        loop For each character in text
            Phone->>Host: HID Report ID 1 (Key Down scancode)
            Phone->>Host: HID Report ID 1 (Key Up / all zeros)
        end
        Interface-->>User: ✔ Typed text (11 characters)
    else Raw HID Report Mode
        User->>Interface: Toggle Tab in TUI or pass `--raw <16-hex>`
        Interface->>Interface: Validate exact 16-hex characters (8 bytes)
        alt Invalid Hex / Incorrect Length
            Interface-->>User: ✖ Error: Must be exactly 16 hex characters
        else Valid 16-Hex Report
            Interface->>Gateway: Raw("08000f0000000000")
            Gateway->>ADB: Broadcast ACTION_CMD (`--es cmd "raw" --es hex "08000f0000000000"`)
            ADB->>Phone: Relay raw intent
            Phone->>Host: Transmit raw 8-byte HID report
            Interface-->>User: ✔ Dispatched raw HID report
        end
    end
```

---

## 7. Snippet Variable Resolution & Intercept Flow

When executing macros containing dynamic variable references (e.g. `$USERNAME`, `$PASSWORD`), the execution engine resolves tokens at runtime through the registered `SnippetStore`. If referenced snippets are unpopulated, the interactive dashboard prompts the user immediately instead of failing.

```mermaid
flowchart TD
    Trigger([Trigger Combo e.g. Login Macro]) --> ExtractVars[hid.ExtractVariables: Scan for $VAR and ${VAR}]
    ExtractVars --> CheckVars{Any variables in combo?}

    CheckVars -->|No| ExecDirect[Execute Keystrokes & Gestures Directly]
    CheckVars -->|Yes| QueryStore[Query snippets.Store for each variable]

    QueryStore --> CheckValues{All variables present & non-empty?}

    CheckValues -->|No (Empty Snippets)| PromptUser[Intercept: Transition to StateLoginPrompt]
    PromptUser --> CaptureInput[User fills Username & Password in TUI]
    CaptureInput --> SaveSnippets[Save text & store secret in Keychain]
    SaveSnippets --> ResumeCombo[Auto-resume and re-trigger pending combo]

    CheckValues -->|Yes| Interpolate[hid.SubstituteVariables: Replace tokens with values]
    ResumeCombo --> Interpolate
    Interpolate --> DispatchADB[adb.Manager.SendComboWithResolver]
    DispatchADB --> SendHID[Phone injects resolved HID keystrokes into host]

    classDef normal fill:#E8F8F5,stroke:#1ABC9C,stroke-width:2px,color:#0E6251
    classDef prompt fill:#FEF9E7,stroke:#F39C12,stroke-width:2px,color:#7D6608
    classDef exec fill:#EBF5FB,stroke:#3498DB,stroke-width:2px,color:#1B4F72

    class Trigger,ExtractVars,CheckVars,QueryStore,Interpolate,DispatchADB,SendHID normal
    class PromptUser,CaptureInput,SaveSnippets,ResumeCombo prompt
    class ExecDirect exec
```
