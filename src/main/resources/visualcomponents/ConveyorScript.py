from vcScript import *
import vcMatrix

comp = getComponent()
app = getApplication()

blockGeo = None
path = None
originalPathFrames = None

def OnStart():
  global path, originalPathFrames
  
  # Create or get 'Produced' property
  produced_prop = comp.getProperty("Produced")
  if not produced_prop:
    produced_prop = comp.createProperty(VC_BOOLEAN, "Produced")
  produced_prop.Value = False
  
  path = comp.findBehaviour("Path")
  if not path:
    print("Path behavior not found!")
    return
  
  # Store original path frames (only once)
  if originalPathFrames is None:
    originalPathFrames = list(path.Path)
  
  # REVERSE THE PATH FRAMES
  reversedPath = []
  for i in range(len(originalPathFrames)-1, -1, -1):
    reversedPath.append(originalPathFrames[i])
  path.Path = reversedPath
  
  path.PathAxis = VC_PATH_AXIS_AUTOMATIC
  path.Enabled = True
  path.RetainOffset = False
  path.Speed = comp.getProperty('ConveyorSpeed').Value if comp.getProperty('ConveyorSpeed').Value > 0 else 500
  path.update()

def OnRun():
  global blockGeo, path
  
  # Wait for Produced to become True
  produced_prop = comp.getProperty("Produced")
  while True:
    if produced_prop and produced_prop.Value:
      break
    delay(0.1)
  
  # Now clone and start the block
  originalBlock = app.findComponent("Block Geo")
  
  if originalBlock and not blockGeo:
    blockGeo = originalBlock.clone(1)
    blockGeo.Name = "Block Geo Clone"
    
    conveyorHeight = comp.getProperty("ConveyorHeight").Value
    
    movementOrigin = vcMatrix.new()
    movementOrigin.P.Z = conveyorHeight + 50
    blockGeo.MovementOrigin = movementOrigin
    
    startDistance = 200
    startMatrix = path.getPathPosition(startDistance)
    blockGeo.PositionMatrix = comp.WorldPositionMatrix * startMatrix
    
    delay(0.1)
    path.grab(blockGeo)
    
    print("Block cloned and started moving on REVERSED conveyor")
  
  # Monitor block movement
  conveyorLength = comp.getProperty("ConveyorLength").Value
  stopDistance = conveyorLength - 200
  
  while True:
    if blockGeo and blockGeo.Container == path:
      distance = blockGeo.getPathDistance()
      
      if distance >= stopDistance:
        blockGeo.stopMovement(True)
        print("Block stopped at end")
        break
    else:
      return
    
    delay(0.05)
  
  # Keep block stopped
  while True:
    if blockGeo and blockGeo.Container == path:
      blockGeo.stopMovement(True)
    delay(0.5)

def OnReset():
  global blockGeo
  
  # Reset Produced property to False
  produced_prop = comp.getProperty("Produced")
  if produced_prop:
    produced_prop.Value = False
  
  # Remove the cloned block if it exists
  if blockGeo:
    try:
      clonedComp = app.findComponent("Block Geo Clone")
      if clonedComp:
        app.deleteComponent(clonedComp)
        print("Cloned block removed on reset")
    except:
      pass
    blockGeo = None