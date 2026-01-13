
data = [
  {"Name": "Pathway Area #2", "X": -3575.119, "Y": -11821.99, "Rz": 0, "AreaLength": 7817.82, "AreaWidth": 5022.88, "isOccupied": False},
  {"Name": "Pathway Area #4", "X": 4140.941, "Y": -16221.492, "Rz": 90, "AreaLength": 4007.51, "AreaWidth": 4298.32, "isOccupied": False},
  {"Name": "Pathway Area", "X": 4142.011, "Y": -11901.853, "Rz": 0, "AreaLength": 8000, "AreaWidth": 5003.29, "isOccupied": False},
  {"Name": "Pathway Area #3", "X": -9359.314, "Y": -6882.734, "Rz": 90, "AreaLength": 15317.17, "AreaWidth": 4895.60, "isOccupied": False},
  {"Name": "Pathway Area #5", "X": -2288.116, "Y": 935.992, "Rz": 0, "AreaLength": 10373.88, "AreaWidth": 5474.01, "isOccupied": False},
  {"Name": "Pathway Area #6", "X": 12026.251, "Y": -11843.178, "Rz": 0, "AreaLength": 8351.18, "AreaWidth": 5197.12, "isOccupied": False},
  {"Name": "Pathway Area #7", "X": -3638.816, "Y": 13340.438, "Rz": 0, "AreaLength": 8000, "AreaWidth": 4298.32, "isOccupied": False},
  {"Name": "Pathway Area #8", "X": 9159.431, "Y": 1303.905, "Rz": 0, "AreaLength": 13199.25, "AreaWidth": 5474.01, "isOccupied": False},
  {"Name": "Pathway Area #9", "X": -16903.329, "Y": -9517.167, "Rz": 0, "AreaLength": 11452.31, "AreaWidth": 4298.32, "isOccupied": False},
  {"Name": "Pathway Area #10", "X": -9628.315, "Y": 8067.253, "Rz": 90, "AreaLength": 15307.06, "AreaWidth": 5082.56, "isOccupied": False},
  {"Name": "Pathway Area #11", "X": -17472.264, "Y": 10847.35, "Rz": 0, "AreaLength": 11753.87, "AreaWidth": 5255.63, "isOccupied": False},
  {"Name": "Pathway Area #12", "X": 18105.147, "Y": 10840.556, "Rz": 90, "AreaLength": 9627.03, "AreaWidth": 4989.85, "isOccupied": False},
  {"Name": "Pathway Area #13", "X": 28357.021, "Y": 392.098, "Rz": 0, "AreaLength": 16132.87, "AreaWidth": 5626.47, "isOccupied": False},
  {"Name": "Pathway Area #14", "X": 12069.708, "Y": 13326.669, "Rz": 0, "AreaLength": 8000, "AreaWidth": 4543.99, "isOccupied": False},
  {"Name": "Pathway Area #15", "X": 18213.197, "Y": 1254.04, "Rz": 90, "AreaLength": 10373.88, "AreaWidth": 5474.01, "isOccupied": False},
  {"Name": "Pathway Area #16", "X": 18157.116, "Y": -9138.552, "Rz": 90, "AreaLength": 10880.82, "AreaWidth": 5216.52, "isOccupied": False},
  {"Name": "Pathway Area #17", "X": 4222.269, "Y": 13445.531, "Rz": 0, "AreaLength": 8000, "AreaWidth": 4298.32, "isOccupied": False},
  {"Name": "Pathway Area #18", "X": 4194.029, "Y": 17401.215, "Rz": 90, "AreaLength": 4007.51, "AreaWidth": 4298.32, "isOccupied": False}
]

def get_bounds(item):
    cx, cy = item['X'], item['Y']
    l_half = item['AreaLength'] / 2.0
    w_half = item['AreaWidth'] / 2.0
    rz = item['Rz']
    
    if abs(rz) < 1.0 or abs(rz - 180.0) < 1.0:
        return (cx - l_half, cx + l_half, cy - w_half, cy + w_half)
    else: # 90 or 270
        return (cx - w_half, cx + w_half, cy - l_half, cy + l_half)

print('Analysis:')
for i, d1 in enumerate(data):
    b1 = get_bounds(d1)
    for j, d2 in enumerate(data):
        if i >= j: continue
        b2 = get_bounds(d2)
        
        # Check Overlap
        overlap_x = max(0, min(b1[1], b2[1]) - max(b1[0], b2[0]))
        overlap_y = max(0, min(b1[3], b2[3]) - max(b1[2], b2[2]))
        
        if overlap_x > 0 and overlap_y > 0:
            print(f'{d1["Name"]} - {d2["Name"]}: Overlap X={overlap_x:.2f}, Y={overlap_y:.2f}')
