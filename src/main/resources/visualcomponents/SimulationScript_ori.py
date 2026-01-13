from vcScript import *
import os

# Configuration
VISUAL_COMPONENTS_VERSIONS = ["4.10", "4.9", "4.8"]
VISUAL_COMPONENTS_PATH = "C:\\Users\\Public\\Documents\\Visual Components\\"

# ==================== QUANTITY CONSTANTS ====================
MobileRobotQuantity = 2
ConveyorQuantity = 2

# ==================== SCRIPT DEFINITIONS ====================

RobotScript = '''from vcScript import *
import vcMatrix
import vcVector
import math

manager = getComponent()
app = getApplication()

pathway_graph = None
robot_states = {}
cloned_robots = []
robot_quantity = 0

COLLISION_THRESHOLD = 2800.0
AVOIDANCE_OFFSET = 1500.0
SAFE_DISTANCE = 1000.0

def get_avoidance_position(robot, other_robot_pos, other_robot_component):
    my_pos = robot.WorldPositionMatrix.P
    dx = other_robot_pos.X - my_pos.X
    dy = other_robot_pos.Y - my_pos.Y
    length = math.sqrt(dx*dx + dy*dy)
    if length < 0.001:
        return None
    dx /= length
    dy /= length
    perp1_x = -dy
    perp1_y = dx
    perp2_x = dy
    perp2_y = -dx
    other_vehicle = other_robot_component.findBehaviour("Vehicle")
    other_direction_x = 0
    other_direction_y = 0
    if other_vehicle:
        try:
            other_mtx = other_robot_component.WorldPositionMatrix
            other_direction_x = other_mtx.N.X
            other_direction_y = other_mtx.N.Y
        except:
            pass
    dot1 = perp1_x * other_direction_x + perp1_y * other_direction_y
    dot2 = perp2_x * other_direction_x + perp2_y * other_direction_y
    if dot1 < dot2:
        chosen_perp_x = perp1_x
        chosen_perp_y = perp1_y
    else:
        chosen_perp_x = perp2_x
        chosen_perp_y = perp2_y
    avoid_x = my_pos.X + chosen_perp_x * AVOIDANCE_OFFSET
    avoid_y = my_pos.Y + chosen_perp_y * AVOIDANCE_OFFSET
    if pathway_graph:
        node = pathway_graph.find_nearest_node(avoid_x, avoid_y, my_pos.Z)
        if node:
            clamped = node.clamp_point_to_area(avoid_x, avoid_y, my_pos.Z)
            return clamped
    return (avoid_x, avoid_y, my_pos.Z)

def is_path_clear(robot, all_robots):
    my_pos = robot.WorldPositionMatrix.P
    for other_robot in all_robots:
        if other_robot != robot:
            other_pos = other_robot.WorldPositionMatrix.P
            dist = math.sqrt((my_pos.X - other_pos.X)**2 + (my_pos.Y - other_pos.Y)**2)
            if dist < SAFE_DISTANCE:
                return False
    return True

def should_robot_avoid(robot, all_robots):
    my_pos = robot.WorldPositionMatrix.P
    my_priority_prop = robot.getProperty("Priority")
    my_priority = my_priority_prop.Value if my_priority_prop else 999
    for other_robot in all_robots:
        if other_robot != robot:
            other_pos = other_robot.WorldPositionMatrix.P
            dist = math.sqrt((my_pos.X - other_pos.X)**2 + (my_pos.Y - other_pos.Y)**2)
            if dist < COLLISION_THRESHOLD:
                other_priority_prop = other_robot.getProperty("Priority")
                other_priority = other_priority_prop.Value if other_priority_prop else 999
                if my_priority > other_priority:
                    return True, other_pos, other_robot
    return False, None, None

class PathwayNode:
    def __init__(self, x, y, z, area_component):
        self.x = x
        self.y = y
        self.z = z
        self.area = area_component
        self.neighbors = []
        self.neighbor_positions = []
        self.parent = None
    def distance_to(self, other):
        dx = self.x - other.x
        dy = self.y - other.y
        dz = self.z - other.z
        return math.sqrt(dx*dx + dy*dy + dz*dz)
    def point_inside_area(self, x, y):
        length_prop = self.area.getProperty("AreaLength")
        width_prop = self.area.getProperty("AreaWidth")
        if not length_prop or not width_prop:
            return False
        half_length = length_prop.Value / 2.0
        half_width = width_prop.Value / 2.0
        area_mtx = self.area.WorldPositionMatrix
        area_pos = area_mtx.P
        dx = x - area_pos.X
        dy = y - area_pos.Y
        local_x = dx * area_mtx.N.X + dy * area_mtx.N.Y
        local_y = dx * area_mtx.O.X + dy * area_mtx.O.Y
        return abs(local_x) <= half_length and abs(local_y) <= half_width
    def clamp_point_to_area(self, x, y, z):
        length_prop = self.area.getProperty("AreaLength")
        width_prop = self.area.getProperty("AreaWidth")
        if not length_prop or not width_prop:
            return (x, y, z)
        half_length = length_prop.Value / 2.0
        half_width = width_prop.Value / 2.0
        area_mtx = self.area.WorldPositionMatrix
        area_pos = area_mtx.P
        dx = x - area_pos.X
        dy = y - area_pos.Y
        local_x = dx * area_mtx.N.X + dy * area_mtx.N.Y
        local_y = dx * area_mtx.O.X + dy * area_mtx.O.Y
        local_x = max(-half_length, min(half_length, local_x))
        local_y = max(-half_width, min(half_width, local_y))
        world_x = area_pos.X + local_x * area_mtx.N.X + local_y * area_mtx.O.X
        world_y = area_pos.Y + local_x * area_mtx.N.Y + local_y * area_mtx.O.Y
        return (world_x, world_y, z)
    def get_intersection_point(self, other):
        mid_x = (self.x + other.x) / 2.0
        mid_y = (self.y + other.y) / 2.0
        mid_z = (self.z + other.z) / 2.0
        point = self.clamp_point_to_area(mid_x, mid_y, mid_z)
        point = other.clamp_point_to_area(point[0], point[1], point[2])
        return point
    def add_neighbor(self, neighbor_node, intersection_point):
        self.neighbors.append(neighbor_node)
        self.neighbor_positions.append(intersection_point)
    def get_connection_point(self, neighbor_node):
        try:
            idx = self.neighbors.index(neighbor_node)
            return self.neighbor_positions[idx]
        except ValueError:
            return None
    def is_connected_to(self, other):
        length_prop = self.area.getProperty("AreaLength")
        width_prop = self.area.getProperty("AreaWidth")
        if not length_prop or not width_prop:
            return False
        half_length = length_prop.Value / 2.0
        half_width = width_prop.Value / 2.0
        area_mtx = self.area.WorldPositionMatrix
        corners = [
            (self.x + half_length * area_mtx.N.X + half_width * area_mtx.O.X,
             self.y + half_length * area_mtx.N.Y + half_width * area_mtx.O.Y),
            (self.x + half_length * area_mtx.N.X - half_width * area_mtx.O.X,
             self.y + half_length * area_mtx.N.Y - half_width * area_mtx.O.Y),
            (self.x - half_length * area_mtx.N.X + half_width * area_mtx.O.X,
             self.y - half_length * area_mtx.N.Y + half_width * area_mtx.O.Y),
            (self.x - half_length * area_mtx.N.X - half_width * area_mtx.O.X,
             self.y - half_length * area_mtx.N.Y - half_width * area_mtx.O.Y)
        ]
        for corner_x, corner_y in corners:
            if other.point_inside_area(corner_x, corner_y):
                return True
        other_length_prop = other.area.getProperty("AreaLength")
        other_width_prop = other.area.getProperty("AreaWidth")
        if not other_length_prop or not other_width_prop:
            return False
        other_half_length = other_length_prop.Value / 2.0
        other_half_width = other_width_prop.Value / 2.0
        other_mtx = other.area.WorldPositionMatrix
        other_corners = [
            (other.x + other_half_length * other_mtx.N.X + other_half_width * other_mtx.O.X,
             other.y + other_half_length * other_mtx.N.Y + other_half_width * other_mtx.O.Y),
            (other.x + other_half_length * other_mtx.N.X - other_half_width * other_mtx.O.X,
             other.y + other_half_length * other_mtx.N.Y - other_half_width * other_mtx.O.Y),
            (other.x - other_half_length * other_mtx.N.X + other_half_width * other_mtx.O.X,
             other.y - other_half_length * other_mtx.N.Y + other_half_width * other_mtx.O.Y),
            (other.x - other_half_length * other_mtx.N.X - other_half_width * other_mtx.O.X,
             other.y - other_half_length * other_mtx.N.Y - other_half_width * other_mtx.O.Y)
        ]
        for corner_x, corner_y in other_corners:
            if self.point_inside_area(corner_x, corner_y):
                return True
        return False

class PathwayGraph:
    def __init__(self):
        self.nodes = []
        self.build_graph()
    def build_graph(self):
        print("🗺️  Building pathway graph...")
        pathway_areas = []
        all_components = app.Components
        if all_components:
            for c in all_components:
                if c and c.Name.startswith("Pathway Area"):
                    pathway_areas.append(c)
        if not pathway_areas:
            print("⚠️  WARNING: No Pathway Area components found!")
            return
        for area in pathway_areas:
            pos = area.WorldPositionMatrix.P
            node = PathwayNode(pos.X, pos.Y, pos.Z, area)
            self.nodes.append(node)
        for i, node1 in enumerate(self.nodes):
            for j, node2 in enumerate(self.nodes):
                if i < j:
                    if node1.is_connected_to(node2):
                        intersection = node1.get_intersection_point(node2)
                        node1.add_neighbor(node2, intersection)
                        node2.add_neighbor(node1, intersection)
                        print("  Connected: {} <-> {}".format(node1.area.Name, node2.area.Name))
        total_connections = sum(len(n.neighbors) for n in self.nodes)
        print("✅ Pathway graph: {} areas, {} connections".format(len(self.nodes), total_connections // 2))
    def find_nearest_node(self, x, y, z):
        if not self.nodes:
            return None
        min_dist = float('inf')
        nearest = None
        for node in self.nodes:
            dist = math.sqrt((node.x - x)**2 + (node.y - y)**2 + (node.z - z)**2)
            if dist < min_dist:
                min_dist = dist
                nearest = node
        return nearest
    def bfs(self, start_x, start_y, start_z, goal_x, goal_y, goal_z):
        if not self.nodes:
            return [(goal_x, goal_y, goal_z)]
        start_node = self.find_nearest_node(start_x, start_y, start_z)
        goal_node = self.find_nearest_node(goal_x, goal_y, goal_z)
        if not start_node or not goal_node:
            return [(goal_x, goal_y, goal_z)]
        if start_node == goal_node:
            return [(goal_x, goal_y, goal_z)]
        queue = [start_node]
        visited = set([start_node])
        for node in self.nodes:
            node.parent = None
        while queue:
            current = queue.pop(0)
            if current == goal_node:
                path = []
                node = current
                while node.parent:
                    connection_point = node.get_connection_point(node.parent)
                    if connection_point:
                        path.append(connection_point)
                    node = node.parent
                path.reverse()
                path.append((goal_x, goal_y, goal_z))
                print("✅ BFS found path with {} waypoints".format(len(path)))
                return path
            for neighbor in current.neighbors:
                if neighbor not in visited:
                    visited.add(neighbor)
                    neighbor.parent = current
                    queue.append(neighbor)
        print("⚠️  No path found")
        return [(goal_x, goal_y, goal_z)]

def clone_robots():
    global cloned_robots
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop or not positions_prop.Value or positions_prop.Value == "[]":
        return
    base_robot = app.findComponent("Mobile Robot Resource")
    if not base_robot:
        print("❌ ERROR: No base robot found")
        return
    try:
        positions = eval(positions_prop.Value)
    except:
        print("❌ ERROR: Invalid InitialPositions format")
        return
    for i, props in enumerate(positions):
        if i == 0:
            mtx = vcMatrix.new()
            mtx.rotateAbsZ(props.get('Rz', 0))
            mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
            base_robot.PositionMatrix = mtx
            base_robot.Name = props.get('Name', 'Mobile Robot Resource')
        else:
            cloned = base_robot.clone()
            if cloned:
                cloned.Name = props.get('Name', 'Robot #' + str(i+1))
                cloned.Visible = True
                mtx = vcMatrix.new()
                mtx.rotateAbsZ(props.get('Rz', 0))
                mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
                cloned.PositionMatrix = mtx
                cloned_robots.append(cloned)
    app.render()
    print("✅ Cloned {} robots".format(len(positions)))

def get_all_robots():
    robots = []
    if app.Components:
        for c in app.Components:
            if c and "Mobile Robot Resource" in c.Name:
                robots.append(c)
    return robots

def initialize_robot(robot, robot_index):
    robot_prefix = "Robot" + str(robot_index)
    
    # Create properties on the manager component for this robot
    location_prop = manager.getProperty(robot_prefix + "Location")
    if not location_prop:
        location_prop = manager.createProperty(VC_STRING, robot_prefix + "Location")
    location_prop.Value = ""
    
    target_prop = manager.getProperty(robot_prefix + "Target")
    if not target_prop:
        target_prop = manager.createProperty(VC_STRING, robot_prefix + "Target")
    target_prop.Value = ""
    
    battery_prop = manager.getProperty(robot_prefix + "BatteryLevel")
    if not battery_prop:
        battery_prop = manager.createProperty(VC_INTEGER, robot_prefix + "BatteryLevel")
    battery_prop.Value = 100
    
    has_product_prop = manager.getProperty(robot_prefix + "HasProduct")
    if not has_product_prop:
        has_product_prop = manager.createProperty(VC_BOOLEAN, robot_prefix + "HasProduct")
    has_product_prop.Value = False
    
    priority_prop = manager.getProperty(robot_prefix + "Priority")
    if not priority_prop:
        priority_prop = manager.createProperty(VC_INTEGER, robot_prefix + "Priority")
    priority_prop.Value = 0
    
    vehicle = robot.findBehaviour("Vehicle")
    if not vehicle:
        try:
            vehicle = robot.createBehaviour(VC_VEHICLE, "Vehicle")
        except:
            print("❌ ERROR: Could not create Vehicle behavior for " + robot.Name)
            return None
    if vehicle:
        vehicle.Acceleration = 2000.0
        vehicle.Deceleration = 2000.0
        vehicle.MaxSpeed = 1200.0
    robot_states[robot.Name] = {'previous_target': '', 'is_avoiding': False, 'saved_target': '', 'index': robot_index}
    print("✅ Robot " + robot.Name + " (index " + str(robot_index) + ") ready")
    return vehicle

def process_robot(robot, all_robots):
    if robot.Name not in robot_states:
        return
    state = robot_states[robot.Name]
    robot_index = state.get('index', 1)
    robot_prefix = "Robot" + str(robot_index)
    
    vehicle = robot.findBehaviour("Vehicle")
    if not vehicle or not pathway_graph:
        return
    try:
        target_prop = manager.getProperty(robot_prefix + "Target")
        if not target_prop:
            return
        target = target_prop.Value if target_prop.Value else ""
        if state['is_avoiding']:
            if is_path_clear(robot, all_robots):
                print("✅ " + robot.Name + " path clear, resuming")
                state['is_avoiding'] = False
                if state['saved_target'] and state['saved_target'] != "" and ";" in state['saved_target']:
                    coords = state['saved_target'].split(";")
                    if len(coords) == 3:
                        try:
                            target_x = float(coords[0])
                            target_y = float(coords[1])
                            target_z = float(coords[2])
                            robot_pos = robot.WorldPositionMatrix.P
                            path = pathway_graph.bfs(robot_pos.X, robot_pos.Y, robot_pos.Z, target_x, target_y, target_z)
                            if path:
                                vehicle.clearMove()
                                for waypoint in path:
                                    vehicle.addControlPoint(vcVector.new(waypoint[0], waypoint[1], waypoint[2]))
                                print("🔄 " + robot.Name + " resuming to saved target")
                        except ValueError:
                            pass
            return
        need_avoid, other_pos, other_robot = should_robot_avoid(robot, all_robots)
        if need_avoid and not state['is_avoiding']:
            state['saved_target'] = target
            avoid_pos = get_avoidance_position(robot, other_pos, other_robot)
            if avoid_pos:
                vehicle.clearMove()
                vehicle.addControlPoint(vcVector.new(avoid_pos[0], avoid_pos[1], avoid_pos[2]))
                state['is_avoiding'] = True
                print("↔️  " + robot.Name + " moving aside for " + other_robot.Name)
            return
        if not state['is_avoiding'] and target and target != "" and target != state['previous_target'] and ";" in target:
            coords = target.split(";")
            if len(coords) == 3:
                try:
                    target_x = float(coords[0])
                    target_y = float(coords[1])
                    target_z = float(coords[2])
                    robot_pos = robot.WorldPositionMatrix.P
                    path = pathway_graph.bfs(robot_pos.X, robot_pos.Y, robot_pos.Z, target_x, target_y, target_z)
                    if path:
                        vehicle.clearMove()
                        for waypoint in path:
                            vehicle.addControlPoint(vcVector.new(waypoint[0], waypoint[1], waypoint[2]))
                        state['previous_target'] = target
                        state['saved_target'] = target
                        print("🚀 " + robot.Name + " moving")
                except ValueError:
                    pass
        if not state['is_avoiding'] and target and target != "" and ";" in target:
            coords = target.split(";")
            if len(coords) == 3:
                try:
                    target_x = float(coords[0])
                    target_y = float(coords[1])
                    robot_pos = robot.WorldPositionMatrix.P
                    distance = math.sqrt((robot_pos.X - target_x)**2 + (robot_pos.Y - target_y)**2)
                    location_prop = manager.getProperty(robot_prefix + "Location")
                    if distance < 50 and location_prop and location_prop.Value != target:
                        location_prop.Value = target
                        print("✅ " + robot.Name + " arrived")
                except ValueError:
                    pass
    except Exception as e:
        print("❌ ERROR in " + robot.Name + ": " + str(e))

def OnStart():
    global pathway_graph, robot_states, robot_quantity
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop:
        positions_prop = manager.createProperty(VC_STRING, "InitialPositions")
        positions_prop.Value = ""
    
    # Get robot quantity from manager property
    quantity_prop = manager.getProperty("RobotQuantity")
    if quantity_prop:
        robot_quantity = quantity_prop.Value
    else:
        robot_quantity = 1
    
    # Create properties on manager for all robots based on quantity
    for i in range(1, robot_quantity + 1):
        robot_prefix = "Robot" + str(i)
        
        location_prop = manager.getProperty(robot_prefix + "Location")
        if not location_prop:
            location_prop = manager.createProperty(VC_STRING, robot_prefix + "Location")
        location_prop.Value = ""
        
        target_prop = manager.getProperty(robot_prefix + "Target")
        if not target_prop:
            target_prop = manager.createProperty(VC_STRING, robot_prefix + "Target")
        target_prop.Value = ""
        
        battery_prop = manager.getProperty(robot_prefix + "BatteryLevel")
        if not battery_prop:
            battery_prop = manager.createProperty(VC_INTEGER, robot_prefix + "BatteryLevel")
        battery_prop.Value = 100
        
        has_product_prop = manager.getProperty(robot_prefix + "HasProduct")
        if not has_product_prop:
            has_product_prop = manager.createProperty(VC_BOOLEAN, robot_prefix + "HasProduct")
        has_product_prop.Value = False
        
        priority_prop = manager.getProperty(robot_prefix + "Priority")
        if not priority_prop:
            priority_prop = manager.createProperty(VC_INTEGER, robot_prefix + "Priority")
        priority_prop.Value = 0
    
    print("✅ Created properties for {} robots on manager".format(robot_quantity))
    robot_states = {}

def OnRun():
    global pathway_graph
    clone_robots()
    print("⏳ Waiting for pathway areas to be created...")
    delay(2)
    pathway_graph = PathwayGraph()
    robots = get_all_robots()
    for i, robot in enumerate(robots):
        initialize_robot(robot, i + 1)
    print("✅ Robot manager ready, managing {} robots".format(len(robots)))
    while True:
        if not pathway_graph:
            delay(1.0)
            continue
        robots = get_all_robots()
        for robot in robots:
            process_robot(robot, robots)
        delay(0.5)

def OnReset():
    global robot_states, pathway_graph, cloned_robots, robot_quantity
    robot_states = {}
    for cloned in cloned_robots:
        try:
            app.deleteComponent(cloned)
        except:
            pass
    cloned_robots = []
    
    # Reset properties on manager for all robots
    for i in range(1, robot_quantity + 1):
        robot_prefix = "Robot" + str(i)
        
        location_prop = manager.getProperty(robot_prefix + "Location")
        if location_prop:
            location_prop.Value = ""
        target_prop = manager.getProperty(robot_prefix + "Target")
        if target_prop:
            target_prop.Value = ""
        battery_prop = manager.getProperty(robot_prefix + "BatteryLevel")
        if battery_prop:
            battery_prop.Value = 100
        has_product_prop = manager.getProperty(robot_prefix + "HasProduct")
        if has_product_prop:
            has_product_prop.Value = False
        priority_prop = manager.getProperty(robot_prefix + "Priority")
        if priority_prop:
            priority_prop.Value = 0
    
    print("🔄 All robot properties reset")
    pathway_graph = PathwayGraph()
'''

IdleLocationScript = '''from vcScript import *
import vcMatrix

manager = getComponent()
app = getApplication()

cloned_idles = []

def clone_idle_locations():
    global cloned_idles
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop or not positions_prop.Value or positions_prop.Value == "[]":
        return
    base_idle = None
    for c in app.Components:
        if c and "idle location" in c.Name.lower():
            base_idle = c
            break
    if not base_idle:
        print("❌ ERROR: No base idle location found")
        return
    try:
        positions = eval(positions_prop.Value)
    except:
        print("❌ ERROR: Invalid InitialPositions format")
        return
    for i, props in enumerate(positions):
        if i == 0:
            mtx = vcMatrix.new()
            mtx.rotateAbsZ(props.get('Rz', 0))
            mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
            base_idle.PositionMatrix = mtx
            base_idle.Name = props.get('Name', base_idle.Name)
        else:
            cloned = base_idle.clone()
            if cloned:
                cloned.Name = props.get('Name', 'Idle Location #' + str(i+1))
                cloned.Visible = True
                mtx = vcMatrix.new()
                mtx.rotateAbsZ(props.get('Rz', 0))
                mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
                cloned.PositionMatrix = mtx
                cloned_idles.append(cloned)
    app.render()
    print("✅ Cloned {} idle locations".format(len(positions)))

def OnStart():
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop:
        positions_prop = manager.createProperty(VC_STRING, "InitialPositions")
        positions_prop.Value = ""

def OnRun():
    clone_idle_locations()

def OnReset():
    global cloned_idles
    for cloned in cloned_idles:
        try:
            app.deleteComponent(cloned)
        except:
            pass
    cloned_idles = []
    print("🔄 Idle locations reset")
'''

PathwayAreaScript = '''from vcScript import *
import vcMatrix

manager = getComponent()
app = getApplication()

cloned_pathways = []

def clone_pathway_areas():
    global cloned_pathways
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop or not positions_prop.Value or positions_prop.Value == "[]":
        return
    base_pathway = None
    for c in app.Components:
        if c and "pathway area" in c.Name.lower():
            base_pathway = c
            break
    if not base_pathway:
        print("❌ ERROR: No base pathway area found")
        return
    try:
        positions = eval(positions_prop.Value)
    except:
        print("❌ ERROR: Invalid InitialPositions format")
        return
    for i, props in enumerate(positions):
        if i == 0:
            mtx = vcMatrix.new()
            mtx.rotateAbsZ(props.get('Rz', 0))
            mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
            base_pathway.PositionMatrix = mtx
            base_pathway.Name = props.get('Name', base_pathway.Name)
            if 'AreaLength' in props:
                base_pathway.AreaLength = props['AreaLength']
            if 'AreaWidth' in props:
                base_pathway.AreaWidth = props['AreaWidth']
        else:
            cloned = base_pathway.clone()
            if cloned:
                cloned.Name = props.get('Name', 'Pathway Area #' + str(i+1))
                cloned.Visible = True
                mtx = vcMatrix.new()
                mtx.rotateAbsZ(props.get('Rz', 0))
                mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
                cloned.PositionMatrix = mtx
                if 'AreaLength' in props:
                    cloned.AreaLength = props['AreaLength']
                if 'AreaWidth' in props:
                    cloned.AreaWidth = props['AreaWidth']
                cloned_pathways.append(cloned)
    app.render()
    print("✅ Cloned {} pathway areas".format(len(positions)))

def OnStart():
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop:
        positions_prop = manager.createProperty(VC_STRING, "InitialPositions")
        positions_prop.Value = ""

def OnRun():
    clone_pathway_areas()

def OnReset():
    global cloned_pathways
    for cloned in cloned_pathways:
        try:
            app.deleteComponent(cloned)
        except:
            pass
    cloned_pathways = []
    print("🔄 Pathway areas reset")
'''

ConveyorScript = '''from vcScript import *
import vcMatrix

manager = getComponent()
app = getApplication()

conveyor_states = {}
cloned_conveyors = []
conveyor_quantity = 0

def clone_conveyors():
    global cloned_conveyors
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop or not positions_prop.Value or positions_prop.Value == "[]":
        return
    base_conveyor = None
    for c in app.Components:
        if c and "conveyor" in c.Name.lower():
            base_conveyor = c
            break
    if not base_conveyor:
        print("❌ ERROR: No base conveyor found")
        return
    try:
        positions = eval(positions_prop.Value)
    except:
        print("❌ ERROR: Invalid InitialPositions format")
        return
    for i, props in enumerate(positions):
        if i == 0:
            mtx = vcMatrix.new()
            mtx.rotateAbsZ(props.get('Rz', 0))
            mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
            base_conveyor.PositionMatrix = mtx
            base_conveyor.Name = props.get('Name', base_conveyor.Name)
        else:
            cloned = base_conveyor.clone()
            if cloned:
                cloned.Name = props.get('Name', 'Conveyor #' + str(i+1))
                cloned.Visible = True
                mtx = vcMatrix.new()
                mtx.rotateAbsZ(props.get('Rz', 0))
                mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
                cloned.PositionMatrix = mtx
                cloned_conveyors.append(cloned)
    app.render()
    print("✅ Cloned {} conveyors".format(len(positions)))

def get_all_conveyors():
    conveyors = []
    if app.Components:
        for c in app.Components:
            if c and "conveyor" in c.Name.lower():
                conveyors.append(c)
    return conveyors

def initialize_conveyor(conveyor, conveyor_index):
    conveyor_prefix = "Conveyor" + str(conveyor_index)
    conveyor_states[conveyor.Name] = {'index': conveyor_index}
    
    path = conveyor.findBehaviour("Path")
    if not path:
        print("Path behavior not found for " + conveyor.Name)
        return None
    if conveyor.Name not in conveyor_states or 'original_path' not in conveyor_states[conveyor.Name]:
        original_path_frames = list(path.Path)
        reversed_path = []
        for i in range(len(original_path_frames)-1, -1, -1):
            reversed_path.append(original_path_frames[i])
        path.Path = reversed_path
        path.PathAxis = VC_PATH_AXIS_AUTOMATIC
        path.Enabled = True
        path.RetainOffset = False
        path.Speed = conveyor.getProperty('ConveyorSpeed').Value if conveyor.getProperty('ConveyorSpeed').Value > 0 else 500
        path.update()
        conveyor_states[conveyor.Name].update({'original_path': original_path_frames, 'block_geo': None, 'waiting_for_production': True})
    print("✅ Conveyor " + conveyor.Name + " (index " + str(conveyor_index) + ") ready")
    return path

def process_conveyor(conveyor):
    if conveyor.Name not in conveyor_states:
        return
    state = conveyor_states[conveyor.Name]
    conveyor_index = state.get('index', 1)
    conveyor_prefix = "Conveyor" + str(conveyor_index)
    
    path = conveyor.findBehaviour("Path")
    if not path:
        return
    
    produced_prop = manager.getProperty(conveyor_prefix + "Produced")
    if state.get('waiting_for_production', False):
        if produced_prop and produced_prop.Value:
            state['waiting_for_production'] = False
            original_block = app.findComponent("Block Geo")
            if original_block and not state.get('block_geo'):
                block_geo = original_block.clone(1)
                block_geo.Name = "Block Geo Clone " + conveyor.Name
                conveyor_height = conveyor.getProperty("ConveyorHeight").Value
                movement_origin = vcMatrix.new()
                movement_origin.P.Z = conveyor_height + 50
                block_geo.MovementOrigin = movement_origin
                start_distance = 200
                start_matrix = path.getPathPosition(start_distance)
                block_geo.PositionMatrix = conveyor.WorldPositionMatrix * start_matrix
                path.grab(block_geo)
                state['block_geo'] = block_geo
                print("Block cloned and started moving on conveyor " + conveyor.Name)
    if state.get('block_geo'):
        block_geo = state['block_geo']
        if block_geo and block_geo.Container == path:
            conveyor_length = conveyor.getProperty("ConveyorLength").Value
            stop_distance = conveyor_length - 200
            distance = block_geo.getPathDistance()
            if distance >= stop_distance:
                block_geo.stopMovement(True)
        else:
            if block_geo and block_geo.Container != path:
                state['block_geo'] = None

def OnStart():
    global conveyor_states, conveyor_quantity
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop:
        positions_prop = manager.createProperty(VC_STRING, "InitialPositions")
        positions_prop.Value = ""
    
    # Get conveyor quantity from manager property
    quantity_prop = manager.getProperty("ConveyorQuantity")
    if quantity_prop:
        conveyor_quantity = quantity_prop.Value
    else:
        conveyor_quantity = 1
    
    # Create properties on manager for all conveyors based on quantity
    for i in range(1, conveyor_quantity + 1):
        conveyor_prefix = "Conveyor" + str(i)
        
        produced_prop = manager.getProperty(conveyor_prefix + "Produced")
        if not produced_prop:
            produced_prop = manager.createProperty(VC_BOOLEAN, conveyor_prefix + "Produced")
        produced_prop.Value = False
    
    print("✅ Created properties for {} conveyors on manager".format(conveyor_quantity))
    conveyor_states = {}

def OnRun():
    clone_conveyors()
    conveyors = get_all_conveyors()
    for i, conveyor in enumerate(conveyors):
        initialize_conveyor(conveyor, i + 1)
    print("✅ Conveyor manager ready, managing {} conveyors".format(len(conveyors)))
    while True:
        conveyors = get_all_conveyors()
        for conveyor in conveyors:
            process_conveyor(conveyor)
        delay(0.1)

def OnReset():
    global conveyor_states, cloned_conveyors, conveyor_quantity
    for cloned in cloned_conveyors:
        try:
            app.deleteComponent(cloned)
        except:
            pass
    cloned_conveyors = []
    
    # Reset properties on manager for all conveyors
    for i in range(1, conveyor_quantity + 1):
        conveyor_prefix = "Conveyor" + str(i)
        produced_prop = manager.getProperty(conveyor_prefix + "Produced")
        if produced_prop:
            produced_prop.Value = False
    
    # Clean up block geo clones
    for conveyor_name, state in conveyor_states.items():
        block_geo = state.get('block_geo')
        if block_geo:
            try:
                cloned_comp = app.findComponent(block_geo.Name)
                if cloned_comp:
                    app.deleteComponent(cloned_comp)
                    print("Cloned block removed on reset for " + conveyor_name)
            except:
                pass
    
    print("🔄 All conveyor properties reset")
    conveyor_states = {}
'''

OutputLocationScript = '''from vcScript import *
import vcMatrix

manager = getComponent()
app = getApplication()

cloned_outputs = []

def clone_output_locations():
    global cloned_outputs
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop or not positions_prop.Value or positions_prop.Value == "[]":
        return
    base_output = None
    for c in app.Components:
        if c and "output location" in c.Name.lower():
            base_output = c
            break
    if not base_output:
        print("❌ ERROR: No base output location found")
        return
    try:
        positions = eval(positions_prop.Value)
    except:
        print("❌ ERROR: Invalid InitialPositions format")
        return
    for i, props in enumerate(positions):
        if i == 0:
            mtx = vcMatrix.new()
            mtx.rotateAbsZ(props.get('Rz', 0))
            mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
            base_output.PositionMatrix = mtx
            base_output.Name = props.get('Name', base_output.Name)
        else:
            cloned = base_output.clone()
            if cloned:
                cloned.Name = props.get('Name', 'Output Location #' + str(i+1))
                cloned.Visible = True
                mtx = vcMatrix.new()
                mtx.rotateAbsZ(props.get('Rz', 0))
                mtx.translateAbs(props.get('X', 0), props.get('Y', 0), 0)
                cloned.PositionMatrix = mtx
                cloned_outputs.append(cloned)
    app.render()
    print("✅ Cloned {} output locations".format(len(positions)))

def OnStart():
    positions_prop = manager.getProperty("InitialPositions")
    if not positions_prop:
        positions_prop = manager.createProperty(VC_STRING, "InitialPositions")
        positions_prop.Value = ""

def OnRun():
    clone_output_locations()

def OnReset():
    global cloned_outputs
    for cloned in cloned_outputs:
        try:
            app.deleteComponent(cloned)
        except:
            pass
    cloned_outputs = []
    print("🔄 Output locations reset")
'''

# ==================== COMPONENT CREATION ====================

def create_robot_properties(component, quantity):
    """Create properties for all robots on the component"""
    for i in range(1, quantity + 1):
        prefix = "Robot" + str(i)
        
        loc_prop = component.createProperty(VC_STRING, prefix + "Location")
        if loc_prop:
            loc_prop.Value = ""
        
        target_prop = component.createProperty(VC_STRING, prefix + "Target")
        if target_prop:
            target_prop.Value = ""
        
        battery_prop = component.createProperty(VC_INTEGER, prefix + "BatteryLevel")
        if battery_prop:
            battery_prop.Value = 100
        
        has_product_prop = component.createProperty(VC_BOOLEAN, prefix + "HasProduct")
        if has_product_prop:
            has_product_prop.Value = False
        
        priority_prop = component.createProperty(VC_INTEGER, prefix + "Priority")
        if priority_prop:
            priority_prop.Value = 0
    
    print("  ✅ Created properties for {} robots".format(quantity))

def create_conveyor_properties(component, quantity):
    """Create properties for all conveyors on the component"""
    for i in range(1, quantity + 1):
        prefix = "Conveyor" + str(i)
        
        produced_prop = component.createProperty(VC_BOOLEAN, prefix + "Produced")
        if produced_prop:
            produced_prop.Value = False
    
    print("  ✅ Created properties for {} conveyors".format(quantity))

def create_component(app, name, folder, script_content=None, quantity_prop_name=None, quantity_value=None):
    """Create a component and attach a script if provided"""
    print("Creating " + name + "...")
    
    vcmx_path = None
    for version in VISUAL_COMPONENTS_VERSIONS:
        base_path = VISUAL_COMPONENTS_PATH + version + "\\Models\\Components\\Visual Components\\"
        path = base_path + folder + "\\" + name + ".vcmx"
        if os.path.exists(path):
            vcmx_path = path
            break
    
    if not vcmx_path:
        print("  ❌ " + name + " .vcmx file not found")
        return None
    
    try:
        component = app.load("file:///" + vcmx_path)
        
        if component:
            component.Name = name
            
            # Always create InitialPositions property for components with scripts
            if script_content:
                init_pos_prop = component.createProperty(VC_STRING, "InitialPositions")
                if init_pos_prop:
                    init_pos_prop.Value = ""
                    print("  ✅ InitialPositions property added")
            
            # Add quantity property if specified
            if quantity_prop_name and quantity_value is not None:
                quantity_prop = component.createProperty(VC_INTEGER, quantity_prop_name)
                if quantity_prop:
                    quantity_prop.Value = quantity_value
                    print("  ✅ {} = {} added to {}".format(quantity_prop_name, quantity_value, name))
                
                # Create individual properties immediately based on component type
                if quantity_prop_name == "RobotQuantity":
                    create_robot_properties(component, quantity_value)
                elif quantity_prop_name == "ConveyorQuantity":
                    create_conveyor_properties(component, quantity_value)
            
            if script_content:
                script_behavior = component.createBehaviour(VC_PYTHONSCRIPT, "ComponentScript")
                script_prop = script_behavior.getProperty("Script")
                if script_prop:
                    script_prop.Value = script_content
                    print("  ✅ Script attached to " + name)
            
            print("  ✅ " + name + " created")
            return component
    except Exception as e:
        print("  ❌ Error: " + str(e))
        return None

# ==================== MAIN EXECUTION ====================

app = getApplication()


# Create Mobile Robot Resource with script and quantity property
robot = create_component(app, "Mobile Robot Resource", "Mobile Robots", RobotScript, "RobotQuantity", MobileRobotQuantity)

# Create Conveyor with script and quantity property
conveyor = create_component(app, "Conveyor", "Conveyors", ConveyorScript, "ConveyorQuantity", ConveyorQuantity)

# Create Output Location with script (no quantity properties needed)
output = create_component(app, "Conveyor", "Conveyors", OutputLocationScript)
if output:
    output.Name = "Output Location"

# Create Idle Location with script (no quantity properties needed)
idle = create_component(app, "Idle Location", "Navigation", IdleLocationScript)

# Create Pathway Area with script (no quantity properties needed)
pathway = create_component(app, "Pathway Area", "Navigation", PathwayAreaScript)

# Create Block Geo (no script needed)
block = create_component(app, "Block Geo", "Basic Shapes")
if block:
    block.Name = "Block Geo"


