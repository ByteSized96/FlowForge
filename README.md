#  FlowForge

**FlowForge** is a lightweight, visual desktop automation builder built with Kotlin and Jetpack Compose.

Create, run, and schedule automation flows using a simple block-based interface  no heavy frameworks, no cloud dependency, just fast local execution.

---

##  Features

###  Visual Flow Builder
- Drag & reorder blocks
- Clean, modern UI
- Simple block-based automation system

###  Automation Blocks
- Manual Trigger
- Open Website
- Take Screenshot
- Create File
- Show Notification
- If File Exists
- Open Application

###  Execution Engine
- Step-by-step execution
- Active block highlighting during runtime
- Detailed execution logs

###  Scheduler
- Run flows automatically
- Repeat every:
  - X seconds
  - X minutes
- Start / Stop toggle
- Tracks:
  - Next run time
  - Last run time
  - Total run count

###  Quick Start Templates
- Screenshot Saver
- Website Launcher
- File Creator
- Morning Startup

---

##  Tech Stack

- **Kotlin**
- **Jetpack Compose (Desktop)**
- **Coroutines**
- Custom automation engine

---

##  Installation

### Requirements
- JDK 17+
- IntelliJ IDEA (recommended)

### Run the app

```bash
git clone https://github.com/your-username/flowforge.git
cd flowforge
./gradlew run
