from vcScript import *
import vcMatrix

# Get component reference
comp = getComponent()
app = getApplication()

# Global variables
vehicle = None
current_location = ""
previous_target = ""


def OnStart():
    """Called when simulation starts"""
    global vehicle
    
    # Create or get required properties
    location_prop = comp.getProperty("Location")
    if not location_prop:
        location_prop = comp.createProperty(VC_STRING, "Location")
    location_prop.Value = ""
    
    target_prop = comp.getProperty("Target")
    if not target_prop:
        target_prop = comp.createProperty(VC_STRING, "Target")
    target_prop.Value = ""
    
    battery_prop = comp.getProperty("BatteryLevel")
    if not battery_prop:
        battery_prop = comp.createProperty(VC_INTEGER, "BatteryLevel")
    battery_prop.Value = 100
    
    has_product_prop = comp.getProperty("HasProduct")
    if not has_product_prop:
        has_product_prop = comp.createProperty(VC_BOOLEAN, "HasProduct")
    has_product_prop.Value = False
    
    print(" Robot properties created/verified: Location, Target, BatteryLevel, HasProduct")
    
    # Get or add Vehicle behavior
    vehicle = comp.findBehaviour("Vehicle")
    if not vehicle:
        print(" Vehicle behavior not found, attempting to add it...")
        try:
            vehicle = comp.createBehaviour(VC_VEHICLE, "Vehicle")
            print(" Vehicle behavior added successfully!")
        except:
            print(" ERROR: Could not create Vehicle behavior. Please add it manually.")
            return
    
    if not vehicle:
        print(" ERROR: No Vehicle behavior available!")
        return
    
    # Initialize vehicle properties
    vehicle.Acceleration = 2000.0  # mm/s^2
    vehicle.Deceleration = 2000.0  # mm/s^2
    vehicle.MaxSpeed = 800.0       # mm/s
    
    # Connect to OnMovementFinished event
    vehicle.OnMovementFinished = onVehicleArrived
    
    print(" Robot " + comp.Name + " initialized successfully!")


def onVehicleArrived(comp, time):
    """Called when vehicle reaches target"""
    # Update Location property
    location_prop = comp.getProperty("Location")
    target_prop = comp.getProperty("Target")
    
    if location_prop and target_prop:
        target = target_prop.Value
        if target and target != "":
            location_prop.Value = target
            print("✅ " + comp.Name + " arrived! Location updated to: " + target)


def OnRun():
    """Main simulation loop - runs continuously"""
    global previous_target, current_location
    
    while True:
        if not vehicle:
            delay(1.0)
            continue
        
        try:
            # Read properties (these are connected to OPC-UA via VC Connectivity tab)
            location_prop = comp.getProperty("Location")
            target_prop = comp.getProperty("Target")
            battery_prop = comp.getProperty("BatteryLevel")
            has_product_prop = comp.getProperty("HasProduct")
            
            if not location_prop or not target_prop:
                delay(1.0)
                continue
            
            # Get current values
            current_location = location_prop.Value if location_prop.Value else ""
            target = target_prop.Value if target_prop.Value else ""
            battery = battery_prop.Value if battery_prop else 100
            has_product = has_product_prop.Value if has_product_prop else False
            
            # Only move if target changed and contains coordinates
            if target and target != "" and target != previous_target and ";" in target:
                coords = target.split(";")
                if len(coords) == 3:
                    try:
                        target_x = float(coords[0])
                        target_y = float(coords[1])
                        target_z = float(coords[2])
                        move_to_location(target_x, target_y, target_z)
                        previous_target = target
                        print(" " + comp.Name + " moving to: " + target)
                    except ValueError:
                        pass
            
            # Check if we arrived at target
            if target and target != "" and ";" in target:
                coords = target.split(";")
                if len(coords) == 3:
                    try:
                        target_x = float(coords[0])
                        target_y = float(coords[1])
                        target_z = float(coords[2])
                        
                        robot_pos = comp.WorldPositionMatrix.P
                        import math
                        distance = math.sqrt((robot_pos.X - target_x)**2 + (robot_pos.Y - target_y)**2)
                        
                        # Only confirm arrival when truly on top of the target
                        if distance < 50 and location_prop.Value != target:
                            location_prop.Value = target
                            print("✅ " + comp.Name + " arrived at: " + target + " (distance: " + str(int(distance)) + "mm)")
                    except ValueError:
                        pass
        
        except Exception as e:
            print("❌ ERROR in " + comp.Name + ": " + str(e))
        
        delay(0.5)


def OnReset():
    """Called when simulation resets"""
    global previous_target
    previous_target = ""
    
    # Reset all properties to initial values
    location_prop = comp.getProperty("Location")
    if location_prop:
        location_prop.Value = ""
    
    target_prop = comp.getProperty("Target")
    if target_prop:
        target_prop.Value = ""
    
    battery_prop = comp.getProperty("BatteryLevel")
    if battery_prop:
        battery_prop.Value = 100
    
    has_product_prop = comp.getProperty("HasProduct")
    if has_product_prop:
        has_product_prop.Value = False
    
    print(" " + comp.Name + " reset - all properties cleared")


def move_to_location(target_x, target_y, target_z):
    """Move robot to specified coordinates using Vehicle behavior"""
    vehicle.clearMove()
    
    import vcVector
    target_pos = vcVector.new(target_x, target_y, target_z)
    
    vehicle.addControlPoint(target_pos)