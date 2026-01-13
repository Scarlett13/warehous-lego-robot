Initial Positions JSON Files
=============================

This directory contains JSON files that define the initial positions for components in the Visual Components simulation.

Files:
------
1. RobotLocation.json - Initial positions for robots
2. ChargingStationLocation.json - Initial positions for charging stations (Idle Locations)
3. ConveyorLocation.json - Initial positions for conveyors
4. PathwayAreaLocation.json - Initial positions for pathway areas (includes AreaLength and AreaWidth)
5. OutputLocation.json - Initial position for output location

Format:
-------
Standard components (Robot, ChargingStation, Conveyor, OutputLocation):
[
  {
    "Name": "Component #1",
    "X": 5000,
    "Y": 3000,
    "Rz": 0
  }
]

Pathway Area (includes dimensions):
[
  {
    "Name": "Pathway Area #1",
    "X": -1564.49,
    "Y": -38542.24,
    "Rz": 90,
    "AreaLength": 12000,
    "AreaWidth": 5000
  }
]

How it works:
-------------
1. Config.java reads these JSON files at startup
2. SimpleNamespace creates OPC-UA variables for each component type
3. InitialPositions variable contains the full JSON string
4. Visual Components reads these variables and clones components accordingly
5. First entry in each JSON repositions the base component
6. Subsequent entries create clones

Notes:
------
- Coordinates are in millimeters
- Rz is rotation in degrees
- Files are loaded from JavaMAS/positions/ directory
- If a file is missing, an empty array "[]" is used

