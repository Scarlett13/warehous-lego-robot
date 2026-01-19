from vcScript import *
import vcMatrix
import vcVector
import math
import heapq
import os
import json

# ==================== DATA CONFIGURATION ====================
DATA_PATHWAY = [
                 {
                   "isOccupied": False,
                   "Name": "Path_Input_Conveyor",
                   "X": 65300.0,
                   "Y": 147500.0,
                   "Rz": 0,
                   "AreaLength": 15000.0,
                   "AreaWidth": 15000.0
                 },
                 {
                   "isOccupied": False,
                   "Name": "Path_Fresh_Output",
                   "X": 126000.0,
                   "Y": 151000.0,
                   "Rz": 0,
                   "AreaLength": 15000.0,
                   "AreaWidth": 15000.0
                 },
                 {
                   "isOccupied": False,
                   "Name": "Path_Rotten_Output",
                   "X": 127200.0,
                   "Y": 140700.0,
                   "Rz": 0,
                   "AreaLength": 15000.0,
                   "AreaWidth": 15000.0
                 },
                 {
                   "isOccupied": False,
                   "Name": "Path_CS1",
                   "X": 63800.0,
                   "Y": 137200.0,
                   "Rz": 0,
                   "AreaLength": 12000.0,
                   "AreaWidth": 12000.0
                 },
                 {
                   "isOccupied": False,
                   "Name": "Path_CS2",
                   "X": 52800.0,
                   "Y": 147200.0,
                   "Rz": 0,
                   "AreaLength": 12000.0,
                   "AreaWidth": 12000.0
                 },
                 {
                   "isOccupied": False,
                   "Name": "Main_Highway_Connector",
                   "X": 96250.0,
                   "Y": 147500.0,
                   "Rz": 0,
                   "AreaLength": 62000.0,
                   "AreaWidth": 20000.0
                 }
               ]

DATA_ROBOT = [
  {"Name": "TinkyWinky", "X": 63800, "Y": 137200, "Rz": 0}
]

DATA_CONVEYOR = [
  {"Name": "Input Location", "X": 65300, "Y": 147500, "Rz": 0}
]

DATA_OUTPUT = [
  {"Name": "Fresh Output", "X": 126000, "Y": 151000, "Rz": 0},
  {"Name": "Rotten Output", "X": 127200, "Y": 140700, "Rz": 0}
]

DATA_CHARGING = [
  {"Name": "Charging Station", "X": 63800, "Y": 137200, "Rz": 0},
  {"Name": "Idle Location #2", "X": 52800, "Y": 147200, "Rz": 180}
]

# ==================== SETUP CONSTANTS ====================
VISUAL_COMPONENTS_VERSIONS = ["4.10", "4.9", "4.8", "4.7", "4.6", "4.5", "4.4", "4.3", "4.2", "4.1", "4.0"]
VISUAL_COMPONENTS_PATH = "C:\\Users\\Public\\Documents\\Visual Components\\"

# ==================== SCRIPTS ====================

# 1. ROBOT SCRIPT
RobotScript = '''from vcScript import *
import vcMatrix
import vcVector
import math
import heapq
import json

manager = getComponent()
app = getApplication()
pathway_graph = None
robot_states = {}
cloned_robots = []
robot_quantity = 0

def get_local_coordinates(component, x, y):
    mtx = component.WorldPositionMatrix
    inv_mtx = mtx
    inv_mtx.invert()
    vec = vcVector.new(x, y, mtx.P.Z)
    local_vec = inv_mtx * vec
    return local_vec.X, local_vec.Y

def is_point_in_area(area_comp, x, y):
    if not area_comp: return False
    l_prop = area_comp.getProperty("AreaLength")
    w_prop = area_comp.getProperty("AreaWidth")
    if not l_prop or not w_prop: return False
    length = l_prop.Value
    width = w_prop.Value
    loc_x, loc_y = get_local_coordinates(area_comp, x, y)
    tol = 10.0
    return (-(length/2+tol) <= loc_x <= (length/2+tol)) and (-(width/2+tol) <= loc_y <= (width/2+tol))

class PathwayNode:
    def __init__(self, area_component):
        self.area = area_component
        self.name = area_component.Name
        self.pos = area_component.WorldPositionMatrix.P
        self.x = self.pos.X
        self.y = self.pos.Y
        self.z = self.pos.Z
        self.neighbors = []
        self.portals = {}
        self.is_occupied = False

    def distance_to(self, other):
        return math.sqrt((self.x - other.x)**2 + (self.y - other.y)**2)
        
    def set_occupied(self, status):
        prop = self.area.getProperty("isOccupied")
        if prop and prop.Value != status:
            prop.Value = status

class PathwayGraph:
    def __init__(self):
        self.nodes = []
        self.build_graph()

    def get_bounds(self, node):
        l = node.area.getProperty("AreaLength").Value / 2.0
        w = node.area.getProperty("AreaWidth").Value / 2.0
        m = node.area.WorldPositionMatrix
        
        # Determine rotation (0/180 or 90/270)
        rz = m.P.W # Simplification for pure Z rotation if available, else standard matrix check
        # Using corners to be safe regardless of matrix storage
        
        c1 = m * vcVector.new(l, w, 0)
        c2 = m * vcVector.new(l, -w, 0)
        c3 = m * vcVector.new(-l, -w, 0)
        c4 = m * vcVector.new(-l, w, 0)
        
        xs = [c1.X, c2.X, c3.X, c4.X]
        ys = [c1.Y, c2.Y, c3.Y, c4.Y]
        
        return min(xs), max(xs), min(ys), max(ys)

    def get_overlap_center(self, n1, n2):
        minx1, maxx1, miny1, maxy1 = self.get_bounds(n1)
        minx2, maxx2, miny2, maxy2 = self.get_bounds(n2)
        
        overlap_min_x = max(minx1, minx2)
        overlap_max_x = min(maxx1, maxx2)
        overlap_min_y = max(miny1, miny2)
        overlap_max_y = min(maxy1, maxy2)
        
        if overlap_min_x <= overlap_max_x and overlap_min_y <= overlap_max_y:
            cx = (overlap_min_x + overlap_max_x) / 2.0
            cy = (overlap_min_y + overlap_max_y) / 2.0
            # Use higher Z to clear potential floor z-fighting
            z = max(n1.z, n2.z) 
            return vcVector.new(cx, cy, z)
        
        # Fallback to simple average if no geometric overlap found (shouldn't happen if connected)
        return vcVector.new((n1.x+n2.x)/2, (n1.y+n2.y)/2, (n1.z+n2.z)/2)

    def build_graph(self):
        print("Building Graph...")
        comps = []
        if app.Components:
            for c in app.Components:
                if c.Name.startswith("Pathway Area"):
                    comps.append(c)
        
        if not comps: return
        self.nodes = [PathwayNode(c) for c in comps]
        connections = 0
        for i, n1 in enumerate(self.nodes):
            for j, n2 in enumerate(self.nodes):
                if i < j and self.are_connected(n1, n2):
                    mid = self.get_overlap_center(n1, n2)
                    n1.neighbors.append(n2)
                    n1.portals[n2] = mid
                    n2.neighbors.append(n1)
                    n2.portals[n1] = mid
                    connections += 1
        print("Graph: " + str(len(self.nodes)) + " nodes, " + str(connections) + " connections")

    def are_connected(self, n1, n2):
        # Stricter check: do the actual bounds overlap? 
        # is_point_in_area checks corners, which is good for containment, but 
        # let's verify connection via geometric overlap logic implicit in get_overlap_center
        # Re-using the logic:
        minx1, maxx1, miny1, maxy1 = self.get_bounds(n1)
        minx2, maxx2, miny2, maxy2 = self.get_bounds(n2)
        
        x_overlap = max(0, min(maxx1, maxx2) - max(minx1, minx2))
        y_overlap = max(0, min(maxy1, maxy2) - max(miny1, miny2))
        
        # Require a small tolerance overlap to call it connected
        return x_overlap > 1.0 and y_overlap > 1.0

    def get_corners(self, node):
        l = node.area.getProperty("AreaLength").Value / 2.0
        w = node.area.getProperty("AreaWidth").Value / 2.0
        m = node.area.WorldPositionMatrix
        c1 = m * vcVector.new(l, w, 0)
        c2 = m * vcVector.new(l, -w, 0)
        c3 = m * vcVector.new(-l, -w, 0)
        c4 = m * vcVector.new(-l, w, 0)
        return [(c.X, c.Y) for c in [c1,c2,c3,c4]]

    def find_node(self, x, y):
        for node in self.nodes:
            if is_point_in_area(node.area, x, y): return node
        return None

    def get_path(self, sx, sy, tx, ty):
        start = self.find_node(sx, sy)
        end = self.find_node(tx, ty)
        if not start: 
            print("  Path Error: Start not on pathway")
            return None
        if not end:
            print("  Path Error: Target not on pathway")
            return None
        
        open_set = []
        heapq.heappush(open_set, (0, 0, start, []))
        visited = set()
        count = 0
        g_score = {node.name: float('inf') for node in self.nodes}
        g_score[start.name] = 0
        
        while open_set:
            _, _, current, path = heapq.heappop(open_set)
            
            if current == end:
                return self.waypoints(path + [current], tx, ty)
            
            if current.name in visited: continue
            visited.add(current.name)
            
            for nb in current.neighbors:
                prop = nb.area.getProperty("isOccupied")
                is_occ = prop.Value if prop else False
                
                # Allow entering the goal even if it's currently occupied
                if is_occ and nb != end: continue
                
                tentative_g = g_score[current.name] + current.distance_to(nb)
                if tentative_g < g_score[nb.name]:
                    g_score[nb.name] = tentative_g
                    f_score = tentative_g + nb.distance_to(end)
                    count += 1
                    heapq.heappush(open_set, (f_score, count, nb, path + [current]))
        return None

    def waypoints(self, path_nodes, tx, ty):
        wps = []
        for i in range(len(path_nodes)-1):
            portal = path_nodes[i].portals.get(path_nodes[i+1])
            if portal: wps.append(portal)
        wps.append(vcVector.new(tx, ty, path_nodes[-1].z))
        return wps

def clone_robots():
    global cloned_robots
    prop = manager.getProperty("InitialPositions")
    if not prop or not prop.Value: return
    base = app.findComponent("Mobile Robot Resource")
    if not base: return
    try: positions = eval(prop.Value)
    except: return
    
    for i, p in enumerate(positions):
        comp = base if i==0 else base.clone()
        name = p.get('Name', 'Robot')
        comp.Name = name
        comp.Visible = True
        
        # Read OPC UA properties for X and Y override
        px = manager.getProperty(name + "posX")
        py = manager.getProperty(name + "posY")
        
        # Use property value if available and non-zero (assuming 0,0 is uninitialized), else config
        x = px.Value if px and px.Value != 0 else p.get('X', 0)
        y = py.Value if py and py.Value != 0 else p.get('Y', 0)

        m = vcMatrix.new()
        m.rotateAbsZ(p.get('Rz',0))
        m.translateAbs(x, y, 0)
        comp.PositionMatrix = m
        if i > 0: cloned_robots.append(comp)
    app.render()

def OnStart():
    global robot_states
    robot_states = {}
    
    try:
        if not app.findMaterial("red_transparent"):
            m = app.createMaterial("red_transparent")
            m.DiffuseColor = vcVector.new(1,0,0,1)
            m.Transparency = 0.5
    except: pass
        
    if not manager.getProperty("InitialPositions"): manager.createProperty(VC_STRING, "InitialPositions")
    
    # [NEW] Share Schema with Java Digital Twin
    if not manager.getProperty("GraphConfiguration"): manager.createProperty(VC_STRING, "GraphConfiguration")
    if not manager.getProperty("OccupancyState"): manager.createProperty(VC_STRING, "OccupancyState")
    
    # Serialize DATA_PATHWAY to JSON string
    # We clean the data to only send relevant fields for graph building
    graph_data = []
    # Use globals().get to be safe against scope weirdness in VC
    data_source = globals().get("DATA_PATHWAY", []) 
    for p in data_source:
        graph_data.append({
            "Name": p.get("Name"),
            "X": p.get("X"),
            "Y": p.get("Y"),
            "Rz": p.get("Rz"),
            "AreaLength": p.get("AreaLength"),
            "AreaWidth": p.get("AreaWidth")
        })
    
    json_str = json.dumps(graph_data)
    manager.getProperty("GraphConfiguration").Value = json_str
    print("Graph Configuration written to property (Length: " + str(len(json_str)) + ")")

    # Read InitialPositions to get robot names dynamically
    prop = manager.getProperty("InitialPositions")
    if prop and prop.Value:
        try:
            positions = eval(prop.Value)
            for p in positions:
                name = p.get('Name', 'Robot')
                if not manager.getProperty(name+"currentState"): manager.createProperty(VC_STRING, name+"currentState")
                if not manager.getProperty(name+"currentWorkId"): manager.createProperty(VC_STRING, name+"currentWorkId")
                if not manager.getProperty(name+"ultrasonicSensorReading"): manager.createProperty(VC_REAL, name+"ultrasonicSensorReading")
                if not manager.getProperty(name+"currentBatteryPercentage"): manager.createProperty(VC_REAL, name+"currentBatteryPercentage")
                if not manager.getProperty(name+"posX"): manager.createProperty(VC_REAL, name+"posX")
                if not manager.getProperty(name+"posY"): manager.createProperty(VC_REAL, name+"posY")
                if not manager.getProperty(name+"yawAngle"): manager.createProperty(VC_REAL, name+"yawAngle")
                if not manager.getProperty(name+"robotName"): manager.createProperty(VC_STRING, name+"robotName")
                if not manager.getProperty(name+"currentPath"): manager.createProperty(VC_STRING, name+"currentPath")
                if not manager.getProperty(name+"targetPath"): manager.createProperty(VC_STRING, name+"targetPath")
        except:
            pass

def OnRun():
    global pathway_graph
    clone_robots()
    delay(4.0)
    pathway_graph = PathwayGraph()
    
    while True:
        robots = []
        if app.Components:
            # We need to find robots that match our configured names
            prop = manager.getProperty("InitialPositions")
            valid_names = []
            if prop and prop.Value:
                try:
                    positions = eval(prop.Value)
                    valid_names = [p.get('Name', 'Robot') for p in positions]
                except: pass

            for c in app.Components:
                # Check if component name is in our valid names list
                if c.Name in valid_names:
                     robots.append(c)
                # Fallback for legacy "Mobile Robot" naming if needed, though we want to use specific names.
                # elif "Mobile Robot" in c.Name: robots.append(c)
                
        for i, r in enumerate(robots):
            state = robot_states.get(r.Name)
            if not state:
                state = {'index': i+1, 'prev': '', 'current_node': None}
                robot_states[r.Name] = state
            
            # --- 1. UPDATE OCCUPANCY AND READ OPC UA ---
            r_name = r.Name
            
            # Read OPC UA Variables (mapped to properties)
            # Read OPC UA Variables (mapped to properties)
            p_state = manager.getProperty(r_name+"currentState")
            p_wid = manager.getProperty(r_name+"currentWorkId")
            p_ultra = manager.getProperty(r_name+"ultrasonicSensorReading")
            p_batt = manager.getProperty(r_name+"currentBatteryPercentage")
            p_px = manager.getProperty(r_name+"posX")
            p_py = manager.getProperty(r_name+"posY")
            p_yaw = manager.getProperty(r_name+"yawAngle")
            p_curr_path = manager.getProperty(r_name+"currentPath")
            p_tgt_path = manager.getProperty(r_name+"targetPath")

            # Use Real Position if available (and not 0,0 which implies unitialized)
            rx, ry = r.WorldPositionMatrix.P.X, r.WorldPositionMatrix.P.Y
            if p_px and p_py:
                 # Optional: Sync visualization to real robot if needed
                 # rx, ry = p_px.Value, p_py.Value
                 pass
            
            if pathway_graph:
                # rx, ry = r.WorldPositionMatrix.P.X, r.WorldPositionMatrix.P.Y # using simulated pos for now
                actual_node = pathway_graph.find_node(rx, ry)
                
                if actual_node and actual_node != state['current_node']:
                    if state['current_node']:
                        state['current_node'].set_occupied(False)
                    actual_node.set_occupied(True)
                    state['current_node'] = actual_node
                    
            # --- 2. MOVEMENT LOGIC (REMOTE EXECUTION) ---
            traj_prop = manager.getProperty(r_name + "currentTrajectory")
            if traj_prop and traj_prop.Value and traj_prop.Value != state.get('prev_traj', ''):
                try:
                    # Expecting JSON list of dicts: [{"X":1.0, "Y":2.0}, ...]
                    points = json.loads(traj_prop.Value)
                    if points and len(points) > 0:
                        veh = r.findBehaviour("Vehicle")
                        if not veh: veh = r.createBehaviour(VC_VEHICLE, "Vehicle")
                        
                        veh.MaxSpeed = 1000.0
                        veh.Acceleration = 500.0
                        veh.Deceleration = 500.0
                        
                        veh.clearMove()
                        for pt in points:
                            px, py = pt.get("X", 0), pt.get("Y", 0)
                            m = vcMatrix.new()
                            m.translate(px, py, 0)
                            veh.addControlPoint(m)
                            
                        state['prev_traj'] = traj_prop.Value
                        print(r.Name + " executing remote trajectory (" + str(len(points)) + " points)")
                    else:
                        print(r.Name + " received empty trajectory")
                except Exception as e:
                     print(r_name + " trajectory error: " + str(e))
        
        # [NEW] Update Global Occupancy State
        occupied_areas = []
        for name, s in robot_states.items():
            if s.get('current_node'):
                occupied_areas.append(s['current_node'].name)
        
        occ_prop = manager.getProperty("OccupancyState")
        if occ_prop:
            occ_prop.Value = json.dumps(list(set(occupied_areas)))

        delay(0.2)

def OnReset():
    global cloned_robots
    for c in cloned_robots: 
        try: app.deleteComponent(c)
        except: pass
    cloned_robots = []
'''

# 2. PATHWAY SCRIPT
PathwayAreaScript = '''from vcScript import *
import vcMatrix
import vcVector

manager = getComponent()
app = getApplication()
clones = []

def OnRun():
    print("--- Pathway Setup Started ---")
    
    aquamarine1 = app.findMaterial("aquamarine")
    if not aquamarine1:
        aquamarine1 = app.createMaterial("aquamarine")
        aquamarine1.DiffuseColor = vcVector.new(0.5, 1.0, 0.83, 1.0)
    
    dark_red_matte1 = app.findMaterial("dark_red_matte")
    if not dark_red_matte1:
        dark_red_matte1 = app.createMaterial("dark_red_matte")
        dark_red_matte1.DiffuseColor = vcVector.new(0.6, 0.0, 0.0, 1.0)

    prop = manager.getProperty("InitialPositions")
    if not prop or not prop.Value: return
    
    base = None
    for c in app.Components:
        if c.Name.startswith("Pathway Area"): base = c; break
    if not base: return
    
    try: positions = eval(prop.Value)
    except: return
    
    active_areas = []

    for i, p in enumerate(positions):
        c = base if i==0 else base.clone()
        
        c.Name = p.get('Name', 'Pathway')
        c.Visible = True
        m = vcMatrix.new()
        m.rotateAbsZ(p.get('Rz',0))
        m.translateAbs(p.get('X',0), p.get('Y',0), 0)
        c.PositionMatrix = m
        
        if 'AreaLength' in p: c.AreaLength = p['AreaLength']
        if 'AreaWidth' in p: c.AreaWidth = p['AreaWidth']
        
        occ = c.getProperty("isOccupied")
        if not occ: occ = c.createProperty(VC_BOOLEAN, "isOccupied")
        occ.Value = p.get('isOccupied', False)
        
        active_areas.append(c)

        if i>0: clones.append(c)
    
    app.render()
    print("--- Pathway Setup Finished ---")

    while True:
        for c in active_areas:
            prop = c.getProperty("isOccupied")
            is_occ = prop.Value if prop else False
            
            target_mat = dark_red_matte1 if is_occ else aquamarine1
            
            if c.Material != target_mat:
                c.Material = target_mat
        
        delay(0.1)

def OnReset():
    for c in clones: 
        try: app.deleteComponent(c)
        except: pass
    del clones[:]
'''

# 3. IDLE SCRIPT
IdleScript = '''from vcScript import *
import vcMatrix
manager = getComponent()
app = getApplication()
clones = []

def OnRun():
    prop = manager.getProperty("InitialPositions")
    if not prop or not prop.Value: return
    base = None
    for c in app.Components:
        if "idle" in c.Name.lower() or "charging" in c.Name.lower(): base = c; break
    if not base: return
    try: positions = eval(prop.Value)
    except: return
    
    for i, p in enumerate(positions):
        c = base if i==0 else base.clone()
        c.Name = p.get('Name', 'Idle')
        m = vcMatrix.new(); m.rotateAbsZ(p.get('Rz',0)); m.translateAbs(p.get('X',0), p.get('Y',0), 0)
        c.PositionMatrix = m
        if i>0: clones.append(c)

def OnReset():
    for c in clones: 
        try: app.deleteComponent(c)
        except: pass
    del clones[:]
'''

# 4. CONVEYOR SCRIPT (Task Dispatcher)
ConveyorScript = '''from vcScript import *
import vcMatrix
manager = getComponent()
app = getApplication()
clones=[]

def OnRun():
    prop = manager.getProperty("InitialPositions")
    if not prop: return
    try: pos=eval(prop.Value)
    except: return
    
    base = manager
    conveyor_list = []
    
    for i,p in enumerate(pos):
        c = base if i==0 else base.clone()
        c.Name = p.get('Name', 'Comp')
        m = vcMatrix.new(); m.rotateAbsZ(p.get('Rz',0)); m.translateAbs(p.get('X',0), p.get('Y',0), 0)
        c.PositionMatrix = m
        conveyor_list.append(c)
        if i>0: clones.append(c)
    
    while True:
        for i, c in enumerate(conveyor_list):
            prod_name = "Conveyor" + str(i+1) + "Produced"
            prod_prop = manager.getProperty(prod_name)
            
            if prod_prop and prod_prop.Value == True:
                robot_target_prop = manager.getProperty("Robot" + str(i+1) + "Target")
                
                if robot_target_prop:
                    loc = c.WorldPositionMatrix.P
                    target_str = str(loc.X) + ";" + str(loc.Y)
                    
                    if robot_target_prop.Value != target_str:
                        robot_target_prop.Value = target_str
                        prod_prop.Value = False
        delay(0.5)

def OnReset():
    for c in clones: 
        try: app.deleteComponent(c)
        except: pass
    del clones[:]
'''

# 5. SIMPLE CLONE SCRIPT (Missing in previous version)
SimpleCloneScript = '''from vcScript import *
import vcMatrix
manager = getComponent()
app = getApplication()
clones=[]
def OnRun():
    prop = manager.getProperty("InitialPositions")
    if not prop: return
    try: pos=eval(prop.Value)
    except: return
    base = manager
    for i,p in enumerate(pos):
        c = base if i==0 else base.clone()
        c.Name = p.get('Name', 'Comp')
        m = vcMatrix.new(); m.rotateAbsZ(p.get('Rz',0)); m.translateAbs(p.get('X',0), p.get('Y',0), 0)
        c.PositionMatrix = m
        if i>0: clones.append(c)
def OnReset():
    for c in clones: 
        try: app.deleteComponent(c)
        except: pass
    del clones[:]
'''

# ==================== BUILDER ====================

def setup_comp(app, name, folder, script, data_list):
    print("Creating " + name + "...")
    vcmx = None
    for ver in VISUAL_COMPONENTS_VERSIONS:
        p = VISUAL_COMPONENTS_PATH + ver + "\\Models\\Components\\Visual Components\\" + folder + "\\" + name + ".vcmx"
        if os.path.exists(p): 
            vcmx = p
            break
    
    comp = None
    if vcmx:
        try: comp = app.load("file:///" + vcmx)
        except: pass
    
    if not comp and name == "Pathway Area":
        print("  Warning: Using Fallback Block for Pathway")
        for ver in VISUAL_COMPONENTS_VERSIONS:
            p = VISUAL_COMPONENTS_PATH + ver + "\\Models\\Components\\Visual Components\\Basic Shapes\\Block Geo.vcmx"
            if os.path.exists(p): 
                comp = app.load("file:///" + p)
                comp.Name = "Pathway Area"
                comp.createProperty(VC_REAL, "AreaLength").Value = 1000
                comp.createProperty(VC_REAL, "AreaWidth").Value = 1000
                h_prop = comp.getProperty("Height")
                if h_prop: h_prop.Value = 10.0
                break
    
    if comp:
        comp.Name = name
        p = comp.createProperty(VC_STRING, "InitialPositions")
        p.Value = str(data_list)
        s = comp.createBehaviour(VC_PYTHONSCRIPT, "ComponentScript")
        s.Script = script
        if name == "Mobile Robot Resource":
            for i, d in enumerate(DATA_ROBOT):
                r_name = d.get('Name')
                comp.createProperty(VC_STRING, r_name + "currentState")
                comp.createProperty(VC_STRING, r_name + "currentWorkId")
                comp.createProperty(VC_REAL, r_name + "ultrasonicSensorReading")
                comp.createProperty(VC_REAL, r_name + "currentBatteryPercentage")
                comp.createProperty(VC_REAL, r_name + "posX")
                comp.createProperty(VC_REAL, r_name + "posY")
                comp.createProperty(VC_REAL, r_name + "yawAngle")
                comp.createProperty(VC_STRING, r_name + "robotName")
                comp.createProperty(VC_STRING, r_name + "currentPath")
                comp.createProperty(VC_STRING, r_name + "targetPath")
                comp.createProperty(VC_STRING, r_name + "currentTrajectory")
        print("  Success: " + name + " ready with " + str(len(data_list)) + " positions.")
        return comp
    print("  Failed to create " + name)
    return None

# ==================== MAIN ====================
app = getApplication()
setup_comp(app, "Mobile Robot Resource", "Mobile Robots", RobotScript, DATA_ROBOT)
setup_comp(app, "Pathway Area", "Navigation", PathwayAreaScript, DATA_PATHWAY)
setup_comp(app, "Idle Location", "Navigation", IdleScript, DATA_CHARGING)
setup_comp(app, "Conveyor", "Conveyors", ConveyorScript, DATA_CONVEYOR)
out = setup_comp(app, "Conveyor", "Conveyors", SimpleCloneScript, DATA_OUTPUT)
if out: out.Name = "Output Location"